package com.hybridtempo.android.data

import kotlin.test.assertEquals
import org.junit.Test

class BreathworkSessionReflectionTest {
    @Test
    fun `completed session can carry post session reflection`() {
        val session = BreathworkSession(
            protocol = "Cooldown Breath Recovery",
            durationMinutes = 5,
            cadence = "4 second inhale - 6 second exhale",
            completed = true,
            breathSkillId = "cooldown-hr-recovery",
            perceivedControl = 7,
            perceivedRecovery = 8,
            reflectionFeeling = "calmer",
            reflectionNotes = "Settled faster than usual.",
        )

        assertEquals("cooldown-hr-recovery", session.breathSkillId)
        assertEquals(7, session.perceivedControl)
        assertEquals(8, session.perceivedRecovery)
        assertEquals("calmer", session.reflectionFeeling)
        assertEquals("Settled faster than usual.", session.reflectionNotes)
    }

    @Test
    fun `completed session can carry breath rhythm checks`() {
        val session = BreathworkSession(
            protocol = "Reset After the Session",
            durationMinutes = 5,
            cadence = "Guided audio",
            completed = true,
            sessionStartedAt = "2026-06-12T14:00:00Z",
            sessionEndedAt = "2026-06-12T14:05:00Z",
            breathRhythmBeforePercent = 72,
            breathRhythmAfterPercent = 84,
            breathRhythmImprovementPercent = 12,
        )

        assertEquals(72, session.breathRhythmBeforePercent)
        assertEquals(84, session.breathRhythmAfterPercent)
        assertEquals(12, session.breathRhythmImprovementPercent)
    }
}
