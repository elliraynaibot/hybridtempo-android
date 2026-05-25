package com.hybridtempo.android.recommendation

import kotlinx.coroutines.runBlocking
import kotlin.test.assertEquals
import org.junit.Test

class DeterministicRecommendationEngineTest {
    private val engine = DeterministicRecommendationEngine()

    @Test
    fun `pre workout intent recommends activation`() = runBlocking {
        val response = engine.recommend(request(sessionIntent = "pre_workout"))

        assertEquals("Activation", response.recommendation.protocol)
    }

    @Test
    fun `evening intent recommends sleep transition`() = runBlocking {
        val response = engine.recommend(request(sessionIntent = "evening_downshift"))

        assertEquals("Sleep transition", response.recommendation.protocol)
    }

    @Test
    fun `post workout intent recommends post training recovery`() = runBlocking {
        val response = engine.recommend(request(sessionIntent = "post_workout"))

        assertEquals("Post-training recovery", response.recommendation.protocol)
    }

    private fun request(sessionIntent: String): RecommendationRequest = RecommendationRequest(
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
            soreness = 4,
            stress = 5,
            workoutType = "Hybrid",
            workoutIntensity = 7,
            timeAvailable = 5,
            sessionIntent = sessionIntent,
        ),
    )
}
