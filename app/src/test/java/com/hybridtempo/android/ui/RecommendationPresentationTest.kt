package com.hybridtempo.android.ui

import com.hybridtempo.android.data.BreathworkRecommendation
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class RecommendationPresentationTest {
    @Test
    fun `presentation exposes skill specific recommendation details`() {
        val presentation = BreathworkRecommendation(
            protocol = "Cooldown Breath Recovery",
            durationMinutes = 5,
            rationale = "Cooldown Breath Recovery matches after training because your current check-in points to recovery.",
            cadence = "4 second inhale - 6 second exhale",
            breathSkillId = "cooldown-hr-recovery",
            trainingCue = "Finish the workout by recovering, not rushing away.",
            measurementFocus = "Compare breath rhythm before and after comparable workouts.",
        ).toRecommendationPresentation()

        assertEquals("Cooldown Breath Recovery", presentation.title)
        assertEquals("5 min - 4 second inhale - 6 second exhale", presentation.meta)
        assertEquals("Finish the workout by recovering, not rushing away.", presentation.trainingCue)
        assertEquals("Compare breath rhythm before and after comparable workouts.", presentation.measurementFocus)
        assertTrue(presentation.badge.contains("Skill"))
    }

    @Test
    fun `presentation provides safe defaults for older recommendation objects`() {
        val presentation = BreathworkRecommendation(
            protocol = "Post-training recovery",
            durationMinutes = 5,
            rationale = "A steady recovery protocol fits the current training context.",
            cadence = "4 second inhale - 5 second exhale",
        ).toRecommendationPresentation()

        assertEquals("Use this cue during the session and return to it after training.", presentation.trainingCue)
        assertEquals("After the session, the app will ask how controlled and recovered you felt.", presentation.measurementFocus)
        assertEquals("", presentation.fallbackReason)
    }
}
