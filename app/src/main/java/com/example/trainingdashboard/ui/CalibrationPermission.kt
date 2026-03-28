package com.example.trainingdashboard.ui

sealed class SensorPermissionStatus {
    /** Permission granted — calibration can proceed. */
    object Granted : SensorPermissionStatus()
    /** Permission denied but the system will show the dialog again. */
    object DeniedRetryable : SensorPermissionStatus()
    /** Permission permanently denied — user must go to Settings. */
    object PermanentlyDenied : SensorPermissionStatus()
}

/**
 * Resolves the UI-visible sensor permission status from raw Android values.
 *
 * @param isGranted true when ContextCompat.checkSelfPermission == PERMISSION_GRANTED
 * @param canAskAgain true when Activity.shouldShowRequestPermissionRationale returns true
 *   (only meaningful after a denial; pass true as a safe default if the Activity is unavailable)
 */
fun resolveSensorPermissionStatus(isGranted: Boolean, canAskAgain: Boolean): SensorPermissionStatus =
    when {
        isGranted   -> SensorPermissionStatus.Granted
        canAskAgain -> SensorPermissionStatus.DeniedRetryable
        else        -> SensorPermissionStatus.PermanentlyDenied
    }
