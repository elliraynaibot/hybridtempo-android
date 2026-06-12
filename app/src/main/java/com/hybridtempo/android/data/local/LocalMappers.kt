package com.hybridtempo.android.data.local

import com.hybridtempo.android.data.AthleteProfile
import com.hybridtempo.android.data.BreathworkSession
import com.hybridtempo.android.data.DailyCheckIn

fun AthleteProfile.toLocalProfileEntity(): LocalProfileEntity = LocalProfileEntity(
    name = name,
    raceName = raceName,
    raceDate = raceDate,
    trainingStyle = trainingStyle,
    weeklyTrainingFrequency = weeklyTrainingFrequency,
    goalsCsv = goals.joinToString(separator = "|"),
    preferredSessionLength = preferredSessionLength,
    eveningReminderEnabled = eveningReminderEnabled,
    eveningReminderHour = eveningReminderHour,
    eveningReminderMinute = eveningReminderMinute,
    healthConnectEnabled = healthConnectEnabled,
)

fun LocalProfileEntity.toAthleteProfile(): AthleteProfile = AthleteProfile(
    name = name,
    raceName = raceName,
    raceDate = raceDate,
    trainingStyle = trainingStyle,
    weeklyTrainingFrequency = weeklyTrainingFrequency,
    goals = goalsCsv.split("|").filter { it.isNotBlank() },
    preferredSessionLength = preferredSessionLength,
    eveningReminderEnabled = eveningReminderEnabled,
    eveningReminderHour = eveningReminderHour,
    eveningReminderMinute = eveningReminderMinute,
    healthConnectEnabled = healthConnectEnabled,
)

fun DailyCheckIn.toLocalCheckInEntity(): LocalCheckInEntity = LocalCheckInEntity(
    date = date,
    energy = energy,
    soreness = soreness,
    stress = stress,
    mood = mood,
    timeAvailable = timeAvailable,
    workoutType = workoutType,
    workoutDurationMinutes = workoutDurationMinutes,
    workoutIntensity = workoutIntensity,
    sessionIntent = sessionIntent,
    createdAt = createdAt,
)

fun LocalCheckInEntity.toDailyCheckIn(): DailyCheckIn = DailyCheckIn(
    date = date,
    energy = energy,
    soreness = soreness,
    stress = stress,
    mood = mood,
    timeAvailable = timeAvailable,
    workoutType = workoutType,
    workoutDurationMinutes = workoutDurationMinutes,
    workoutIntensity = workoutIntensity,
    sessionIntent = sessionIntent,
    createdAt = createdAt,
)

fun BreathworkSession.toLocalSessionEntity(): LocalSessionEntity = LocalSessionEntity(
    id = id.ifBlank { completedAt },
    protocol = protocol,
    durationMinutes = durationMinutes,
    cadence = cadence,
    completed = completed,
    completedAt = completedAt,
    breathSkillId = breathSkillId,
    perceivedControl = perceivedControl,
    perceivedRecovery = perceivedRecovery,
    reflectionFeeling = reflectionFeeling,
    reflectionNotes = reflectionNotes,
    sessionStartedAt = sessionStartedAt,
    sessionEndedAt = sessionEndedAt,
    heartRateBeforeBpm = heartRateBeforeBpm,
    heartRateAfterBpm = heartRateAfterBpm,
    heartRateDeltaBpm = heartRateDeltaBpm,
)

fun LocalSessionEntity.toBreathworkSession(): BreathworkSession = BreathworkSession(
    id = id,
    protocol = protocol,
    durationMinutes = durationMinutes,
    cadence = cadence,
    completed = completed,
    completedAt = completedAt,
    breathSkillId = breathSkillId,
    perceivedControl = perceivedControl,
    perceivedRecovery = perceivedRecovery,
    reflectionFeeling = reflectionFeeling,
    reflectionNotes = reflectionNotes,
    sessionStartedAt = sessionStartedAt,
    sessionEndedAt = sessionEndedAt,
    heartRateBeforeBpm = heartRateBeforeBpm,
    heartRateAfterBpm = heartRateAfterBpm,
    heartRateDeltaBpm = heartRateDeltaBpm,
)
