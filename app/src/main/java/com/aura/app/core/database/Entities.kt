package com.aura.app.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "audio_tracks")
data class AudioTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val category: String, // "Meditation", "Sleep", "Focus", "Nature sounds", "Ambient music", "Imported"
    val durationMs: Long,
    val filePath: String,
    val isGuided: Boolean,
    val isFavorite: Boolean = false,
    val lastPlayedTimestamp: Long? = null,
    val description: String = "",
    val isImported: Boolean = false,
    val albumArtUri: String? = null,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AudioTrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["trackId"]),
        Index(value = ["playlistId"])
    ]
)
data class PlaylistTrackCrossRef(
    val playlistId: String,
    val trackId: String,
    val sortOrder: Int = 0
)

@Entity(tableName = "voice_folders")
data class VoiceFolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String = "#77866A",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "meditation_sessions")
data class MeditationSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String?,
    val title: String,
    val durationSeconds: Long,
    val targetDurationSeconds: Long,
    val completed: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val intention: String,
    val durationSeconds: Long,
    val targetDurationSeconds: Long,
    val completed: Boolean,
    val soundTrackId: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "breathing_sessions")
data class BreathingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patternName: String, // "Box Breathing", "Relaxing (4-7-8)", "Equal Breathing", "Custom"
    val durationSeconds: Long,
    val completed: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "voice_notes")
data class VoiceNoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val transcript: String = "",
    val mood: String = "Reflective",
    val isFavorite: Boolean = false,
    val folderId: String? = null,
    val tags: String = "", // Comma-delimited: "morning, calm, gratitude"
    val writtenNotes: String = "",
    val language: String = "en-US",
    val timestampsJson: String = "", // JSON list of timestamped segments
    val summary: String = ""
)

@Entity(tableName = "mood_entries")
data class MoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mood: String, // "Calm", "Happy", "Tired", "Stressed", "Unfocused", "Reflective"
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_rules")
data class FocusRuleEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isRestricted: Boolean = true,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String, // "MEDITATION", "BREATHING", "FOCUS", "QUIET_HOUR"
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true
)
