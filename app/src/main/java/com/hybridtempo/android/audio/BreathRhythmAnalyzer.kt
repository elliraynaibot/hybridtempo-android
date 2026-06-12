package com.hybridtempo.android.audio

import kotlin.math.roundToInt
import kotlin.math.sqrt

data class BreathRhythmCheckResult(
    val rhythmMatchedPercent: Int,
    val detectedBreaths: Int,
    val durationSeconds: Int,
)

object BreathRhythmAnalyzer {
    fun analyze(
        amplitudeSamples: List<Float>,
        durationSeconds: Int,
    ): BreathRhythmCheckResult {
        val samples = amplitudeSamples
            .map { it.coerceAtLeast(0f) }
            .filter { java.lang.Float.isFinite(it) }

        if (samples.size < 6) {
            return BreathRhythmCheckResult(
                rhythmMatchedPercent = 0,
                detectedBreaths = 0,
                durationSeconds = durationSeconds,
            )
        }

        val threshold = maxOf(
            samples.average().toFloat() + (samples.standardDeviation() * 0.25f),
            (samples.maxOrNull() ?: 0f) * 0.35f,
        )
        val peakIndexes = samples.localPeakIndexes(threshold)

        if (peakIndexes.size < 3) {
            return BreathRhythmCheckResult(
                rhythmMatchedPercent = 0,
                detectedBreaths = peakIndexes.size,
                durationSeconds = durationSeconds,
            )
        }

        val intervals = peakIndexes.zipWithNext { first, second -> (second - first).toFloat() }
        val intervalAverage = intervals.average().toFloat().coerceAtLeast(0.001f)
        val intervalDeviation = intervals.standardDeviation()
        val consistency = (1f - ((intervalDeviation / intervalAverage) * 1.4f))
            .coerceIn(0f, 1f)

        return BreathRhythmCheckResult(
            rhythmMatchedPercent = (consistency * 100f).roundToInt(),
            detectedBreaths = peakIndexes.size,
            durationSeconds = durationSeconds,
        )
    }

    fun compare(
        before: BreathRhythmCheckResult?,
        after: BreathRhythmCheckResult?,
    ): String {
        val beforeScore = before?.rhythmMatchedPercent
        val afterScore = after?.rhythmMatchedPercent

        return when {
            beforeScore != null && afterScore != null -> {
                val delta = afterScore - beforeScore
                if (delta >= 0) {
                    "Breath control improved +${delta}%."
                } else {
                    "Breath control changed ${delta}%."
                }
            }

            beforeScore != null ->
                "Baseline rhythm matched ${beforeScore}%. Add an after-workout check to compare."

            afterScore != null ->
                "After-workout rhythm matched ${afterScore}%."

            else -> "No breath rhythm check saved for this session."
        }
    }
}

fun BreathRhythmCheckResult.toSummaryLabel(): String =
    "Rhythm matched $rhythmMatchedPercent% · $detectedBreaths breaths detected"

private fun List<Float>.localPeakIndexes(threshold: Float): List<Int> {
    if (size < 3) return emptyList()

    return buildList {
        var index = 0
        while (index < this@localPeakIndexes.size) {
            if (this@localPeakIndexes[index] < threshold) {
                index += 1
                continue
            }

            var peakIndex = index
            var peakValue = this@localPeakIndexes[index]
            while (index < this@localPeakIndexes.size && this@localPeakIndexes[index] >= threshold) {
                val value = this@localPeakIndexes[index]
                if (value > peakValue) {
                    peakValue = value
                    peakIndex = index
                }
                index += 1
            }
            add(peakIndex)
        }
    }
}

private fun List<Float>.standardDeviation(): Float {
    if (isEmpty()) return 0f
    val average = average()
    val variance = map { sample ->
        val difference = sample - average
        difference * difference
    }.average()
    return sqrt(variance).toFloat()
}
