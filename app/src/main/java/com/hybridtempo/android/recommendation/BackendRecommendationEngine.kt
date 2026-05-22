package com.hybridtempo.android.recommendation

import com.google.firebase.FirebaseApp
import com.google.firebase.functions.FirebaseFunctions
import com.hybridtempo.android.data.BreathPhase
import com.hybridtempo.android.data.BreathworkProtocol
import com.hybridtempo.android.data.BreathworkRecommendation
import kotlinx.coroutines.tasks.await

class BackendRecommendationEngine(
    private val fallback: RecommendationEngine = DeterministicRecommendationEngine(),
    private val functionsProvider: () -> FirebaseFunctions? = {
        runCatching { FirebaseApp.getInstance() }.getOrNull()
            ?.let { FirebaseFunctions.getInstance("us-central1") }
    },
) : RecommendationEngine {
    override suspend fun recommend(request: RecommendationRequest): RecommendationResponse {
        val functions = runCatching { functionsProvider() }.getOrNull()
            ?: return fallback.recommend(request)

        return runCatching {
            val result = functions
                .getHttpsCallable("recommendBreathwork")
                .call(request.toCallableMap())
                .await()

            RecommendationResponse(
                recommendation = result.data.asRecommendation(),
                source = RecommendationSource.BackendAi,
            )
        }.getOrElse {
            fallback.recommend(request)
        }
    }
}

private fun RecommendationRequest.toCallableMap(): Map<String, Any> = mapOf(
    "profile" to mapOf(
        "trainingStyle" to profile.trainingStyle,
        "weeklyTrainingFrequency" to profile.weeklyTrainingFrequency,
        "goals" to profile.goals,
        "preferredSessionLength" to profile.preferredSessionLength,
        "raceDate" to profile.raceDate,
    ),
    "checkIn" to mapOf(
        "energy" to checkIn.energy,
        "soreness" to checkIn.soreness,
        "stress" to checkIn.stress,
        "workoutType" to checkIn.workoutType,
        "workoutIntensity" to checkIn.workoutIntensity,
        "timeAvailable" to checkIn.timeAvailable,
    ),
    "recentTrends" to mapOf(
        "energy" to recentTrends.energy,
        "soreness" to recentTrends.soreness,
        "stress" to recentTrends.stress,
    ),
)

private fun Any?.asRecommendation(): BreathworkRecommendation {
    val map = asMap()
    val duration = map["durationMinutes"].asInt().coerceIn(1, 15)
    val protocol = map["protocol"].asString().ifBlank { "Post-training recovery" }

    return BreathworkRecommendation(
        protocol = protocol,
        durationMinutes = duration,
        rationale = map["rationale"].asString().ifBlank { "A recovery-focused protocol was selected for your current state." },
        cadence = map["cadence"].asString().ifBlank { "4 second inhale · 5 second exhale" },
        breathworkProtocol = map["breathworkProtocol"].asProtocol(duration, protocol),
    )
}

private fun Any?.asProtocol(
    fallbackDuration: Int,
    fallbackTitle: String,
): BreathworkProtocol {
    val map = asMap()
    val duration = map["durationMinutes"].asInt().takeIf { it in 1..15 } ?: fallbackDuration
    val phases = (map["phases"] as? List<*>)
        ?.mapNotNull { it.asPhaseOrNull() }
        ?.takeIf { it.isNotEmpty() }
        ?: BreathworkProtocol.postTrainingRecovery(duration).phases

    return BreathworkProtocol(
        category = map["category"].asString().ifBlank { "post_training_recovery" },
        title = map["title"].asString().ifBlank { fallbackTitle },
        durationMinutes = duration,
        phases = phases,
        ambientTrackName = map["ambientTrackName"].asString().ifBlank { "ambient_loop" },
    )
}

private fun Any?.asPhaseOrNull(): BreathPhase? {
    val map = asMap()
    val label = map["label"].asString()
    if (label !in setOf("Inhale", "Hold", "Exhale", "Rest")) return null
    val seconds = map["seconds"].asInt()
    if (seconds !in 1..20) return null
    val scaleTarget = map["scaleTarget"].asFloat()
    if (scaleTarget !in 0.5f..1.5f) return null

    return BreathPhase(
        label = label,
        seconds = seconds,
        instruction = map["instruction"].asString().ifBlank { label },
        scaleTarget = scaleTarget,
    )
}

private fun Any?.asMap(): Map<*, *> = this as? Map<*, *> ?: emptyMap<Any, Any>()

private fun Any?.asString(): String = this as? String ?: ""

private fun Any?.asInt(): Int = when (this) {
    is Number -> toInt()
    is String -> toIntOrNull() ?: 0
    else -> 0
}

private fun Any?.asFloat(): Float = when (this) {
    is Number -> toFloat()
    is String -> toFloatOrNull() ?: 0f
    else -> 0f
}
