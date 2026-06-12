package com.hybridtempo.android.ui

data class WorkoutReviewDraft(
    val cueHelpfulness: Int = 0,
    val breathControl: Int = 0,
    val notes: String = "",
) {
    val isComplete: Boolean
        get() = cueHelpfulness in 1..10 && breathControl in 1..10
}

data class WorkoutReviewPresentation(
    val cue: String,
    val reviewQuestion: String,
    val scoreSummary: String,
    val notes: String,
)

fun WorkoutReviewDraft.toWorkoutReviewPresentation(cue: TodayCuePresentation): WorkoutReviewPresentation =
    WorkoutReviewPresentation(
        cue = cue.cue,
        reviewQuestion = cue.reviewQuestion,
        scoreSummary = "Cue $cueHelpfulness/10 - Breath control $breathControl/10",
        notes = notes.trim(),
    )
