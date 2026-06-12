package com.hybridtempo.android.data

import kotlin.test.assertEquals
import org.junit.Test

class BreathworkSessionReflectionTest {
    @Test
    fun `completed session can carry post session reflection`() {
        val session = BreathworkSession(
            protocol = "Cooldown HR Recovery",
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
}
