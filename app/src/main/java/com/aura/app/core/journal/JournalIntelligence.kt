package com.aura.app.core.journal

import com.aura.app.core.common.Formatters
import com.aura.app.core.database.VoiceNoteEntity

object JournalIntelligence {

    data class NoteSummary(
        val keyTakeaways: List<String>,
        val moodDetected: String,
        val suggestedIntention: String
    )

    fun summarizeNote(transcript: String, writtenNotes: String): NoteSummary {
        val combined = "$transcript $writtenNotes".trim()
        if (combined.isBlank()) {
            return NoteSummary(
                keyTakeaways = listOf("Quiet reflection without spoken or written notes recorded."),
                moodDetected = "Peaceful Silence",
                suggestedIntention = "Stay present and carry stillness into your day."
            )
        }

        val takeaways = mutableListOf<String>()
        val words = combined.lowercase()

        // Extract key focus themes
        if (words.contains("breath") || words.contains("relax") || words.contains("calm")) {
            takeaways.add("Engaged conscious breathing to ground somatic awareness.")
        }
        if (words.contains("stress") || words.contains("busy") || words.contains("overwhelm") || words.contains("tired")) {
            takeaways.add("Acknowledged cognitive fatigue and recognized the need for restorative pacing.")
        }
        if (words.contains("gratitude") || words.contains("thank") || words.contains("happy") || words.contains("appreciat")) {
            takeaways.add("Practiced positive appreciation for present conditions.")
        }
        if (words.contains("work") || words.contains("focus") || words.contains("project") || words.contains("plan")) {
            takeaways.add("Clarified priorities and mentally organized next actionable steps.")
        }

        if (takeaways.isEmpty()) {
            // General bullet from sentences
            val sentences = combined.split(". ", "? ", "! ")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            if (sentences.isNotEmpty()) {
                takeaways.add(sentences.first().replaceFirstChar { it.uppercase() })
            }
            if (sentences.size > 1) {
                takeaways.add(sentences.last().replaceFirstChar { it.uppercase() })
            }
        }

        val detectedMood = when {
            words.contains("stress") || words.contains("anxious") -> "Needs Grounding"
            words.contains("grateful") || words.contains("peace") || words.contains("calm") -> "Centered & Grateful"
            words.contains("tired") || words.contains("exhaust") -> "Rest Seeking"
            words.contains("focus") || words.contains("ready") -> "Determined"
            else -> "Reflective"
        }

        val intention = when (detectedMood) {
            "Needs Grounding" -> "Take three slow diaphragmatic breaths before your next task."
            "Centered & Grateful" -> "Notice the quiet ease around you and let it anchor your evening."
            "Rest Seeking" -> "Allow yourself permission to unplug without guilt tonight."
            else -> "Carry this calm self-awareness into your upcoming interactions."
        }

        return NoteSummary(
            keyTakeaways = takeaways.ifEmpty { listOf("Grounded self-reflection recorded.") },
            moodDetected = detectedMood,
            suggestedIntention = intention
        )
    }

    fun generateExportText(note: VoiceNoteEntity): String {
        val summary = summarizeNote(note.transcript, note.writtenNotes)
        return buildString {
            appendLine("=== AURA VOICE JOURNAL ===")
            appendLine("Title: ${note.title}")
            appendLine("Date: ${Formatters.formatDate(note.timestamp)}")
            appendLine("Duration: ${Formatters.formatDurationMinutes(note.durationMs)}")
            appendLine("Mood: ${note.mood} (Detected: ${summary.moodDetected})")
            if (note.tags.isNotBlank()) {
                appendLine("Tags: ${note.tags}")
            }
            appendLine()
            appendLine("--- TRANSCRIPT ---")
            appendLine(note.transcript.ifBlank { "(No transcript available)" })
            appendLine()
            if (note.writtenNotes.isNotBlank()) {
                appendLine("--- WRITTEN NOTES ---")
                appendLine(note.writtenNotes)
                appendLine()
            }
            appendLine("--- KEY TAKEAWAYS ---")
            for (bullet in summary.keyTakeaways) {
                appendLine("• $bullet")
            }
            appendLine()
            appendLine("--- RECOMMENDED INTENTION ---")
            appendLine(summary.suggestedIntention)
            appendLine()
            appendLine("Exported privately from Aura Native Android Sanctuary")
        }
    }
}
