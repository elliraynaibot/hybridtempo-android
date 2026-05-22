package com.hybridtempo.android.recommendation

import com.hybridtempo.android.data.BreathworkRecommendation

data class RecommendationRequest(
    val profile: AthleteProfileContext,
    val checkIn: CheckInContext,
    val recentTrends: RecentTrendContext = RecentTrendContext(),
)

data class AthleteProfileContext(
    val trainingStyle: String,
    val weeklyTrainingFrequency: Int,
    val goals: List<String>,
    val preferredSessionLength: Int,
    val raceDate: String,
)

data class CheckInContext(
    val energy: Int,
    val soreness: Int,
    val stress: Int,
    val workoutType: String,
    val workoutIntensity: Int,
    val timeAvailable: Int,
)

data class RecentTrendContext(
    val energy: List<Int> = emptyList(),
    val soreness: List<Int> = emptyList(),
    val stress: List<Int> = emptyList(),
)

data class RecommendationResponse(
    val recommendation: BreathworkRecommendation,
    val source: RecommendationSource,
)

enum class RecommendationSource {
    DeterministicFallback,
    BackendAi,
}

interface RecommendationEngine {
    fun recommend(request: RecommendationRequest): RecommendationResponse
}
