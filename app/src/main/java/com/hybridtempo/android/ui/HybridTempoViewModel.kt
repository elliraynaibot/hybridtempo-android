package com.hybridtempo.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hybridtempo.android.data.AthleteProfile
import com.hybridtempo.android.data.BreathworkRecommendation
import com.hybridtempo.android.data.BreathworkSession
import com.hybridtempo.android.data.DailyCheckIn
import com.hybridtempo.android.data.FirebaseHybridTempoRepository
import com.hybridtempo.android.recommendation.AthleteProfileContext
import com.hybridtempo.android.recommendation.BackendRecommendationEngine
import com.hybridtempo.android.recommendation.CheckInContext
import com.hybridtempo.android.recommendation.RecentTrendContext
import com.hybridtempo.android.recommendation.RecommendationEngine
import com.hybridtempo.android.recommendation.RecommendationRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckInDraft(
    val energy: Int = 6,
    val soreness: Int = 4,
    val stress: Int = 5,
    val workoutType: String = "Hybrid",
    val workoutIntensity: Int = 7,
    val timeAvailable: Int = 5,
)

data class AthleteProfileDraft(
    val name: String = "",
    val raceDate: String = "",
    val trainingStyle: String = "Hybrid",
    val weeklyTrainingFrequency: Int = 5,
    val goals: List<String> = listOf("recovery", "race prep"),
    val preferredSessionLength: Int = 5,
)

data class HybridTempoUiState(
    val profileDraft: AthleteProfileDraft = AthleteProfileDraft(),
    val hasCompletedOnboarding: Boolean = false,
    val isLoadingProfile: Boolean = true,
    val draft: CheckInDraft = CheckInDraft(),
    val recommendation: BreathworkRecommendation = BackendRecommendationEngine().recommend(
        RecommendationRequest(
            profile = AthleteProfileDraft().toProfileContext(),
            checkIn = CheckInDraft().toCheckInContext(),
        ),
    ).recommendation,
    val recentSessions: List<BreathworkSession> = emptyList(),
    val recentCheckIns: List<DailyCheckIn> = emptyList(),
    val saveMessage: String = "Firebase persistence is ready when app/google-services.json is added.",
    val isSaving: Boolean = false,
)

class HybridTempoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FirebaseHybridTempoRepository(application.applicationContext)
    private val recommendationEngine: RecommendationEngine = BackendRecommendationEngine()
    private val _uiState = MutableStateFlow(HybridTempoUiState())
    val uiState: StateFlow<HybridTempoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadProfile()
            refreshHistory()
            refreshCheckIns()
        }
    }

    private suspend fun loadProfile() {
        val profile = runCatching { repository.currentProfile() }.getOrNull()
        _uiState.update { current ->
            if (profile == null) {
                current.copy(isLoadingProfile = false)
            } else {
                val profileDraft = profile.toAthleteProfileDraft()
                val checkInDraft = current.draft.copy(timeAvailable = profile.preferredSessionLength)
                current.copy(
                    profileDraft = profileDraft,
                    hasCompletedOnboarding = true,
                    isLoadingProfile = false,
                    draft = checkInDraft,
                    recommendation = recommendationFor(checkInDraft, profileDraft, current.recentCheckIns),
                    saveMessage = "Profile loaded from Firestore.",
                )
            }
        }
    }

    fun updateProfileDraft(draft: AthleteProfileDraft) {
        _uiState.update { it.copy(profileDraft = draft) }
    }

    fun saveProfile() {
        val profileDraft = _uiState.value.profileDraft
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = runCatching {
                repository.upsertProfile(profileDraft.toAthleteProfile())
            }.fold(
                onSuccess = { it },
                onFailure = { com.hybridtempo.android.data.SaveResult(false, it.message ?: "Profile kept in memory.") },
            )
            _uiState.update {
                val nextDraft = it.draft.copy(timeAvailable = profileDraft.preferredSessionLength)
                it.copy(
                    hasCompletedOnboarding = true,
                    draft = nextDraft,
                    recommendation = recommendationFor(nextDraft, profileDraft, it.recentCheckIns),
                    isSaving = false,
                    saveMessage = result.message,
                )
            }
        }
    }

    fun updateDraft(draft: CheckInDraft) {
        _uiState.update {
            it.copy(
                draft = draft,
                recommendation = recommendationFor(draft, it.profileDraft, it.recentCheckIns),
            )
        }
    }

    fun saveCheckIn() {
        val state = _uiState.value
        val checkIn = state.draft.toDailyCheckIn()
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = runCatching {
                repository.saveCheckIn(checkIn, state.recommendation)
            }.fold(
                onSuccess = { it },
                onFailure = { com.hybridtempo.android.data.SaveResult(false, it.message ?: "Check-in kept in memory.") },
            )
            _uiState.update {
                it.copy(
                    isSaving = false,
                    saveMessage = result.message,
                )
            }
            refreshCheckIns(checkIn)
        }
    }

    fun completeCurrentSession() {
        val recommendation = _uiState.value.recommendation
        val session = BreathworkSession(
            protocol = recommendation.protocol,
            durationMinutes = recommendation.durationMinutes,
            cadence = recommendation.cadence,
            completed = true,
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = runCatching {
                repository.completeSession(session)
            }.fold(
                onSuccess = { it },
                onFailure = { com.hybridtempo.android.data.SaveResult(false, it.message ?: "Session kept in memory.") },
            )
            refreshHistory(session)
            _uiState.update {
                it.copy(
                    isSaving = false,
                    saveMessage = result.message,
                )
            }
        }
    }

    fun refreshHistory(fallbackSession: BreathworkSession? = null) {
        viewModelScope.launch {
            val sessions = runCatching { repository.recentSessions() }.getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    recentSessions = if (sessions.isNotEmpty()) sessions else listOfNotNull(fallbackSession),
                )
            }
        }
    }

    private fun refreshCheckIns(fallbackCheckIn: DailyCheckIn? = null) {
        viewModelScope.launch {
            val checkIns = runCatching { repository.recentCheckIns() }.getOrDefault(emptyList())
            _uiState.update {
                val nextCheckIns = if (checkIns.isNotEmpty()) checkIns else listOfNotNull(fallbackCheckIn)
                it.copy(
                    recentCheckIns = nextCheckIns,
                    recommendation = recommendationFor(it.draft, it.profileDraft, nextCheckIns),
                )
            }
        }
    }

    private fun recommendationFor(
        draft: CheckInDraft,
        profile: AthleteProfileDraft,
        recentCheckIns: List<DailyCheckIn>,
    ): BreathworkRecommendation = recommendationEngine.recommend(
        RecommendationRequest(
            profile = profile.toProfileContext(),
            checkIn = draft.toCheckInContext(),
            recentTrends = recentCheckIns.toTrendContext(),
        ),
    ).recommendation
}

private fun CheckInDraft.toDailyCheckIn(): DailyCheckIn = DailyCheckIn(
    energy = energy,
    soreness = soreness,
    stress = stress,
    timeAvailable = timeAvailable,
    workoutType = workoutType,
    workoutIntensity = workoutIntensity,
)

private fun CheckInDraft.toCheckInContext(): CheckInContext = CheckInContext(
    energy = energy,
    soreness = soreness,
    stress = stress,
    workoutType = workoutType,
    workoutIntensity = workoutIntensity,
    timeAvailable = timeAvailable,
)

private fun AthleteProfileDraft.toAthleteProfile(): AthleteProfile = AthleteProfile(
    name = name,
    raceDate = raceDate,
    trainingStyle = trainingStyle,
    weeklyTrainingFrequency = weeklyTrainingFrequency,
    goals = goals,
    preferredSessionLength = preferredSessionLength,
)

private fun AthleteProfile.toAthleteProfileDraft(): AthleteProfileDraft = AthleteProfileDraft(
    name = name,
    raceDate = raceDate,
    trainingStyle = trainingStyle,
    weeklyTrainingFrequency = weeklyTrainingFrequency,
    goals = goals,
    preferredSessionLength = preferredSessionLength,
)

private fun AthleteProfileDraft.toProfileContext(): AthleteProfileContext = AthleteProfileContext(
    trainingStyle = trainingStyle,
    weeklyTrainingFrequency = weeklyTrainingFrequency,
    goals = goals,
    preferredSessionLength = preferredSessionLength,
    raceDate = raceDate,
)

private fun List<DailyCheckIn>.toTrendContext(): RecentTrendContext = RecentTrendContext(
    energy = map { it.energy }.filter { it > 0 }.take(7),
    soreness = map { it.soreness }.filter { it > 0 }.take(7),
    stress = map { it.stress }.filter { it > 0 }.take(7),
)
