package com.hybridtempo.android.ui

import com.hybridtempo.android.health.HealthConnectAvailability
import com.hybridtempo.android.health.HealthConnectUiStatus
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class HealthConnectInsightPresentationTest {
    @Test
    fun `disconnected health insight frames Health Connect as HR-backed review`() {
        val presentation = HealthConnectUiStatus(
            availability = HealthConnectAvailability.Available,
            enabled = false,
        ).toHealthConnectInsightPresentation()

        assertEquals("HR-backed review", presentation.title)
        assertTrue(presentation.body.contains("heart rate", ignoreCase = true))
        assertTrue(presentation.body.contains("after training", ignoreCase = true))
        assertEquals("Connect in settings", presentation.actionLabel)
    }

    @Test
    fun `connected health insight explains steady HR evaluation`() {
        val presentation = HealthConnectUiStatus(
            availability = HealthConnectAvailability.Available,
            enabled = true,
        ).toHealthConnectInsightPresentation()

        assertTrue(presentation.body.contains("steadier", ignoreCase = true))
        assertTrue(presentation.body.contains("breathing", ignoreCase = true))
        assertEquals(null, presentation.actionLabel)
    }
}
