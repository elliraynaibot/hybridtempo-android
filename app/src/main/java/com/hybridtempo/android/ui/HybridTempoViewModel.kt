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
import com.hybridtempo.android.recommendation.DeterministicRecommendationEngine
import com.hybridtempo.android.recommendation.RecentTrendContext
import com.hybridtempo.android.recommendation.RecommendationEngine
import com.hybridtempo.android.recommendation.RecommendationQuota
import com.hybridtempo.android.recommendation.RecommendationRequest
import com.hybridtempo.android.recommendation.RecommendationSource
import com.hybridtempo.android.notifications.RecoveryReminderScheduler
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
    val eveningReminderEnabled: Boolean = false,
    val eveningReminderHour: Int = 20,
    val eveningReminderMinute: Int = 30,
)

data class HybridTempoUiState(
    val profileDraft: AthleteProfileDraft = AthleteProfileDraft(),
    val hasCompletedOnboarding: Boolean = false,
    val isLoadingProfile: Boolean = true,
    val draft: CheckInDraft = CheckInDraft(),
    val recommendation: BreathworkRecommendation = com.hybridtempo.android.data.BreathworkRecommendation(),
    val recommendationSource: RecommendationSource = RecommendationSource.DeterministicFallback,
    val recommendationQuota: RecommendationQuota? = null,
    val recommendationNotice: String? = null,
    val isRefreshingRecommendation: Boolean = false,
    val recentSessions: List<BreathworkSession> = emptyList(),
    val recentCheckIns: List<DailyCheckIn> = emptyList(),
    val saveMessage: String = "Firebase persistence is ready when app/google-services.json is added.",
    val isSaving: Boolean = false,
)

class HybridTempoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FirebaseHybridTempoRepository(application.applicationContext)
    private val previewRecommendationEngine: RecommendationEngine = DeterministicRecommendationEngine()
    private val backendRecommendationEngine: RecommendationEngine = BackendRecommendationEngine()
    private val reminderScheduler = RecoveryReminderScheduler(application.applicationContext)
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
                    saveMessage = "Profile loaded from Firestore.",
                )
            }
        }
        profile?.let {
            reminderScheduler.applySettings(
                enabled = it.eveningReminderEnabled,
                hour = it.eveningReminderHour,
                minute = it.eveningReminderMinute,
            )
        }
        refreshPreviewRecommendation()
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
                    isSaving = false,
                    saveMessage = result.message,
                )
            }
            reminderScheduler.applySettings(
                enabled = profileDraft.eveningReminderEnabled,
                hour = profileDraft.eveningReminderHour,
                minute = profileDraft.eveningReminderMinute,
            )
            refreshPreviewRecommendation()
        }
    }

    fun updateDraft(draft: CheckInDraft) {
        _uiState.update {
            it.copy(
                draft = draft,
            )
        }
        refreshPreviewRecommendation()
    }

    fun requestRecommendationAndSaveCheckIn() {
        val state = _uiState.value
        val checkIn = state.draft.toDailyCheckIn()
        val request = state.toRecommendationRequest()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    isRefreshingRecommendation = true,
                    recommendationNotice = null,
                    saveMessage = "Getting recommendation and saving check-in...",
                )
            }
            val response = backendRecommendationEngine.recommend(request)
            val result = runCatching {
                repository.saveCheckIn(checkIn, response.recommendation)
            }.fold(
                onSuccess = { it },
                onFailure = { com.hybridtempo.android.data.SaveResult(false, it.message ?: "Check-in kept in memory.") },
            )
            _uiState.update {
                it.copy(
                    recommendation = response.recommendation,
                    recommendationSource = response.source,
                    recommendationQuota = response.quota,
                    recommendationNotice = response.notice,
                    isRefreshingRecommendation = false,
                    isSaving = false,
                    saveMessage = result.message,
                )
            }
            refreshCheckIns(checkIn, refreshPreview = false)
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

    private fun refreshCheckIns(
        fallbackCheckIn: DailyCheckIn? = null,
        refreshPreview: Boolean = true,
    ) {
        viewModelScope.launch {
            val checkIns = runCatching { repository.recentCheckIns() }.getOrDefault(emptyList())
            _uiState.update {
                val nextCheckIns = if (checkIns.isNotEmpty()) checkIns else listOfNotNull(fallbackCheckIn)
                it.copy(
                    recentCheckIns = nextCheckIns,
                )
            }
            if (refreshPreview) {
                refreshPreviewRecommendation()
            }
        }
    }

    private fun refreshPreviewRecommendation() {
        val request = _uiState.value.toRecommendationRequest()

        viewModelScope.launch {
            val response = previewRecommendationEngine.recommend(request)
            _uiState.update {
                it.copy(
                    recommendation = response.recommendation,
                    recommendationSource = response.source,
                    recommendationNotice = null,
                )
            }
        }
    }
}

private fun HybridTempoUiState.toRecommendationRequest(): RecommendationRequest = RecommendationRequest(
    profile = profileDraft.toProfileContext(),
    checkIn = draft.toCheckInContext(),
    recentTrends = recentCheckIns.toTrendContext(),
)

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
    eveningReminderEnabled = eveningReminderEnabled,
    eveningReminderHour = eveningReminderHour,
    eveningReminderMinute = eveningReminderMinute,
)

private fun AthleteProfile.toAthleteProfileDraft(): AthleteProfileDraft = AthleteProfileDraft(
    name = name,
    raceDate = raceDate,
    trainingStyle = trainingStyle,
    weeklyTrainingFrequency = weeklyTrainingFrequency,
    goals = goals,
    preferredSessionLength = preferredSessionLength,
    eveningReminderEnabled = eveningReminderEnabled,
    eveningReminderHour = eveningReminderHour,
    eveningReminderMinute = eveningReminderMinute,
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
