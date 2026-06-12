package com.hybridtempo.android.ui

import com.hybridtempo.android.domain.model.ImportedWorkout
import com.hybridtempo.android.domain.model.TrainingIntensity
import com.hybridtempo.android.domain.model.WorkoutType
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class WorkoutPlanSummary(
    val headline: String,
    val structure: String,
    val breathWindow: String,
    val timeAvailable: String,
)

data class PlanOption(
    val label: String,
    val value: String,
)

data class IntensityOption(
    val label: String,
    val value: Int,
)

data class ImportedWorkoutPresentation(
    val title: String,
    val meta: String,
    val source: String,
)

fun workoutTypeOptions(): List<String> = listOf(
    "Strength",
    "Intervals",
    "Conditioning",
    "Run",
    "Recovery",
)

fun intensityOptions(): List<IntensityOption> = listOf(
    IntensityOption("Easy", 3),
    IntensityOption("Moderate", 5),
    IntensityOption("Hard", 7),
    IntensityOption("Max", 9),
)

fun breathWindowOptions(): List<PlanOption> = listOf(
    PlanOption("Before effort", "pre_workout"),
    PlanOption("Between sets", "between_sets"),
    PlanOption("After workout", "post_workout"),
    PlanOption("Before sleep", "evening_downshift"),
)

fun CheckInDraft.toWorkoutPlanSummary(): WorkoutPlanSummary = WorkoutPlanSummary(
    headline = "$workoutType · ${workoutIntensity.toIntensityLabel()}",
    structure = workoutStructureLabel(),
    breathWindow = sessionIntent.toBreathWindowLabel(),
    timeAvailable = "$timeAvailable min available",
)

fun guideModeForSessionIntent(sessionIntent: String): GuideModePresentation {
    val target = when (sessionIntent) {
        "pre_workout" -> "pre_set"
        "between_sets" -> "between_sets"
        "post_workout", "evening_downshift" -> "post_workout"
        else -> "learn"
    }

    return defaultGuideModes().first { it.value == target }
}

fun Int.toIntensityLabel(): String = when {
    this >= 9 -> "Max"
    this >= 7 -> "Hard"
    this >= 5 -> "Moderate"
    else -> "Easy"
}

fun String.toBreathWindowLabel(): String = breathWindowOptions()
    .firstOrNull { it.value == this }
    ?.label
    ?: "Reset"

fun ImportedWorkout.toCheckInDraft(current: CheckInDraft): CheckInDraft = current.copy(
    workoutType = workoutType.toPlanWorkoutLabel(),
    workoutIntensity = intensity.toPlanIntensity(),
    sessionIntent = "post_workout",
)

fun ImportedWorkout.toImportedWorkoutPresentation(): ImportedWorkoutPresentation {
    val minutes = Duration.between(startedAt, endedAt).toMinutes().coerceAtLeast(1)
    val endedLabel = DateTimeFormatter.ofPattern("h:mm a")
        .withZone(ZoneId.systemDefault())
        .format(endedAt)

    return ImportedWorkoutPresentation(
        title = workoutType.toPlanWorkoutLabel(),
        meta = "$minutes min · finished $endedLabel",
        source = source,
    )
}

private fun CheckInDraft.workoutStructureLabel(): String = when (workoutType) {
    "Strength", "Conditioning" -> "$setCount sets · $repsPerSet reps"
    "Intervals", "Run" -> "$intervalCount rounds · $intervalMinutes min work"
    "Recovery" -> "No sets · easy reset"
    else -> "$setCount blocks · $intervalMinutes min focus"
}

private fun WorkoutType.toPlanWorkoutLabel(): String = when (this) {
    WorkoutType.STRENGTH -> "Strength"
    WorkoutType.INTERVALS -> "Intervals"
    WorkoutType.CONDITIONING -> "Conditioning"
    WorkoutType.RUNNING,
    WorkoutType.TEMPO_THRESHOLD,
    WorkoutType.LONG_ENDURANCE -> "Run"
    WorkoutType.CYCLING -> "Cycling"
    WorkoutType.ROWING -> "Conditioning"
    WorkoutType.EASY_RECOVERY,
    WorkoutType.MOBILITY_RECOVERY -> "Recovery"
    WorkoutType.HYBRID,
    WorkoutType.RACE_EVENT -> "Conditioning"
    WorkoutType.TEAM_SPORT,
    WorkoutType.OTHER -> "Conditioning"
}

private fun TrainingIntensity?.toPlanIntensity(): Int = when (this) {
    TrainingIntensity.EASY -> 3
    TrainingIntensity.MODERATE -> 5
    TrainingIntensity.HARD -> 7
    TrainingIntensity.MAX_EFFORT_COMPETITION -> 9
    null -> 5
}
