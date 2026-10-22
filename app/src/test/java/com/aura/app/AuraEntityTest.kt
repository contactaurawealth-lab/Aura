package com.aura.app

import com.aura.app.core.database.*
import org.junit.Assert.*
import org.junit.Test

class AuraEntityTest {

    @Test
    fun testAudioTrackEntityCreationAndDefaults() {
        val track = AudioTrackEntity(
            id = "test_rainfall",
            title = "Rainfall & Soft Leaves",
            artist = "Aura Nature Sounds",
            category = "Nature sounds",
            durationMs = 12000L,
            filePath = "/data/user/0/com.aura.app/files/starter_audio/rain.wav",
            isGuided = false,
            description = "Gentle rain sounds"
        )

        assertEquals("test_rainfall", track.id)
        assertEquals("Rainfall & Soft Leaves", track.title)
        assertFalse(track.isFavorite)
        assertNull(track.lastPlayedTimestamp)
        assertFalse(track.isGuided)

        val updated = track.copy(isFavorite = true, lastPlayedTimestamp = 123456789L)
        assertTrue(updated.isFavorite)
        assertEquals(123456789L, updated.lastPlayedTimestamp)
    }

    @Test
    fun testVoiceNoteEntityCreation() {
        val note = VoiceNoteEntity(
            id = "note_uuid_1",
            title = "Reflections on Stillness",
            filePath = "/data/user/0/com.aura.app/files/voice_notes/note_1.m4a",
            durationMs = 45000L,
            transcript = "Today I learned to let go of distractions.",
            mood = "Calm"
        )

        assertEquals("note_uuid_1", note.id)
        assertEquals("Calm", note.mood)
        assertEquals(45000L, note.durationMs)
        assertEquals("Today I learned to let go of distractions.", note.transcript)
        assertFalse(note.isFavorite)
    }

    @Test
    fun testFocusRuleEntity() {
        val rule = FocusRuleEntity(
            packageName = "com.social.app",
            appName = "Social App",
            isRestricted = true
        )

        assertEquals("com.social.app", rule.packageName)
        assertTrue(rule.isRestricted)

        val toggled = rule.copy(isRestricted = false)
        assertFalse(toggled.isRestricted)
    }

    @Test
    fun testMeditationAndFocusSessionCompletionLogic() {
        val meditation = MeditationSessionEntity(
            trackId = "track_singing_bowl",
            title = "Inner Stillness",
            durationSeconds = 600L,
            targetDurationSeconds = 600L,
            completed = true
        )

        assertTrue(meditation.completed)
        assertEquals(600L, meditation.durationSeconds)

        val focus = FocusSessionEntity(
            intention = "Writing PRD",
            durationSeconds = 1500L,
            targetDurationSeconds = 1500L,
            completed = true,
            soundTrackId = "track_brown_noise"
        )

        assertTrue(focus.completed)
        assertEquals("Writing PRD", focus.intention)
    }
}
