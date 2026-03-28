package com.example.trainingdashboard.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepPeakDetectorTest {

    private val threshold = 6f   // well above NOISE_FLOOR (5f), well below peak magnitudes

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Feeds [samples] into a fresh [RepPeakDetector] and returns the count of
     * detected reps.
     */
    private fun countReps(samples: List<TimestampedSample>, th: Float = threshold): Int {
        val detector = RepPeakDetector(threshold = th)
        return samples.count { detector.onSample(it) }
    }

    /**
     * Builds a synthetic signal at 50Hz with sharp peaks at the requested times.
     * Each peak spans 5 samples and has the classic rise/plateau/fall shape.
     */
    private fun buildSignal(
        durationMs: Long,
        peakTimesMs: List<Long>,
        peakMagnitude: Float = 15f,
        baseline: Float = 0f,
    ): List<TimestampedSample> {
        val intervalMs = 20L
        val samples = (0 until (durationMs / intervalMs).toInt())
            .map { i -> TimestampedSample(i * intervalMs, baseline) }
            .toMutableList()

        val shape = listOf(0.3f, 0.7f, 1.0f, 0.7f, 0.3f)
        for (peakMs in peakTimesMs) {
            val centre = (peakMs / intervalMs).toInt()
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
    // Basic detection
    // -------------------------------------------------------------------------

    @Test
    fun detectsNothingUntilBufferFills() {
        // Feed fewer samples than the internal buffer size — must never fire
        val detector = RepPeakDetector(threshold = threshold)
        val results = (0 until 14).map { i ->
            detector.onSample(TimestampedSample(i * 20L, 15f))
        }
        assertFalse("Should not detect before buffer is full", results.any { it })
    }

    @Test
    fun detectsFlatSignalAsNoReps() {
        // Sustained high signal with no local maximum — not a rep
        val samples = (0 until 200).map { i -> TimestampedSample(i * 20L, 15f) }
        assertEquals(0, countReps(samples))
    }

    @Test
    fun detectsThreeCleanPeaks() {
        val samples = buildSignal(
            durationMs = 6000L,
            peakTimesMs = listOf(1000L, 2500L, 4000L),
        )
        assertEquals(3, countReps(samples))
    }

    @Test
    fun detectsFiveWellSpacedPeaks() {
        val samples = buildSignal(
            durationMs = 10000L,
            peakTimesMs = listOf(1000L, 2500L, 4000L, 5500L, 7000L),
        )
        assertEquals(5, countReps(samples))
    }

    // -------------------------------------------------------------------------
    // Noise floor and threshold gating
    // -------------------------------------------------------------------------

    @Test
    fun ignoresPeaksBelowNoiseFloor() {
        // Peaks at 4 m/s² are below the 5 m/s² noise floor
        val samples = buildSignal(
            durationMs = 6000L,
            peakTimesMs = listOf(1000L, 2500L, 4000L),
            peakMagnitude = 4f,
        )
        assertEquals(0, countReps(samples))
    }

    @Test
    fun ignoresPeaksBelowThreshold() {
        // Peaks above noise floor but below the calibrated threshold
        val samples = buildSignal(
            durationMs = 6000L,
            peakTimesMs = listOf(1000L, 2500L, 4000L),
            peakMagnitude = 5.5f,  // above NOISE_FLOOR but below threshold of 6f
        )
        assertEquals(0, countReps(samples))
    }

    // -------------------------------------------------------------------------
    // Minimum peak separation
    // -------------------------------------------------------------------------

    @Test
    fun twoCloseTogetherPeaksCountAsOne() {
        // Two peaks 500ms apart — below the 1000ms minimum separation — count as one
        val samples = buildSignal(
            durationMs = 5000L,
            peakTimesMs = listOf(1000L, 1500L, 3000L),  // first two are too close
        )
        assertEquals(2, countReps(samples))
    }

    @Test
    fun peaksExactlyAtSeparationBoundaryAreAllowed() {
        // Peaks exactly 1000ms apart should both be counted
        val samples = buildSignal(
            durationMs = 5000L,
            peakTimesMs = listOf(1000L, 2000L, 3000L),
        )
        assertEquals(3, countReps(samples))
    }

    // -------------------------------------------------------------------------
    // reset()
    // -------------------------------------------------------------------------

    @Test
    fun resetClearsSeparationTimer() {
        val detector = RepPeakDetector(threshold = threshold)
        val firstSet = buildSignal(durationMs = 3000L, peakTimesMs = listOf(1000L, 2000L))
        firstSet.forEach { detector.onSample(it) }

        detector.reset()

        // After reset the buffer is empty, so we need to refill it before detection.
        // A peak placed early in the second set should still be detected without
        // being blocked by the pre-reset separation timer.
        var detected = false
        val secondSet = buildSignal(durationMs = 2000L, peakTimesMs = listOf(500L))
        secondSet.forEach { if (detector.onSample(it)) detected = true }
        assertTrue("Peak after reset should be detected", detected)
    }

    // -------------------------------------------------------------------------
    // Intermediate-bump rejection (reproduces real-world false-positive scenario)
    // -------------------------------------------------------------------------

    @Test
    fun intermediateSmallBumpBetweenRepsIsIgnored() {
        // Simulate the pattern from the debug log: genuine rep at high magnitude,
        // followed by an intermediate bump at sub-threshold magnitude, then another
        // genuine rep — the bump must not be counted.
        val intervalMs = 20L
        val samples = (0 until 500).map { i -> TimestampedSample(i * intervalMs, 0f) }.toMutableList()

        // Genuine rep 1 at t=1000ms (magnitude 15f — above threshold 6f)
        val shape = listOf(0.3f, 0.7f, 1.0f, 0.7f, 0.3f)
        for ((offset, factor) in shape.withIndex()) {
            val idx = (1000L / intervalMs).toInt() - 2 + offset
            if (idx in samples.indices) samples[idx] = samples[idx].copy(magnitude = 15f * factor)
        }

        // Intermediate bump at t=2000ms (magnitude 5.5f — above noise floor, below threshold)
        for ((offset, factor) in shape.withIndex()) {
            val idx = (2000L / intervalMs).toInt() - 2 + offset
            if (idx in samples.indices) samples[idx] = samples[idx].copy(magnitude = 5.5f * factor)
        }

        // Genuine rep 2 at t=3000ms (magnitude 15f)
        for ((offset, factor) in shape.withIndex()) {
            val idx = (3000L / intervalMs).toInt() - 2 + offset
            if (idx in samples.indices) samples[idx] = samples[idx].copy(magnitude = 15f * factor)
        }

        assertEquals(2, countReps(samples))
    }
}
