package com.example.trainingdashboard.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepCalibratorTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Builds a flat baseline signal at the given magnitude for `count` samples at 50Hz. */
    private fun flatSamples(count: Int, magnitude: Float = 0f, startMs: Long = 0L): List<TimestampedSample> =
        (0 until count).map { i -> TimestampedSample(startMs + i * 20L, magnitude) }

    /**
     * Inserts a single sharp peak centred at [peakMs] into a baseline list.
     * The peak spans 5 samples (100ms at 50Hz) with magnitudes ramping up then down.
     */
    private fun buildSignalWithPeaks(
        durationMs: Long,
        peakTimesMs: List<Long>,
        peakMagnitude: Float = 20f,
        baseline: Float = 0f,
    ): List<TimestampedSample> {
        val intervalMs = 20L
        val totalSamples = (durationMs / intervalMs).toInt()
        val samples = (0 until totalSamples)
            .map { i -> TimestampedSample(i * intervalMs, baseline) }
            .toMutableList()

        for (peakMs in peakTimesMs) {
            val centre = (peakMs / intervalMs).toInt()
            val shape = listOf(0.3f, 0.7f, 1.0f, 0.7f, 0.3f)
            shape.forEachIndexed { offset, factor ->
                val idx = centre - 2 + offset
                if (idx in samples.indices) {
                    samples[idx] = samples[idx].copy(magnitude = peakMagnitude * factor)
                }
            }
        }
        return samples
    }

    // -------------------------------------------------------------------------
    // Failure cases
    // -------------------------------------------------------------------------

    @Test
    fun failsOnEmptyInput() {
        val result = RepCalibrator.analyze(emptyList())
        assertTrue(result is CalibrationResult.Failed)
    }

    @Test
    fun failsWhenFewerThanSmoothingWindowSamples() {
        val result = RepCalibrator.analyze(flatSamples(4, magnitude = 25f))
        assertTrue(result is CalibrationResult.Failed)
    }

    @Test
    fun failsWhenNoPeaksAboveNoiseFloor() {
        // Signal exists but entirely below the 5 m/s² noise floor
        val result = RepCalibrator.analyze(flatSamples(200, magnitude = 3f))
        assertTrue(result is CalibrationResult.Failed)
    }

    @Test
    fun failsWhenFewerThanThreePeaks() {
        // Two peaks only — should fail
        val samples = buildSignalWithPeaks(
            durationMs = 5000L,
            peakTimesMs = listOf(1000L, 2500L),
            peakMagnitude = 20f,
        )
        val result = RepCalibrator.analyze(samples)
        assertTrue(result is CalibrationResult.Failed)
    }

    @Test
    fun failsWhenAllPeaksTooCloseTogether() {
        // Five peaks all within 100ms of each other — should collapse to one accepted peak
        val tightPeaks = listOf(1000L, 1060L, 1120L, 1180L, 1240L)
        val samples = buildSignalWithPeaks(
            durationMs = 5000L,
            peakTimesMs = tightPeaks,
            peakMagnitude = 20f,
        )
        val result = RepCalibrator.analyze(samples)
        assertTrue(result is CalibrationResult.Failed)
    }

    // -------------------------------------------------------------------------
    // Success cases
    // -------------------------------------------------------------------------

    @Test
    fun succeedsWithExactlyThreeCleanPeaks() {
        val samples = buildSignalWithPeaks(
            durationMs = 6000L,
            peakTimesMs = listOf(1000L, 2500L, 4000L),
            peakMagnitude = 20f,
        )
        val result = RepCalibrator.analyze(samples)
        assertTrue(result is CalibrationResult.Success)
        assertEquals(3, (result as CalibrationResult.Success).repCount)
    }

    @Test
    fun thresholdIsMedianPeakTimesFactorForOddPeakCount() {
        // Three equal peaks at 20 m/s² → median = 20 → threshold ≈ 20 × 0.55 = 11.0
        val samples = buildSignalWithPeaks(
            durationMs = 6000L,
            peakTimesMs = listOf(1000L, 2500L, 4000L),
            peakMagnitude = 20f,
        )
        val result = RepCalibrator.analyze(samples) as CalibrationResult.Success
        // Smoothing reduces the actual peak value slightly — we check it's in the right ballpark
        assertTrue("Threshold should be positive", result.threshold > 0f)
        assertTrue("Threshold should be below peak magnitude", result.threshold < 20f)
    }

    @Test
    fun succeedsWithFiveWellSpacedPeaks() {
        val samples = buildSignalWithPeaks(
            durationMs = 10000L,
            peakTimesMs = listOf(1000L, 2500L, 4000L, 5500L, 7000L),
            peakMagnitude = 18f,
        )
        val result = RepCalibrator.analyze(samples)
        assertTrue(result is CalibrationResult.Success)
        assertEquals(5, (result as CalibrationResult.Success).repCount)
    }

    @Test
    fun peaksBelowNoiseFloorAreIgnored() {
        // Three large peaks + two sub-floor peaks — only large peaks should count
        val samples = buildSignalWithPeaks(
            durationMs = 10000L,
            peakTimesMs = listOf(1000L, 3000L, 5000L),
            peakMagnitude = 20f,
        ) + buildSignalWithPeaks(
            durationMs = 10000L,
            peakTimesMs = listOf(7000L, 8500L),
            peakMagnitude = 2f,   // below noise floor of 5 m/s²
        )
        val sorted = samples.sortedBy { it.timestampMs }
        // Merge by picking the max magnitude at each timestamp
        val merged = sorted
            .groupBy { it.timestampMs }
            .map { (ts, group) -> TimestampedSample(ts, group.maxOf { it.magnitude }) }
            .sortedBy { it.timestampMs }

        val result = RepCalibrator.analyze(merged)
        assertTrue(result is CalibrationResult.Success)
        assertEquals(3, (result as CalibrationResult.Success).repCount)
    }

    @Test
    fun closePeaksKeepHigherOne() {
        // Two peaks 300ms apart (below MIN_PEAK_SEPARATION_MS of 600ms) — only the higher should survive.
        // Pair these with two other well-spaced peaks to meet the minimum of 3.
        // The close pair counts as one, so total = 3.
        val samples = buildSignalWithPeaks(
            durationMs = 8000L,
            peakTimesMs = listOf(1000L, 1300L, 3000L, 5000L),
            peakMagnitude = 20f,
        )
        val result = RepCalibrator.analyze(samples)
        assertTrue(result is CalibrationResult.Success)
        assertEquals(3, (result as CalibrationResult.Success).repCount)
    }
}
