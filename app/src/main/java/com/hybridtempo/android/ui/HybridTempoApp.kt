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
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.hybridtempo.android.recommendation.RecommendationQuota
import com.hybridtempo.android.recommendation.RecommendationSource
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import kotlinx.coroutines.delay

private enum class AppScreen {
    Onboarding,
    Welcome,
    Settings,
    CheckIn,
    Recommendation,
    Session,
    History,
}

@Composable
fun HybridTempoApp(viewModel: HybridTempoViewModel = viewModel()) {
    var screen by remember { mutableStateOf(AppScreen.Onboarding) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.hasCompletedOnboarding, uiState.isLoadingProfile) {
        if (!uiState.isLoadingProfile && uiState.hasCompletedOnboarding && screen == AppScreen.Onboarding) {
            screen = AppScreen.Welcome
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
                    AppScreen.Onboarding -> OnboardingScreen(
                        state = uiState.profileDraft,
                        isLoading = uiState.isLoadingProfile,
                        onStateChange = viewModel::updateProfileDraft,
                        onComplete = {
                            viewModel.saveProfile()
                            screen = AppScreen.Welcome
                        },
                    )

                    AppScreen.Welcome -> WelcomeScreen(
                        onStart = { screen = AppScreen.CheckIn },
                        onHistory = { screen = AppScreen.History },
                        onSettings = { screen = AppScreen.Settings },
                    )

                    AppScreen.Settings -> SettingsScreen(
                        state = uiState.profileDraft,
                        saveMessage = uiState.saveMessage,
                        onStateChange = viewModel::updateProfileDraft,
                        onSave = {
                            viewModel.saveProfile()
                            screen = AppScreen.Welcome
                        },
                        onBack = { screen = AppScreen.Welcome },
                    )

                    AppScreen.CheckIn -> CheckInScreen(
                        state = uiState.draft,
                        onSettings = { screen = AppScreen.Settings },
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
                        onSettings = { screen = AppScreen.Settings },
                        onStartSession = { screen = AppScreen.Session },
                        onEdit = { screen = AppScreen.CheckIn },
                    )

                    AppScreen.Session -> SessionScreen(
                        recommendation = uiState.recommendation,
                        onSettings = { screen = AppScreen.Settings },
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
                        onSettings = { screen = AppScreen.Settings },
                        onCheckIn = { screen = AppScreen.CheckIn },
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingScreen(
    state: AthleteProfileDraft,
    isLoading: Boolean,
    onStateChange: (AthleteProfileDraft) -> Unit,
    onComplete: () -> Unit,
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

    ProfileFormScreen(
        eyebrow = "Athlete setup",
        title = "Tune breathwork around your training.",
        body = "A short profile gives the recommendation engine better defaults without making the app feel heavy.",
        action = "Save athlete profile",
        state = state,
        onStateChange = onStateChange,
        onAction = onComplete,
    )
}

@Composable
private fun SettingsScreen(
    state: AthleteProfileDraft,
    saveMessage: String,
    onStateChange: (AthleteProfileDraft) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    ProfileFormScreen(
        eyebrow = "Profile settings",
        title = "Edit your athlete defaults.",
        body = "These values shape future recommendations and are saved as your reusable training profile.",
        action = "Save changes",
        state = state,
        footer = {
            InsightCard(
                title = "Sync status",
                body = saveMessage,
                modifier = Modifier.padding(top = 16.dp),
            )
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text("Back")
            }
        },
        onStateChange = onStateChange,
        onAction = onSave,
    )
}

@Composable
private fun ProfileFormScreen(
    eyebrow: String,
    title: String,
    body: String,
    action: String,
    state: AthleteProfileDraft,
    footer: @Composable ColumnScope.() -> Unit = {},
    onStateChange: (AthleteProfileDraft) -> Unit,
    onAction: () -> Unit,
) {
    ScreenFrame {
        Eyebrow(eyebrow)
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
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
            label = { Text("Race date (optional)") },
            placeholder = { Text("YYYY-MM-DD") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
        Spacer(modifier = Modifier.height(22.dp))
        Text("Training style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
        GoalSelector(
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
        ReminderSettingsCard(
            state = state,
            onStateChange = onStateChange,
        )
        Spacer(modifier = Modifier.height(30.dp))
        PrimaryAction(text = action, onClick = onAction)
        footer()
    }
}

@Composable
private fun WelcomeScreen(
    onStart: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
) {
    ScreenFrame {
        Spacer(modifier = Modifier.height(28.dp))
        BrandMark()
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "HybridTempo",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "Breathwork designed around how you train.",
            style = MaterialTheme.typography.headlineMedium,
            lineHeight = 34.sp,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = "Check in, match your recovery state, and start a short protocol built for the training you actually did.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 18.dp),
        )
        Spacer(modifier = Modifier.height(32.dp))
        MetricStrip()
        Spacer(modifier = Modifier.height(36.dp))
        PrimaryAction(text = "Start daily check-in", onClick = onStart)
        OutlinedButton(
            onClick = onHistory,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            Text("View history")
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
private fun CheckInScreen(
    state: CheckInDraft,
    onSettings: () -> Unit,
    onStateChange: (CheckInDraft) -> Unit,
    onRecommend: () -> Unit,
) {
    ScreenFrame {
        Eyebrow("Daily check-in")
        Text(
            text = "What state are you bringing into recovery?",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp, bottom = 22.dp),
        )
        ScoreSlider("Energy", state.energy) { onStateChange(state.copy(energy = it)) }
        ScoreSlider("Soreness", state.soreness) { onStateChange(state.copy(soreness = it)) }
        ScoreSlider("Stress", state.stress) { onStateChange(state.copy(stress = it)) }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Training context",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        ChipRow(
            options = listOf("Intervals", "Strength", "Hybrid", "Conditioning", "Recovery", "Long run"),
            selected = state.workoutType,
            onSelect = { onStateChange(state.copy(workoutType = it)) },
        )
        ScoreSlider("Workout intensity", state.workoutIntensity) {
            onStateChange(state.copy(workoutIntensity = it))
        }
        DurationRow(
            selected = state.timeAvailable,
            onSelect = { onStateChange(state.copy(timeAvailable = it)) },
        )
        Spacer(modifier = Modifier.height(30.dp))
        PrimaryAction(text = "Get recommendation", onClick = onRecommend)
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
        Eyebrow("Recommended protocol")
        Text(
            text = recommendation.protocol,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = "${recommendation.durationMinutes} minutes",
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
        InsightCard(
            title = "Persistence",
            body = saveMessage,
            modifier = Modifier.padding(top = 14.dp),
        )
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
    val trends = remember(checkIns) { checkIns.toRecoveryTrends() }

    ScreenFrame {
        Eyebrow("History")
        Text(
            text = "Recovery consistency, not noise.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(summary.currentStreak.toString(), "day streak", Modifier.weight(1f))
            MetricCard(summary.totalSessions.toString(), "sessions", Modifier.weight(1f))
            MetricCard(summary.totalMinutes.toString(), "minutes", Modifier.weight(1f))
        }
        InsightCard(
            title = "Recommended now",
            body = "${recommendation.protocol} · ${recommendation.durationMinutes} min · ${recommendation.cadence} · ${recommendationSource.label}",
            modifier = Modifier.padding(top = 18.dp),
        )
        trends.latest?.let { latest ->
            InsightCard(
                title = "Today's state",
                body = "Energy ${latest.energy}/10 · Stress ${latest.stress}/10 · Soreness ${latest.soreness}/10 · ${latest.workoutType}",
                modifier = Modifier.padding(top = 14.dp),
            )
        }
        Text(
            text = "Recovery trends",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 24.dp, bottom = 10.dp),
        )
        if (trends.items.isEmpty()) {
            InsightCard(
                title = "No check-in trend yet",
                body = "Complete a few daily check-ins and this section will show how energy, stress, and soreness are moving.",
            )
        } else {
            trends.items.forEach { trend ->
                TrendCard(trend = trend)
            }
        }
        if (sessions.isEmpty()) {
            InsightCard(
                title = "No completed sessions yet",
                body = "Finish your first protocol and this screen becomes your recovery log. The goal is repeatable regulation after training.",
                modifier = Modifier.padding(top = 18.dp),
            )
        } else {
            Text(
                text = "Protocol mix",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 24.dp, bottom = 10.dp),
            )
            summary.protocolMix.forEach { item ->
                ProgressRow(label = item.protocol, count = item.count, total = summary.totalSessions)
            }
            Text(
                text = "Recent sessions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
            )
            sessions.take(6).forEach { session ->
                SessionRow(session = session)
            }
        }
        InsightCard(
            title = "Sync status",
            body = saveMessage,
            modifier = Modifier.padding(top = 18.dp),
        )
        Spacer(modifier = Modifier.height(36.dp))
        PrimaryAction(text = "New check-in", onClick = onCheckIn)
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
