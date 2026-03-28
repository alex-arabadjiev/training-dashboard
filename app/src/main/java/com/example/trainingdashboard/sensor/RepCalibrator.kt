package com.example.trainingdashboard.sensor

import kotlin.math.sqrt

data class TimestampedSample(val timestampMs: Long, val magnitude: Float)

sealed class CalibrationResult {
    data class Success(
        val threshold: Float,
        val repCount: Int,
        /** Accepted peaks used to derive the threshold, in chronological order. */
        val peaks: List<TimestampedSample>
    ) : CalibrationResult()
    object Failed : CalibrationResult()
}

object RepCalibrator {

    private const val SMOOTHING_WINDOW = 5
    private const val NOISE_FLOOR = 5f
    private const val MIN_PEAK_SEPARATION_MS = 600L
    private const val MIN_PEAKS = 3
    private const val THRESHOLD_FACTOR = 0.85f

    fun analyze(samples: List<TimestampedSample>): CalibrationResult {
        if (samples.size < SMOOTHING_WINDOW) return CalibrationResult.Failed

        val smoothed = smooth(samples)
        val peaks = findPeaks(smoothed)

        if (peaks.size < MIN_PEAKS) return CalibrationResult.Failed

        val threshold = median(peaks.map { it.magnitude }) * THRESHOLD_FACTOR
        return CalibrationResult.Success(threshold = threshold, repCount = peaks.size, peaks = peaks)
    }

    private fun smooth(samples: List<TimestampedSample>): List<TimestampedSample> {
        val half = SMOOTHING_WINDOW / 2
        return samples.mapIndexed { i, sample ->
            val from = maxOf(0, i - half)
            val to = minOf(samples.lastIndex, i + half)
            val avg = samples.subList(from, to + 1).map { it.magnitude }.average().toFloat()
            sample.copy(magnitude = avg)
        }
    }

    private fun findPeaks(samples: List<TimestampedSample>): List<TimestampedSample> {
        val peaks = mutableListOf<TimestampedSample>()
        val neighborWindow = 5

        for (i in neighborWindow..samples.lastIndex - neighborWindow) {
            val current = samples[i]
            if (current.magnitude <= NOISE_FLOOR) continue

            val isLocalMax = (1..neighborWindow).all { offset ->
                current.magnitude >= samples[i - offset].magnitude &&
                current.magnitude >= samples[i + offset].magnitude
            }
            if (!isLocalMax) continue

            // Enforce minimum separation from the last accepted peak
            if (peaks.isEmpty() || current.timestampMs - peaks.last().timestampMs >= MIN_PEAK_SEPARATION_MS) {
                peaks.add(current)
            } else if (current.magnitude > peaks.last().magnitude) {
                // Same peak window — keep the higher one
                peaks[peaks.lastIndex] = current
            }
        }

        return peaks
    }

    private fun median(values: List<Float>): Float {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2f else sorted[mid]
    }
}
