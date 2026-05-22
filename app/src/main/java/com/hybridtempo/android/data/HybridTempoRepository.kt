package com.hybridtempo.android.data

interface HybridTempoRepository {
    suspend fun currentProfile(): AthleteProfile?
    suspend fun upsertProfile(profile: AthleteProfile): SaveResult
    suspend fun saveCheckIn(checkIn: DailyCheckIn, recommendation: BreathworkRecommendation): SaveResult
    suspend fun recentCheckIns(limit: Long = 10): List<DailyCheckIn>
    suspend fun completeSession(session: BreathworkSession): SaveResult
    suspend fun recentSessions(limit: Long = 10): List<BreathworkSession>
}
