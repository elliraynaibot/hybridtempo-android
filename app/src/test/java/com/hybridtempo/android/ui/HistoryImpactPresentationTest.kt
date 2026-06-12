package com.hybridtempo.android.ui

import com.hybridtempo.android.data.BreathworkSession
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class HistoryImpactPresentationTest {
    @Test
    fun `impact summary averages reflected sessions`() {
        val impact = listOf(
            reflectedSession(control = 8, recovery = 7),
            reflectedSession(control = 6, recovery = 9),
            BreathworkSession(
                protocol = "Older session",
                durationMinutes = 5,
                cadence = "4 second inhale - 5 second exhale",
                completed = true,
            ),
        ).toHistoryImpactPresentation()

        assertTrue(impact.hasReflectionData)
        assertEquals("2 reflected sessions", impact.sampleLabel)
        assertEquals("7.0", impact.averageControlLabel)
        assertEquals("8.0", impact.averageRecoveryLabel)
        assertEquals("Breathwork is trending useful when control and recovery stay near 7+.", impact.message)
    }

    @Test
    fun `impact summary explains when there is no reflection data`() {
        val impact = emptyList<BreathworkSession>().toHistoryImpactPresentation()

        assertFalse(impact.hasReflectionData)
        assertEquals("No reflections yet", impact.sampleLabel)
        assertEquals("--", impact.averageControlLabel)
        assertEquals("--", impact.averageRecoveryLabel)
        assertEquals("Complete a session reflection to start seeing breathwork impact.", impact.message)
    }

    private fun reflectedSession(
        control: Int,
        recovery: Int,
    ): BreathworkSession = BreathworkSession(
        protocol = "Cooldown Breath Recovery",
        durationMinutes = 5,
        cadence = "4 second inhale - 6 second exhale",
        completed = true,
        perceivedControl = control,
        perceivedRecovery = recovery,
        reflectionFeeling = "calmer",
    )
}
