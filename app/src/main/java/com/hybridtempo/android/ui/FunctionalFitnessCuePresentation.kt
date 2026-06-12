package com.hybridtempo.android.ui

data class TodayCuePresentation(
    val workoutFormat: String,
    val breathingProblem: String,
    val category: String,
    val cue: String,
    val why: String,
    val practice: String,
    val reviewQuestion: String,
    val practiceAction: String = "Start cue practice",
)

data class WorkoutHandoffPresentation(
    val title: String,
    val workoutInstruction: String,
    val reviewInstruction: String,
    val reviewQuestion: String,
)

fun workoutFormatOptions(): List<String> = listOf(
    "AMRAP",
    "EMOM",
    "For time",
    "Intervals",
    "Strength + conditioning",
    "Cooldown",
)

fun breathingProblemOptions(): List<String> = listOf(
    "I start too fast",
    "I hold my breath",
    "I lose rhythm",
    "I can't recover between movements",
    "I panic when it gets hard",
    "I tense my shoulders/jaw",
    "I breathe too shallow",
    "I fall apart late",
    "I struggle during transitions",
    "I can't calm down after",
)

fun workoutFormatDisplayLabel(value: String): String = when (value) {
    "Strength + conditioning" -> "Strength +\nconditioning"
    else -> value
}

fun breathingProblemDisplayLabel(value: String): String = when (value) {
    "I can't recover between movements" -> "Can't recover\nbetween moves"
    "I panic when it gets hard" -> "Panic when\nit gets hard"
    "I tense my shoulders/jaw" -> "Tense\nshoulders/jaw"
    "I breathe too shallow" -> "Breathe\ntoo shallow"
    "I struggle during transitions" -> "Struggle in\ntransitions"
    "I can't calm down after" -> "Can't calm\ndown after"
    else -> value
}

fun String.toWorkoutTypeLabel(): String = when (this) {
    "Intervals" -> "Intervals"
    "Strength + conditioning" -> "Strength"
    "Cooldown" -> "Recovery"
    else -> "Conditioning"
}

fun String.toDefaultSessionIntent(): String = when (this) {
    "EMOM", "Intervals", "Strength + conditioning" -> "between_sets"
    "Cooldown" -> "post_workout"
    else -> "pre_workout"
}

fun TodayCuePresentation.toWorkoutHandoffPresentation(): WorkoutHandoffPresentation {
    return WorkoutHandoffPresentation(
        title = "Take this cue to the workout",
        workoutInstruction = "Use \"${cue}\" when ${breathingProblem.lowercase()} starts to show up.",
        reviewInstruction = "After training, come back and log whether it helped under fatigue.",
        reviewQuestion = reviewQuestion,
    )
}

fun CheckInDraft.forGuidedMoment(sessionIntent: String): CheckInDraft {
    return when (sessionIntent) {
        "pre_workout" -> copy(
            sessionIntent = "pre_workout",
            workoutFormat = "Before workout",
            workoutType = "Preparation",
            breathingProblem = "I need to get organized before training",
        )

        "post_workout" -> copy(
            sessionIntent = "post_workout",
            workoutFormat = "After workout",
            workoutType = "Recovery",
            breathingProblem = "I need to downshift after training",
        )

        else -> copy(sessionIntent = sessionIntent)
    }
}

