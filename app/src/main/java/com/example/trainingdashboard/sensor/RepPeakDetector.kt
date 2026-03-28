package com.example.trainingdashboard.sensor

/**
 * Streaming peak detector for real-time rep counting.
 *
 * Mirrors the algorithm in [RepCalibrator]: buffers incoming samples, applies the same
 * smoothing and local-maxima check, and enforces minimum peak separation. Calling
 * [onSample] returns true exactly when a new rep peak is confirmed.
 *
 * There is an inherent latency of [NEIGHBOR_WINDOW] samples (~100ms at SENSOR_DELAY_GAME)
 * before a candidate peak can be confirmed, because the algorithm needs to observe the
 * samples after a candidate to verify it is a local maximum.
 */
class RepPeakDetector(private val threshold: Float) {

    private val buffer = ArrayDeque<TimestampedSample>(BUFFER_SIZE)
    // Initialised to -MIN_PEAK_SEPARATION_MS so the first detected peak is never blocked
    // by the separation guard regardless of its timestamp.
    private var lastPeakTimestampMs = -MIN_PEAK_SEPARATION_MS

    /**
     * Feed the next sample into the detector.
     * @return true if this sample's arrival confirms a new rep peak.
     */
    fun onSample(sample: TimestampedSample): Boolean {
        buffer.addLast(sample)
        if (buffer.size > BUFFER_SIZE) buffer.removeFirst()
        if (buffer.size < BUFFER_SIZE) return false

        // Compute smoothed magnitudes for the full window so the local-max check
        // operates on smoothed values, matching RepCalibrator's two-pass approach.
        val smoothed = FloatArray(BUFFER_SIZE) { i ->
            val from = maxOf(0, i - SMOOTHING_HALF)
            val to = minOf(BUFFER_SIZE - 1, i + SMOOTHING_HALF)
            var sum = 0f
            for (j in from..to) sum += buffer[j].magnitude
            sum / (to - from + 1)
        }

        val centerSmoothed = smoothed[CENTER]
        if (centerSmoothed <= NOISE_FLOOR) return false
        if (centerSmoothed < threshold) return false

        val isLocalMax = (1..NEIGHBOR_WINDOW).all { offset ->
            centerSmoothed > smoothed[CENTER - offset] &&
                centerSmoothed > smoothed[CENTER + offset]
        }
        if (!isLocalMax) return false

        val candidateTimestampMs = buffer[CENTER].timestampMs
        if (candidateTimestampMs - lastPeakTimestampMs < MIN_PEAK_SEPARATION_MS) return false

        lastPeakTimestampMs = candidateTimestampMs
        return true
    }

    /** Reset state — call when starting a new set. */
    fun reset() {
        buffer.clear()
        lastPeakTimestampMs = -MIN_PEAK_SEPARATION_MS
    }

    companion object {
        // These must stay in sync with RepCalibrator so calibration and counting
        // apply identical signal processing.
        private const val SMOOTHING_HALF = 2       // = SMOOTHING_WINDOW / 2
        private const val NEIGHBOR_WINDOW = 5      // local-max check radius
        private const val NOISE_FLOOR = 5f         // m/s²

        // Raised from 600ms to handle the two-phase motion of compound exercises
        // (e.g. squat descent + ascent). At 1000ms the maximum countable rate is 60 reps/min,
        // which comfortably covers all three exercises.
        private const val MIN_PEAK_SEPARATION_MS = 1000L

        // Buffer must hold enough raw samples to smooth every neighbour of the centre candidate.
        // Centre is at NEIGHBOR_WINDOW; its outermost smoothing input is at NEIGHBOR_WINDOW ± SMOOTHING_HALF.
        // So the required span is NEIGHBOR_WINDOW + SMOOTHING_HALF on each side of centre.
        private const val BUFFER_SIZE = (NEIGHBOR_WINDOW + SMOOTHING_HALF) * 2 + 1  // = 15
        private const val CENTER = NEIGHBOR_WINDOW + SMOOTHING_HALF                  // = 7
    }
}
