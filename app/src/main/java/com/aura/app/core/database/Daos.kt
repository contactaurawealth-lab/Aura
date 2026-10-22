package com.aura.app.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioDao {
    @Query("SELECT * FROM audio_tracks ORDER BY title ASC")
    fun getAllTracks(): Flow<List<AudioTrackEntity>>

    @Query("SELECT * FROM audio_tracks WHERE category = :category ORDER BY title ASC")
    fun getTracksByCategory(category: String): Flow<List<AudioTrackEntity>>

    @Query("SELECT * FROM audio_tracks WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavorites(): Flow<List<AudioTrackEntity>>

    @Query("SELECT * FROM audio_tracks WHERE lastPlayedTimestamp IS NOT NULL ORDER BY lastPlayedTimestamp DESC LIMIT 10")
    fun getRecentlyPlayed(): Flow<List<AudioTrackEntity>>

    @Query("SELECT * FROM audio_tracks WHERE isImported = 1 ORDER BY dateAdded DESC")
    fun getImportedTracks(): Flow<List<AudioTrackEntity>>

    @Query("SELECT * FROM audio_tracks WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchTracks(query: String): Flow<List<AudioTrackEntity>>

    @Query("SELECT * FROM audio_tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): AudioTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<AudioTrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: AudioTrackEntity)

    @Update
    suspend fun updateTrack(track: AudioTrackEntity)

    @Query("UPDATE audio_tracks SET isFavorite = :isFav WHERE id = :id")
    suspend fun toggleFavorite(id: String, isFav: Boolean)

    @Query("UPDATE audio_tracks SET lastPlayedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastPlayed(id: String, timestamp: Long)

    @Query("DELETE FROM audio_tracks WHERE id = :id")
    suspend fun deleteTrack(id: String)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)

    @Query("""
        SELECT a.* FROM audio_tracks a
        INNER JOIN playlist_tracks pt ON a.id = pt.trackId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.sortOrder ASC
    """)
    fun getTracksForPlaylist(playlistId: String): Flow<List<AudioTrackEntity>>

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    fun getTrackCountForPlaylist(playlistId: String): Flow<Int>
}

@Dao
interface VoiceFolderDao {
    @Query("SELECT * FROM voice_folders ORDER BY createdAt ASC")
    fun getAllFolders(): Flow<List<VoiceFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: VoiceFolderEntity)

    @Query("DELETE FROM voice_folders WHERE id = :id")
    suspend fun deleteFolder(id: String)
}

@Dao
interface MeditationDao {
    @Query("SELECT * FROM meditation_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<MeditationSessionEntity>>

    @Query("SELECT SUM(durationSeconds) FROM meditation_sessions WHERE completed = 1")
    fun getTotalCompletedSeconds(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM meditation_sessions WHERE completed = 1")
    fun getCompletedCount(): Flow<Int>

    @Insert
    suspend fun insertSession(session: MeditationSessionEntity): Long
}

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT SUM(durationSeconds) FROM focus_sessions WHERE completed = 1")
    fun getTotalFocusedSeconds(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE completed = 1")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT * FROM focus_sessions WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    fun getTodaySessions(startOfDay: Long): Flow<List<FocusSessionEntity>>

    @Insert
    suspend fun insertSession(session: FocusSessionEntity): Long
}

@Dao
interface BreathingDao {
    @Query("SELECT * FROM breathing_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<BreathingSessionEntity>>

    @Query("SELECT COUNT(*) FROM breathing_sessions WHERE completed = 1")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT SUM(durationSeconds) FROM breathing_sessions WHERE completed = 1")
    fun getTotalBreathingSeconds(): Flow<Long?>

    @Insert
    suspend fun insertSession(session: BreathingSessionEntity): Long
}

@Dao
interface VoiceNoteDao {
    @Query("SELECT * FROM voice_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE folderId = :folderId ORDER BY timestamp DESC")
    fun getNotesByFolder(folderId: String): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteNotes(): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE title LIKE '%' || :query || '%' OR transcript LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchNotes(query: String): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): VoiceNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: VoiceNoteEntity)

    @Update
    suspend fun updateNote(note: VoiceNoteEntity)

    @Query("UPDATE voice_notes SET isFavorite = :isFav WHERE id = :id")
    suspend fun toggleFavorite(id: String, isFav: Boolean)

    @Query("UPDATE voice_notes SET folderId = :folderId WHERE id = :id")
    suspend fun updateFolderForNote(id: String, folderId: String?)

    @Query("DELETE FROM voice_notes WHERE id = :id")
    suspend fun deleteNote(id: String)

    @Query("SELECT COUNT(*) FROM voice_notes")
    fun getNoteCount(): Flow<Int>
}

@Dao
interface MoodDao {
    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC")
    fun getAllMoodEntries(): Flow<List<MoodEntryEntity>>

    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC LIMIT 1")
    fun getLatestMood(): Flow<MoodEntryEntity?>

    @Insert
    suspend fun insertMood(mood: MoodEntryEntity): Long

    @Query("DELETE FROM mood_entries WHERE id = :id")
    suspend fun deleteMood(id: Long)
}

@Dao
interface AppBlockerDao {
    @Query("SELECT * FROM focus_rules WHERE isRestricted = 1")
    fun getRestrictedApps(): Flow<List<FocusRuleEntity>>

    @Query("SELECT * FROM focus_rules WHERE isRestricted = 1")
    suspend fun getRestrictedAppsSync(): List<FocusRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setAppRestricted(rule: FocusRuleEntity)

    @Query("DELETE FROM focus_rules WHERE packageName = :pkg")
    suspend fun removeRule(pkg: String)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)
}
