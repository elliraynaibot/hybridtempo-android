package com.hybridtempo.android.ui

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class WorkoutReviewPresentationTest {
    @Test
    fun `workout review is complete when cue and control ratings are selected`() {
        val draft = WorkoutReviewDraft(
            cueHelpfulness = 8,
            breathControl = 7,
        )

        assertTrue(draft.isComplete)
    }

    @Test
    fun `workout review is incomplete without cue helpfulness`() {
        val draft = WorkoutReviewDraft(
            cueHelpfulness = 0,
            breathControl = 7,
        )

        assertFalse(draft.isComplete)
    }

    @Test
    fun `workout review presentation is about the workout cue not session recovery`() {
        val presentation = WorkoutReviewDraft(
            cueHelpfulness = 8,
            breathControl = 7,
            notes = "Stayed smooth through round three.",
        ).toWorkoutReviewPresentation(
            cue = TodayCuePresentation(
                workoutFormat = "AMRAP",
                breathingProblem = "I start too fast",
                category = "Pacing",
                cue = "First round smooth.",
                why = "Protect the early part of the workout.",
                practice = "2-minute pacing breath drill.",
                reviewQuestion = "Did the cue help you stay controlled past halfway?",
            ),
        )

        assertEquals("Cue 8/10 - Breath control 7/10", presentation.scoreSummary)
        assertEquals("Did the cue help you stay controlled past halfway?", presentation.reviewQuestion)
        assertEquals("Stayed smooth through round three.", presentation.notes)
    }
}
