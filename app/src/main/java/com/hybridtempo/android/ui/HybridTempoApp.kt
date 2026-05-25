package com.hybridtempo.android.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.hybridtempo.android.audio.AmbientAudioController
import com.hybridtempo.android.data.BreathPhase
import com.hybridtempo.android.data.BreathworkProtocol
import com.hybridtempo.android.data.BreathworkRecommendation
import com.hybridtempo.android.data.BreathworkSession
import com.hybridtempo.android.data.DailyCheckIn
import com.hybridtempo.android.health.HealthConnectAvailability
import com.hybridtempo.android.health.HealthConnectManager
import com.hybridtempo.android.health.HealthConnectUiStatus
import com.hybridtempo.android.readiness.RaceCountdown
import com.hybridtempo.android.readiness.RaceCountdownCalculator
import com.hybridtempo.android.readiness.ReadinessCalculator
import com.hybridtempo.android.readiness.ReadinessScore
import com.hybridtempo.android.recommendation.RecommendationQuota
import com.hybridtempo.android.recommendation.RecommendationSource
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import kotlinx.coroutines.delay

private enum class AppScreen {
    OnboardingStep1,
    OnboardingStep2,
    OnboardingStep3,
    Home,
    CheckIn,
    Recommendation,
    Session,
    History,
}

private val AppScreen.isOnboarding: Boolean
    get() = this in setOf(
        AppScreen.OnboardingStep1,
        AppScreen.OnboardingStep2,
        AppScreen.OnboardingStep3,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HybridTempoApp(viewModel: HybridTempoViewModel = viewModel()) {
    var screen by remember { mutableStateOf(AppScreen.OnboardingStep1) }
    var showSettings by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val healthConnectLauncher = rememberLauncherForActivityResult(
        contract = HealthConnectManager.permissionsContract(),
    ) { grantedPermissions ->
        viewModel.onHealthConnectPermissionsResult(grantedPermissions)
    }

    LaunchedEffect(uiState.hasCompletedOnboarding, uiState.isLoadingProfile) {
        if (!uiState.isLoadingProfile && uiState.hasCompletedOnboarding && screen.isOnboarding) {
            screen = AppScreen.Home
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                    ),
                ),
        ) {
            AnimatedContent(
                targetState = screen,
                label = "screen",
                modifier = Modifier.fillMaxSize(),
            ) { target ->
                when (target) {
                    AppScreen.OnboardingStep1 -> OnboardingStep1Screen(
                        state = uiState.profileDraft,
                        isLoading = uiState.isLoadingProfile,
                        onStateChange = viewModel::updateProfileDraft,
                        onNext = { screen = AppScreen.OnboardingStep2 },
                    )

                    AppScreen.OnboardingStep2 -> OnboardingStep2Screen(
                        state = uiState.profileDraft,
                        onStateChange = viewModel::updateProfileDraft,
                        onBack = { screen = AppScreen.OnboardingStep1 },
                        onNext = { screen = AppScreen.OnboardingStep3 },
                    )

                    AppScreen.OnboardingStep3 -> OnboardingStep3Screen(
                        state = uiState.profileDraft,
                        healthConnectStatus = uiState.healthConnectStatus,
                        onStateChange = viewModel::updateProfileDraft,
                        onConnectHealth = { healthConnectLauncher.launch(HealthConnectManager.PERMISSIONS) },
                        onBack = { screen = AppScreen.OnboardingStep2 },
                        onComplete = {
                            viewModel.saveProfile()
                            screen = AppScreen.Home
                        },
                    )

                    AppScreen.Home -> HomeScreen(
                        profile = uiState.profileDraft,
                        draft = uiState.draft,
                        recommendation = uiState.recommendation,
                        latestCheckIn = uiState.recentCheckIns.firstOrNull(),
                        recentSessions = uiState.recentSessions,
                        healthConnectStatus = uiState.healthConnectStatus,
                        onStart = { screen = AppScreen.CheckIn },
                        onHistory = { screen = AppScreen.History },
                        onSettings = { showSettings = true },
                    )

                    AppScreen.CheckIn -> CheckInScreen(
                        state = uiState.draft,
                        onSettings = { showSettings = true },
                        onStateChange = viewModel::updateDraft,
                        onRecommend = {
                            viewModel.requestRecommendationAndSaveCheckIn()
                            screen = AppScreen.Recommendation
                        },
                    )

                    AppScreen.Recommendation -> RecommendationScreen(
                        recommendation = uiState.recommendation,
                        recommendationSource = uiState.recommendationSource,
                        recommendationQuota = uiState.recommendationQuota,
                        recommendationNotice = uiState.recommendationNotice,
                        isRefreshingRecommendation = uiState.isRefreshingRecommendation,
                        saveMessage = uiState.saveMessage,
                        onSettings = { showSettings = true },
                        onStartSession = { screen = AppScreen.Session },
                        onEdit = { screen = AppScreen.CheckIn },
                    )

                    AppScreen.Session -> SessionScreen(
                        recommendation = uiState.recommendation,
                        onSettings = { showSettings = true },
                        onFinish = {
                            viewModel.completeCurrentSession()
                            screen = AppScreen.History
                        },
                    )

                    AppScreen.History -> HistoryScreen(
                        recommendation = uiState.recommendation,
                        recommendationSource = uiState.recommendationSource,
                        sessions = uiState.recentSessions,
                        checkIns = uiState.recentCheckIns,
                        saveMessage = uiState.saveMessage,
                        onSettings = { showSettings = true },
                        onCheckIn = { screen = AppScreen.CheckIn },
                    )
                }
            }
            if (showSettings) {
                SettingsSheet(
                    state = uiState.profileDraft,
                    healthConnectStatus = uiState.healthConnectStatus,
                    saveMessage = uiState.saveMessage,
                    onStateChange = viewModel::updateProfileDraft,
                    onConnectHealth = { healthConnectLauncher.launch(HealthConnectManager.PERMISSIONS) },
                    onDismiss = { showSettings = false },
                    onSave = {
                        viewModel.saveProfile()
                        showSettings = false
                    },
                )
            }
        }
    }
}

