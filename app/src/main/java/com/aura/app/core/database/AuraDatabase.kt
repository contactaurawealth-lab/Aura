package com.aura.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AudioTrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        VoiceFolderEntity::class,
        MeditationSessionEntity::class,
        FocusSessionEntity::class,
        BreathingSessionEntity::class,
        VoiceNoteEntity::class,
        MoodEntryEntity::class,
        FocusRuleEntity::class,
        ReminderEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun audioDao(): AudioDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun voiceFolderDao(): VoiceFolderDao
    abstract fun meditationDao(): MeditationDao
    abstract fun focusDao(): FocusDao
    abstract fun breathingDao(): BreathingDao
    abstract fun voiceNoteDao(): VoiceNoteDao
    abstract fun moodDao(): MoodDao
    abstract fun appBlockerDao(): AppBlockerDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AuraDatabase? = null

        fun getInstance(context: Context): AuraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AuraDatabase::class.java,
                    "aura_database.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
