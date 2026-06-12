package com.hybridtempo.android.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.hybridtempo.android.audio.GuidedAudioController
import com.hybridtempo.android.data.BreathworkRecommendation
import com.hybridtempo.android.data.BreathworkSession
import com.hybridtempo.android.data.DailyCheckIn
import com.hybridtempo.android.domain.model.ImportedWorkout
import com.hybridtempo.android.health.HealthConnectAvailability
import com.hybridtempo.android.health.HealthConnectManager
import com.hybridtempo.android.health.HealthConnectUiStatus
import com.hybridtempo.android.readiness.RaceCountdown
import com.hybridtempo.android.readiness.RaceCountdownCalculator
import com.hybridtempo.android.readiness.ManualDetailsPrompt
import com.hybridtempo.android.readiness.ManualDetailsPromptCalculator
import com.hybridtempo.android.readiness.ReadinessCalculator
import com.hybridtempo.android.readiness.ReadinessScore
import com.hybridtempo.android.recommendation.RecommendationQuota
import com.hybridtempo.android.recommendation.RecommendationSource
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AppScreen {
    Landing,
    SignIn,
    SignUp,
    OnboardingStep1,
    OnboardingStep2,
    OnboardingStep3,
    Home,
    WorkoutFormat,
    BreathingProblem,
    TodayCue,
    WorkoutReview,
    GuideMode,
    Session,
    WorkoutHandoff,
    History,
}

private val AppScreen.isOnboarding: Boolean
    get() = this in setOf(
        AppScreen.OnboardingStep1,
        AppScreen.OnboardingStep2,
        AppScreen.OnboardingStep3,
    )

// Keep false for distributed demo builds so completed onboarding reaches Home.
private const val FORCE_ONBOARDING_AFTER_AUTH = false

private fun postAuthScreen(hasCompletedOnboarding: Boolean): AppScreen {
    return if (FORCE_ONBOARDING_AFTER_AUTH || !hasCompletedOnboarding) {
        AppScreen.OnboardingStep1
    } else {
        AppScreen.Home
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HybridTempoApp(viewModel: HybridTempoViewModel = viewModel()) {
    var screen by remember { mutableStateOf(AppScreen.Landing) }
    var showSettings by remember { mutableStateOf(false) }
    var selectedGuideMode by remember { mutableStateOf(defaultGuideModes().first { it.value == "between_sets" }) }
    var selectedGuidedSession by remember { mutableStateOf(arriveOrganizeGuidedSession()) }
    var guideModeBackTarget by remember { mutableStateOf(AppScreen.Home) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val healthConnectLauncher = rememberLauncherForActivityResult(
        contract = HealthConnectManager.permissionsContract(),
    ) { grantedPermissions ->
        viewModel.onHealthConnectPermissionsResult(grantedPermissions)
    }

    LaunchedEffect(uiState.hasCompletedOnboarding, uiState.isLoadingProfile) {
        if (
            !FORCE_ONBOARDING_AFTER_AUTH &&
            !uiState.isLoadingProfile &&
            uiState.hasCompletedOnboarding &&
            screen.isOnboarding
        ) {
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
                    AppScreen.Landing -> LandingScreen(
                        onGetStarted = { screen = AppScreen.SignUp },
                        onSignIn = { screen = AppScreen.SignIn },
                    )

                    AppScreen.SignIn -> SignInScreen(
                        onBack = { screen = AppScreen.Landing },
                        onSignIn = {
                            screen = postAuthScreen(uiState.hasCompletedOnboarding)
                        },
                        onSignUp = { screen = AppScreen.SignUp },
                    )

                    AppScreen.SignUp -> SignUpScreen(
                        onBack = { screen = AppScreen.Landing },
                        onCreateAccount = {
                            screen = postAuthScreen(uiState.hasCompletedOnboarding)
                        },
                        onSignIn = { screen = AppScreen.SignIn },
                    )

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
                        recentSessions = uiState.recentSessions,
                        onSelectMoment = { sessionIntent ->
                            viewModel.updateDraft(uiState.draft.forGuidedMoment(sessionIntent))
                            selectedGuideMode = guideModeForSessionIntent(sessionIntent)
                            selectedGuidedSession = selectGuidedSessionForIntent(sessionIntent)
                            screen = AppScreen.TodayCue
                        },
                        onStartStrategy = { screen = AppScreen.WorkoutFormat },
                        onHistory = { screen = AppScreen.History },
                        onSettings = { showSettings = true },
                    )

                    AppScreen.WorkoutFormat -> WorkoutFormatScreen(
                        state = uiState.draft,
                        onSettings = { showSettings = true },
                        onStateChange = viewModel::updateDraft,
                        onHome = { screen = AppScreen.Home },
                        onNext = { screen = AppScreen.BreathingProblem },
                    )

                    AppScreen.BreathingProblem -> BreathingProblemScreen(
                        state = uiState.draft,
                        onSettings = { showSettings = true },
                        onStateChange = viewModel::updateDraft,
                        onBack = { screen = AppScreen.WorkoutFormat },
                        onHome = { screen = AppScreen.Home },
                        onCue = {
                            selectedGuideMode = guideModeForSessionIntent(uiState.draft.sessionIntent)
                            screen = AppScreen.TodayCue
                        },
                    )

                    AppScreen.TodayCue -> TodayCueScreen(
                        draft = uiState.draft,
                        onSettings = { showSettings = true },
                        onBack = { screen = AppScreen.BreathingProblem },
                        onPractice = {
                            selectedGuidedSession = selectGuidedSessionForIntent(uiState.draft.sessionIntent)
                            screen = AppScreen.Session
                        },
                        onReview = { screen = AppScreen.WorkoutReview },
                        onEdit = { screen = AppScreen.WorkoutFormat },
                    )

                    AppScreen.WorkoutReview -> WorkoutReviewScreen(
                        draft = uiState.workoutReviewDraft,
                        cue = uiState.draft.toTodayCuePresentation(),
                        healthConnectStatus = uiState.healthConnectStatus,
                        onSettings = { showSettings = true },
                        onDraftChange = viewModel::updateWorkoutReviewDraft,
                        onBack = { screen = AppScreen.TodayCue },
                        onComplete = {
                            viewModel.completeWorkoutReview()
                            screen = AppScreen.Home
                        },
                    )

                    AppScreen.GuideMode -> GuideModeScreen(
                        selected = selectedGuideMode,
                        onSettings = { showSettings = true },
                        onSelect = { selectedGuideMode = it },
                        onStart = { screen = AppScreen.Session },
                        backLabel = when (guideModeBackTarget) {
                            AppScreen.TodayCue -> "Back to cue"
                            else -> "Back home"
                        },
                        onBack = { screen = guideModeBackTarget },
                    )

                    AppScreen.Session -> SessionScreen(
                        recommendation = uiState.recommendation,
                        guideMode = selectedGuideMode,
                        guidedSession = selectedGuidedSession,
                        onSettings = { showSettings = true },
                        onBack = { screen = AppScreen.TodayCue },
                        onFinish = {
                            viewModel.completeCurrentSession()
                            screen = AppScreen.WorkoutHandoff
                        },
                    )

                    AppScreen.WorkoutHandoff -> WorkoutHandoffScreen(
                        cue = uiState.draft.toTodayCuePresentation(),
                        healthConnectStatus = uiState.healthConnectStatus,
                        onSettings = { showSettings = true },
                        onHome = { screen = AppScreen.Home },
                        onReview = { screen = AppScreen.WorkoutReview },
                        onBack = { screen = AppScreen.Session },
                    )

                    AppScreen.History -> HistoryScreen(
                        recommendation = uiState.recommendation,
                        recommendationSource = uiState.recommendationSource,
                        sessions = uiState.recentSessions,
                        checkIns = uiState.recentCheckIns,
                        healthConnectStatus = uiState.healthConnectStatus,
                        saveMessage = uiState.saveMessage,
                        onSettings = { showSettings = true },
                        onCheckIn = { screen = AppScreen.WorkoutFormat },
                        onHome = { screen = AppScreen.Home },
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
private fun LandingScreen(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit,
) {
    AuthScreenFrame {
        Spacer(modifier = Modifier.height(54.dp))
        BreathHeroMark()
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Breathe.\nRecover.\nPerform.",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            lineHeight = 42.sp,
        )
        AuthBrandLabel(
            modifier = Modifier.padding(top = 10.dp),
            isHero = true,
        )
        Text(
            text = "Science-backed breathwork protocols built for hybrid athletes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp, start = 18.dp, end = 18.dp),
        )
        Spacer(modifier = Modifier.weight(1f))
        PrimaryAction(text = "GET STARTED", onClick = onGetStarted)
        AuthGhostButton(text = "Sign In", onClick = onSignIn, modifier = Modifier.padding(top = 12.dp))
        Text(
            text = "By continuing you agree to our Terms & Privacy Policy",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.54f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
        )
    }
}

@Composable
private fun SignInScreen(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
) {
    AuthScreenFrame {
        BackCircleButton(onClick = onBack)
        Spacer(modifier = Modifier.height(28.dp))
        AuthBrandLabel()
        Text(
            text = "Welcome back",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
        Text(
            text = "Sign in to continue your recovery",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 28.dp),
        )
        GoogleAuthButton(text = "Continue with Google", onClick = onSignIn)
        AuthDivider()
        AuthInput(label = "EMAIL", value = "you@example.com")
        AuthInput(label = "PASSWORD", value = "••••••••", modifier = Modifier.padding(top = 18.dp))
        Text(
            text = "Forgot password?",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        Spacer(modifier = Modifier.height(28.dp))
        PrimaryAction(text = "SIGN IN", onClick = onSignIn)
        Spacer(modifier = Modifier.weight(1f))
        AuthInlineLink(
            text = "Don't have an account? ",
            action = "Sign up",
            onClick = onSignUp,
        )
    }
}

@Composable
private fun SignUpScreen(
    onBack: () -> Unit,
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit,
) {
    AuthScreenFrame {
        BackCircleButton(onClick = onBack)
        Spacer(modifier = Modifier.height(28.dp))
        AuthBrandLabel()
        Text(
            text = "Create account",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
        Text(
            text = "Start your recovery journey today.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 28.dp),
        )
        GoogleAuthButton(text = "Sign up with Google", onClick = onCreateAccount)
        AuthDivider()
        AuthInput(label = "FULL NAME", value = "Alex Rivera")
        AuthInput(label = "EMAIL", value = "you@example.com", modifier = Modifier.padding(top = 18.dp))
        AuthInput(label = "PASSWORD", value = "••••••••", helper = "Minimum 8 characters", modifier = Modifier.padding(top = 18.dp))
        Spacer(modifier = Modifier.height(28.dp))
        PrimaryAction(text = "CREATE ACCOUNT", onClick = onCreateAccount)
        Text(
            text = "By continuing you agree to our Terms of Service and Privacy Policy.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.54f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
        )
        Spacer(modifier = Modifier.weight(1f))
        AuthInlineLink(
            text = "Already have an account? ",
            action = "Sign in",
            onClick = onSignIn,
        )
    }
}

@Composable
private fun AuthScreenFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF3B130B),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 28.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
private fun AuthBrandLabel(
    modifier: Modifier = Modifier,
    isHero: Boolean = false,
) {
    Text(
        text = "HybridTempo",
        style = if (isHero) MaterialTheme.typography.displaySmall else MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = if (isHero) 0.sp else 2.6.sp,
        modifier = modifier,
    )
}

@Composable
private fun BreathHeroMark() {
    val transition = rememberInfiniteTransition(label = "breath-hero")
    val breathPulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "breath-pulse",
    )
    val centerPulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "center-pulse",
    )

    Box(modifier = Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val orange = Color(0xFFEB470A)
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension * 0.48f
            val smoothCenterProgress = smoothStep(centerPulse)
            val centerRadius = size.minDimension * (0.12f + (smoothCenterProgress * 0.045f))

            listOf(0f, 0.32f, 0.64f).forEachIndexed { index, offset ->
                val progress = (breathPulse + offset) % 1f
                val easedProgress = easeOutCubic(progress)
                val radius = size.minDimension * (0.2f + (easedProgress * 0.3f))
                val alpha = (0.36f * (1f - easedProgress)).coerceIn(0f, 0.36f)
                val strokeWidth = (2.6f - (index * 0.35f)).dp.toPx()

                drawCircle(
                    color = orange.copy(alpha = alpha),
                    radius = radius.coerceAtMost(maxRadius),
                    center = center,
                    style = Stroke(width = strokeWidth),
                )
            }

            drawCircle(
                color = orange.copy(alpha = 0.12f + (smoothCenterProgress * 0.12f)),
                radius = centerRadius * 1.52f,
                center = center,
            )
            drawCircle(
                color = orange.copy(alpha = 0.82f + (smoothCenterProgress * 0.08f)),
                radius = centerRadius,
                center = center,
            )
        }
//        Text(
//            text = "BREATHE",
//            style = MaterialTheme.typography.labelSmall,
//            fontWeight = FontWeight.Black,
//            color = MaterialTheme.colorScheme.onPrimary,
//        )
    }
}

private fun easeOutCubic(progress: Float): Float {
    val clamped = progress.coerceIn(0f, 1f)
    val inverse = 1f - clamped
    return 1f - (inverse * inverse * inverse)
}

private fun smoothStep(progress: Float): Float {
    val clamped = progress.coerceIn(0f, 1f)
    return clamped * clamped * (3f - (2f * clamped))
}

@Composable
private fun BackCircleButton(onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.size(42.dp),
            shape = CircleShape,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
        ) {
            Text("‹", fontSize = 26.sp)
        }
    }
}

@Composable
private fun GoogleAuthButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(99.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF111111)),
    ) {
        Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 10.dp))
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AuthGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(99.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
    ) {
        Text(text)
    }
}

