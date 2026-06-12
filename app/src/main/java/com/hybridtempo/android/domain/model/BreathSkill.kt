package com.hybridtempo.android.domain.model

enum class BreathSkillCategory {
    BEFORE_TRAINING,
    DURING_TRAINING,
    AFTER_TRAINING,
    SKILL_BASICS,
}

enum class BreathSkillDifficulty {
    BASIC,
    INTERMEDIATE,
}

enum class WorkoutType {
    INTERVALS,
    TEMPO_THRESHOLD,
    LONG_ENDURANCE,
    EASY_RECOVERY,
    STRENGTH,
    CONDITIONING,
    RACE_EVENT,
    MOBILITY_RECOVERY,
    RUNNING,
    CYCLING,
    ROWING,
    HYBRID,
    TEAM_SPORT,
    OTHER,
}

data class BreathSkill(
    val id: String,
    val title: String,
    val category: BreathSkillCategory,
    val difficulty: BreathSkillDifficulty,
    val athleteProblem: String,
    val goal: String,
    val instructions: List<String>,
    val trainingCue: String,
    val measurementFocus: String,
    val safetyNotes: String,
    val durationOptionsMinutes: List<Int>,
    val bestForWorkoutTypes: Set<WorkoutType>,
)
