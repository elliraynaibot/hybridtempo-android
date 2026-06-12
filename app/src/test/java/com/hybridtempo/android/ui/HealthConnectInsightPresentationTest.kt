package com.hybridtempo.android.ui

import com.hybridtempo.android.health.HealthConnectAvailability
import com.hybridtempo.android.health.HealthConnectUiStatus
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class HealthConnectInsightPresentationTest {
    @Test
    fun `disconnected health insight frames Health Connect as optional context`() {
        val presentation = HealthConnectUiStatus(
            availability = HealthConnectAvailability.Available,
            enabled = false,
        ).toHealthConnectInsightPresentation()

        assertEquals("Optional workout context", presentation.title)
        assertTrue(presentation.body.contains("workout", ignoreCase = true))
        assertTrue(presentation.body.contains("breath checks", ignoreCase = true))
        assertEquals("Connect in settings", presentation.actionLabel)
    }

    @Test
    fun `connected health insight explains workouts without relying on heart rate`() {
        val presentation = HealthConnectUiStatus(
            availability = HealthConnectAvailability.Available,
            enabled = true,
        ).toHealthConnectInsightPresentation()

        assertTrue(presentation.body.contains("completed workouts", ignoreCase = true))
        assertTrue(presentation.body.contains("breath rhythm", ignoreCase = true))
        assertEquals(null, presentation.actionLabel)
    }
}
