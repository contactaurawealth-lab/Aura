package com.aura.app.core.common

import android.content.Context
import com.aura.app.core.audio.AmbientSoundscapeMixer
import com.aura.app.core.audio.AudioImportManager
import com.aura.app.core.audio.AudioPlaybackManager
import com.aura.app.core.database.AuraDatabase
import com.aura.app.core.database.AudioTrackEntity
import com.aura.app.core.database.PlaylistEntity
import com.aura.app.core.database.PlaylistTrackCrossRef
import com.aura.app.core.database.VoiceFolderEntity
import com.aura.app.core.datastore.UserPreferencesRepository
import com.aura.app.core.recording.AndroidSpeechTranscriptionEngine
import com.aura.app.core.recording.VoiceRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuraAppContainer(private val context: Context) {

    val database: AuraDatabase by lazy {
        AuraDatabase.getInstance(context)
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    val audioPlaybackManager: AudioPlaybackManager by lazy {
        AudioPlaybackManager(context)
    }

    val audioImportManager: AudioImportManager by lazy {
        AudioImportManager(context, database)
    }

    val ambientSoundscapeMixer: AmbientSoundscapeMixer by lazy {
        AmbientSoundscapeMixer(context)
    }

    val voiceRecorder: VoiceRecorder by lazy {
        VoiceRecorder(context)
    }

    val speechTranscriptionEngine: AndroidSpeechTranscriptionEngine by lazy {
        AndroidSpeechTranscriptionEngine(context)
    }

    fun seedStarterLibraryIfNeeded() {
        CoroutineScope(Dispatchers.IO).launch {
            val audioFiles = ProceduralAudioGenerator.ensureStarterAudioFiles(context)
            val audioDao = database.audioDao()
            val playlistDao = database.playlistDao()
            val folderDao = database.voiceFolderDao()

            val starterTracks = listOf(
                AudioTrackEntity(
                    id = "track_rainfall",
                    title = "Rainfall & Soft Leaves",
                    artist = "Aura Nature Sounds",
                    category = "Nature sounds",
                    durationMs = 12000L,
                    filePath = audioFiles[ProceduralAudioGenerator.SoundType.RAIN]?.absolutePath ?: "",
                    isGuided = false,
                    description = "Gentle rain tapping on woodland foliage, calming racing thoughts."
                ),
                AudioTrackEntity(
                    id = "track_stream",
                    title = "Forest Stream",
                    artist = "Aura Nature Sounds",
                    category = "Nature sounds",
                    durationMs = 12000L,
                    filePath = audioFiles[ProceduralAudioGenerator.SoundType.FOREST_STREAM]?.absolutePath ?: "",
                    isGuided = false,
                    description = "Crisp, flowing mountain brook water over river stones."
                ),
                AudioTrackEntity(
                    id = "track_singing_bowl",
                    title = "Inner Stillness (432 Hz)",
                    artist = "Aura Guided Practice",
                    category = "Meditation",
                    durationMs = 12000L,
                    filePath = audioFiles[ProceduralAudioGenerator.SoundType.SINGING_BOWL]?.absolutePath ?: "",
                    isGuided = true,
                    description = "Pure Tibetan singing bowl harmonics tuned to restorative 432 Hz."
                ),
                AudioTrackEntity(
                    id = "track_brown_noise",
                    title = "Deep Focus Brown Noise",
                    artist = "Aura Ambient",
                    category = "Focus",
                    durationMs = 12000L,
                    filePath = audioFiles[ProceduralAudioGenerator.SoundType.BROWN_NOISE]?.absolutePath ?: "",
                    isGuided = false,
                    description = "Warm low-frequency cascade for uninterrupted concentration."
                ),
                AudioTrackEntity(
                    id = "track_meditation_bell",
                    title = "Midnight Slumber Bell",
                    artist = "Aura Sleep Series",
                    category = "Sleep",
                    durationMs = 12000L,
                    filePath = audioFiles[ProceduralAudioGenerator.SoundType.MEDITATION_BELL]?.absolutePath ?: "",
                    isGuided = false,
                    description = "Resonant temple chimes gently lulling the mind into rest."
                ),
                AudioTrackEntity(
                    id = "track_ocean",
                    title = "Pacific Ocean Waves",
                    artist = "Aura Coastal Waves",
                    category = "Nature sounds",
                    durationMs = 12000L,
                    filePath = audioFiles[ProceduralAudioGenerator.SoundType.OCEAN_WAVES]?.absolutePath ?: "",
                    isGuided = false,
                    description = "Rolling rhythmic oceanic swell dissolving cognitive clutter."
                ),
                AudioTrackEntity(
                    id = "track_wind",
                    title = "Alpine Gentle Wind",
                    artist = "Aura Atmosphere",
                    category = "Ambient music",
                    durationMs = 12000L,
                    filePath = audioFiles[ProceduralAudioGenerator.SoundType.GENTLE_WIND]?.absolutePath ?: "",
                    isGuided = false,
                    description = "Whispering mountain breeze providing a serene sound blanket."
                ),
                AudioTrackEntity(
                    id = "track_white_noise",
                    title = "Gentle White Noise",
                    artist = "Aura Soundscapes",
                    category = "Focus",
                    durationMs = 12000L,
                    filePath = audioFiles[ProceduralAudioGenerator.SoundType.WHITE_NOISE]?.absolutePath ?: "",
                    isGuided = false,
                    description = "Pure full-spectrum acoustic masking to eliminate ambient distractions."
                )
            )

            audioDao.insertTracks(starterTracks)

            // Seed starter playlists if none exist
            val samplePlaylist = PlaylistEntity(
                id = "playlist_peaceful_evening",
                name = "Peaceful Evening",
                description = "Soothing acoustic layers to unwind before sleep."
            )
            playlistDao.insertPlaylist(samplePlaylist)
            playlistDao.insertCrossRef(PlaylistTrackCrossRef(samplePlaylist.id, "track_rainfall", 0))
            playlistDao.insertCrossRef(PlaylistTrackCrossRef(samplePlaylist.id, "track_singing_bowl", 1))
            playlistDao.insertCrossRef(PlaylistTrackCrossRef(samplePlaylist.id, "track_ocean", 2))

            val focusPlaylist = PlaylistEntity(
                id = "playlist_deep_work",
                name = "Deep Work & Flow",
                description = "Mask distractions and lock into uninterrupted momentum."
            )
            playlistDao.insertPlaylist(focusPlaylist)
            playlistDao.insertCrossRef(PlaylistTrackCrossRef(focusPlaylist.id, "track_brown_noise", 0))
            playlistDao.insertCrossRef(PlaylistTrackCrossRef(focusPlaylist.id, "track_stream", 1))

            // Seed starter voice folders
            folderDao.insertFolder(VoiceFolderEntity(id = "folder_reflections", name = "Daily Reflections", colorHex = "#77866A"))
            folderDao.insertFolder(VoiceFolderEntity(id = "folder_gratitude", name = "Gratitude Log", colorHex = "#C88C4C"))
            folderDao.insertFolder(VoiceFolderEntity(id = "folder_ideas", name = "Creative Insights", colorHex = "#414B3D"))
        }
    }
}
