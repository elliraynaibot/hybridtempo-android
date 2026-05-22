package com.hybridtempo.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hybridtempo.android.data.AthleteProfile
import com.hybridtempo.android.data.BreathworkRecommendation
import com.hybridtempo.android.data.BreathworkSession
import com.hybridtempo.android.data.DailyCheckIn
import com.hybridtempo.android.data.FirebaseHybridTempoRepository
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

data class HybridTempoUiState(
    val draft: CheckInDraft = CheckInDraft(),
    val recommendation: BreathworkRecommendation = buildRecommendation(CheckInDraft()),
    val recentSessions: List<BreathworkSession> = emptyList(),
    val saveMessage: String = "Firebase persistence is ready when app/google-services.json is added.",
    val isSaving: Boolean = false,
)

class HybridTempoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FirebaseHybridTempoRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(HybridTempoUiState())
    val uiState: StateFlow<HybridTempoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.upsertProfile(AthleteProfile()) }
                .onSuccess { result -> _uiState.update { it.copy(saveMessage = result.message) } }
            refreshHistory()
        }
    }

    fun updateDraft(draft: CheckInDraft) {
        _uiState.update {
            it.copy(
                draft = draft,
                recommendation = buildRecommendation(draft),
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
}

fun buildRecommendation(draft: CheckInDraft): BreathworkRecommendation {
    val highLoad = draft.workoutIntensity >= 7 || draft.soreness >= 7
    val highStress = draft.stress >= 7
    val lowEnergy = draft.energy <= 4

    return when {
        highLoad && highStress -> BreathworkRecommendation(
            protocol = "Downregulation",
            durationMinutes = draft.timeAvailable,
            rationale = "High training load plus stress calls for extended exhales and a fast shift out of sympathetic drive.",
            cadence = "4 second inhale · 6 second exhale",
        )

        lowEnergy && draft.workoutType == "Recovery" -> BreathworkRecommendation(
            protocol = "Recovery reset",
            durationMinutes = draft.timeAvailable,
            rationale = "Low energy on a lighter day points to a calm reset instead of more stimulation.",
            cadence = "4 second inhale · 4 second exhale",
        )

        draft.workoutIntensity <= 4 && draft.energy >= 7 -> BreathworkRecommendation(
            protocol = "Activation",
            durationMinutes = draft.timeAvailable,
            rationale = "Your recovery cost is low and energy is available, so the session can sharpen focus without overloading you.",
            cadence = "3 second inhale · 3 second exhale",
        )

        else -> BreathworkRecommendation(
            protocol = "Post-training recovery",
            durationMinutes = draft.timeAvailable,
            rationale = "Your check-in suggests moderate load. This keeps the protocol steady, controlled, and recovery-oriented.",
            cadence = "4 second inhale · 5 second exhale",
        )
    }
}

private fun CheckInDraft.toDailyCheckIn(): DailyCheckIn = DailyCheckIn(
    energy = energy,
    soreness = soreness,
    stress = stress,
    timeAvailable = timeAvailable,
    workoutType = workoutType,
    workoutIntensity = workoutIntensity,
)
