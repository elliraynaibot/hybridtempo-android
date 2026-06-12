package com.hybridtempo.android.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class BreathRhythmRecorder(
    private val context: Context,
) {
    @SuppressLint("MissingPermission")
    suspend fun record(durationSeconds: Int = DEFAULT_DURATION_SECONDS): BreathRhythmCheckResult =
        withContext(Dispatchers.IO) {
            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                error("Microphone permission is required for breath rhythm checks.")
            }

            val sampleRate = 16_000
            val minBufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            val bufferSize = minBufferSize.coerceAtLeast(sampleRate / 4)
            val buffer = ShortArray(bufferSize)
            val amplitudeSamples = mutableListOf<Float>()
            val totalSamplesToRead = sampleRate * durationSeconds
            var samplesRead = 0

            val recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
            )

            try {
                recorder.startRecording()
                while (samplesRead < totalSamplesToRead) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        amplitudeSamples += buffer.rms(read)
                        samplesRead += read
                    }
                }
            } finally {
                runCatching { recorder.stop() }
                recorder.release()
            }

            BreathRhythmAnalyzer.analyze(
                amplitudeSamples = amplitudeSamples,
                durationSeconds = durationSeconds,
            )
        }

    private fun ShortArray.rms(size: Int): Float {
        if (size <= 0) return 0f
        var sum = 0.0
        for (index in 0 until size.coerceAtMost(this.size)) {
            val normalized = this[index].toFloat() / Short.MAX_VALUE.toFloat()
            sum += normalized * normalized
        }
        return sqrt(sum / size).toFloat()
    }

    companion object {
        const val DEFAULT_DURATION_SECONDS = 20
    }
}
