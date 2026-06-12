package com.hybridtempo.android.recommendation

import kotlinx.coroutines.runBlocking
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class DeterministicRecommendationEngineTest {
    private val engine = DeterministicRecommendationEngine()

    @Test
    fun `pre workout intent recommends activation`() = runBlocking {
        val response = engine.recommend(request(sessionIntent = "pre_workout"))

        assertEquals("Avoid the Early Spike", response.recommendation.protocol)
        assertEquals("avoid-early-spike", response.recommendation.breathSkillId)
    }

    @Test
    fun `evening intent recommends sleep transition`() = runBlocking {
        val response = engine.recommend(request(sessionIntent = "evening_downshift"))

        assertEquals("Evening Training Wind-Down", response.recommendation.protocol)
        assertEquals("evening-training-wind-down", response.recommendation.breathSkillId)
    }

    @Test
    fun `post workout intent recommends post training recovery`() = runBlocking {
        val response = engine.recommend(request(sessionIntent = "post_workout"))

        assertEquals("Cooldown Breath Recovery", response.recommendation.protocol)
        assertEquals("cooldown-hr-recovery", response.recommendation.breathSkillId)
    }

    @Test
    fun `between sets intent recommends during training reset`() = runBlocking {
        val response = engine.recommend(
            request(
                sessionIntent = "between_sets",
                workoutType = "Strength",
                workoutIntensity = 8,
            ),
        )

        assertEquals("Between-Set Reset", response.recommendation.protocol)
        assertEquals("between-set-reset", response.recommendation.breathSkillId)
    }

    @Test
    fun `race context recommends race composure before training`() = runBlocking {
        val response = engine.recommend(
            request(
                sessionIntent = "pre_workout",
                workoutType = "Race event",
                workoutIntensity = 8,
                stress = 8,
            ),
        )

        assertEquals("race-event-composure", response.recommendation.breathSkillId)
    }

    @Test
    fun `hard post workout with high stress recommends downshift`() = runBlocking {
        val response = engine.recommend(
            request(
                sessionIntent = "post_workout",
                workoutIntensity = 9,
                stress = 8,
                soreness = 7,
            ),
        )

        assertEquals("post-conditioning-downshift", response.recommendation.breathSkillId)
    }

    @Test
    fun `recommendation explains cue and measurement`() = runBlocking {
        val response = engine.recommend(request(sessionIntent = "post_workout"))

        assertTrue(response.recommendation.rationale.contains("matches", ignoreCase = true))
        assertTrue(response.recommendation.trainingCue.isNotBlank())
        assertTrue(response.recommendation.measurementFocus.isNotBlank())
        assertTrue(response.recommendation.fallbackReason.isBlank())
    }

    private fun request(
        sessionIntent: String,
        workoutType: String = "Hybrid",
        workoutIntensity: Int = 7,
        stress: Int = 5,
        soreness: Int = 4,
    ): RecommendationRequest = RecommendationRequest(
        profile = AthleteProfileContext(
            trainingStyle = "Hybrid",
            weeklyTrainingFrequency = 5,
            goals = listOf("recovery"),
            preferredSessionLength = 5,
            raceName = "",
            raceDate = "",
        ),
        checkIn = CheckInContext(
            energy = 6,
            soreness = soreness,
            stress = stress,
            workoutType = workoutType,
            workoutIntensity = workoutIntensity,
            timeAvailable = 5,
            sessionIntent = sessionIntent,
        ),
    )
}