@Composable
private fun OnboardingStep1Screen(
    state: AthleteProfileDraft,
    isLoading: Boolean,
    onStateChange: (AthleteProfileDraft) -> Unit,
    onNext: () -> Unit,
) {
    if (isLoading) {
        ScreenFrame(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(120.dp))
            BrandMark()
            Text(
                text = "Loading athlete profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
        return
    }

    OnboardingFrame(
        step = 1,
        title = "Who are you?",
        footer = {
            PrimaryAction(text = "Next", onClick = onNext)
        },
    ) {
        Column {
            OutlinedTextField(
                value = state.name,
                onValueChange = { onStateChange(state.copy(name = it)) },
                label = { Text("Your name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text("Training style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ChipRow(
                options = listOf("Hybrid", "Running", "Strength"),
                selected = state.trainingStyle,
                onSelect = { onStateChange(state.copy(trainingStyle = it)) },
            )
            FrequencyRow(
                selected = state.weeklyTrainingFrequency,
                onSelect = { onStateChange(state.copy(weeklyTrainingFrequency = it)) },
            )
        }
    }
}

@Composable
private fun OnboardingStep2Screen(
    state: AthleteProfileDraft,
    onStateChange: (AthleteProfileDraft) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    OnboardingFrame(
        step = 2,
        title = "Your goals",
        subtitle = "Select all that apply.",
        footer = {
            NavigationActions(
                primaryText = "Next",
                onPrimary = onNext,
                onBack = onBack,
            )
        },
    ) {
        GoalGrid(
            selected = state.goals,
            onToggle = { goal ->
                val nextGoals = if (goal in state.goals) {
                    state.goals - goal
                } else {
                    state.goals + goal
                }
                onStateChange(state.copy(goals = nextGoals.ifEmpty { listOf("recovery") }))
            },
        )
    }
}

@Composable
private fun OnboardingStep3Screen(
    state: AthleteProfileDraft,
    healthConnectStatus: HealthConnectUiStatus,
    onStateChange: (AthleteProfileDraft) -> Unit,
    onConnectHealth: () -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    OnboardingFrame(
        step = 3,
        title = "Preferences",
        footer = {
            NavigationActions(
                primaryText = "Get started",
                onPrimary = onComplete,
                onBack = onBack,
            )
        },
    ) {
        Column {
            DurationRow(
                selected = state.preferredSessionLength,
                label = "Preferred session length",
                onSelect = { onStateChange(state.copy(preferredSessionLength = it)) },
            )
            ReminderSettingsCard(
                state = state,
                onStateChange = onStateChange,
            )
            HealthConnectCard(
                status = healthConnectStatus,
                enabled = state.healthConnectEnabled,
                onConnect = onConnectHealth,
                onSkip = { onStateChange(state.copy(healthConnectEnabled = false)) },
                modifier = Modifier.padding(top = 12.dp),
            )
            OutlinedTextField(
                value = state.raceName,
                onValueChange = { onStateChange(state.copy(raceName = it)) },
                label = { Text("Race name (optional)") },
                placeholder = { Text("HYROX Toronto") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
            RaceDatePickerField(
                selectedDate = state.raceDate,
                onDateSelected = { onStateChange(state.copy(raceDate = it)) },
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    state: AthleteProfileDraft,
    healthConnectStatus: HealthConnectUiStatus,
    saveMessage: String,
    onStateChange: (AthleteProfileDraft) -> Unit,
    onConnectHealth: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 10.dp),
        ) {
            Eyebrow("Profile settings")
            Text(
                text = "Edit your athlete defaults.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 18.dp),
            )
            OutlinedTextField(
                value = state.name,
                onValueChange = { onStateChange(state.copy(name = it)) },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.raceDate,
                onValueChange = { onStateChange(state.copy(raceDate = it)) },
                label = { Text("Race date") },
                placeholder = { Text("YYYY-MM-DD") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
            OutlinedTextField(
                value = state.raceName,
                onValueChange = { onStateChange(state.copy(raceName = it)) },
                label = { Text("Race name") },
                placeholder = { Text("HYROX Toronto") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
            Text(
                text = "Training style",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 18.dp),
            )
            ChipRow(
                options = listOf("Hybrid", "Running", "Strength", "Functional fitness", "Recovery focused"),
                selected = state.trainingStyle,
                onSelect = { onStateChange(state.copy(trainingStyle = it)) },
            )
            FrequencyRow(
                selected = state.weeklyTrainingFrequency,
                onSelect = { onStateChange(state.copy(weeklyTrainingFrequency = it)) },
            )
            DurationRow(
                selected = state.preferredSessionLength,
                label = "Preferred session length",
                onSelect = { onStateChange(state.copy(preferredSessionLength = it)) },
            )
            HealthConnectCard(
                status = healthConnectStatus,
                enabled = state.healthConnectEnabled,
                onConnect = onConnectHealth,
                onSkip = { onStateChange(state.copy(healthConnectEnabled = false)) },
                modifier = Modifier.padding(top = 16.dp),
            )
            InsightCard(
                title = "Sync status",
                body = saveMessage,
                modifier = Modifier.padding(top = 16.dp),
            )
            Spacer(modifier = Modifier.height(18.dp))
            PrimaryAction(text = "Save changes", onClick = onSave)
            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}

@Composable
private fun HomeScreen(
    profile: AthleteProfileDraft,
    draft: CheckInDraft,
    recommendation: BreathworkRecommendation,
    latestCheckIn: DailyCheckIn?,
    recentSessions: List<BreathworkSession>,
    healthConnectStatus: HealthConnectUiStatus,
    onStart: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
) {
    val readiness = remember(latestCheckIn, recentSessions, healthConnectStatus.metrics, profile.raceName, profile.raceDate) {
        ReadinessCalculator.calculate(
            latestCheckIn = latestCheckIn,
            recentSessions = recentSessions,
            healthMetrics = healthConnectStatus.metrics,
            raceName = profile.raceName,
            raceDate = profile.raceDate,
        )
    }
    val raceCountdown = remember(profile.raceName, profile.raceDate) {
        RaceCountdownCalculator.calculate(
            raceName = profile.raceName,
            raceDate = profile.raceDate,
        )
    }

    ScreenFrame {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text(
                    text = "HybridTempo",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "Breathwork designed around how you train.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            OutlinedButton(onClick = onSettings) {
                Text("Settings")
            }
        }
        Spacer(modifier = Modifier.height(26.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark()
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ready to recover?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = latestCheckIn?.let {
                        "Latest check-in: Energy ${it.energy} · Stress ${it.stress} · Soreness ${it.soreness}"
                    } ?: "No session today yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        ReadinessCard(
            readiness = readiness,
            modifier = Modifier.padding(top = 28.dp),
        )
        raceCountdown?.let {
            RaceCountdownCard(
                countdown = it,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        InsightCard(
            title = "Today's check-in",
            body = latestCheckIn?.let {
                "${it.workoutType} · Energy ${it.energy}/10 · Stress ${it.stress}/10 · Soreness ${it.soreness}/10"
            } ?: "Preview: ${draft.workoutType} · Energy ${draft.energy}/10 · Stress ${draft.stress}/10 · Soreness ${draft.soreness}/10 → ${recommendation.protocol}",
            modifier = Modifier.padding(top = 14.dp),
        )
        Spacer(modifier = Modifier.height(30.dp))
        PrimaryAction(text = "Start check-in", onClick = onStart)
        OutlinedButton(
            onClick = onHistory,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            Text("View history")
        }
    }
}

@Composable
private fun RaceCountdownCard(
    countdown: RaceCountdown,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Race countdown",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = countdown.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                text = countdown.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun ReadinessCard(
    readiness: ReadinessScore,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Recovery readiness",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = readiness.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.74f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        text = readiness.sourceLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.64f),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Text(
                    text = "${readiness.score}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                text = readiness.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = readiness.nudge,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.74f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun HealthConnectCard(
    status: HealthConnectUiStatus,
    enabled: Boolean,
    onConnect: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isAvailable = status.availability == HealthConnectAvailability.Available
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Health Connect",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = status.toDisplayLabel(enabled),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Switch(
                    checked = enabled && status.hasRequiredPermissions,
                    enabled = isAvailable,
                    onCheckedChange = { checked ->
                        if (checked) {
                            onConnect()
                        } else {
                            onSkip()
                        }
                    },
                )
            }
            Text(
                text = "Optional: improve readiness using sleep, workouts, and resting heart rate. You can skip this and still use manual check-ins.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 14.dp),
            ) {
                OutlinedButton(
                    onClick = onConnect,
                    enabled = isAvailable,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (enabled) "Reconnect" else "Connect")
                }
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Skip for now")
                }
            }
        }
    }
}

private fun HealthConnectUiStatus.toDisplayLabel(enabled: Boolean): String = when {
    availability == HealthConnectAvailability.Unavailable -> "Unavailable on this device"
    availability == HealthConnectAvailability.UpdateRequired -> "Update Health Connect to connect"
    enabled && metrics?.hasData == true -> "Connected: readiness can use recent health data"
    enabled && hasRequiredPermissions -> "Connected: waiting for recent health data"
    enabled -> "Enabled, but permissions need review"
    else -> "Not connected"
}

@Composable
private fun CheckInScreen(
    state: CheckInDraft,
    onSettings: () -> Unit,
    onStateChange: (CheckInDraft) -> Unit,
    onRecommend: () -> Unit,
) {
    ScreenFrame {
        ScreenHeader(
            eyebrow = "Daily check-in",
            title = "How are you feeling?",
            onSettings = onSettings,
        )
        ScoreSlider("Energy", state.energy) { onStateChange(state.copy(energy = it)) }
        ScoreSlider("Stress", state.stress) { onStateChange(state.copy(stress = it)) }
        ScoreSlider("Soreness", state.soreness) { onStateChange(state.copy(soreness = it)) }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Workout context",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        ChipRow(
            options = listOf("Intervals", "Strength", "Hybrid", "Conditioning", "Recovery", "Long run"),
            selected = state.workoutType,
            onSelect = { onStateChange(state.copy(workoutType = it)) },
        )
        DurationRow(
            selected = state.timeAvailable,
            onSelect = { onStateChange(state.copy(timeAvailable = it)) },
        )
        Spacer(modifier = Modifier.height(30.dp))
        PrimaryAction(text = "Get recommendation", onClick = onRecommend)
    }
}

@Composable
private fun RecommendationScreen(
    recommendation: BreathworkRecommendation,
    recommendationSource: RecommendationSource,
    recommendationQuota: RecommendationQuota?,
    recommendationNotice: String?,
    isRefreshingRecommendation: Boolean,
    saveMessage: String,
    onSettings: () -> Unit,
    onStartSession: () -> Unit,
    onEdit: () -> Unit,
) {
    ScreenFrame {
        ScreenHeader(
            eyebrow = "Your protocol",
            title = recommendation.protocol,
            onSettings = onSettings,
        )
        Text(
            text = "${recommendation.durationMinutes} min · ${recommendation.cadence}",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = if (isRefreshingRecommendation) {
                "Refreshing recommendation..."
            } else {
                recommendationSource.label
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        InsightCard(
            title = "Why this fits",
            body = recommendation.rationale,
            modifier = Modifier.padding(top = 28.dp),
        )
        InsightCard(
            title = "Breathing cadence",
            body = recommendation.cadence,
            modifier = Modifier.padding(top = 14.dp),
        )
        val quotaMessage = recommendationNotice ?: recommendationQuota?.toUsageMessage()
        quotaMessage?.let {
            InsightCard(
                title = "AI usage",
                body = it,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
        Spacer(modifier = Modifier.height(36.dp))
        PrimaryAction(text = "Start session", onClick = onStartSession)
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            Text("Adjust check-in")
        }
    }
}

@Composable
private fun SessionScreen(
    recommendation: BreathworkRecommendation,
    onSettings: () -> Unit,
    onFinish: () -> Unit,
) {
    val protocol = recommendation.breathworkProtocol
    val context = LocalContext.current
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(true) }
    var ambientEnabled by remember { mutableStateOf(true) }
    val playbackState = remember(protocol, elapsedSeconds) {
        protocol.playbackStateAt(elapsedSeconds)
    }
    val progress by animateFloatAsState(
        targetValue = elapsedSeconds.toFloat() / protocol.totalSeconds.toFloat(),
        animationSpec = tween(300),
        label = "sessionProgress",
    )
    val breathScale by animateFloatAsState(
        targetValue = playbackState.phase.scaleTarget,
        animationSpec = tween((playbackState.phase.seconds * 850).coerceAtLeast(700)),
        label = "breathScale",
    )
    val ambientAudioController = remember(protocol.ambientTrackName) {
        AmbientAudioController(
            context = context.applicationContext,
            trackName = protocol.ambientTrackName,
        )
    }

    LaunchedEffect(running, elapsedSeconds) {
        if (running && elapsedSeconds < protocol.totalSeconds) {
            delay(1000)
            elapsedSeconds += 1
        }
    }

    LaunchedEffect(running, ambientEnabled, protocol.ambientTrackName) {
        ambientAudioController.setPlaying(running && ambientEnabled)
    }

    androidx.compose.runtime.DisposableEffect(ambientAudioController) {
        onDispose { ambientAudioController.release() }
    }

    ScreenFrame(horizontalAlignment = Alignment.CenterHorizontally) {
        Eyebrow("Active session")
        Text(
            text = protocol.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = "Cycle ${playbackState.cycleNumber}/${playbackState.cycleCount}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(modifier = Modifier.height(42.dp))
        BreathRing(
            progress = progress,
            phaseScale = breathScale,
            phaseLabel = playbackState.phase.label,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = formatTime(protocol.totalSeconds - elapsedSeconds),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = playbackState.phase.instruction,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "${playbackState.phase.label}: ${playbackState.phaseRemainingSeconds}s · Next: ${playbackState.nextPhase.label}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 10.dp),
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ambient layer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Loops when `res/raw/${protocol.ambientTrackName}` exists.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = ambientEnabled,
                    onCheckedChange = { ambientEnabled = it },
                )
            }
        }
        Spacer(modifier = Modifier.height(42.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = { running = !running },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (running) "Pause" else "Resume")
            }
            Button(
                onClick = onFinish,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Finish", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        OutlinedButton(
            onClick = onSettings,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            Text("Profile settings")
        }
    }
}

@Composable
private fun HistoryScreen(
    recommendation: BreathworkRecommendation,
    recommendationSource: RecommendationSource,
    sessions: List<BreathworkSession>,
    checkIns: List<DailyCheckIn>,
    saveMessage: String,
    onSettings: () -> Unit,
    onCheckIn: () -> Unit,
) {
    val summary = remember(sessions) { sessions.toHistorySummary() }

    ScreenFrame {
        ScreenHeader(
            eyebrow = "History",
            title = "Recovery consistency",
            onSettings = onSettings,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(summary.currentStreak.toString(), "day streak", Modifier.weight(1f))
            MetricCard(summary.totalSessions.toString(), "sessions", Modifier.weight(1f))
            MetricCard(summary.totalMinutes.toString(), "minutes", Modifier.weight(1f))
        }
        Text(
            text = "Recent sessions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
        )
        if (sessions.isEmpty()) {
            InsightCard(
                title = "No completed sessions yet",
                body = "Finish your first protocol and this screen becomes your recovery log. The goal is repeatable regulation after training.",
                modifier = Modifier.padding(top = 18.dp),
            )
        } else {
            sessions.take(6).forEach { session ->
                SessionRow(session = session)
            }
        }
        Spacer(modifier = Modifier.height(36.dp))
        PrimaryAction(text = "New check-in", onClick = onCheckIn)
    }
}

@Composable
private fun ScreenFrame(
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 24.dp),
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

@Composable
private fun OnboardingFrame(
    step: Int,
    title: String,
    subtitle: String? = null,
    footer: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 22.dp, vertical = 24.dp),
    ) {
        OnboardingBreadcrumb(currentStep = step)
        Spacer(modifier = Modifier.height(28.dp))
        Eyebrow("Step $step of 3")
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp),
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(modifier = Modifier.weight(0.7f))
        Column(content = content)
        Spacer(modifier = Modifier.weight(1f))
        footer()
    }
}

@Composable
private fun OnboardingBreadcrumb(currentStep: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        (1..3).forEach { step ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(7.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(
                        if (step <= currentStep) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
            )
        }
    }
}

@Composable
private fun NavigationActions(
    primaryText: String,
    onPrimary: () -> Unit,
    onBack: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .weight(0.42f)
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("Back")
        }
        Button(
            onClick = onPrimary,
            modifier = Modifier
                .weight(0.58f)
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(primaryText, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RaceDatePickerField(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val selectedMillis = remember(selectedDate) { selectedDate.toEpochMillisOrNull() }
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedMillis)

    OutlinedButton(
        onClick = { showPicker = true },
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(
            text = if (selectedDate.isBlank()) {
                "Choose race date (optional)"
            } else {
                "Race date: $selectedDate"
            },
        )
    }

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.toLocalDateString()?.let(onDateSelected)
                        showPicker = false
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text("HT", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun MetricStrip() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        MetricCard("3", "min reset", Modifier.weight(1f))
        MetricCard("5", "min recover", Modifier.weight(1f))
        MetricCard("10", "min downshift", Modifier.weight(1f))
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ScreenHeader(
    eyebrow: String,
    title: String,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Eyebrow(eyebrow)
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        OutlinedButton(onClick = onSettings) {
            Text("Settings")
        }
    }
}

@Composable
private fun ScoreSlider(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text("$value/10", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt().coerceIn(1, 10)) },
            valueRange = 1f..10f,
            steps = 8,
        )
    }
}

private data class GoalOption(
    val label: String,
    val value: String,
)

@Composable
private fun GoalGrid(
    selected: List<String>,
    onToggle: (String) -> Unit,
) {
    val goals = listOf(
        GoalOption("Recovery", "recovery"),
        GoalOption("Activation", "activation"),
        GoalOption("Focus", "focus"),
        GoalOption("Race prep", "race prep"),
        GoalOption("Sleep", "sleep support"),
        GoalOption("Stress", "stress"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        goals.chunked(2).forEach { rowGoals ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                rowGoals.forEach { goal ->
                    SelectButton(
                        text = goal.label,
                        selected = goal.value in selected,
                        onClick = { onToggle(goal.value) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 8.dp)
            .horizontalScroll(rememberScrollState()),
    ) {
        options.forEach { option ->
            if (option == selected) {
                ElevatedAssistChip(onClick = { onSelect(option) }, label = { Text(option) })
            } else {
                AssistChip(onClick = { onSelect(option) }, label = { Text(option) })
            }
        }
    }
}

@Composable
private fun FrequencyRow(selected: Int, onSelect: (Int) -> Unit) {
    Text(
        text = "Weekly training frequency",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 14.dp, bottom = 10.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(3, 4, 5, 6).forEach { frequency ->
            SelectButton(
                text = "$frequency x",
                selected = frequency == selected,
                onClick = { onSelect(frequency) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DurationRow(
    selected: Int,
    label: String = "Time available",
    onSelect: (Int) -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(3, 5, 10).forEach { minutes ->
            SelectButton(
                text = "${minutes}m",
                selected = minutes == selected,
                onClick = { onSelect(minutes) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GoalSelector(
    selected: List<String>,
    onToggle: (String) -> Unit,
) {
    Text(
        text = "Goals",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 18.dp),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .horizontalScroll(rememberScrollState()),
    ) {
        listOf("recovery", "activation", "focus", "race prep", "sleep support").forEach { goal ->
            if (goal in selected) {
                ElevatedAssistChip(onClick = { onToggle(goal) }, label = { Text(goal) })
            } else {
                AssistChip(onClick = { onToggle(goal) }, label = { Text(goal) })
            }
        }
    }
}

@Composable
private fun ReminderSettingsCard(
    state: AthleteProfileDraft,
    onStateChange: (AthleteProfileDraft) -> Unit,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            onStateChange(state.copy(eveningReminderEnabled = true))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Evening recovery reminder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${state.eveningReminderHour.toDisplayHour()}:${state.eveningReminderMinute.toString().padStart(2, '0')} PM daily",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.eveningReminderEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !context.hasNotificationPermission()) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onStateChange(state.copy(eveningReminderEnabled = enabled))
                        }
                    },
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 14.dp),
            ) {
                listOf(19, 20, 21).forEach { hour ->
                    SelectButton(
                        text = "${hour.toDisplayHour()} PM",
                        selected = state.eveningReminderHour == hour,
                        onClick = { onStateChange(state.copy(eveningReminderHour = hour)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Text(
                "Save settings to schedule or cancel the reminder on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun SelectButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedContainer = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val selectedContent = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = selectedContainer),
    ) {
        Text(text, color = selectedContent)
    }
}

@Composable
private fun InsightCard(title: String, body: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun ProgressRow(label: String, count: Int, total: Int) {
    val fraction = if (total <= 0) 0f else count.toFloat() / total.toFloat()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("$count", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0.04f, 1f))
                        .height(8.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun SessionRow(session: BreathworkSession) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.protocol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    session.completedAt.toHistoryDateLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                "${session.durationMinutes}m",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun TrendCard(trend: RecoveryTrend) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(trend.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        trend.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "${trend.latest}/10",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 14.dp),
            ) {
                trend.values.forEach { value ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height((14 + value * 5).dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f + value.coerceIn(1, 10) * 0.055f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun BreathRing(
    progress: Float,
    phaseScale: Float,
    phaseLabel: String,
) {
    val color = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = Modifier.size(220.dp)) {
        drawCircle(color = track, style = Stroke(width = 18.dp.toPx()))
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = progress.coerceIn(0f, 1f) * 360f,
            useCenter = false,
            style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.3f), Color.Transparent),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.minDimension / 2,
            ),
        )
        drawCircle(
            color = color.copy(alpha = 0.28f),
            radius = (size.minDimension * 0.24f * phaseScale).coerceAtMost(size.minDimension * 0.42f),
        )
        drawCircle(
            color = color.copy(alpha = if (phaseLabel == "Hold" || phaseLabel == "Rest") 0.5f else 0.82f),
            radius = size.minDimension * 0.08f,
        )
    }
}

@Composable
private fun PrimaryAction(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Text(text, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
    )
}

private fun formatTime(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val remainder = safeSeconds % 60
    return "$minutes:${remainder.toString().padStart(2, '0')}"
}

private data class ProtocolPlaybackState(
    val phase: BreathPhase,
    val nextPhase: BreathPhase,
    val phaseRemainingSeconds: Int,
    val cycleNumber: Int,
    val cycleCount: Int,
)

private data class HistorySummary(
    val totalSessions: Int,
    val totalMinutes: Int,
    val currentStreak: Int,
    val protocolMix: List<ProtocolMixItem>,
)

private data class ProtocolMixItem(
    val protocol: String,
    val count: Int,
)

private data class RecoveryTrends(
    val latest: DailyCheckIn?,
    val items: List<RecoveryTrend>,
)

private data class RecoveryTrend(
    val label: String,
    val latest: Int,
    val delta: Int,
    val values: List<Int>,
) {
    val message: String
        get() = when {
            delta > 0 -> "Up $delta from recent baseline"
            delta < 0 -> "Down ${kotlin.math.abs(delta)} from recent baseline"
            else -> "Stable against recent baseline"
        }
}

private fun BreathworkProtocol.playbackStateAt(elapsedSeconds: Int): ProtocolPlaybackState {
    val safePhases = phases.ifEmpty {
        BreathworkProtocol.postTrainingRecovery(durationMinutes).phases
    }
    val cycleLength = safePhases.sumOf { it.seconds }.coerceAtLeast(1)
    val clampedElapsed = elapsedSeconds.coerceIn(0, (totalSeconds - 1).coerceAtLeast(0))
    val phaseOffset = clampedElapsed % cycleLength
    val cycleNumber = (clampedElapsed / cycleLength) + 1
    val cycleCount = ((totalSeconds + cycleLength - 1) / cycleLength).coerceAtLeast(1)
    var phaseStart = 0

    safePhases.forEachIndexed { index, phase ->
        val phaseEnd = phaseStart + phase.seconds
        if (phaseOffset < phaseEnd) {
            return ProtocolPlaybackState(
                phase = phase,
                nextPhase = safePhases[(index + 1) % safePhases.size],
                phaseRemainingSeconds = (phaseEnd - phaseOffset).coerceAtLeast(1),
                cycleNumber = cycleNumber.coerceAtMost(cycleCount),
                cycleCount = cycleCount,
            )
        }
        phaseStart = phaseEnd
    }

    val fallback = safePhases.last()
    return ProtocolPlaybackState(
        phase = fallback,
        nextPhase = safePhases.first(),
        phaseRemainingSeconds = 1,
        cycleNumber = cycleNumber.coerceAtMost(cycleCount),
        cycleCount = cycleCount,
    )
}

private fun List<DailyCheckIn>.toRecoveryTrends(): RecoveryTrends {
    val sorted = sortedByDescending { it.createdAt }
    val latest = sorted.firstOrNull()
    if (latest == null) {
        return RecoveryTrends(latest = null, items = emptyList())
    }

    return RecoveryTrends(
        latest = latest,
        items = listOf(
            buildTrend("Energy", sorted.map { it.energy }),
            buildTrend("Stress", sorted.map { it.stress }),
            buildTrend("Soreness", sorted.map { it.soreness }),
        ),
    )
}

private fun buildTrend(label: String, newestFirstValues: List<Int>): RecoveryTrend {
    val values = newestFirstValues.filter { it > 0 }.take(7)
    val latest = values.firstOrNull() ?: 0
    val baselineValues = values.drop(1)
    val baseline = if (baselineValues.isEmpty()) latest else baselineValues.average().toInt()

    return RecoveryTrend(
        label = label,
        latest = latest,
        delta = latest - baseline,
        values = values.reversed().ifEmpty { listOf(0) },
    )
}

private fun List<BreathworkSession>.toHistorySummary(): HistorySummary {
    val completedSessions = filter { it.completed }
    return HistorySummary(
        totalSessions = completedSessions.size,
        totalMinutes = completedSessions.sumOf { it.durationMinutes },
        currentStreak = completedSessions.currentStreak(),
        protocolMix = completedSessions
            .groupingBy { it.protocol }
            .eachCount()
            .map { (protocol, count) -> ProtocolMixItem(protocol = protocol, count = count) }
            .sortedWith(compareByDescending<ProtocolMixItem> { it.count }.thenBy { it.protocol }),
    )
}

private fun List<BreathworkSession>.currentStreak(): Int {
    val completedDates = mapNotNull { it.completedAt.toLocalDateOrNull() }.toSet()
    if (completedDates.isEmpty()) return 0

    var cursor = LocalDate.now()
    if (cursor !in completedDates && cursor.minusDays(1) in completedDates) {
        cursor = cursor.minusDays(1)
    }

    var streak = 0
    while (cursor in completedDates) {
        streak += 1
        cursor = cursor.minusDays(1)
    }
    return streak
}

private fun String.toHistoryDateLabel(): String = toLocalDateOrNull()?.let { date ->
    when (date) {
        LocalDate.now() -> "Today"
        LocalDate.now().minusDays(1) -> "Yesterday"
        else -> date.toString()
    }
} ?: "Recently completed"

private fun String.toLocalDateOrNull(): LocalDate? = try {
    OffsetDateTime.parse(this).toLocalDate()
} catch (_: DateTimeParseException) {
    try {
        LocalDate.parse(this.take(10))
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun String.toEpochMillisOrNull(): Long? = try {
    LocalDate.parse(this)
        .atStartOfDay()
        .toInstant(ZoneOffset.UTC)
        .toEpochMilli()
} catch (_: DateTimeParseException) {
    null
}

private fun Long.toLocalDateString(): String =
    java.time.Instant.ofEpochMilli(this)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .toString()

private val RecommendationSource.label: String
    get() = when (this) {
        RecommendationSource.BackendAi -> "AI-backed recommendation"
        RecommendationSource.DeterministicFallback -> "Local fallback recommendation"
        RecommendationSource.DailyLimitReached -> "Daily AI limit reached"
    }

private fun RecommendationQuota.toUsageMessage(): String {
    if (remaining <= 0) {
        return "You have used all $limit AI recommendations for today. The local protocol still works, so save AI requests for meaningful check-ins and come back tomorrow."
    }

    return "$remaining of $limit AI recommendations left today. Slider changes use a local preview, and only this button uses AI."
}

private fun android.content.Context.hasNotificationPermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

private fun Int.toDisplayHour(): Int = when (val hour = this % 12) {
    0 -> 12
    else -> hour
}
