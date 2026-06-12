package com.hybridtempo.android.recommendation

import com.hybridtempo.android.data.BreathworkProtocol
import com.hybridtempo.android.data.BreathworkRecommendation
import com.hybridtempo.android.data.seed.BreathSkillSeed
import com.hybridtempo.android.domain.model.BreathSkill
import com.hybridtempo.android.domain.model.BreathSkillCategory
import com.hybridtempo.android.domain.model.WorkoutType

class DeterministicRecommendationEngine(
    private val breathSkills: List<BreathSkill> = BreathSkillSeed.skills,
) : RecommendationEngine {
    override suspend fun recommend(request: RecommendationRequest): RecommendationResponse {
        val context = request.toSelectionContext()
        val selected = breathSkills
            .maxByOrNull { skill -> skill.score(context) }
            ?: BreathSkillSeed.skills.first { it.id == "exhale-control-basics" }

        return RecommendationResponse(
            recommendation = selected.toRecommendation(
                durationMinutes = context.durationMinutes,
                rationale = selected.rationaleFor(context),
                fallbackReason = context.fallbackReason,
            ),
            source = RecommendationSource.DeterministicFallback,
        )
    }
}

private data class SelectionContext(
    val moment: TrainingMoment,
    val workoutType: WorkoutType,
    val durationMinutes: Int,
    val highLoad: Boolean,
    val highStress: Boolean,
    val highSoreness: Boolean,
    val lowEnergy: Boolean,
    val raceContext: Boolean,
    val risingStress: Boolean,
    val fallbackReason: String,
)

private enum class TrainingMoment {
    BEFORE,
    DURING,
    AFTER,
    SLEEP,
    RESET,
}

private fun RecommendationRequest.toSelectionContext(): SelectionContext {
    val workoutType = checkIn.workoutType.toWorkoutType()
    val moment = checkIn.sessionIntent.toTrainingMoment()
    val duration = checkIn.timeAvailable.takeIf { it > 0 } ?: profile.preferredSessionLength
    val raceContext = workoutType == WorkoutType.RACE_EVENT ||
        profile.goals.any { it.contains("race", ignoreCase = true) } ||
        profile.raceName.isNotBlank()
    val fallbackReason = if (checkIn.sessionIntent.toTrainingMomentOrNull() == null) {
        "Unknown session intent '${checkIn.sessionIntent}', so the local engine used a safe reset match."
    } else {
        ""
    }

    return SelectionContext(
        moment = moment,
        workoutType = workoutType,
        durationMinutes = duration.coerceIn(3, 10),
        highLoad = checkIn.workoutIntensity >= 8 || workoutType == WorkoutType.RACE_EVENT,
        highStress = checkIn.stress >= 7,
        highSoreness = checkIn.soreness >= 7,
        lowEnergy = checkIn.energy <= 4,
        raceContext = raceContext,
        risingStress = recentTrends.stress.isRising(),
        fallbackReason = fallbackReason,
    )
}

private fun BreathSkill.score(context: SelectionContext): Int {
    var score = 0

    if (category == context.moment.preferredCategory) score += 40
    if (context.workoutType in bestForWorkoutTypes) score += 18
    if (context.durationMinutes in durationOptionsMinutes) score += 6

    score += when (id) {
        "race-event-composure" -> if (context.moment == TrainingMoment.BEFORE && context.raceContext) 36 else 0
        "avoid-early-spike" -> if (context.moment == TrainingMoment.BEFORE && context.highLoad) 30 else 0
        "tempo-rhythm-primer" -> if (
            context.moment == TrainingMoment.BEFORE &&
            context.workoutType in setOf(WorkoutType.TEMPO_THRESHOLD, WorkoutType.RUNNING, WorkoutType.CYCLING)
        ) 24 else 0
        "warmup-breath-check" -> if (context.moment == TrainingMoment.BEFORE && !context.highLoad) 16 else 0
        "between-rep-recovery" -> if (context.moment == TrainingMoment.DURING && context.highLoad) 24 else 0
        "between-set-reset" -> if (context.moment == TrainingMoment.DURING && context.workoutType == WorkoutType.STRENGTH) 24 else 0
        "breathing-under-discomfort" -> if (context.moment == TrainingMoment.DURING && context.highStress) 24 else 0
        "post-conditioning-downshift" -> if (
            context.moment == TrainingMoment.AFTER &&
            (context.highStress || context.highSoreness) &&
            context.highLoad
        ) 34 else 0
        "cooldown-hr-recovery" -> if (context.moment == TrainingMoment.AFTER && !context.lowEnergy) 30 else 0
        "recovery-day-reset" -> if (
            context.moment == TrainingMoment.AFTER &&
            (context.lowEnergy || context.workoutType == WorkoutType.EASY_RECOVERY)
        ) 26 else 0
        "evening-training-wind-down" -> if (
            context.moment == TrainingMoment.SLEEP ||
            (context.moment == TrainingMoment.AFTER && context.risingStress)
        ) 42 else 0
        "exhale-control-basics" -> if (context.moment == TrainingMoment.RESET) 24 else 0
        "breath-awareness-scan" -> if (context.moment == TrainingMoment.RESET && context.highStress) 22 else 0
        "relax-the-tension-chain" -> if (context.moment == TrainingMoment.RESET && context.highSoreness) 22 else 0
        else -> 0
    }

    return score
}

