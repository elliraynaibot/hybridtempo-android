package com.hybridtempo.android.ui

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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hybridtempo.android.data.BreathworkRecommendation
import com.hybridtempo.android.data.BreathworkSession
import kotlinx.coroutines.delay

private enum class AppScreen {
    Welcome,
    CheckIn,
    Recommendation,
    Session,
    History,
}

@Composable
fun HybridTempoApp(viewModel: HybridTempoViewModel = viewModel()) {
    var screen by remember { mutableStateOf(AppScreen.Welcome) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                    AppScreen.Welcome -> WelcomeScreen(
                        onStart = { screen = AppScreen.CheckIn },
                        onHistory = { screen = AppScreen.History },
                    )

                    AppScreen.CheckIn -> CheckInScreen(
                        state = uiState.draft,
                        onStateChange = viewModel::updateDraft,
                        onRecommend = {
                            viewModel.saveCheckIn()
                            screen = AppScreen.Recommendation
                        },
                    )

                    AppScreen.Recommendation -> RecommendationScreen(
                        recommendation = uiState.recommendation,
                        saveMessage = uiState.saveMessage,
                        onStartSession = { screen = AppScreen.Session },
                        onEdit = { screen = AppScreen.CheckIn },
                    )

                    AppScreen.Session -> SessionScreen(
                        recommendation = uiState.recommendation,
                        onFinish = {
                            viewModel.completeCurrentSession()
                            screen = AppScreen.History
                        },
                    )

                    AppScreen.History -> HistoryScreen(
                        recommendation = uiState.recommendation,
                        sessions = uiState.recentSessions,
                        saveMessage = uiState.saveMessage,
                        onCheckIn = { screen = AppScreen.CheckIn },
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeScreen(
    onStart: () -> Unit,
    onHistory: () -> Unit,
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
    }
}

@Composable
private fun CheckInScreen(
    state: CheckInDraft,
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
    }
}

@Composable
private fun RecommendationScreen(
    recommendation: BreathworkRecommendation,
    saveMessage: String,
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
    }
}

@Composable
private fun SessionScreen(
    recommendation: BreathworkRecommendation,
    onFinish: () -> Unit,
) {
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(true) }
    val totalSeconds = recommendation.durationMinutes * 60
    val progress by animateFloatAsState(
        targetValue = elapsedSeconds.toFloat() / totalSeconds.toFloat(),
        animationSpec = tween(300),
        label = "sessionProgress",
    )

    LaunchedEffect(running, elapsedSeconds) {
        if (running && elapsedSeconds < totalSeconds) {
            delay(1000)
            elapsedSeconds += 1
        }
    }

    ScreenFrame(horizontalAlignment = Alignment.CenterHorizontally) {
        Eyebrow("Active session")
        Text(
            text = recommendation.protocol,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(modifier = Modifier.height(42.dp))
        BreathRing(progress = progress)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = formatTime(totalSeconds - elapsedSeconds),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = if ((elapsedSeconds / 4) % 2 == 0) "Inhale through the nose" else "Extend the exhale",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
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
    }
}

@Composable
private fun HistoryScreen(
    recommendation: BreathworkRecommendation,
    sessions: List<BreathworkSession>,
    saveMessage: String,
    onCheckIn: () -> Unit,
) {
    ScreenFrame {
        Eyebrow("History")
        Text(
            text = "Build awareness over time.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
        )
        MetricStrip()
        InsightCard(
            title = "Latest recommendation",
            body = "${recommendation.protocol} · ${recommendation.durationMinutes} min · ${recommendation.cadence}",
            modifier = Modifier.padding(top = 18.dp),
        )
        InsightCard(
            title = "Persistence",
            body = saveMessage,
            modifier = Modifier.padding(top = 14.dp),
        )
        sessions.forEach { session ->
            InsightCard(
                title = session.protocol,
                body = "${session.durationMinutes} min · ${session.cadence} · ${session.completedAt}",
                modifier = Modifier.padding(top = 14.dp),
            )
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
private fun DurationRow(selected: Int, onSelect: (Int) -> Unit) {
    Text(
        text = "Time available",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(3, 5, 10).forEach { minutes ->
            val selectedContainer = if (minutes == selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            }
            val selectedContent = if (minutes == selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            Button(
                onClick = { onSelect(minutes) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = selectedContainer),
            ) {
                Text("${minutes}m", color = selectedContent)
            }
        }
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
private fun BreathRing(progress: Float) {
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
