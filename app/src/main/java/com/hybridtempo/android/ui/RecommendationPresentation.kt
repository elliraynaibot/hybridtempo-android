package com.hybridtempo.android.ui

import com.hybridtempo.android.data.BreathworkRecommendation

data class RecommendationPresentation(
    val title: String,
    val meta: String,
    val badge: String,
    val rationale: String,
    val cadence: String,
    val trainingCue: String,
    val measurementFocus: String,
    val fallbackReason: String,
)

fun BreathworkRecommendation.toRecommendationPresentation(): RecommendationPresentation {
    val skillLabel = breathSkillId
        .takeIf { it.isNotBlank() }
        ?.let { "Skill - $it" }
        ?: "Local protocol"

    return RecommendationPresentation(
        title = protocol,
        meta = "$durationMinutes min - $cadence",
        badge = skillLabel,
        rationale = rationale,
        cadence = cadence,
        trainingCue = trainingCue.ifBlank {
            "Use this cue during the session and return to it after training."
        },
        measurementFocus = measurementFocus.ifBlank {
            "After the session, the app will ask how controlled and recovered you felt."
        },
        fallbackReason = fallbackReason,
    )
}
