package com.hybridtempo.android.ui

import com.hybridtempo.android.health.HealthConnectAvailability
import com.hybridtempo.android.health.HealthConnectManager
import com.hybridtempo.android.health.HealthConnectUiStatus
import com.hybridtempo.android.readiness.HealthMetricsSnapshot
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class ProfileHealthConnectPresentationTest {
    @Test
    fun `profile health card makes Google Health connection explicit`() {
        val presentation = HealthConnectUiStatus(
            availability = HealthConnectAvailability.Available,
            enabled = false,
        ).toProfileHealthConnectPresentation(enabled = false)

        assertEquals("Google Health Connect", presentation.title)
        assertEquals("Connect", presentation.connectAction)
        assertTrue(presentation.body.contains("Google Health", ignoreCase = true))
        assertTrue(presentation.body.contains("Health Connect", ignoreCase = true))
        assertTrue(presentation.canConnect)
    }

    @Test
    fun `profile health card explains connected but waiting state`() {
        val presentation = HealthConnectUiStatus(
            availability = HealthConnectAvailability.Available,
            enabled = true,
            grantedPermissions = HealthConnectManager.PERMISSIONS,
        ).toProfileHealthConnectPresentation(enabled = true)

        assertEquals("Refresh", presentation.connectAction)
        assertTrue(presentation.statusLabel.contains("waiting", ignoreCase = true))
    }

    @Test
    fun `profile health card disables connect when unavailable`() {
        val presentation = HealthConnectUiStatus(
            availability = HealthConnectAvailability.Unavailable,
            enabled = false,
        ).toProfileHealthConnectPresentation(enabled = false)

        assertFalse(presentation.canConnect)
        assertEquals("Unavailable on this device", presentation.statusLabel)
    }

    @Test
    fun `profile health card shows recent health data without exposing HR debug`() {
        val presentation = HealthConnectUiStatus(
            availability = HealthConnectAvailability.Available,
            enabled = true,
            grantedPermissions = HealthConnectManager.PERMISSIONS,
            metrics = HealthMetricsSnapshot(
                workoutsLast7Days = 2,
            ),
        ).toProfileHealthConnectPresentation(enabled = true)

        assertEquals("Recent health data is available.", presentation.dataDiagnostic)
    }

    @Test
    fun `profile health card frames disconnected state as optional workout context`() {
        val presentation = HealthConnectUiStatus(
            availability = HealthConnectAvailability.Available,
            enabled = false,
            grantedPermissions = emptySet(),
        ).toProfileHealthConnectPresentation(enabled = false)

        assertEquals("Optional: connect later if you want workout imports.", presentation.dataDiagnostic)
    }

    @Test
    fun `profile health card shows connected waiting state without HR error copy`() {
        val presentation = HealthConnectUiStatus(
            availability = HealthConnectAvailability.Available,
            enabled = true,
            grantedPermissions = HealthConnectManager.PERMISSIONS,
        ).toProfileHealthConnectPresentation(enabled = true)

        assertEquals("Connected. Waiting for recent Google Health data.", presentation.dataDiagnostic)
    }
}
