package com.hybridtempo.android.health

import java.time.Duration
import java.time.Instant
import kotlin.math.abs

data class HeartRateSample(
    val time: Instant,
    val bpm: Int,
)

data class HeartRateLock(
    val sampledAt: Instant,
    val bpm: Int,
) {
    val summary: String
        get() = "Starting HR locked: $bpm bpm"
}

data class HeartRateImpact(
    val beforeBpm: Int,
    val afterBpm: Int,
) {
    val deltaBpm: Int = afterBpm - beforeBpm

    val summary: String
        get() = "HR $beforeBpm -> $afterBpm bpm (${deltaBpm.toSignedString()})"
}

object HeartRateImpactCalculator {
    private val sampleWindow: Duration = Duration.ofMinutes(2)

    fun calculate(
        sessionStartedAt: Instant,
        sessionEndedAt: Instant,
        samples: List<HeartRateSample>,
    ): HeartRateImpact? {
        val before = samples
            .filter { !it.time.isAfter(sessionStartedAt) && Duration.between(it.time, sessionStartedAt) <= sampleWindow }
            .minByOrNull { abs(Duration.between(it.time, sessionStartedAt).toMillis()) }

        val after = samples
            .filter { !it.time.isBefore(sessionEndedAt) && Duration.between(sessionEndedAt, it.time) <= sampleWindow }
            .minByOrNull { abs(Duration.between(sessionEndedAt, it.time).toMillis()) }

        return if (before != null && after != null) {
            HeartRateImpact(beforeBpm = before.bpm, afterBpm = after.bpm)
        } else {
            null
        }
    }

    fun lockRecentStart(
        sessionStartedAt: Instant,
        samples: List<HeartRateSample>,
    ): HeartRateLock? {
        val sample = samples
            .filter { !it.time.isAfter(sessionStartedAt) && Duration.between(it.time, sessionStartedAt) <= sampleWindow }
            .minByOrNull { abs(Duration.between(it.time, sessionStartedAt).toMillis()) }

        return sample?.let { HeartRateLock(sampledAt = it.time, bpm = it.bpm) }
    }

    fun calculateFromLockedStart(
        lockedStart: HeartRateLock,
        sessionEndedAt: Instant,
        samples: List<HeartRateSample>,
    ): HeartRateImpact? {
        val after = samples
            .filter { !it.time.isBefore(sessionEndedAt) && Duration.between(sessionEndedAt, it.time) <= sampleWindow }
            .minByOrNull { abs(Duration.between(sessionEndedAt, it.time).toMillis()) }

        return after?.let {
            HeartRateImpact(beforeBpm = lockedStart.bpm, afterBpm = it.bpm)
        }
    }
}

private fun Int.toSignedString(): String = if (this > 0) "+$this" else toString()
