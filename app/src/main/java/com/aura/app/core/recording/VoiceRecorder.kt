package com.aura.app.core.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import java.util.UUID

sealed interface RecordingState {
    object Idle : RecordingState
    data class Recording(
        val durationMs: Long,
        val amplitudes: List<Float>
    ) : RecordingState
    data class Paused(
        val durationMs: Long,
        val amplitudes: List<Float>
    ) : RecordingState
    data class Completed(
        val file: File,
        val durationMs: Long
    ) : RecordingState
    data class Error(val message: String) : RecordingState
}

class VoiceRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var lastResumeTimeMs = 0L
    private var accumulatedDurationMs = 0L
    private var isPaused = false

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val amplitudesList = mutableListOf<Float>()
    private val handler = Handler(Looper.getMainLooper())

    private val amplitudeRunnable = object : Runnable {
        override fun run() {
            mediaRecorder?.let { mr ->
                try {
                    if (!isPaused) {
                        val maxAmp = mr.maxAmplitude
                        val normalized = (maxAmp / 32767f).coerceIn(0.05f, 1.0f)
                        amplitudesList.add(normalized)
                        val elapsed = accumulatedDurationMs + (System.currentTimeMillis() - lastResumeTimeMs)
                        _recordingState.value = RecordingState.Recording(
                            durationMs = elapsed,
                            amplitudes = amplitudesList.toList()
                        )
                        handler.postDelayed(this, 100)
                    }
                } catch (e: Exception) {
                    // Recorder released or stopped
                }
            }
        }
    }

    fun startRecording(): Boolean {
        if (_recordingState.value is RecordingState.Recording || _recordingState.value is RecordingState.Paused) return false

        val notesDir = File(context.filesDir, "voice_notes")
        if (!notesDir.exists()) notesDir.mkdirs()

        val fileName = "aura_note_${UUID.randomUUID()}.m4a"
        val outputFile = File(notesDir, fileName)
        currentFile = outputFile

        amplitudesList.clear()
        accumulatedDurationMs = 0L
        isPaused = false

        return try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            lastResumeTimeMs = System.currentTimeMillis()
            _recordingState.value = RecordingState.Recording(0L, emptyList())
            handler.post(amplitudeRunnable)
            true
        } catch (e: IOException) {
            _recordingState.value = RecordingState.Error("Failed to initialize microphone: ${e.message}")
            currentFile?.delete()
            currentFile = null
            false
        } catch (e: Exception) {
            _recordingState.value = RecordingState.Error("Unexpected recording error: ${e.message}")
            currentFile?.delete()
            currentFile = null
            false
        }
    }

    fun pauseRecording(): Boolean {
        if (mediaRecorder == null || isPaused || _recordingState.value !is RecordingState.Recording) return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
                accumulatedDurationMs += (System.currentTimeMillis() - lastResumeTimeMs)
                isPaused = true
                handler.removeCallbacks(amplitudeRunnable)
                _recordingState.value = RecordingState.Paused(
                    durationMs = accumulatedDurationMs,
                    amplitudes = amplitudesList.toList()
                )
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun resumeRecording(): Boolean {
        if (mediaRecorder == null || !isPaused || _recordingState.value !is RecordingState.Paused) return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
                isPaused = false
                lastResumeTimeMs = System.currentTimeMillis()
                _recordingState.value = RecordingState.Recording(
                    durationMs = accumulatedDurationMs,
                    amplitudes = amplitudesList.toList()
                )
                handler.post(amplitudeRunnable)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun stopRecording(): File? {
        handler.removeCallbacks(amplitudeRunnable)
        val recorder = mediaRecorder ?: return null
        val file = currentFile
        val finalDuration = if (isPaused) {
            accumulatedDurationMs
        } else {
            accumulatedDurationMs + (System.currentTimeMillis() - lastResumeTimeMs)
        }

        return try {
            recorder.stop()
            if (file != null && file.exists()) {
                _recordingState.value = RecordingState.Completed(file, finalDuration)
                file
            } else {
                _recordingState.value = RecordingState.Error("Recording file was not saved.")
                null
            }
        } catch (e: Exception) {
            _recordingState.value = RecordingState.Error("Error finalizing recording: ${e.message}")
            file?.delete()
            null
        } finally {
            try {
                recorder.release()
            } catch (ignored: Exception) {}
            mediaRecorder = null
            currentFile = null
            isPaused = false
        }
    }

    fun cancelRecording() {
        handler.removeCallbacks(amplitudeRunnable)
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            // Ignore
        } finally {
            try {
                mediaRecorder?.release()
            } catch (ignored: Exception) {}
            mediaRecorder = null
        }
        currentFile?.delete()
        currentFile = null
        isPaused = false
        accumulatedDurationMs = 0L
        _recordingState.value = RecordingState.Idle
    }

    fun resetState() {
        _recordingState.value = RecordingState.Idle
        isPaused = false
        accumulatedDurationMs = 0L
    }
}
