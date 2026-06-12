package com.hybridtempo.android.ui

import com.hybridtempo.android.data.BreathworkSession
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class HistorySessionPresentationTest {
    @Test
    fun `history presentation includes reflection scores and feeling when present`() {
        val presentation = BreathworkSession(
            protocol = "Cooldown Breath Recovery",
            durationMinutes = 5,
            cadence = "4 second inhale - 6 second exhale",
            completed = true,
            completedAt = "2026-06-05T20:10:00Z",
            breathSkillId = "cooldown-hr-recovery",
            perceivedControl = 7,
            perceivedRecovery = 8,
            reflectionFeeling = "calmer",
            reflectionNotes = "Settled faster than usual.",
            breathRhythmBeforePercent = 72,
            breathRhythmAfterPercent = 84,
            breathRhythmImprovementPercent = 12,
        ).toHistorySessionPresentation()

        assertEquals("Cooldown Breath Recovery", presentation.title)
        assertEquals("5m", presentation.durationLabel)
        assertEquals("Control 7/10 - Recovery 8/10", presentation.reflectionSummary)
        assertEquals("Felt calmer", presentation.feelingLabel)
        assertEquals("Settled faster than usual.", presentation.notes)
        assertEquals("Breath control improved +12%.", presentation.breathRhythmSummary)
        assertTrue(presentation.hasReflection)
    }

    @Test
    fun `history presentation omits reflection copy for older sessions`() {
        val presentation = BreathworkSession(
            protocol = "Post-training recovery",
            durationMinutes = 5,
            cadence = "4 second inhale - 5 second exhale",
            completed = true,
        ).toHistorySessionPresentation()

        assertEquals("", presentation.reflectionSummary)
        assertEquals("", presentation.feelingLabel)
        assertEquals("", presentation.notes)
        assertEquals("No breath rhythm check saved for this session.", presentation.breathRhythmSummary)
        assertFalse(presentation.hasReflection)
    }

    @Test
    fun `history presentation shows before rhythm when after check is unavailable`() {
        val presentation = BreathworkSession(
            protocol = "Brace & Breathe",
            durationMinutes = 5,
            cadence = "4 second inhale - 6 second exhale",
            completed = true,
            breathRhythmBeforePercent = 78,
        ).toHistorySessionPresentation()

        assertEquals("Baseline rhythm matched 78%. Add an after-workout check to compare.", presentation.breathRhythmSummary)
    }
}