fun CheckInDraft.toTodayCuePresentation(): TodayCuePresentation {
    when (sessionIntent) {
        "pre_workout" -> return TodayCuePresentation(
            workoutFormat = "Before workout",
            breathingProblem = "Prepare before effort",
            category = "Before workout",
            cue = "Brace & Breathe",
            why = "Before training, the goal is to organize posture, breathing, and tension before the workout asks for intensity.",
            practice = "Start Brace & Breathe. Use it to feel tall, braced, and able to breathe again before hard work begins.",
            reviewQuestion = "Did you feel more organized when the workout started?",
            practiceAction = "Start Brace & Breathe",
        )

        "post_workout" -> return TodayCuePresentation(
            workoutFormat = "After workout",
            breathingProblem = "Recover after effort",
            category = "After workout",
            cue = "Reset After the Session",
            why = "After training, the priority is bringing your breathing and arousal down before you move on with the day.",
            practice = "Start Reset After the Session. Use it to lengthen the exhale and close the workout with control.",
            reviewQuestion = "Did your breathing feel calmer after the reset?",
            practiceAction = "Start Reset",
        )
    }

    val cue = when {
        breathingProblem == "I start too fast" -> CueRule(
            category = "Pacing",
            cue = "First round smooth.",
            why = "This protects the early part of the workout so you do not burn control before fatigue builds.",
            practice = "2-minute pacing breath drill: inhale easy, long exhale, then repeat the cue before starting.",
            reviewQuestion = "Did the cue help you stay controlled past halfway?",
        )

        breathingProblem == "I can't recover between movements" ||
            workoutFormat in setOf("EMOM", "Intervals") -> CueRule(
                category = "Recovery",
                cue = "Long exhale before the next effort.",
                why = "$workoutFormat gives you built-in rest or transitions. Use that space to regain control.",
                practice = "2-minute reset drill: one long exhale, relaxed jaw, then resume normal breathing.",
                reviewQuestion = "Did you recover faster before the next effort?",
            )

        breathingProblem == "I hold my breath" -> CueRule(
            category = "Bracing",
            cue = "Brace, move, breathe again.",
            why = "Functional fitness often demands tension, but breath-holding through every rep makes fatigue arrive faster.",
            practice = "2-minute brace drill: exhale, brace for one rep, then breathe again before the next rep.",
            reviewQuestion = "Did you breathe again after each braced effort?",
        )

        breathingProblem == "I lose rhythm" || breathingProblem == "I breathe too shallow" -> CueRule(
            category = "Rhythm",
            cue = "Find the rhythm before chasing pace.",
            why = "Mixed-modal workouts get messy when your movement pace outruns your breathing rhythm.",
            practice = "2-minute rhythm drill: steady inhale, longer exhale, repeat at the pace you want to hold.",
            reviewQuestion = "Did your breathing stay rhythmic when fatigue built?",
        )

        breathingProblem == "I struggle during transitions" -> CueRule(
            category = "Transition",
            cue = "One reset breath before the next station.",
            why = "Transitions are the easiest place to regain control without stopping the workout.",
            practice = "2-minute transition drill: step, exhale, relax your jaw, then move.",
            reviewQuestion = "Did transitions help you reset instead of spiral?",
        )

        breathingProblem == "I panic when it gets hard" || breathingProblem == "I fall apart late" -> CueRule(
            category = "Composure",
            cue = "Control the next three breaths.",
            why = "Late-workout fatigue feels bigger when the goal is the whole workout. Narrow it to the next three breaths.",
            practice = "2-minute composure drill: count three controlled breaths, then restart the count.",
            reviewQuestion = "Did the cue help you stay composed late?",
        )

        breathingProblem == "I can't calm down after" || workoutFormat == "Cooldown" -> CueRule(
            category = "Cooldown",
            cue = "Slow the exhale first.",
            why = "After training, the fastest win is shifting from rushed breathing to a longer exhale.",
            practice = "2-minute cooldown drill: inhale normally, then make each exhale slightly longer.",
            reviewQuestion = "Did your breathing settle before leaving the gym?",
        )

        else -> CueRule(
            category = "Composure",
            cue = "Breathe before speed.",
            why = "Hard workouts expose breath control. This cue keeps the next effort simple.",
            practice = "2-minute control drill: long exhale, relaxed face, repeat the cue.",
            reviewQuestion = "Did the cue help when the workout got messy?",
        )
    }

    return TodayCuePresentation(
        workoutFormat = workoutFormat,
        breathingProblem = breathingProblem,
        category = cue.category,
        cue = cue.cue,
        why = cue.why,
        practice = cue.practice,
        reviewQuestion = cue.reviewQuestion,
    )
}

private data class CueRule(
    val category: String,
    val cue: String,
    val why: String,
    val practice: String,
    val reviewQuestion: String,
)