@Composable
private fun AuthDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .height(1.dp)
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        )
        Text(
            text = "or continue with email",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Box(
            modifier = Modifier
                .height(1.dp)
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        )
    }
}

@Composable
private fun AuthInput(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    helper: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        helper?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun AuthInlineLink(
    text: String,
    action: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(action, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onHorizontalOnboardingSwipe(onSwipeLeft = onNext)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF3B130B),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 28.dp, vertical = 28.dp),
    ) {
        Text(
            text = "01",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
            fontSize = 78.sp,
            modifier = Modifier.align(Alignment.TopEnd),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(56.dp))
            Text(
                text = "BREATH SKILLS FOR TRAINING",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Train your\nbreath around\nhard efforts",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                lineHeight = 50.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 38.dp),
            )
            Text(
                text = "HybridTempo recommends short breath sessions based on how you train, recover, and prepare.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .padding(top = 22.dp),
            )
            OnboardingSectionLabel(
                text = "YOUR NAME",
                modifier = Modifier.padding(top = 52.dp),
            )
            UnderlineNameInput(
                value = state.name,
                onValueChange = { onStateChange(state.copy(name = it)) },
                modifier = Modifier.padding(top = 24.dp),
            )
            OnboardingSectionLabel(
                text = "TRAINING STYLE",
                modifier = Modifier.padding(top = 70.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OnboardingPill(
                        text = "Runner",
                        selected = state.trainingStyle == "Runner" || state.trainingStyle == "Running",
                        onClick = { onStateChange(state.copy(trainingStyle = "Runner")) },
                        modifier = Modifier.weight(1f),
                    )
                    OnboardingPill(
                        text = "Lifter",
                        selected = state.trainingStyle == "Lifter" || state.trainingStyle == "Strength",
                        onClick = { onStateChange(state.copy(trainingStyle = "Lifter")) },
                        modifier = Modifier.weight(1f),
                    )
                    OnboardingPill(
                        text = "Hybrid",
                        selected = state.trainingStyle == "Hybrid",
                        onClick = { onStateChange(state.copy(trainingStyle = "Hybrid")) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OnboardingPill(
                        text = "Cyclist",
                        selected = state.trainingStyle == "Cyclist",
                        onClick = { onStateChange(state.copy(trainingStyle = "Cyclist")) },
                        modifier = Modifier.fillMaxWidth(0.48f),
                    )
                }
            }
            OnboardingSectionLabel(
                text = "WEEKLY SESSIONS",
                modifier = Modifier.padding(top = 64.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
            ) {
                listOf(3, 4, 5, 6).forEach { frequency ->
                    OnboardingPill(
                        text = "${frequency}×",
                        selected = state.weeklyTrainingFrequency == frequency,
                        onClick = { onStateChange(state.copy(weeklyTrainingFrequency = frequency)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(120.dp))
            OnboardingDots(currentStep = 1)
            PrimaryAction(
                text = "NEXT",
                onClick = onNext,
            )
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

private fun Modifier.onHorizontalOnboardingSwipe(
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    onSwipeRightProgress: ((Float) -> Unit)? = null,
): Modifier {
    return pointerInput(onSwipeLeft, onSwipeRight, onSwipeRightProgress) {
        var dragDistance = 0f
        detectHorizontalDragGestures(
            onDragStart = {
                dragDistance = 0f
                onSwipeRightProgress?.invoke(0f)
            },
            onHorizontalDrag = { _, dragAmount ->
                dragDistance += dragAmount
                onSwipeRightProgress?.invoke(swipeBackIndicatorProgress(dragDistance))
            },
            onDragEnd = {
                when {
                    dragDistance <= -swipeBackThreshold() -> onSwipeLeft?.invoke()
                    dragDistance >= swipeBackThreshold() -> onSwipeRight?.invoke()
                }
                dragDistance = 0f
                onSwipeRightProgress?.invoke(0f)
            },
            onDragCancel = {
                dragDistance = 0f
                onSwipeRightProgress?.invoke(0f)
            },
        )
    }
}

@Composable
private fun SwipeBackBox(
    onSwipeBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSwipeLeft: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    var swipeProgress by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier.onHorizontalOnboardingSwipe(
            onSwipeLeft = onSwipeLeft,
            onSwipeRight = onSwipeBack,
            onSwipeRightProgress = { swipeProgress = it },
        ),
    ) {
        content()
        SwipeBackIndicator(progress = swipeProgress)
    }
}

@Composable
private fun BoxScope.SwipeBackIndicator(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(120),
        label = "swipeBackIndicator",
    )

    if (animatedProgress <= 0.01f) return

    Box(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .offset(x = ((-16f + (16f * animatedProgress)).dp))
            .alpha(animatedProgress)
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f + (0.12f * animatedProgress)))
            .border(
                width = 1.4.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f + (0.32f * animatedProgress)),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "←",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun OnboardingSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 2.2.sp,
        modifier = modifier,
    )
}

@Composable
private fun UnderlineNameInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(0.72f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.displaySmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                fontSize = 42.sp,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)),
        )
    }
}

