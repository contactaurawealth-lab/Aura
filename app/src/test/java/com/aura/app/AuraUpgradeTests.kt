package com.aura.app

import androidx.media3.common.Player
import com.aura.app.core.audio.PlaybackState
import com.aura.app.core.database.*
import com.aura.app.core.journal.JournalIntelligence
import com.aura.app.core.recording.AndroidSpeechTranscriptionEngine
import com.aura.app.core.recording.TranscriptSegment
import com.aura.app.feature.appblocker.AppBlockerBypass
import org.junit.Assert.*
import org.junit.Test

class AuraUpgradeTests {

    @Test
    fun testPlaylistAndFolderEntities() {
        val playlist = PlaylistEntity(
            id = "pl_1",
            name = "Morning Calm",
            description = "Acoustic chimes and breathing tracks"
        )
        assertEquals("pl_1", playlist.id)
        assertEquals("Morning Calm", playlist.name)
        assertEquals("Acoustic chimes and breathing tracks", playlist.description)

        val crossRef = PlaylistTrackCrossRef(
            playlistId = "pl_1",
            trackId = "track_rainfall",
            sortOrder = 0
        )
        assertEquals("pl_1", crossRef.playlistId)
        assertEquals("track_rainfall", crossRef.trackId)
        assertEquals(0, crossRef.sortOrder)

        val folder = VoiceFolderEntity(
            id = "f_1",
            name = "Deep Thoughts",
            colorHex = "#77866A"
        )
        assertEquals("f_1", folder.id)
        assertEquals("Deep Thoughts", folder.name)
    }

    @Test
    fun testEnhancedVoiceNoteFields() {
        val note = VoiceNoteEntity(
            id = "note_101",
            title = "Forest Walking Thoughts",
            filePath = "/data/user/0/com.aura.app/files/voice_notes/note_101.m4a",
            durationMs = 60000L,
            transcript = "I took a deep breath and listened to the river.",
            mood = "Peaceful",
            isFavorite = true,
            folderId = "f_1",
            tags = "nature, walk, river",
            writtenNotes = "Notice the birdsong at dawn.",
            language = "en-US",
            timestampsJson = """[{"startMs": 0, "text": "I took a deep breath"},{"startMs": 15000, "text": "and listened to the river."}]"""
        )

        assertEquals("f_1", note.folderId)
        assertEquals("nature, walk, river", note.tags)
        assertTrue(note.isFavorite)
        assertEquals("en-US", note.language)

        val segments = AndroidSpeechTranscriptionEngine.parseSegmentsFromJson(note.timestampsJson)
        assertEquals(2, segments.size)
        assertEquals(0L, segments[0].startMs)
        assertEquals("I took a deep breath", segments[0].text)
        assertEquals(15000L, segments[1].startMs)
        assertEquals("and listened to the river.", segments[1].text)
    }

    @Test
    fun testJournalIntelligenceSummarizationAndExport() {
        val transcript = "Today I felt stressed with work deadlines. I took five deep breaths to relax and calm my mind. Afterwards, I felt immense gratitude for the evening quiet."
        val writtenNotes = "Remember to keep phone away after 9 PM."

        val summary = JournalIntelligence.summarizeNote(transcript, writtenNotes)
        assertTrue(summary.keyTakeaways.isNotEmpty())
        assertTrue(summary.suggestedIntention.isNotBlank())
        assertEquals("Needs Grounding", summary.moodDetected)

        val note = VoiceNoteEntity(
            id = "note_export",
            title = "Evening Decompression",
            filePath = "/fake/path.m4a",
            durationMs = 45000L,
            transcript = transcript,
            writtenNotes = writtenNotes,
            mood = "Reflective",
            tags = "evening, calm"
        )

        val exportText = JournalIntelligence.generateExportText(note)
        assertTrue(exportText.contains("AURA VOICE JOURNAL"))
        assertTrue(exportText.contains("Title: Evening Decompression"))
        assertTrue(exportText.contains("--- TRANSCRIPT ---"))
        assertTrue(exportText.contains("--- KEY TAKEAWAYS ---"))
    }

    @Test
    fun testTranscriptSegmentJsonSerialization() {
        val original = listOf(
            TranscriptSegment(0L, "Breathing in peace"),
            TranscriptSegment(5000L, "Breathing out tension")
        )

        val json = AndroidSpeechTranscriptionEngine.segmentsToJson(original)
        assertTrue(json.contains("Breathing in peace"))
        assertTrue(json.contains("Breathing out tension"))

        val parsed = AndroidSpeechTranscriptionEngine.parseSegmentsFromJson(json)
        assertEquals(2, parsed.size)
        assertEquals(0L, parsed[0].startMs)
        assertEquals("Breathing in peace", parsed[0].text)
        assertEquals(5000L, parsed[1].startMs)
        assertEquals("Breathing out tension", parsed[1].text)
    }

    @Test
    fun testPlaybackStateQueueNavigation() {
        val track1 = AudioTrackEntity(
            id = "t1",
            title = "Track 1",
            artist = "Artist 1",
            category = "Meditation",
            durationMs = 10000L,
            filePath = "/path/1.wav",
            isGuided = false
        )
        val track2 = AudioTrackEntity(
            id = "t2",
            title = "Track 2",
            artist = "Artist 2",
            category = "Sleep",
            durationMs = 20000L,
            filePath = "/path/2.wav",
            isGuided = false
        )

        val state = PlaybackState(
            currentTrack = track1,
            isPlaying = true,
            queue = listOf(track1, track2),
            currentQueueIndex = 0,
            repeatMode = Player.REPEAT_MODE_ALL
        )

        assertEquals(2, state.queue.size)
        assertEquals(0, state.currentQueueIndex)
        assertEquals("Track 1", state.currentTrack?.title)

        val nextIndex = (state.currentQueueIndex + 1) % state.queue.size
        assertEquals(1, nextIndex)
        val nextTrack = state.queue[nextIndex]
        assertEquals("Track 2", nextTrack.title)
    }

    @Test
    fun testAppBlockerBypassLogic() {
        val testPkg = "com.example.distracting.app"
        AppBlockerBypass.clear()
        assertFalse(AppBlockerBypass.isBypassed(testPkg))

        // Grant 2 minute bypass
        AppBlockerBypass.grantBypass(testPkg, durationMs = 120_000L)
        assertTrue(AppBlockerBypass.isBypassed(testPkg))

        // Expired bypass test
        AppBlockerBypass.grantBypass(testPkg, durationMs = -1000L)
        assertFalse(AppBlockerBypass.isBypassed(testPkg))
    }
}
