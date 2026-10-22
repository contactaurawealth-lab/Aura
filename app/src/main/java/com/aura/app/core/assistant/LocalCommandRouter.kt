package com.aura.app.core.assistant

sealed interface AssistantCommand {
    object PlayRainSounds : AssistantCommand
    data class StartBreathing(val minutes: Int) : AssistantCommand
    data class StartFocus(val minutes: Int) : AssistantCommand
    object RecordThought : AssistantCommand
    object PauseMusic : AssistantCommand
    object ResumeMusic : AssistantCommand
    data class Unknown(val raw: String) : AssistantCommand
}

object LocalCommandRouter {
    fun parse(input: String): AssistantCommand {
        val clean = input.trim().lowercase()
        return when {
            clean.contains("rain") && (clean.contains("play") || clean.contains("sound")) -> AssistantCommand.PlayRainSounds
            clean.contains("pause") -> AssistantCommand.PauseMusic
            clean.contains("resume") || clean.contains("unpause") -> AssistantCommand.ResumeMusic
            clean.contains("breath") -> {
                val mins = extractMinutes(clean, default = 5)
                AssistantCommand.StartBreathing(mins)
            }
            clean.contains("focus") -> {
                val mins = extractMinutes(clean, default = 25)
                AssistantCommand.StartFocus(mins)
            }
            clean.contains("record") || clean.contains("thought") || clean.contains("journal") -> AssistantCommand.RecordThought
            else -> AssistantCommand.Unknown(input)
        }
    }

    private fun extractMinutes(text: String, default: Int): Int {
        val regex = Regex("""(\d+)\s*(?:min|minute)?""")
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: default
    }
}
