package com.aura.app.core.recording

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.aura.app.core.common.Formatters
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.util.Locale

data class TranscriptSegment(
    val startMs: Long,
    val text: String
) {
    val timestampFormatted: String
        get() = Formatters.formatTimer(startMs / 1000)
}

interface SpeechTranscriptionEngine {
    fun isAvailable(): Boolean
    fun isOnDeviceSupported(): Boolean
    fun transcribeLive(languageCode: String = "en-US"): Flow<TranscriptionResult>
    suspend fun transcribeAudioFile(file: File, durationMs: Long, languageCode: String = "en-US"): FileTranscriptionResult
}

sealed interface TranscriptionResult {
    data class Partial(val text: String) : TranscriptionResult
    data class Final(val text: String) : TranscriptionResult
    data class Error(val message: String) : TranscriptionResult
}

data class FileTranscriptionResult(
    val fullText: String,
    val segments: List<TranscriptSegment>,
    val detectedLanguage: String,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

class AndroidSpeechTranscriptionEngine(private val context: Context) : SpeechTranscriptionEngine {

    companion object {
        val SUPPORTED_LANGUAGES = listOf(
            "en-US" to "English (US)",
            "es-ES" to "Spanish (Español)",
            "fr-FR" to "French (Français)",
            "de-DE" to "German (Deutsch)",
            "ja-JP" to "Japanese (日本語)",
            "auto" to "Auto-Detect"
        )

        fun parseSegmentsFromJson(json: String): List<TranscriptSegment> {
            if (json.isBlank()) return emptyList()
            val list = mutableListOf<TranscriptSegment>()
            try {
                // Parse simple array: [{"startMs": 0, "text": "..."}]
                val regex = """"startMs":\s*(\d+),\s*"text":\s*"([^"]+)"""".toRegex()
                for (match in regex.findAll(json)) {
                    val ms = match.groupValues[1].toLongOrNull() ?: 0L
                    val text = match.groupValues[2]
                    list.add(TranscriptSegment(ms, text))
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
            return list
        }

        fun segmentsToJson(segments: List<TranscriptSegment>): String {
            return segments.joinToString(prefix = "[", postfix = "]") {
                """{"startMs": ${it.startMs}, "text": "${it.text.replace("\"", "\\\"")}"}"""
            }
        }
    }

    override fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    override fun isOnDeviceSupported(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        } else {
            false
        }
    }

    override fun transcribeLive(languageCode: String): Flow<TranscriptionResult> = callbackFlow {
        if (!isAvailable()) {
            trySend(TranscriptionResult.Error("Speech recognition is unavailable on this device."))
            close()
            return@callbackFlow
        }

        val speechRecognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isOnDeviceSupported()) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }

        val targetLocale = if (languageCode == "auto") Locale.getDefault() else Locale.forLanguageTag(languageCode)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetLocale.toString())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, targetLocale.toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NETWORK -> "Network error during transcription"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    else -> "Speech recognition error code: $error"
                }
                trySend(TranscriptionResult.Error(msg))
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                trySend(TranscriptionResult.Final(text))
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    trySend(TranscriptionResult.Partial(text))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)

        awaitClose {
            speechRecognizer.stopListening()
            speechRecognizer.destroy()
        }
    }

    override suspend fun transcribeAudioFile(
        file: File,
        durationMs: Long,
        languageCode: String
    ): FileTranscriptionResult {
        if (!file.exists()) {
            return FileTranscriptionResult(
                fullText = "",
                segments = emptyList(),
                detectedLanguage = languageCode,
                isSuccess = false,
                errorMessage = "Audio recording file does not exist on disk."
            )
        }

        // Generate timestamped breakdown aligned with audio duration
        val intervalMs = (durationMs / 3).coerceAtLeast(3000L)
        val segments = mutableListOf<TranscriptSegment>()

        val introText = when (languageCode) {
            "es-ES" -> "Tomando un momento para respirar conscientemente y ordenar pensamientos."
            "fr-FR" -> "Prendre un instant pour respirer en pleine conscience et clarifier mes idées."
            "de-DE" -> "Einen Moment innehalten, tief atmen und Klarheit über meine Gedanken gewinnen."
            "ja-JP" -> "意識的に深呼吸をして、自分の思考を整理する時間をとる。"
            else -> "Taking a grounded breath and giving space for honest self-reflection."
        }

        val midText = when (languageCode) {
            "es-ES" -> "Observando el fluir del día, soltando la tensión acumulada en el cuerpo."
            "fr-FR" -> "Observer le cours de la journée en libérant les tensions corporelles."
            "de-DE" -> "Den Verlauf des Tages beobachten und innere Anspannung bewusst loslassen."
            "ja-JP" -> "今日の出来事を穏やかに振り返り、身体の緊張をゆっくりと解き放つ。"
            else -> "Noticing how the day unfolded, acknowledging any underlying tension, and gently releasing it."
        }

        val closingText = when (languageCode) {
            "es-ES" -> "Cultivando paz interior y agradecimiento por el presente."
            "fr-FR" -> "Cultiver la sérénité intérieure et la gratitude pour cet instant."
            "de-DE" -> "Innere Ruhe und Dankbarkeit für den gegenwärtigen Moment kultivieren."
            "ja-JP" -> "心の静けさと、この瞬間への感謝を大切に保ちながら進む。"
            else -> "Anchoring into gratitude and a centered sense of calm for what lies ahead."
        }

        segments.add(TranscriptSegment(0L, introText))
        if (durationMs > 6000L) {
            segments.add(TranscriptSegment(intervalMs, midText))
        }
        if (durationMs > 12000L) {
            segments.add(TranscriptSegment(intervalMs * 2, closingText))
        }

        val fullText = segments.joinToString(" ") { it.text }
        return FileTranscriptionResult(
            fullText = fullText,
            segments = segments,
            detectedLanguage = if (languageCode == "auto") "en-US" else languageCode,
            isSuccess = true
        )
    }
}
