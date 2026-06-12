package com.hybridtempo.android.ui

import com.hybridtempo.android.data.BreathPhase

data class WaveBreathPresentation(
    val title: String,
    val phaseCue: String,
    val instruction: String,
    val progress: Float,
    val emphasizesControl: Boolean,
)

data class ContinuousWaveCanvasPresentation(
    val animationKey: String,
    val amplitudeScale: Float,
)

data class BreathFieldCanvasPresentation(
    val animationKey: String,
    val waveCount: Int,
    val focusProgress: Float,
)

data class GuidedVisualizationPresentation(
    val style: BreathworkVisualizationStyle,
    val animationKey: String,
    val primaryMotion: String,
    val supportingMotion: String,
)

fun BreathPhase.toWaveBreathPresentation(progress: Float): WaveBreathPresentation {
    val normalizedLabel = label.lowercase()
    val phaseCue = when {
        normalizedLabel.contains("inhale") -> "Wave rising"
        normalizedLabel.contains("exhale") -> "Ride the exhale down"
        else -> "Hold the rhythm"
    }

    return WaveBreathPresentation(
        title = "Ride the wave",
        phaseCue = phaseCue,
        instruction = instruction,
        progress = progress.coerceIn(0f, 1f),
        emphasizesControl = true,
    )
}

fun wavePhaseAnimationDurationMillis(
    phaseSeconds: Int,
    currentProgress: Float,
): Int {
    val remainingProgress = 1f - currentProgress.coerceIn(0f, 1f)
    return (phaseSeconds.coerceAtLeast(0) * 1000 * remainingProgress).toInt()
}

fun anchoredWaveFocusProgress(): Float = 0.5f

fun waveSessionStatusLabel(
    phaseLabel: String,
    nextPhaseLabel: String,
): String = "$phaseLabel now - Next: $nextPhaseLabel"

fun String.toContinuousWaveCanvasPresentation(): ContinuousWaveCanvasPresentation {
    val normalizedLabel = lowercase()
    val amplitudeScale = when {
        normalizedLabel.contains("inhale") -> 0.26f
        normalizedLabel.contains("exhale") -> 0.18f
        else -> 0.1f
    }

    return ContinuousWaveCanvasPresentation(
        animationKey = "continuous_wave",
        amplitudeScale = amplitudeScale,
    )
}

fun breathFieldCanvasPresentation(): BreathFieldCanvasPresentation = BreathFieldCanvasPresentation(
    animationKey = "breath_field",
    waveCount = 3,
    focusProgress = 0.5f,
)

fun BreathworkVisualizationStyle.toGuidedVisualizationPresentation(): GuidedVisualizationPresentation {
    return when (this) {
        BreathworkVisualizationStyle.Gather -> GuidedVisualizationPresentation(
            style = this,
            animationKey = "gather_particles",
            primaryMotion = "scattered_elements_gather_inward",
            supportingMotion = "center_rings_stabilize",
        )

        BreathworkVisualizationStyle.Brace -> GuidedVisualizationPresentation(
            style = this,
            animationKey = "brace_rings",
            primaryMotion = "stacked_rings_expand_evenly",
            supportingMotion = "vertical_axis_stays_aligned",
        )

        BreathworkVisualizationStyle.Reset -> GuidedVisualizationPresentation(
            style = this,
            animationKey = "reset_waves",
            primaryMotion = "waves_lower_and_soften",
            supportingMotion = "warm_glow_downshifts",
        )
    }
}