private val TrainingMoment.preferredCategory: BreathSkillCategory
    get() = when (this) {
        TrainingMoment.BEFORE -> BreathSkillCategory.BEFORE_TRAINING
        TrainingMoment.DURING -> BreathSkillCategory.DURING_TRAINING
        TrainingMoment.AFTER -> BreathSkillCategory.AFTER_TRAINING
        TrainingMoment.SLEEP -> BreathSkillCategory.AFTER_TRAINING
        TrainingMoment.RESET -> BreathSkillCategory.SKILL_BASICS
    }

private fun BreathSkill.toRecommendation(
    durationMinutes: Int,
    rationale: String,
    fallbackReason: String,
): BreathworkRecommendation = BreathworkRecommendation(
    protocol = title,
    durationMinutes = durationMinutes,
    rationale = rationale,
    cadence = cadence,
    breathworkProtocol = protocolFor(durationMinutes),
    breathSkillId = id,
    trainingCue = trainingCue,
    measurementFocus = measurementFocus,
    fallbackReason = fallbackReason,
)

private val BreathSkill.cadence: String
    get() = when (id) {
        "avoid-early-spike" -> "3 second inhale · 5 second exhale"
        "tempo-rhythm-primer" -> "3 step inhale · 4 step exhale"
        "evening-training-wind-down" -> "4 second inhale · 6 second exhale"
        "post-conditioning-downshift", "cooldown-hr-recovery" -> "4 second inhale · 6 second exhale"
        "activation" -> "3 second inhale · 3 second exhale"
        else -> "4 second inhale · 5 second exhale"
    }

private fun BreathSkill.protocolFor(durationMinutes: Int): BreathworkProtocol = when (category) {
    BreathSkillCategory.BEFORE_TRAINING -> when (id) {
        "avoid-early-spike", "race-event-composure" -> BreathworkProtocol.downregulation(durationMinutes)
        "tempo-rhythm-primer" -> BreathworkProtocol.recoveryReset(durationMinutes)
        else -> BreathworkProtocol.activation(durationMinutes)
    }
    BreathSkillCategory.DURING_TRAINING -> BreathworkProtocol.recoveryReset(durationMinutes)
    BreathSkillCategory.AFTER_TRAINING -> when (id) {
        "evening-training-wind-down" -> BreathworkProtocol.sleepTransition(durationMinutes)
        "recovery-day-reset" -> BreathworkProtocol.recoveryReset(durationMinutes)
        else -> BreathworkProtocol.downregulation(durationMinutes)
    }
    BreathSkillCategory.SKILL_BASICS -> BreathworkProtocol.recoveryReset(durationMinutes)
}

private fun BreathSkill.rationaleFor(context: SelectionContext): String {
    val momentLabel = when (context.moment) {
        TrainingMoment.BEFORE -> "before training"
        TrainingMoment.DURING -> "during training"
        TrainingMoment.AFTER -> "after training"
        TrainingMoment.SLEEP -> "before sleep"
        TrainingMoment.RESET -> "for a reset"
    }
    val loadLabel = when {
        context.highLoad && context.highStress -> "high load and high stress"
        context.highLoad -> "higher training load"
        context.highStress -> "elevated stress"
        context.lowEnergy -> "lower energy"
        else -> "your current check-in"
    }

    return "$title matches $momentLabel because $loadLabel points to this goal: $goal"
}

private fun String.toTrainingMoment(): TrainingMoment =
    toTrainingMomentOrNull() ?: TrainingMoment.RESET

private fun String.toTrainingMomentOrNull(): TrainingMoment? {
    val normalized = normalize()
    return when (normalized) {
        "preworkout", "beforeworkout", "beforetraining", "primer" -> TrainingMoment.BEFORE
        "duringworkout", "duringtraining", "midworkout", "betweensets", "betweenreps" -> TrainingMoment.DURING
        "postworkout", "afterworkout", "aftertraining", "recovery" -> TrainingMoment.AFTER
        "eveningdownshift", "sleep", "beforesleep", "winddown" -> TrainingMoment.SLEEP
        "generalreset", "reset" -> TrainingMoment.RESET
        else -> null
    }
}

private fun String.toWorkoutType(): WorkoutType {
    val normalized = normalize()
    return when {
        "race" in normalized || "event" in normalized -> WorkoutType.RACE_EVENT
        "interval" in normalized -> WorkoutType.INTERVALS
        "tempo" in normalized || "threshold" in normalized -> WorkoutType.TEMPO_THRESHOLD
        "long" in normalized || "endurance" in normalized -> WorkoutType.LONG_ENDURANCE
        "recover" in normalized || "rest" in normalized || "easy" in normalized -> WorkoutType.EASY_RECOVERY
        "strength" in normalized || "lift" in normalized -> WorkoutType.STRENGTH
        "conditioning" in normalized -> WorkoutType.CONDITIONING
        "mobility" in normalized -> WorkoutType.MOBILITY_RECOVERY
        "run" in normalized -> WorkoutType.RUNNING
        "cycle" in normalized || "bike" in normalized -> WorkoutType.CYCLING
        "row" in normalized -> WorkoutType.ROWING
        "hybrid" in normalized -> WorkoutType.HYBRID
        "team" in normalized -> WorkoutType.TEAM_SPORT
        else -> WorkoutType.OTHER
    }
}

private fun String.normalize(): String =
    lowercase().filter { it.isLetterOrDigit() }

private fun List<Int>.isRising(): Boolean {
    val values = filter { it > 0 }.take(4)
    if (values.size < 3) return false

    return values.first() > values.drop(1).average()
}
