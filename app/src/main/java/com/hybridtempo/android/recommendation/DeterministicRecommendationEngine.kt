package com.hybridtempo.android.recommendation

import com.hybridtempo.android.data.BreathworkProtocol
import com.hybridtempo.android.data.BreathworkRecommendation

class DeterministicRecommendationEngine : RecommendationEngine {
    override suspend fun recommend(request: RecommendationRequest): RecommendationResponse {
        val checkIn = request.checkIn
        val profile = request.profile
        val highLoad = checkIn.workoutIntensity >= 7 || checkIn.soreness >= 7
        val highStress = checkIn.stress >= 7
        val lowEnergy = checkIn.energy <= 4
        val wantsSleepSupport = "sleep support" in profile.goals
        val risingStress = request.recentTrends.stress.isRising()

        val recommendation = when {
            checkIn.sessionIntent == "pre_workout" -> BreathworkRecommendation(
                protocol = "Activation",
                durationMinutes = checkIn.timeAvailable,
                rationale = "You chose pre-workout breathwork, so this session is designed to sharpen focus and controlled arousal before training.",
                cadence = "3 second inhale · 3 second exhale",
                breathworkProtocol = BreathworkProtocol.activation(checkIn.timeAvailable),
            )

            checkIn.sessionIntent == "evening_downshift" -> BreathworkRecommendation(
                protocol = "Sleep transition",
                durationMinutes = checkIn.timeAvailable,
                rationale = "You chose evening breathwork, so this session emphasizes longer exhales to help shift toward sleep and recovery.",
                cadence = "4 second inhale · 7 second exhale",
                breathworkProtocol = BreathworkProtocol.sleepTransition(checkIn.timeAvailable),
            )

            checkIn.sessionIntent == "general_reset" -> BreathworkRecommendation(
                protocol = "Recovery reset",
                durationMinutes = checkIn.timeAvailable,
                rationale = "You chose a general reset, so this keeps the cadence balanced and restorative without assuming a workout just happened.",
                cadence = "4 second inhale · 4 second exhale",
                breathworkProtocol = BreathworkProtocol.recoveryReset(checkIn.timeAvailable),
            )

            checkIn.sessionIntent == "post_workout" && highLoad && highStress -> BreathworkRecommendation(
                protocol = "Downregulation",
                durationMinutes = checkIn.timeAvailable,
                rationale = "You chose post-workout breathwork and your load/stress is high, so this uses extended exhales to downshift.",
                cadence = "4 second inhale · 6 second exhale",
                breathworkProtocol = BreathworkProtocol.downregulation(checkIn.timeAvailable),
            )

            checkIn.sessionIntent == "post_workout" -> BreathworkRecommendation(
                protocol = "Post-training recovery",
                durationMinutes = checkIn.timeAvailable,
                rationale = "You chose post-workout breathwork, so this focuses on a steady transition into recovery.",
                cadence = "4 second inhale · 5 second exhale",
                breathworkProtocol = BreathworkProtocol.postTrainingRecovery(checkIn.timeAvailable),
            )

            wantsSleepSupport && (highStress || risingStress) -> BreathworkRecommendation(
                protocol = "Sleep transition",
                durationMinutes = checkIn.timeAvailable,
                rationale = "Your goals include sleep support and stress is elevated, so this shifts the body toward a calmer night state.",
                cadence = "4 second inhale · 7 second exhale",
                breathworkProtocol = BreathworkProtocol.sleepTransition(checkIn.timeAvailable),
            )

            highLoad && highStress -> BreathworkRecommendation(
                protocol = "Downregulation",
                durationMinutes = checkIn.timeAvailable,
                rationale = "High training load plus stress calls for extended exhales and a fast shift out of sympathetic drive.",
                cadence = "4 second inhale · 6 second exhale",
                breathworkProtocol = BreathworkProtocol.downregulation(checkIn.timeAvailable),
            )

            lowEnergy && checkIn.workoutType == "Recovery" -> BreathworkRecommendation(
                protocol = "Recovery reset",
                durationMinutes = checkIn.timeAvailable,
                rationale = "Low energy on a lighter day points to a calm reset instead of more stimulation.",
                cadence = "4 second inhale · 4 second exhale",
                breathworkProtocol = BreathworkProtocol.recoveryReset(checkIn.timeAvailable),
            )

            checkIn.workoutIntensity <= 4 && checkIn.energy >= 7 -> BreathworkRecommendation(
                protocol = "Activation",
                durationMinutes = checkIn.timeAvailable,
                rationale = "Your recovery cost is low and energy is available, so the session can sharpen focus without overloading you.",
                cadence = "3 second inhale · 3 second exhale",
                breathworkProtocol = BreathworkProtocol.activation(checkIn.timeAvailable),
            )

            else -> BreathworkRecommendation(
                protocol = "Post-training recovery",
                durationMinutes = checkIn.timeAvailable,
                rationale = "Your check-in suggests moderate load. This keeps the protocol steady, controlled, and recovery-oriented.",
                cadence = "4 second inhale · 5 second exhale",
                breathworkProtocol = BreathworkProtocol.postTrainingRecovery(checkIn.timeAvailable),
            )
        }

        return RecommendationResponse(
            recommendation = recommendation,
            source = RecommendationSource.DeterministicFallback,
        )
    }
}

private fun List<Int>.isRising(): Boolean {
    val values = filter { it > 0 }.take(4)
    if (values.size < 3) return false

    return values.first() > values.drop(1).average()
}