@Composable
private fun OnboardingPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(99.dp)
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    }
    val background = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    }
    val textColor = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .height(82.dp)
            .clip(shape)
            .background(background)
            .border(width = 1.5.dp, color = borderColor, shape = shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OnboardingDots(currentStep: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 22.dp),
    ) {
        (1..3).forEach { step ->
            Box(
                modifier = Modifier
                    .size(if (step == currentStep) 20.dp else 16.dp)
                    .clip(CircleShape)
                    .background(
                        if (step == currentStep) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                    )
                    .border(
                        width = 2.dp,
                        color = if (step == currentStep) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun GoalPillGrid(
    selected: List<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val goals = listOf(
        GoalOption("Recover after training", "recovery"),
        GoalOption("Prime before training", "activation"),
        GoalOption("Stay focused", "focus"),
        GoalOption("Race-day nerves", "race prep"),
        GoalOption("Wind down for sleep", "sleep support"),
        GoalOption("Stress reset", "stress"),
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        goals.chunked(2).forEach { rowGoals ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                rowGoals.forEach { goal ->
                    OnboardingPill(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingRaceDatePickerField(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val selectedMillis = remember(selectedDate) { selectedDate.toEpochMillisOrNull() }
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedMillis)

    Box(
        modifier = modifier
            .fillMaxWidth(0.58f)
            .height(78.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                shape = RoundedCornerShape(99.dp),
            )
            .clickable { showPicker = true },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = selectedDate.ifBlank { "Select a date..." },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = if (selectedDate.isBlank()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
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
private fun EveningReminderCompactRow(
    state: AthleteProfileDraft,
    onStateChange: (AthleteProfileDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            onStateChange(state.copy(eveningReminderEnabled = true))
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${state.eveningReminderHour.toDisplayHour()}:${state.eveningReminderMinute.toString().padStart(2, '0')} PM",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
        )
        Switch(
            checked = state.eveningReminderEnabled,
            onCheckedChange = { enabled ->
                if (enabled && !context.hasNotificationPermission()) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onStateChange(state.copy(eveningReminderEnabled = enabled))
                }
            },
            modifier = Modifier.padding(start = 24.dp),
        )
    }
}

@Composable
private fun OnboardingStep2Screen(
    state: AthleteProfileDraft,
    onStateChange: (AthleteProfileDraft) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    SwipeBackBox(
        onSwipeBack = onBack,
        onSwipeLeft = onNext,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF3B130B),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 28.dp, vertical = 28.dp),
    ) {
        Text(
            text = "02",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
            fontSize = 78.sp,
            modifier = Modifier.align(Alignment.TopEnd),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(56.dp))
            Text(
                text = "YOUR BREATH FOCUS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Where do you\nneed control?",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                lineHeight = 50.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 38.dp),
            )
            Text(
                text = "These moments help HybridTempo choose one breath skill at a time.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .padding(top = 22.dp),
            )
            GoalPillGrid(
                selected = state.goals,
                onToggle = { goal ->
                    val nextGoals = if (goal in state.goals) {
                        state.goals - goal
                    } else {
                        state.goals + goal
                    }
                    onStateChange(state.copy(goals = nextGoals.ifEmpty { listOf("recovery") }))
                },
                modifier = Modifier.padding(top = 56.dp),
            )
            Spacer(modifier = Modifier.height(360.dp))
            OnboardingDots(currentStep = 2)
            PrimaryAction(text = "NEXT", onClick = onNext)
            Spacer(modifier = Modifier.height(18.dp))
        }
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
    SwipeBackBox(
        onSwipeBack = onBack,
        onSwipeLeft = onComplete,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF3B130B),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 28.dp, vertical = 28.dp),
    ) {
        Text(
            text = "03",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
            fontSize = 78.sp,
            modifier = Modifier.align(Alignment.TopEnd),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(56.dp))
            Text(
                text = "YOUR RECOVERY LOOP",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Set your\nfirst routine",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                lineHeight = 50.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 38.dp),
            )
            Text(
                text = "Check in manually, get one protocol, complete a short session, then reflect on what changed.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .padding(top = 22.dp),
            )
            OnboardingSectionLabel(
                text = "AVERAGE SESSION",
                modifier = Modifier.padding(top = 56.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp),
            ) {
                listOf(3, 5, 10).forEach { minutes ->
                    OnboardingPill(
                        text = "$minutes MIN",
                        selected = state.preferredSessionLength == minutes,
                        onClick = { onStateChange(state.copy(preferredSessionLength = minutes)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            OnboardingSectionLabel(
                text = "RACE DATE (OPTIONAL)",
                modifier = Modifier.padding(top = 74.dp),
            )
            Text(
                text = "Used for countdowns and race-prep context.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            OnboardingRaceDatePickerField(
                selectedDate = state.raceDate,
                onDateSelected = { onStateChange(state.copy(raceDate = it)) },
                modifier = Modifier.padding(top = 26.dp),
            )
            OnboardingSectionLabel(
                text = "EVENING REMINDER",
                modifier = Modifier.padding(top = 74.dp),
            )
            EveningReminderCompactRow(
                state = state,
                onStateChange = onStateChange,
                modifier = Modifier.padding(top = 28.dp),
            )
            Text(
                text = "Health data can be connected later. Manual check-ins work now.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(top = 42.dp),
            )
            Spacer(modifier = Modifier.height(170.dp))
            PrimaryAction(text = "GET STARTED", onClick = onComplete)
            OnboardingDots(currentStep = 3)
            Spacer(modifier = Modifier.height(18.dp))
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Eyebrow("Settings")
            Text(
                text = "Athlete defaults",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 22.dp),
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
                title = "Sync",
                body = saveMessage,
                modifier = Modifier.padding(top = 16.dp),
            )
            Spacer(modifier = Modifier.height(18.dp))
            PrimaryAction(text = "SAVE CHANGES", onClick = onSave)
            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}

@Composable
private fun HomeScreen(
    recentSessions: List<BreathworkSession>,
    onSelectMoment: (String) -> Unit,
    onStartStrategy: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
) {
    val presentation = remember { HomePresentation() }
    val streak = remember(recentSessions) { recentSessions.currentStreak() }
    val completedToday = remember(recentSessions) {
        recentSessions.any { session ->
            session.completed && session.completedAt.toLocalDateOrNull() == LocalDate.now()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF3B130B),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        ProfileCircleButton(
            onClick = onSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 36.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(104.dp))
            Eyebrow(presentation.eyebrow)
            Text(
                text = presentation.title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                lineHeight = 52.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp),
            )
            Text(
                text = presentation.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .padding(top = 18.dp),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 54.dp),
            ) {
                InsightCard(
                    title = presentation.primaryAction,
                    body = presentation.flowSummary,
                )
                presentation.secondaryActions.forEach { mode ->
                    HomeModeCard(
                        mode = mode,
                        onClick = { onSelectMoment(mode.sessionIntent) },
                    )
                }
            }
            TextButton(onClick = onStartStrategy, modifier = Modifier.padding(top = 18.dp)) {
                Text("Build a custom workout cue")
            }
            InsightCard(
                title = "Today",
                body = if (completedToday) {
                    "$streak day streak. You logged a breath session today."
                } else {
                    "$streak day streak. No breath session logged today."
                },
                modifier = Modifier.padding(top = 28.dp),
            )
            HomeSecondaryAction(
                text = "View history",
                onClick = onHistory,
                modifier = Modifier.padding(top = 20.dp),
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun HomeModeCard(
    mode: HomeModePresentation,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.66f))
            .border(
                width = 1.2.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                shape = RoundedCornerShape(30.dp),
            )
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                .border(
                    width = 1.4.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = mode.label.take(1),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mode.action,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = mode.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun ProfileCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.34f))
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            val stroke = 2.dp.toPx()
            val icon = Color.White
            drawCircle(
                color = icon,
                radius = size.minDimension * 0.18f,
                center = Offset(size.width / 2f, size.height * 0.32f),
                style = Stroke(width = stroke),
            )
            drawArc(
                color = icon,
                startAngle = 205f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(size.width * 0.22f, size.height * 0.42f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.56f, size.height * 0.52f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun HomeStreakRing(
    streak: Int,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.primary
    val track = Color(0xFF3B130B).copy(alpha = 0.72f)
    val progress = when {
        streak <= 0 -> 0.08f
        streak % 10 == 0 -> 1f
        else -> (streak % 10) / 10f
    }

    Box(
        modifier = modifier.size(290.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = track,
                style = Stroke(width = 4.dp.toPx()),
            )
            drawArc(
                color = color,
                startAngle = -118f,
                sweepAngle = progress.coerceIn(0f, 1f) * 320f,
                useCenter = false,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = streak.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 82.sp,
            )
            Text(
                text = "DAY STREAK",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.4.sp,
            )
        }
    }
}

@Composable
private fun HomePrimaryAction(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.72f)
            .height(82.dp),
        shape = RoundedCornerShape(99.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.6.sp,
        )
    }
}

@Composable
private fun HomeSecondaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(0.72f)
            .height(72.dp),
        shape = RoundedCornerShape(99.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ManualDetailsPromptCard(
    prompt: ManualDetailsPrompt,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = prompt.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = prompt.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.76f),
                modifier = Modifier.padding(top = 8.dp),
            )
            OutlinedButton(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
            ) {
                Text(prompt.actionLabel)
            }
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
                        text = "${readiness.sourceLabel} · ${readiness.confidenceLabel} confidence",
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
            ReadinessSignalSection(
                title = "Based on",
                items = readiness.basedOn,
                emptyText = "Baseline only. Complete a check-in to add useful signal.",
                modifier = Modifier.padding(top = 16.dp),
            )
            if (readiness.missingSignals.isNotEmpty()) {
                ReadinessSignalSection(
                    title = "Missing",
                    items = readiness.missingSignals.take(3),
                    emptyText = "",
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Text(
                text = "Next action",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = readiness.nextAction,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.74f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun ReadinessSignalSection(
    title: String,
    items: List<String>,
    emptyText: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        if (items.isEmpty()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            items.take(4).forEach { item ->
                Text(
                    text = "- $item",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.74f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
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
private fun WorkoutFormatScreen(
    state: CheckInDraft,
    onSettings: () -> Unit,
    onStateChange: (CheckInDraft) -> Unit,
    onHome: () -> Unit,
    onNext: () -> Unit,
) {
    SwipeBackBox(
        onSwipeBack = onHome,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF2A0F0A),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 28.dp, vertical = 34.dp),
    ) {
        TextButton(
            onClick = onHome,
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            Text(
                "Home",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Black,
            )
        }
        ProfileCircleButton(
            onClick = onSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(48.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(42.dp))
            Eyebrow("Step 1 of 2")
            Text(
                text = "What kind of\nworkout is it?",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                lineHeight = 52.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp),
            )
            Text(
                text = "Start with the workout format. This keeps the cue specific to how fatigue will show up.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(top = 18.dp),
            )
            OnboardingSectionLabel(
                text = "WORKOUT FORMAT",
                modifier = Modifier.padding(top = 58.dp),
            )
            WorkoutFormatSelector(
                selected = state.workoutFormat,
                onSelect = { format ->
                    onStateChange(
                        state.copy(
                            workoutFormat = format,
                            workoutType = format.toWorkoutTypeLabel(),
                            sessionIntent = format.toDefaultSessionIntent(),
                        ),
                    )
                },
                modifier = Modifier.padding(top = 28.dp),
            )
            Spacer(modifier = Modifier.height(52.dp))
            HomePrimaryAction(text = "NEXT", onClick = onNext)
            Spacer(modifier = Modifier.height(34.dp))
        }
    }
}

@Composable
private fun BreathingProblemScreen(
    state: CheckInDraft,
    onSettings: () -> Unit,
    onStateChange: (CheckInDraft) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onCue: () -> Unit,
) {
    SwipeBackBox(
        onSwipeBack = onBack,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF2A0F0A),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 28.dp, vertical = 34.dp),
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black)
        }
        ProfileCircleButton(
            onClick = onSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(48.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(42.dp))
            Eyebrow("Step 2 of 2")
            Text(
                text = "What usually\nbreaks down?",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                lineHeight = 52.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp),
            )
            Text(
                text = "Choose the breathing problem. HybridTempo will turn it into one cue for today's ${state.workoutFormat}.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(top = 18.dp),
            )
            OnboardingSectionLabel(
                text = "WHAT BREAKS DOWN?",
                modifier = Modifier.padding(top = 58.dp),
            )
            BreathingProblemSelector(
                selected = state.breathingProblem,
                onSelect = { onStateChange(state.copy(breathingProblem = it)) },
                modifier = Modifier.padding(top = 28.dp),
            )
            InsightCard(
                title = "Today's cue preview",
                body = state.toTodayCuePresentation().let { "${it.category}: ${it.cue}\n${it.why}" },
                modifier = Modifier.padding(top = 34.dp),
            )
            Spacer(modifier = Modifier.height(52.dp))
            HomePrimaryAction(text = "GET TODAY'S CUE", onClick = onCue)
            TextButton(onClick = onHome, modifier = Modifier.padding(top = 8.dp)) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.height(34.dp))
        }
    }
}

@Composable
private fun DotRatingRow(
    label: String,
    value: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
        ) {
            (1..10).forEach { index ->
                val selected = index <= value
                val isActive = index == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isActive -> MaterialTheme.colorScheme.primary
                                selected -> Color(0xFF7A2A16)
                                else -> Color.Transparent
                            },
                        )
                        .border(
                            width = 2.5.dp,
                            color = if (selected) {
                                Color.Transparent
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                            },
                            shape = CircleShape,
                        )
                        .clickable { onSelect(index) },
                )
            }
        }
    }
}

