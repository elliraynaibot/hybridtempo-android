package com.hybridtempo.android.ui

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class SessionReflectionPresentationTest {
    @Test
    fun `reflection draft is complete when control and recovery ratings are selected`() {
        val draft = SessionReflectionDraft(
            perceivedControl = 7,
            perceivedRecovery = 8,
            feeling = SessionReflectionFeeling.Calmer,
        )

        assertTrue(draft.isComplete)
    }

    @Test
    fun `reflection draft is incomplete without ratings`() {
        val draft = SessionReflectionDraft(
            perceivedControl = 0,
            perceivedRecovery = 8,
            feeling = SessionReflectionFeeling.Calmer,
        )

        assertFalse(draft.isComplete)
    }

    @Test
    fun `presentation summarizes what the user is reporting`() {
        val presentation = SessionReflectionDraft(
            perceivedControl = 7,
            perceivedRecovery = 8,
            feeling = SessionReflectionFeeling.Calmer,
            notes = "Settled faster than usual.",
        ).toSessionReflectionPresentation()

        assertEquals("Control 7/10 - Recovery 8/10", presentation.scoreSummary)
        assertEquals("Felt calmer", presentation.feelingLabel)
        assertEquals("Settled faster than usual.", presentation.notes)
    }
}
