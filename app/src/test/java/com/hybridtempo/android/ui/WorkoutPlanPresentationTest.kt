package com.hybridtempo.android.ui

import com.hybridtempo.android.domain.model.ImportedWorkout
import com.hybridtempo.android.domain.model.TrainingIntensity
import com.hybridtempo.android.domain.model.WorkoutType
import java.time.Instant
import kotlin.test.assertEquals
import org.junit.Test

class WorkoutPlanPresentationTest {
    @Test
    fun `strength plan summarizes workout structure and breath window`() {
        val summary = CheckInDraft(
            workoutType = "Strength",
            workoutIntensity = 7,
            setCount = 4,
            repsPerSet = 5,
            timeAvailable = 5,
            sessionIntent = "between_sets",
        ).toWorkoutPlanSummary()

        assertEquals("Strength · Hard", summary.headline)
        assertEquals("4 sets · 5 reps", summary.structure)
        assertEquals("Between sets", summary.breathWindow)
        assertEquals("5 min available", summary.timeAvailable)
    }

    @Test
    fun `interval plan summarizes rounds and work duration`() {
        val summary = CheckInDraft(
            workoutType = "Intervals",
            workoutIntensity = 9,
            intervalCount = 6,
            intervalMinutes = 2,
            timeAvailable = 10,
            sessionIntent = "pre_workout",
        ).toWorkoutPlanSummary()

        assertEquals("Intervals · Max", summary.headline)
        assertEquals("6 rounds · 2 min work", summary.structure)
        assertEquals("Before effort", summary.breathWindow)
        assertEquals("10 min available", summary.timeAvailable)
    }

    @Test
    fun `breath window chooses matching guide mode`() {
        assertEquals("pre_set", guideModeForSessionIntent("pre_workout").value)
        assertEquals("between_sets", guideModeForSessionIntent("between_sets").value)
        assertEquals("post_workout", guideModeForSessionIntent("post_workout").value)
    }

    @Test
    fun `imported health workout prefills track plan without changing breath duration`() {
        val draft = ImportedWorkout(
            id = "health-connect-1",
            source = "Google Health",
            workoutType = WorkoutType.RUNNING,
            startedAt = Instant.parse("2026-06-07T12:00:00Z"),
            endedAt = Instant.parse("2026-06-07T12:42:00Z"),
            intensity = TrainingIntensity.HARD,
        ).toCheckInDraft(CheckInDraft(timeAvailable = 5))

        assertEquals("Run", draft.workoutType)
        assertEquals(7, draft.workoutIntensity)
        assertEquals("post_workout", draft.sessionIntent)
        assertEquals(5, draft.timeAvailable)
    }
}
