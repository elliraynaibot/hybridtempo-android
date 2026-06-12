package com.hybridtempo.android.ui

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class FunctionalFitnessCuePresentationTest {
    @Test
    fun `home copy positions app around functional fitness cues`() {
        val presentation = HomePresentation()
        val modes = defaultHomeModes()

        assertEquals("Breathwork for where you are in the workout", presentation.title)
        assertTrue(presentation.subtitle.contains("before", ignoreCase = true))
        assertEquals("Prime before training", modes.first().action)
    }

    @Test
    fun `before workout route recommends brace breathe`() {
        val cue = CheckInDraft().forGuidedMoment("pre_workout").toTodayCuePresentation()

        assertEquals("Before workout", cue.category)
        assertEquals("Brace & Breathe", cue.cue)
        assertTrue(cue.practice.contains("Brace & Breathe", ignoreCase = true))
        assertEquals("Start Brace & Breathe", cue.practiceAction)
    }

    @Test
    fun `after workout route recommends reset after the session`() {
        val cue = CheckInDraft().forGuidedMoment("post_workout").toTodayCuePresentation()

        assertEquals("After workout", cue.category)
        assertEquals("Reset After the Session", cue.cue)
        assertTrue(cue.practice.contains("Reset After the Session", ignoreCase = true))
        assertEquals("Start Reset", cue.practiceAction)
    }

    @Test
    fun `amrap plus starting too fast recommends pacing cue`() {
        val draft = CheckInDraft(
            workoutFormat = "AMRAP",
            breathingProblem = "I start too fast",
        )

        val cue = draft.toTodayCuePresentation()

        assertEquals("First round smooth.", cue.cue)
        assertEquals("Pacing", cue.category)
        assertTrue(cue.practice.contains("2-minute", ignoreCase = true))
        assertTrue(cue.reviewQuestion.contains("stay controlled", ignoreCase = true))
    }

    @Test
    fun `today cue primary action starts practice directly`() {
        val cue = CheckInDraft(
            workoutFormat = "Intervals",
            breathingProblem = "I lose rhythm",
        ).toTodayCuePresentation()

        assertEquals("Start cue practice", cue.practiceAction)
    }

    @Test
    fun `emom plus poor recovery recommends reset cue`() {
        val draft = CheckInDraft(
            workoutFormat = "EMOM",
            breathingProblem = "I can't recover between movements",
        )

        val cue = draft.toTodayCuePresentation()

        assertEquals("Long exhale before the next effort.", cue.cue)
        assertEquals("Recovery", cue.category)
        assertTrue(cue.why.contains("built-in rest", ignoreCase = true))
    }

    @Test
    fun `functional fitness format options are specific to mixed modal workouts`() {
        assertEquals(
            listOf("AMRAP", "EMOM", "For time", "Intervals", "Strength + conditioning", "Cooldown"),
            workoutFormatOptions(),
        )
    }

    @Test
    fun `long selector values have compact display labels`() {
        assertEquals("Strength +\nconditioning", workoutFormatDisplayLabel("Strength + conditioning"))
        assertEquals("Can't recover\nbetween moves", breathingProblemDisplayLabel("I can't recover between movements"))
        assertEquals("Tense\nshoulders/jaw", breathingProblemDisplayLabel("I tense my shoulders/jaw"))
    }

    @Test
    fun `workout handoff tells athlete to use cue then return to review`() {
        val cue = CheckInDraft(
            workoutFormat = "For time",
            breathingProblem = "I fall apart late",
        ).toTodayCuePresentation()

        val handoff = cue.toWorkoutHandoffPresentation()

        assertEquals("Take this cue to the workout", handoff.title)
        assertTrue(handoff.workoutInstruction.contains(cue.cue, ignoreCase = true))
        assertTrue(handoff.reviewInstruction.contains("come back", ignoreCase = true))
        assertEquals(cue.reviewQuestion, handoff.reviewQuestion)
    }
}
