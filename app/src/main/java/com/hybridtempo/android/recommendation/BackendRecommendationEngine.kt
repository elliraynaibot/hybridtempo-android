package com.hybridtempo.android.recommendation

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
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
    private val authProvider: () -> FirebaseAuth? = {
        runCatching { FirebaseApp.getInstance() }.getOrNull()
            ?.let { FirebaseAuth.getInstance() }
    },
) : RecommendationEngine {
    override suspend fun recommend(request: RecommendationRequest): RecommendationResponse {
        val functions = runCatching { functionsProvider() }.getOrNull()
            ?: return fallback.recommend(request)
        val auth = runCatching { authProvider() }.getOrNull()
            ?: return fallbackWithNotice("AI backend unavailable because Firebase Auth is not configured.", request)

        return try {
            auth.ensureSignedIn()
            val result = functions
                .getHttpsCallable("recommendBreathwork")
                .call(request.toCallableMap())
                .await()
            val data = result.data.asMap()

            RecommendationResponse(
                recommendation = data.asRecommendation(),
                source = RecommendationSource.BackendAi,
                quota = data["quota"].asQuotaOrNull(),
            )
        } catch (error: Throwable) {
            val fallbackResponse = fallback.recommend(request)
            if (error.isDailyLimitError()) {
                fallbackResponse.copy(
                    source = RecommendationSource.DailyLimitReached,
                    quota = (error as FirebaseFunctionsException).details.asQuotaOrNull()
                        ?: RecommendationQuota(limit = 5, used = 5, remaining = 0, resetDate = ""),
                    notice = error.message
                        ?: "You have used today's AI recommendations. Use the local protocol for now and check back tomorrow.",
                )
            } else {
                fallbackResponse.copy(notice = error.toBackendFallbackNotice())
            }
        }
    }

    private suspend fun fallbackWithNotice(
        notice: String,
        request: RecommendationRequest,
    ): RecommendationResponse = fallback.recommend(request).copy(notice = notice)
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

private fun Any?.asQuotaOrNull(): RecommendationQuota? {
    val map = asMap()
    val limit = map["limit"].asInt()
    if (limit <= 0) return null

    return RecommendationQuota(
        limit = limit,
        used = map["used"].asInt().coerceAtLeast(0),
        remaining = map["remaining"].asInt().coerceAtLeast(0),
        resetDate = map["resetDate"].asString(),
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

private fun Throwable.isDailyLimitError(): Boolean =
    this is FirebaseFunctionsException && code == FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED

private fun Throwable.toBackendFallbackNotice(): String {
    if (this is FirebaseFunctionsException) {
        val message = message.orEmpty().ifBlank { "No backend message returned." }
        return "AI backend unavailable (${code.name.lowercase()}): $message Using the local protocol for this check-in."
    }

    val message = message.orEmpty().ifBlank { this::class.java.simpleName }
    return "AI backend unavailable: $message. Using the local protocol for this check-in."
}

private suspend fun FirebaseAuth.ensureSignedIn() {
    if (currentUser != null) return
    signInAnonymously().await()
}
