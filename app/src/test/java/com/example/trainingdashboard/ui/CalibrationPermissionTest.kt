package com.example.trainingdashboard.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CalibrationPermissionTest {

    // -------------------------------------------------------------------------
    // Granted
    // -------------------------------------------------------------------------

    @Test
    fun grantedWhenPermissionGranted() {
        val status = resolveSensorPermissionStatus(isGranted = true, canAskAgain = false)
        assertEquals(SensorPermissionStatus.Granted, status)
    }

    @Test
    fun grantedEvenWhenCanAskAgainIsTrue() {
        // canAskAgain is irrelevant once the permission is granted
        val status = resolveSensorPermissionStatus(isGranted = true, canAskAgain = true)
        assertEquals(SensorPermissionStatus.Granted, status)
    }

    // -------------------------------------------------------------------------
    // Denied — retryable
    // -------------------------------------------------------------------------

    @Test
    fun deniedRetryableWhenDeniedAndCanAskAgain() {
        // First denial: system indicates it will show the dialog again
        val status = resolveSensorPermissionStatus(isGranted = false, canAskAgain = true)
        assertEquals(SensorPermissionStatus.DeniedRetryable, status)
    }

    @Test
    fun deniedRetryableWhenActivityIsUnavailable() {
        // Safe default: canAskAgain = true when Activity reference is null
        val canAskAgain = null ?: true
        val status = resolveSensorPermissionStatus(isGranted = false, canAskAgain = canAskAgain)
        assertEquals(SensorPermissionStatus.DeniedRetryable, status)
    }

    // -------------------------------------------------------------------------
    // Permanently denied
    // -------------------------------------------------------------------------

    @Test
    fun permanentlyDeniedWhenDeniedAndCannotAskAgain() {
        // shouldShowRequestPermissionRationale returns false after permanent denial
        val status = resolveSensorPermissionStatus(isGranted = false, canAskAgain = false)
        assertEquals(SensorPermissionStatus.PermanentlyDenied, status)
    }

    // -------------------------------------------------------------------------
    // Outcome implies correct UI gate
    // -------------------------------------------------------------------------

    @Test
    fun onlyGrantedStatusAllowsCalibrationToStart() {
        val granted    = resolveSensorPermissionStatus(isGranted = true,  canAskAgain = false)
        val retryable  = resolveSensorPermissionStatus(isGranted = false, canAskAgain = true)
        val permanent  = resolveSensorPermissionStatus(isGranted = false, canAskAgain = false)

        assertEquals(true,  granted   == SensorPermissionStatus.Granted)
        assertEquals(false, retryable == SensorPermissionStatus.Granted)
        assertEquals(false, permanent == SensorPermissionStatus.Granted)
    }

    @Test
    fun onlyPermanentlyDeniedStatusRequiresSettingsDeepLink() {
        val granted   = resolveSensorPermissionStatus(isGranted = true,  canAskAgain = false)
        val retryable = resolveSensorPermissionStatus(isGranted = false, canAskAgain = true)
        val permanent = resolveSensorPermissionStatus(isGranted = false, canAskAgain = false)

        assertEquals(false, granted   == SensorPermissionStatus.PermanentlyDenied)
        assertEquals(false, retryable == SensorPermissionStatus.PermanentlyDenied)
        assertEquals(true,  permanent == SensorPermissionStatus.PermanentlyDenied)
    }
}