@Composable
private fun HealthWorkoutImportCard(
    status: HealthConnectUiStatus,
    importedWorkouts: List<ImportedWorkout>,
    onConnectHealth: () -> Unit,
    onUseWorkout: (ImportedWorkout) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.66f))
            .border(
                width = 1.2.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                shape = RoundedCornerShape(30.dp),
            )
            .padding(18.dp),
    ) {
        Text(
            text = "HEALTH CONNECT",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.6.sp,
        )
        Text(
            text = when {
                importedWorkouts.isNotEmpty() -> "Workout detected. Use it to prefill this tracking flow."
                status.enabled -> "Connected. Finish a workout in Google Health, then come back here."
                else -> "Connect to import a completed workout from Google Health after it syncs through Health Connect."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (importedWorkouts.isEmpty()) {
            OutlinedButton(
                onClick = onConnectHealth,
                enabled = status.availability == HealthConnectAvailability.Available,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(top = 14.dp),
                shape = RoundedCornerShape(99.dp),
            ) {
                Text(if (status.enabled) "Refresh Health Connect" else "Connect Health Connect")
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 14.dp),
            ) {
                importedWorkouts.forEach { workout ->
                    ImportedWorkoutRow(
                        workout = workout,
                        onClick = { onUseWorkout(workout) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportedWorkoutRow(
    workout: ImportedWorkout,
    onClick: () -> Unit,
) {
    val presentation = remember(workout) { workout.toImportedWorkoutPresentation() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.52f),
                shape = RoundedCornerShape(22.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = presentation.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${presentation.meta} · ${presentation.source}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Text(
            text = "Use",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun WorkoutFormatSelector(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlanOptionGrid(
        options = workoutFormatOptions(),
        selected = selected,
        onSelect = onSelect,
        labelFor = ::workoutFormatDisplayLabel,
        modifier = modifier,
    )
}

@Composable
private fun BreathingProblemSelector(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlanOptionGrid(
        options = breathingProblemOptions(),
        selected = selected,
        onSelect = onSelect,
        labelFor = ::breathingProblemDisplayLabel,
        modifier = modifier,
    )
}

@Composable
private fun WorkoutTypeSelector(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlanOptionGrid(
        options = workoutTypeOptions(),
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
    )
}

@Composable
private fun IntensitySelector(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        intensityOptions().forEach { option ->
            SelectButton(
                text = option.label,
                selected = selected == option.value,
                onClick = { onSelect(option.value) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WorkoutStructureSection(
    state: CheckInDraft,
    onStateChange: (CheckInDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OnboardingSectionLabel("STRUCTURE")
        when (state.workoutType) {
            "Strength", "Conditioning" -> {
                PlanNumberRow(
                    label = "Sets",
                    options = listOf(3, 4, 5),
                    selected = state.setCount,
                    onSelect = { onStateChange(state.copy(setCount = it)) },
                    modifier = Modifier.padding(top = 28.dp),
                )
                PlanNumberRow(
                    label = "Reps",
                    options = listOf(3, 5, 8),
                    selected = state.repsPerSet,
                    onSelect = { onStateChange(state.copy(repsPerSet = it)) },
                    modifier = Modifier.padding(top = 24.dp),
                )
            }

            "Intervals", "Run" -> {
                PlanNumberRow(
                    label = "Rounds",
                    options = listOf(4, 6, 8),
                    selected = state.intervalCount,
                    onSelect = { onStateChange(state.copy(intervalCount = it)) },
                    modifier = Modifier.padding(top = 28.dp),
                )
                PlanNumberRow(
                    label = "Work",
                    options = listOf(1, 2, 3),
                    valueSuffix = " min",
                    selected = state.intervalMinutes,
                    onSelect = { onStateChange(state.copy(intervalMinutes = it)) },
                    modifier = Modifier.padding(top = 24.dp),
                )
            }

            else -> {
                InsightCard(
                    title = "Recovery structure",
                    body = "No sets needed. Use this as a low-load reset before or after training.",
                    modifier = Modifier.padding(top = 28.dp),
                )
            }
        }
    }
}

@Composable
private fun PlanNumberRow(
    label: String,
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    valueSuffix: String = "",
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.6.sp,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            options.forEach { option ->
                SelectButton(
                    text = "$option$valueSuffix",
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BreathWindowSelector(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlanOptionGrid(
        options = breathWindowOptions().map { it.label },
        selected = selected.toBreathWindowLabel(),
        onSelect = { label ->
            breathWindowOptions()
                .firstOrNull { it.label == label }
                ?.value
                ?.let(onSelect)
        },
        modifier = modifier,
    )
}

@Composable
private fun TimeAvailableSelector(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        listOf(2, 5, 10, 15).forEach { minutes ->
            SelectButton(
                text = if (minutes == 15) "15+" else "$minutes min",
                selected = selected == minutes,
                onClick = { onSelect(minutes) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PlanOptionGrid(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    labelFor: (String) -> String = { it },
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        options.chunked(2).forEach { rowOptions ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                rowOptions.forEach { option ->
                    SelectButton(
                        text = labelFor(option),
                        selected = selected == option,
                        onClick = { onSelect(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowOptions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SessionTimingPills(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        listOf(
            SessionIntentOption("Prime", "pre_workout"),
            SessionIntentOption("Recover", "post_workout"),
            SessionIntentOption("Sleep", "evening_downshift"),
        ).forEach { option ->
            OnboardingPill(
                text = option.label,
                selected = selected == option.value,
                onClick = { onSelect(option.value) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WorkoutContextPills(
    selected: String,
    onSelect: (workoutType: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            listOf("Hybrid", "Intervals", "Strength").forEach { workoutType ->
                OnboardingPill(
                    text = workoutType,
                    selected = selected == workoutType,
                    onClick = { onSelect(workoutType) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        OnboardingPill(
            text = "Rest",
            selected = selected == "Rest",
            onClick = { onSelect("Rest") },
            modifier = Modifier.fillMaxWidth(0.28f),
        )
    }
}

private data class SessionIntentOption(
    val label: String,
    val value: String,
)

@Composable
private fun BreathWindowsScreen(
    onSettings: () -> Unit,
    primaryAction: String,
    secondaryAction: String,
    onContinue: () -> Unit,
    onEdit: () -> Unit,
) {
    val presentation = remember { BreathWindowsPresentation() }

    ScreenFrame(horizontalAlignment = Alignment.CenterHorizontally) {
        ScreenHeader(
            eyebrow = presentation.eyebrow,
            title = presentation.title,
            onSettings = onSettings,
        )
        Text(
            text = presentation.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(bottom = 32.dp),
        )
        BreathWindowsModelCard(
            windows = presentation.windows,
            modifier = Modifier.padding(top = 10.dp),
        )
        InsightCard(
            title = "Remember this",
            body = presentation.reminder,
            modifier = Modifier.padding(top = 22.dp),
        )
        Spacer(modifier = Modifier.height(42.dp))
        PrimaryAction(text = primaryAction, onClick = onContinue)
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(top = 12.dp),
            shape = RoundedCornerShape(99.dp),
        ) {
            Text(secondaryAction)
        }
    }
}

@Composable
private fun TodayCueScreen(
    draft: CheckInDraft,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    onPractice: () -> Unit,
    onReview: () -> Unit,
    onEdit: () -> Unit,
) {
    val cue = draft.toTodayCuePresentation()
    val guidedSession = remember(draft.sessionIntent) {
        selectGuidedSessionForIntent(draft.sessionIntent)
    }

    ScreenFrame(
        horizontalAlignment = Alignment.CenterHorizontally,
        onSwipeBack = onBack,
    ) {
        ScreenHeader(
            eyebrow = "Today's cue",
            title = cue.cue,
            onSettings = onSettings,
        )
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Text("Back to strategy")
        }
        Text(
            text = "${cue.workoutFormat} · ${cue.category}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        InsightCard(
            title = "Problem",
            body = cue.breathingProblem,
            modifier = Modifier.padding(top = 34.dp),
        )
        InsightCard(
            title = "Why this fits",
            body = cue.why,
            modifier = Modifier.padding(top = 14.dp),
        )
        InsightCard(
            title = "Practice",
            body = cue.practice,
            modifier = Modifier.padding(top = 14.dp),
        )
        InsightCard(
            title = "Guided session",
            body = "${guidedSession.title} · ${formatTime(guidedSession.durationSeconds)}\n${guidedSession.intention}",
            modifier = Modifier.padding(top = 14.dp),
        )
        TodayBreathWindowsCard(
            cue = cue,
            modifier = Modifier.padding(top = 14.dp),
        )
        InsightCard(
            title = "Review after training",
            body = cue.reviewQuestion,
            modifier = Modifier.padding(top = 14.dp),
        )
        Spacer(modifier = Modifier.height(38.dp))
        PrimaryAction(text = cue.practiceAction.uppercase(), onClick = onPractice)
        OutlinedButton(
            onClick = onReview,
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(top = 12.dp),
            shape = RoundedCornerShape(99.dp),
        ) {
            Text("Review workout")
        }
        TextButton(onClick = onEdit, modifier = Modifier.padding(top = 6.dp)) {
            Text("Adjust strategy")
        }
    }
}

@Composable
private fun TodayBreathWindowsCard(
    cue: TodayCuePresentation,
    modifier: Modifier = Modifier,
) {
    InsightCard(
        title = "Today's breath windows",
        body = "Before: Start controlled.\nDuring: ${cue.cue}\nBetween: Reset before the next movement.\nAfter: Slow the exhale before you leave the gym.",
        modifier = modifier,
    )
}

@Composable
private fun WorkoutReviewScreen(
    draft: WorkoutReviewDraft,
    cue: TodayCuePresentation,
    healthConnectStatus: HealthConnectUiStatus,
    onSettings: () -> Unit,
    onDraftChange: (WorkoutReviewDraft) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val presentation = draft.toWorkoutReviewPresentation(cue)

    ScreenFrame(
        horizontalAlignment = Alignment.CenterHorizontally,
        onSwipeBack = onBack,
    ) {
        ScreenHeader(
            eyebrow = "Workout review",
            title = "Did the cue\nhelp?",
            onSettings = onSettings,
        )
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Text("Back to cue")
        }
        InsightCard(
            title = "Cue used",
            body = presentation.cue,
            modifier = Modifier.padding(top = 18.dp),
        )
        InsightCard(
            title = "Question",
            body = presentation.reviewQuestion,
            modifier = Modifier.padding(top = 14.dp),
        )
        HealthConnectInsightCard(
            status = healthConnectStatus,
            onAction = onSettings,
            modifier = Modifier.padding(top = 14.dp),
        )
        DotRatingRow(
            label = "Cue helped",
            value = draft.cueHelpfulness,
            onSelect = { onDraftChange(draft.copy(cueHelpfulness = it)) },
            modifier = Modifier.padding(top = 34.dp),
        )
        DotRatingRow(
            label = "Breath control",
            value = draft.breathControl,
            onSelect = { onDraftChange(draft.copy(breathControl = it)) },
            modifier = Modifier.padding(top = 34.dp),
        )
        ReflectionNotesField(
            value = draft.notes,
            onValueChange = { onDraftChange(draft.copy(notes = it)) },
            modifier = Modifier.padding(top = 30.dp),
        )
        Text(
            text = presentation.scoreSummary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp),
        )
        Spacer(modifier = Modifier.height(34.dp))
        Button(
            onClick = onComplete,
            enabled = draft.isComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(99.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(
                text = "SAVE WORKOUT REVIEW",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.8.sp,
            )
        }
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
            Text("Not now")
        }
    }
}

@Composable
private fun BreathWindowsModelCard(
    windows: List<BreathWindowPresentation>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.58f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(32.dp),
            )
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "THE BREATH WINDOWS MODEL",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.6.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Breathe where you can. Brace where you must. Reset between efforts.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
        )
        windows.chunked(2).forEach { rowWindows ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                rowWindows.forEach { window ->
                    BreathWindowTile(
                        window = window,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BreathWindowTile(
    window: BreathWindowPresentation,
    modifier: Modifier = Modifier,
) {
    val accent = window.colorRole.toAccentColor()
    Column(
        modifier = modifier
            .height(156.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(
                width = 1.4.dp,
                color = accent.copy(alpha = 0.88f),
                shape = RoundedCornerShape(26.dp),
            )
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = window.number.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = accent,
        )
        Text(
            text = window.title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = window.cue,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun BreathWindowColorRole.toAccentColor(): Color = when (this) {
    BreathWindowColorRole.Settle -> Color(0xFF21D5B5)
    BreathWindowColorRole.Effort -> MaterialTheme.colorScheme.primary
    BreathWindowColorRole.Reset -> Color(0xFF2E9BFF)
    BreathWindowColorRole.Recover -> Color(0xFF8C5CFF)
}

@Composable
private fun SessionIntentGrid(
    selected: String,
    onSelect: (String) -> Unit,
) {
    val options = listOf(
        SessionIntentOption("Before workout", "pre_workout"),
        SessionIntentOption("After workout", "post_workout"),
        SessionIntentOption("Evening", "evening_downshift"),
        SessionIntentOption("Reset", "general_reset"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.chunked(2).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                rowOptions.forEach { option ->
                    SelectButton(
                        text = option.label,
                        selected = selected == option.value,
                        onClick = { onSelect(option.value) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationScreen(
    recommendation: BreathworkRecommendation,
    planSummary: WorkoutPlanSummary,
    recommendationSource: RecommendationSource,
    recommendationQuota: RecommendationQuota?,
    recommendationNotice: String?,
    isRefreshingRecommendation: Boolean,
    saveMessage: String,
    onSettings: () -> Unit,
    onStartSession: () -> Unit,
    onEdit: () -> Unit,
) {
    val presentation = recommendation.toRecommendationPresentation()

    ScreenFrame(horizontalAlignment = Alignment.CenterHorizontally) {
        ScreenHeader(
            eyebrow = "Your protocol",
            title = presentation.title,
            onSettings = onSettings,
        )
        Text(
            text = presentation.meta,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        if (isRefreshingRecommendation) {
            Text(
                text = "Tuning this to your check-in...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        InsightCard(
            title = "Workout plan",
            body = "${planSummary.headline}\n${planSummary.structure} · ${planSummary.breathWindow}\n${planSummary.timeAvailable}",
            modifier = Modifier.padding(top = 42.dp),
        )
        InsightCard(
            title = "Why this fits",
            body = presentation.rationale,
            modifier = Modifier.padding(top = 14.dp),
        )
        InsightCard(
            title = "Cadence",
            body = presentation.cadence,
            modifier = Modifier.padding(top = 14.dp),
        )
        InsightCard(
            title = "Cue",
            body = presentation.trainingCue.ifBlank { "Stay easy. Let the breath set the pace." },
            modifier = Modifier.padding(top = 14.dp),
        )
        val quotaMessage = recommendationNotice ?: recommendationQuota?.toUsageMessage()
        quotaMessage?.let {
            InsightCard(
                title = "Status",
                body = it,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
        Spacer(modifier = Modifier.height(42.dp))
        PrimaryAction(text = "START SESSION", onClick = onStartSession)
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(top = 12.dp),
            shape = RoundedCornerShape(99.dp),
        ) {
            Text("Adjust check-in")
        }
    }
}

@Composable
private fun GuideModeScreen(
    selected: GuideModePresentation,
    onSettings: () -> Unit,
    onSelect: (GuideModePresentation) -> Unit,
    onStart: () -> Unit,
    backLabel: String,
    onBack: () -> Unit,
) {
    val modes = remember { defaultGuideModes() }
    val active = selected.toActiveGuidePresentation()

    ScreenFrame(
        horizontalAlignment = Alignment.CenterHorizontally,
        onSwipeBack = onBack,
    ) {
        ScreenHeader(
            eyebrow = "Visual guide",
            title = "Choose your\nbreath window",
            onSettings = onSettings,
        )
        Text(
            text = "Use Learn to practice, Pre-set to settle, Between sets to recover, or Post-workout to downshift.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(bottom = 28.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            modes.forEach { mode ->
                GuideModeCard(
                    mode = mode,
                    selected = mode.value == selected.value,
                    onClick = { onSelect(mode) },
                )
            }
        }
        InsightCard(
            title = active.eyebrow,
            body = "${active.primaryCue}\n${active.supportingCue}",
            modifier = Modifier.padding(top = 24.dp),
        )
        Spacer(modifier = Modifier.height(38.dp))
        PrimaryAction(text = "START GUIDE", onClick = onStart)
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(top = 12.dp),
            shape = RoundedCornerShape(99.dp),
        ) {
            Text(backLabel)
        }
    }
}

@Composable
private fun GuideModeCard(
    mode: GuideModePresentation,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
                },
            )
            .border(
                width = 1.4.dp,
                color = accent,
                shape = RoundedCornerShape(28.dp),
            )
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mode.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = mode.purpose,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Text(
            text = mode.windowTitle,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 14.dp),
        )
    }
}

@Composable
private fun SessionScreen(
    recommendation: BreathworkRecommendation,
    guideMode: GuideModePresentation,
    guidedSession: GuidedBreathworkSession,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(true) }
    var guidedAudioEnabled by remember { mutableStateOf(true) }
    val guidedAudioController = remember(guidedSession.audioTrackName) {
        GuidedAudioController(
            context = context.applicationContext,
            trackName = guidedSession.audioTrackName,
        )
    }

    LaunchedEffect(running, elapsedSeconds) {
        if (running && elapsedSeconds < guidedSession.durationSeconds) {
            delay(1000)
            elapsedSeconds += 1
        }
    }

    LaunchedEffect(running, guidedAudioEnabled, guidedSession.audioTrackName) {
        guidedAudioController.setPlaying(running && guidedAudioEnabled)
    }

    androidx.compose.runtime.DisposableEffect(guidedAudioController) {
        onDispose { guidedAudioController.release() }
    }

    ScreenFrame(
        horizontalAlignment = Alignment.CenterHorizontally,
        onSwipeBack = onBack,
    ) {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Text("Back to guide")
        }
        Eyebrow("Guided audio")
        Text(
            text = guidedSession.title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            lineHeight = 46.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = guidedSession.intention,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(modifier = Modifier.height(34.dp))
        WaveBreathGuide(
            isRunning = running,
            visualizationStyle = guidedSession.visualizationStyle,
        )
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = formatTime(guidedSession.durationSeconds - elapsedSeconds),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Guided audio",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
            Switch(
                checked = guidedAudioEnabled,
                onCheckedChange = { guidedAudioEnabled = it },
            )
        }
        Spacer(modifier = Modifier.height(46.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = { running = !running },
                modifier = Modifier
                    .weight(1f)
                    .height(62.dp),
                shape = RoundedCornerShape(99.dp),
            ) {
                Text(if (running) "Pause" else "Resume")
            }
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .weight(1f)
                    .height(62.dp),
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Finish", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun WorkoutHandoffScreen(
    cue: TodayCuePresentation,
    healthConnectStatus: HealthConnectUiStatus,
    onSettings: () -> Unit,
    onHome: () -> Unit,
    onReview: () -> Unit,
    onBack: () -> Unit,
) {
    val handoff = remember(cue) { cue.toWorkoutHandoffPresentation() }

    ScreenFrame(
        horizontalAlignment = Alignment.CenterHorizontally,
        onSwipeBack = onBack,
    ) {
        ScreenHeader(
            eyebrow = "Use it today",
            title = handoff.title,
            onSettings = onSettings,
        )
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Text("Back to practice")
        }
        InsightCard(
            title = "Your cue",
            body = cue.cue,
            modifier = Modifier.padding(top = 18.dp),
        )
        InsightCard(
            title = "During the workout",
            body = handoff.workoutInstruction,
            modifier = Modifier.padding(top = 14.dp),
        )
        InsightCard(
            title = "After training",
            body = "${handoff.reviewInstruction}\n\n${handoff.reviewQuestion}",
            modifier = Modifier.padding(top = 14.dp),
        )
        HealthConnectInsightCard(
            status = healthConnectStatus,
            onAction = onSettings,
            modifier = Modifier.padding(top = 14.dp),
        )
        Spacer(modifier = Modifier.height(42.dp))
        PrimaryAction(text = "I'LL COME BACK AFTER TRAINING", onClick = onHome)
        OutlinedButton(
            onClick = onReview,
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(top = 12.dp),
            shape = RoundedCornerShape(99.dp),
        ) {
            Text("Review now")
        }
    }
}

@Composable
private fun SetCueScreen(
    guideMode: GuideModePresentation,
    onSettings: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val activeGuide = remember(guideMode) { guideMode.toActiveGuidePresentation() }

    ScreenFrame(horizontalAlignment = Alignment.CenterHorizontally) {
        ScreenHeader(
            eyebrow = "Working set",
            title = activeGuide.duringSetCue,
            onSettings = onSettings,
        )
        Text(
            text = "Keep the cue short. The goal is to remember it when the session gets hard.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(top = 8.dp),
        )
        Spacer(modifier = Modifier.height(88.dp))
        CuePulseMark()
        Spacer(modifier = Modifier.height(66.dp))
        InsightCard(
            title = "During the set",
            body = "Do not overthink breathing. Breathe where you can, brace where you must, then reset when the window opens.",
        )
        Spacer(modifier = Modifier.height(42.dp))
        PrimaryAction(text = "FINISH SET", onClick = onContinue)
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(top = 12.dp),
            shape = RoundedCornerShape(99.dp),
        ) {
            Text("Back to guide")
        }
    }
}

@Composable
private fun CuePulseMark() {
    val transition = rememberInfiniteTransition(label = "cue-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cue-pulse-scale",
    )
    val color = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.size(170.dp)) {
        drawCircle(
            color = color.copy(alpha = 0.12f),
            radius = size.minDimension * 0.46f * pulse,
            style = Stroke(width = 2.dp.toPx()),
        )
        drawCircle(
            color = color.copy(alpha = 0.22f),
            radius = size.minDimension * 0.32f * pulse,
        )
        drawCircle(
            color = color.copy(alpha = 0.88f),
            radius = size.minDimension * 0.12f,
        )
    }
}

@Composable
private fun SessionReflectionScreen(
    recommendation: BreathworkRecommendation,
    draft: SessionReflectionDraft,
    onDraftChange: (SessionReflectionDraft) -> Unit,
    onSettings: () -> Unit,
    onBackToSession: () -> Unit,
    onComplete: () -> Unit,
) {
    val recommendationPresentation = recommendation.toRecommendationPresentation()
    val reflectionPresentation = draft.toSessionReflectionPresentation()

    ScreenFrame(horizontalAlignment = Alignment.CenterHorizontally) {
        ScreenHeader(
            eyebrow = "Reflection",
            title = "How did\nthat land?",
            onSettings = onSettings,
        )
        Text(
            text = recommendation.protocol,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp),
        )
        InsightCard(
            title = "Cue used",
            body = recommendationPresentation.trainingCue,
            modifier = Modifier.padding(top = 20.dp),
        )
        DotRatingRow(
            label = "Control",
            value = draft.perceivedControl.coerceAtLeast(1),
            onSelect = { onDraftChange(draft.copy(perceivedControl = it)) },
            modifier = Modifier.padding(top = 34.dp),
        )
        DotRatingRow(
            label = "Recovery",
            value = draft.perceivedRecovery.coerceAtLeast(1),
            onSelect = { onDraftChange(draft.copy(perceivedRecovery = it)) },
            modifier = Modifier.padding(top = 34.dp),
        )
        OnboardingSectionLabel(
            text = "AFTERWARD",
            modifier = Modifier.padding(top = 42.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
        ) {
            SessionReflectionFeeling.entries.forEach { feeling ->
                OnboardingPill(
                    text = feeling.label,
                    selected = draft.feeling == feeling,
                    onClick = { onDraftChange(draft.copy(feeling = feeling)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        ReflectionNotesField(
            value = draft.notes,
            onValueChange = { onDraftChange(draft.copy(notes = it)) },
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = "${reflectionPresentation.scoreSummary} · ${reflectionPresentation.feelingLabel}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp),
        )
        Spacer(modifier = Modifier.height(34.dp))
        Button(
            onClick = onComplete,
            enabled = draft.isComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(99.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(
                text = "SAVE REFLECTION",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.8.sp,
            )
        }
        OutlinedButton(
            onClick = onBackToSession,
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(top = 12.dp),
            shape = RoundedCornerShape(99.dp),
        ) {
            Text("Back to session")
        }
    }
}

@Composable
private fun ReflectionNotesField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OnboardingSectionLabel(text = "NOTES")
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.68f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(18.dp),
            decorationBox = { innerTextField ->
                if (value.isBlank()) {
                    Text(
                        text = "Optional: what changed after the session?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    )
                }
                innerTextField()
            },
        )
    }
}

@Composable
private fun HistoryScreen(
    recommendation: BreathworkRecommendation,
    recommendationSource: RecommendationSource,
    sessions: List<BreathworkSession>,
    checkIns: List<DailyCheckIn>,
    healthConnectStatus: HealthConnectUiStatus,
    saveMessage: String,
    onSettings: () -> Unit,
    onCheckIn: () -> Unit,
    onHome: () -> Unit,
) {
    val summary = remember(sessions) { sessions.toHistorySummary() }
    val impact = remember(sessions) { sessions.toHistoryImpactPresentation() }

    ScreenFrame {
        ScreenHeader(
            eyebrow = "History",
            title = "Recovery\nconsistency",
            onSettings = onSettings,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(summary.currentStreak.toString(), "day streak", Modifier.weight(1f))
            MetricCard(summary.totalSessions.toString(), "sessions", Modifier.weight(1f))
            MetricCard(summary.totalMinutes.toString(), "minutes", Modifier.weight(1f))
        }
        ImpactSummaryCard(
            impact = impact,
            modifier = Modifier.padding(top = 22.dp),
        )
        HealthConnectInsightCard(
            status = healthConnectStatus,
            onAction = onSettings,
            modifier = Modifier.padding(top = 18.dp),
        )
        Text(
            text = "Recent sessions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 34.dp, bottom = 4.dp),
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
        Spacer(modifier = Modifier.height(42.dp))
        PrimaryAction(text = "NEW CHECK-IN", onClick = onCheckIn)
        OutlinedButton(
            onClick = onHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(top = 12.dp),
            shape = RoundedCornerShape(99.dp),
        ) {
            Text("Back to readiness")
        }
    }
}

@Composable
private fun ImpactSummaryCard(
    impact: HistoryImpactPresentation,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(30.dp),
            )
            .padding(18.dp),
    ) {
        Text(
            text = "BREATH IMPACT",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.6.sp,
        )
        Text(
            text = impact.sampleLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
        ) {
            MetricCard(
                value = impact.averageControlLabel,
                label = "control",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                value = impact.averageRecoveryLabel,
                label = "recovery",
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = impact.message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (impact.hasReflectionData) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(top = 18.dp),
        )
    }
}

@Composable
private fun HealthConnectInsightCard(
    status: HealthConnectUiStatus,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val presentation = remember(status) { status.toHealthConnectInsightPresentation() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(28.dp),
            )
            .padding(18.dp),
    ) {
        Text(
            text = presentation.title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.6.sp,
        )
        Text(
            text = presentation.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
        presentation.actionLabel?.let { label ->
            TextButton(
                onClick = onAction,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun ScreenFrame(
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    modifier: Modifier = Modifier,
    onSwipeBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val frameContent: @Composable BoxScope.() -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF3B130B),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                )
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 30.dp),
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
    }

    if (onSwipeBack == null) {
        Box(modifier = modifier.fillMaxSize(), content = frameContent)
    } else {
        SwipeBackBox(
            onSwipeBack = onSwipeBack,
            modifier = modifier.fillMaxSize(),
            content = frameContent,
        )
    }
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
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 22.dp, bottom = 18.dp)
                .verticalScroll(rememberScrollState()),
            content = content,
        )
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
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ScreenHeader(
    eyebrow: String,
    title: String,
    onSettings: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 42.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Eyebrow(eyebrow)
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                lineHeight = 42.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        ProfileCircleButton(
            onClick = onSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(46.dp),
        )
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
    Box(
        modifier = modifier
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                },
            )
            .border(
                width = 1.3.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(99.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun InsightCard(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(28.dp),
            )
            .padding(18.dp),
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.6.sp,
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
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
    val presentation = session.toHistorySessionPresentation()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        presentation.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        session.completedAt.toHistoryDateLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    presentation.durationLabel,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            if (presentation.hasReflection) {
                Text(
                    text = presentation.reflectionSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Text(
                    text = presentation.feelingLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (presentation.notes.isNotBlank()) {
                    Text(
                        text = presentation.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
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
private fun WaveBreathGuide(
    isRunning: Boolean,
    visualizationStyle: BreathworkVisualizationStyle,
    modifier: Modifier = Modifier,
) {
    val surfaceShape = RoundedCornerShape(34.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(surfaceShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.58f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                shape = surfaceShape,
            )
            .padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BreathFieldCanvas(
            isRunning = isRunning,
            visualizationStyle = visualizationStyle,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
        )
    }
}

@Composable
private fun BreathFieldCanvas(
    isRunning: Boolean,
    visualizationStyle: BreathworkVisualizationStyle,
    modifier: Modifier = Modifier,
) {
    val canvasPresentation = visualizationStyle.toGuidedVisualizationPresentation()
    val waveDrift = remember { androidx.compose.animation.core.Animatable(0f) }
    val ringDrift = remember { androidx.compose.animation.core.Animatable(0f) }
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(canvasPresentation.animationKey, isRunning) {
        if (!isRunning) {
            waveDrift.stop()
            ringDrift.stop()
            return@LaunchedEffect
        }

        kotlinx.coroutines.coroutineScope {
            launch {
                while (true) {
                    waveDrift.animateTo(
                        targetValue = waveDrift.value + 1f,
                        animationSpec = tween(
                            durationMillis = 5200,
                            easing = LinearEasing,
                        ),
                    )
                }
            }
            launch {
                while (true) {
                    ringDrift.animateTo(
                        targetValue = ringDrift.value + 1f,
                        animationSpec = tween(
                            durationMillis = 6400,
                            easing = LinearEasing,
                        ),
                    )
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        when (canvasPresentation.style) {
            BreathworkVisualizationStyle.Gather -> drawGatherVisualization(
                drift = waveDrift.value,
                pulse = ringDrift.value,
                primary = primary,
                muted = muted,
            )

            BreathworkVisualizationStyle.Brace -> drawBraceVisualization(
                drift = waveDrift.value,
                pulse = ringDrift.value,
                primary = primary,
                muted = muted,
            )

            BreathworkVisualizationStyle.Reset -> drawResetVisualization(
                drift = waveDrift.value,
                pulse = ringDrift.value,
                primary = primary,
                muted = muted,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGatherVisualization(
    drift: Float,
    pulse: Float,
    primary: Color,
    muted: Color,
) {
    val pi = PI.toFloat()
    val center = Offset(size.width * 0.5f, size.height * 0.52f)
    val blue = Color(0xFF9BB5C8)
    val green = Color(0xFF9DB5A0)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primary.copy(alpha = 0.2f),
                blue.copy(alpha = 0.08f),
                Color.Transparent,
            ),
            center = center,
            radius = size.minDimension * 0.46f,
        ),
        radius = size.minDimension * 0.46f,
        center = center,
    )

    repeat(4) { index ->
        val offset = ((pulse + index / 4f) % 1f).coerceIn(0f, 1f)
        val radius = size.minDimension * (0.08f + smoothStep(offset) * 0.25f)
        val alpha = (sin(offset * pi) * 0.22f).coerceAtLeast(0f)
        drawCircle(
            color = primary.copy(alpha = alpha),
            radius = radius,
            center = center,
            style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
        )
    }

    repeat(42) { index ->
        val seed = index + 1
        val baseAngle = seed * 2.3999632f
        val radiusBand = 0.18f + ((seed * 37) % 100) / 100f * 0.42f
        val gather = 0.78f + (sin((drift * 2f * pi) + seed) * 0.1f)
        val radius = size.minDimension * radiusBand * gather
        val wobble = sin((drift * 2.4f * pi) + seed * 0.31f) * 0.07f
        val x = center.x + cos(baseAngle + wobble) * radius
        val y = center.y + sin(baseAngle - wobble) * radius * 0.86f
        val dotColor = when (index % 3) {
            0 -> blue
            1 -> green
            else -> muted
        }
        val dotRadius = (2.2f + (seed % 5) * 1.15f).dp.toPx()

        drawCircle(
            color = dotColor.copy(alpha = 0.22f + (seed % 4) * 0.06f),
            radius = dotRadius,
            center = Offset(x, y),
        )
    }

    drawCircle(
        color = primary.copy(alpha = 0.16f),
        radius = 30.dp.toPx(),
        center = center,
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.86f),
        radius = 12.dp.toPx(),
        center = center,
    )
    drawCircle(
        color = primary.copy(alpha = 0.7f),
        radius = 5.dp.toPx(),
        center = center,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBraceVisualization(
    drift: Float,
    pulse: Float,
    primary: Color,
    muted: Color,
) {
    val pi = PI.toFloat()
    val centerX = size.width * 0.5f
    val centerY = size.height * 0.52f
    val braceGreen = Color(0xFF86A681)
    val structureBlue = Color(0xFF8FAFC9)

    drawLine(
        color = muted.copy(alpha = 0.34f),
        start = Offset(centerX, size.height * 0.12f),
        end = Offset(centerX, size.height * 0.9f),
        strokeWidth = 1.2.dp.toPx(),
        cap = StrokeCap.Round,
    )

    drawOval(
        color = Color.White.copy(alpha = 0.035f),
        topLeft = Offset(size.width * 0.22f, size.height * 0.08f),
        size = androidx.compose.ui.geometry.Size(size.width * 0.56f, size.height * 0.78f),
    )

    repeat(8) { index ->
        val verticalProgress = index / 7f
        val y = size.height * (0.18f + verticalProgress * 0.62f)
        val breathPulse = 0.92f + sin((pulse * 2f * pi) + index * 0.35f) * 0.055f
        val width = size.width * (0.48f + sin(verticalProgress * pi) * 0.16f) * breathPulse
        val height = size.height * 0.055f
        val alpha = 0.18f + sin(verticalProgress * pi) * 0.24f
        val color = if (index == 4) braceGreen else structureBlue

        drawOval(
            color = color.copy(alpha = alpha),
            topLeft = Offset(centerX - width / 2f, y - height / 2f),
            size = androidx.compose.ui.geometry.Size(width, height),
            style = Stroke(width = if (index == 4) 4.dp.toPx() else 2.dp.toPx(), cap = StrokeCap.Round),
        )
    }

    val controlY = centerY + sin(drift * 2f * pi) * size.height * 0.035f
    drawLine(
        color = braceGreen.copy(alpha = 0.5f),
        start = Offset(size.width * 0.22f, controlY),
        end = Offset(size.width * 0.78f, controlY),
        strokeWidth = 1.2.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawCircle(
        color = braceGreen.copy(alpha = 0.18f),
        radius = 22.dp.toPx(),
        center = Offset(centerX, controlY),
    )
    drawCircle(
        color = braceGreen.copy(alpha = 0.86f),
        radius = 6.dp.toPx(),
        center = Offset(centerX, controlY),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawResetVisualization(
    drift: Float,
    pulse: Float,
    primary: Color,
    muted: Color,
) {
    val pi = PI.toFloat()
    val warm = Color(0xFFFFB66D)
    val lavender = Color(0xFF9B85B7)
    val blue = Color(0xFF6A9CC4)
    val glowCenter = Offset(size.width * 0.5f, size.height * (0.28f + sin(pulse * 2f * pi) * 0.02f))

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                warm.copy(alpha = 0.32f),
                primary.copy(alpha = 0.12f),
                Color.Transparent,
            ),
            center = glowCenter,
            radius = size.minDimension * 0.72f,
        ),
        radius = size.minDimension * 0.72f,
        center = glowCenter,
    )

    repeat(5) { layer ->
        val path = Path()
        val baseY = size.height * (0.48f + layer * 0.082f)
        val amplitude = size.height * (0.035f + layer * 0.01f)
        val phase = drift * 1.25f + layer * 0.22f
        val steps = 92

        for (step in 0..steps) {
            val progress = step.toFloat() / steps
            val x = progress * size.width
            val y = baseY + sin((progress * 1.7f - phase) * 2f * pi) * amplitude
            if (step == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.lineTo(size.width, size.height)
        path.lineTo(0f, size.height)
        path.close()

        val color = when (layer % 3) {
            0 -> warm
            1 -> lavender
            else -> blue
        }
        drawPath(
            path = path,
            color = color.copy(alpha = 0.08f + layer * 0.025f),
        )
    }

    repeat(11) { index ->
        val progress = index / 10f
        val y = size.height * (0.24f + progress * 0.56f)
        val x = size.width * 0.5f + sin((progress * 2.7f + drift) * 2f * pi) * size.width * 0.04f
        val alpha = 0.08f + progress * 0.16f
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = (1.8f + progress * 2.8f).dp.toPx(),
            center = Offset(x, y),
        )
    }

    val bottomCenter = Offset(size.width * 0.5f, size.height * 0.82f)
    drawCircle(
        color = lavender.copy(alpha = 0.18f),
        radius = 24.dp.toPx(),
        center = bottomCenter,
    )
    drawCircle(
        color = lavender.copy(alpha = 0.78f),
        radius = 7.dp.toPx(),
        center = bottomCenter,
    )
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
            .height(72.dp),
        shape = RoundedCornerShape(99.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.8.sp,
        )
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.8.sp,
        textAlign = TextAlign.Center,
    )
}

private fun formatTime(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val remainder = safeSeconds % 60
    return "$minutes:${remainder.toString().padStart(2, '0')}"
}

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
