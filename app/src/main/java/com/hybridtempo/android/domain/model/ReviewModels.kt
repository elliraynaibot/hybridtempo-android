package com.hybridtempo.android.domain.model

import java.time.Instant

enum class ReflectionFeeling {
    CALMER,
    ABOUT_THE_SAME,
    MORE_ACTIVATED,
}

enum class InsightConfidence {
    LOW,
    MEDIUM,
    HIGH,
}

data class PostWorkoutReflection(
    val workoutId: String,
    val breathSkillId: String,
    val completedAt: Instant,
    val perceivedControl: Int,
    val perceivedRecovery: Int,
    val feeling: ReflectionFeeling,
    val notes: String? = null,
)

data class HeartRateAnalysisResult(
    val workoutId: String,
    val recoveryDropAfterTwoMinutes: Int?,
    val timeToSettleMinutes: Int?,
    val comparedWithSimilarSessions: String,
    val confidence: InsightConfidence,
)

data class InsightCard(
    val id: String,
    val title: String,
    val body: String,
    val breathSkillId: String?,
    val confidence: InsightConfidence,
)
