package com.aura.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aura.app.core.common.Formatters
import com.aura.app.core.database.*
import com.aura.app.core.designsystem.*
import com.aura.app.core.navigation.AuraNavHost
import com.aura.app.core.navigation.Screen
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as AuraApplication
        val container = app.container

        setContent {
            val scope = rememberCoroutineScope()
            val navController = rememberNavController()

            // Datastore State
            val themeMode by container.userPreferencesRepository.themeMode.collectAsState(initial = "SYSTEM")
            val reducedMotion by container.userPreferencesRepository.reducedMotion.collectAsState(initial = false)
            val assistantEnabled by container.userPreferencesRepository.assistantEnabled.collectAsState(initial = false)
            val activeFocusSession by container.userPreferencesRepository.activeFocusSession.collectAsState(initial = null)
            val onboardingCompleted by container.userPreferencesRepository.onboardingCompleted.collectAsState(initial = true)

            // Database Flows
            val allTracks by container.database.audioDao().getAllTracks().collectAsState(initial = emptyList())
            val recentlyPlayed by container.database.audioDao().getRecentlyPlayed().collectAsState(initial = emptyList())
            val playlists by container.database.playlistDao().getAllPlaylists().collectAsState(initial = emptyList())
            val folders by container.database.voiceFolderDao().getAllFolders().collectAsState(initial = emptyList())
            val latestMood by container.database.moodDao().getLatestMood().collectAsState(initial = null)
            val voiceNotes by container.database.voiceNoteDao().getAllNotes().collectAsState(initial = emptyList())
            val restrictedRules by container.database.appBlockerDao().getRestrictedApps().collectAsState(initial = emptyList())

            val totalMeditationSec by container.database.meditationDao().getTotalCompletedSeconds().collectAsState(initial = 0L)
            val completedMeditationCount by container.database.meditationDao().getCompletedCount().collectAsState(initial = 0)
            val totalFocusSec by container.database.focusDao().getTotalFocusedSeconds().collectAsState(initial = 0L)
            val completedFocusCount by container.database.focusDao().getCompletedCount().collectAsState(initial = 0)
            val completedBreathingCount by container.database.breathingDao().getCompletedCount().collectAsState(initial = 0)
            val voiceNotesCount by container.database.voiceNoteDao().getNoteCount().collectAsState(initial = 0)

            // Audio Playback & Ambient Mixer State
            val playbackState by container.audioPlaybackManager.playbackState.collectAsState()
            val ambientMixerState by container.ambientSoundscapeMixer.mixerState.collectAsState()

            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            // Handle incoming intent deep-link
            LaunchedEffect(intent) {
                intent?.getStringExtra("navigate_to")?.let { target ->
                    when (target) {
                        "breathing" -> navController.navigate(Screen.Breathing.route)
                        "focus" -> navController.navigate(Screen.Focus.route)
                        "journal" -> navController.navigate(Screen.VoiceJournal.route)
                    }
                }
            }

            AuraTheme(darkTheme = isDark) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomNav = currentRoute in listOf(
                    Screen.Home.route,
                    Screen.MusicLibrary.route,
                    Screen.VoiceJournal.route,
                    Screen.Focus.route,
                    Screen.Settings.route
                )

                Scaffold(
                    bottomBar = {
                        if (showBottomNav) {
                            Column {
                                // Mini-Player Bar (if active track)
                                playbackState.currentTrack?.let { track ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable {
                                                navController.navigate(Screen.Player.createRoute(track.id))
                                            },
                                        color = SoftIvory,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
                                        shadowElevation = 4.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(DeepBotanical),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_soundscape),
                                                        contentDescription = null,
                                                        tint = SoftIvory,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = track.title,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = CharcoalText,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = "${track.artist} • ${Formatters.formatTimer(playbackState.currentPositionMs / 1000)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = SecondaryText
                                                    )
                                                }
                                            }

                                            IconButton(onClick = { container.audioPlaybackManager.togglePlayPause() }) {
                                                Icon(
                                                    painter = painterResource(
                                                        id = if (playbackState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                                                    ),
                                                    contentDescription = "Play/Pause",
                                                    tint = SageBrand,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Custom Botanical Bottom Navigation Bar
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider)
                                ) {
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 0.dp
                                    ) {
                                        val items = listOf(
                                            Triple(Screen.Home.route, "Home", R.drawable.ic_nav_home),
                                            Triple(Screen.MusicLibrary.route, "Library", R.drawable.ic_music),
                                            Triple(Screen.VoiceJournal.route, "Journal", R.drawable.ic_nav_journal),
                                            Triple(Screen.Focus.route, "Focus", R.drawable.ic_nav_focus),
                                            Triple(Screen.Settings.route, "Settings", R.drawable.ic_nav_settings)
                                        )

                                        items.forEach { (route, label, iconRes) ->
                                            val selected = currentRoute == route
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = {
                                                    if (currentRoute != route) {
                                                        navController.navigate(route) {
                                                            popUpTo(navController.graph.findStartDestination().id) {
                                                                saveState = true
                                                            }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                },
                                                icon = {
                                                    Icon(
                                                        painter = painterResource(id = iconRes),
                                                        contentDescription = label,
                                                        tint = if (selected) SageBrand else StoneNeutral,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (selected) SageBrand else StoneNeutral
                                                    )
                                                },
                                                colors = NavigationBarItemDefaults.colors(
                                                    indicatorColor = SageBrand.copy(alpha = 0.12f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    AuraNavHost(
                        navController = navController,
                        startDestination = if (onboardingCompleted) Screen.Home.route else Screen.Onboarding.route,
                        contentPadding = innerPadding,
                        allTracks = allTracks,
                        recentlyPlayed = recentlyPlayed,
                        playlists = playlists,
                        folders = folders,
                        latestMood = latestMood,
                        latestVoiceNote = voiceNotes.firstOrNull(),
                        todayFocusedSeconds = totalFocusSec ?: 0L,
                        totalMeditationSeconds = totalMeditationSec ?: 0L,
                        completedMeditationCount = completedMeditationCount,
                        totalFocusSeconds = totalFocusSec ?: 0L,
                        completedFocusCount = completedFocusCount,
                        completedBreathingCount = completedBreathingCount,
                        voiceNotesCount = voiceNotesCount,
                        voiceNotes = voiceNotes,
                        restrictedRules = restrictedRules,
                        activeFocusSession = activeFocusSession,
                        playbackState = playbackState,
                        ambientMixerState = ambientMixerState,
                        recorder = container.voiceRecorder,
                        transcriptionEngine = container.speechTranscriptionEngine,
                        currentTheme = themeMode,
                        isReducedMotion = reducedMotion,
                        isAssistantEnabled = assistantEnabled,
                        onSaveGoals = { goals ->
                            container.userPreferencesRepository.setUserGoals(goals)
                        },
                        onSetOnboardingCompleted = { completed ->
                            container.userPreferencesRepository.setOnboardingCompleted(completed)
                        },
                        onPlayTrack = { track ->
                            scope.launch {
                                container.database.audioDao().updateLastPlayed(track.id, System.currentTimeMillis())
                                container.audioPlaybackManager.playTrack(track)
                            }
                        },
                        onPlayQueue = { tracks, startIndex ->
                            scope.launch {
                                tracks.getOrNull(startIndex)?.let {
                                    container.database.audioDao().updateLastPlayed(it.id, System.currentTimeMillis())
                                }
                                container.audioPlaybackManager.playQueue(tracks, startIndex)
                            }
                        },
                        onAddToQueue = { track ->
                            container.audioPlaybackManager.addToQueue(track)
                        },
                        onRemoveFromQueue = { index ->
                            container.audioPlaybackManager.removeFromQueue(index)
                        },
                        onPlayNext = {
                            container.audioPlaybackManager.playNext()
                        },
                        onPlayPrevious = {
                            container.audioPlaybackManager.playPrevious()
                        },
                        onTogglePlayPause = {
                            container.audioPlaybackManager.togglePlayPause()
                        },
                        onSeekTo = { pos ->
                            container.audioPlaybackManager.seekTo(pos)
                        },
                        onToggleShuffle = {
                            container.audioPlaybackManager.toggleShuffle()
                        },
                        onCycleRepeat = {
                            container.audioPlaybackManager.cycleRepeatMode()
                        },
                        onSetSleepTimer = { mins, fade ->
                            container.audioPlaybackManager.setSleepTimer(mins, fade)
                        },
                        onCancelSleepTimer = {
                            container.audioPlaybackManager.cancelSleepTimer()
                        },
                        onSetAmbientVolume = { soundType, volume ->
                            container.ambientSoundscapeMixer.setLayerVolume(soundType, volume)
                        },
                        onToggleFavorite = { trackId, isFav ->
                            scope.launch {
                                container.database.audioDao().toggleFavorite(trackId, isFav)
                            }
                        },
                        onImportUris = { uris ->
                            container.audioImportManager.importAudioUris(uris)
                        },
                        onCreatePlaylist = { name, desc ->
                            scope.launch {
                                container.database.playlistDao().insertPlaylist(
                                    PlaylistEntity(
                                        id = "playlist_${UUID.randomUUID()}",
                                        name = name,
                                        description = desc
                                    )
                                )
                            }
                        },
                        onDeletePlaylist = { playlistId ->
                            scope.launch {
                                container.database.playlistDao().deletePlaylist(playlistId)
                            }
                        },
                        onAddTrackToPlaylist = { playlistId, trackId ->
                            scope.launch {
                                container.database.playlistDao().insertCrossRef(
                                    PlaylistTrackCrossRef(playlistId, trackId)
                                )
                            }
                        },
                        onRemoveTrackFromPlaylist = { playlistId, trackId ->
                            scope.launch {
                                container.database.playlistDao().removeTrackFromPlaylist(playlistId, trackId)
                            }
                        },
                        onDeleteAudioTrack = { track ->
                            scope.launch {
                                container.audioImportManager.deleteTrack(track)
                            }
                        },
                        onCreateVoiceFolder = { folderName ->
                            scope.launch {
                                container.database.voiceFolderDao().insertFolder(
                                    VoiceFolderEntity(
                                        id = "folder_${UUID.randomUUID()}",
                                        name = folderName
                                    )
                                )
                            }
                        },
                        onDeleteVoiceFolder = { folderId ->
                            scope.launch {
                                container.database.voiceFolderDao().deleteFolder(folderId)
                            }
                        },
                        onSaveMeditationSession = { trackId, title, dur, target, completed ->
                            scope.launch {
                                container.database.meditationDao().insertSession(
                                    MeditationSessionEntity(
                                        trackId = trackId,
                                        title = title,
                                        durationSeconds = dur,
                                        targetDurationSeconds = target,
                                        completed = completed
                                    )
                                )
                            }
                        },
                        onSaveBreathingSession = { pattern, dur, completed ->
                            scope.launch {
                                container.database.breathingDao().insertSession(
                                    BreathingSessionEntity(
                                        patternName = pattern,
                                        durationSeconds = dur,
                                        completed = completed
                                    )
                                )
                            }
                        },
                        onSaveVoiceNote = { note ->
                            scope.launch {
                                container.database.voiceNoteDao().insertNote(note)
                            }
                        },
                        onDeleteVoiceNote = { noteId ->
                            scope.launch {
                                container.database.voiceNoteDao().deleteNote(noteId)
                            }
                        },
                        onStartFocusSession = { mins, intention, soundTrackId ->
                            scope.launch {
                                val endTime = System.currentTimeMillis() + (mins * 60 * 1000L)
                                container.userPreferencesRepository.setActiveFocusSession(endTime, intention)
                                if (soundTrackId != null) {
                                    val sound = container.database.audioDao().getTrackById(soundTrackId)
                                    sound?.let { container.audioPlaybackManager.playTrack(it) }
                                }
                            }
                        },
                        onEndFocusSession = { elapsed, target, completed ->
                            scope.launch {
                                container.userPreferencesRepository.clearActiveFocusSession()
                                container.audioPlaybackManager.stop()
                                container.database.focusDao().insertSession(
                                    FocusSessionEntity(
                                        intention = "Focus Session",
                                        durationSeconds = elapsed,
                                        targetDurationSeconds = target,
                                        completed = completed,
                                        soundTrackId = null
                                    )
                                )
                            }
                        },
                        onToggleRule = { pkg, name, restricted ->
                            scope.launch {
                                if (restricted) {
                                    container.database.appBlockerDao().setAppRestricted(
                                        FocusRuleEntity(pkg, name, true)
                                    )
                                } else {
                                    container.database.appBlockerDao().removeRule(pkg)
                                }
                            }
                        },
                        onSaveMood = { mood, note ->
                            scope.launch {
                                container.database.moodDao().insertMood(
                                    MoodEntryEntity(mood = mood, note = note)
                                )
                            }
                        },
                        onSetTheme = { mode ->
                            scope.launch {
                                container.userPreferencesRepository.setThemeMode(mode)
                            }
                        },
                        onSetReducedMotion = { enabled ->
                            scope.launch {
                                container.userPreferencesRepository.setReducedMotion(enabled)
                            }
                        },
                        onSetAssistantEnabled = { enabled ->
                            scope.launch {
                                container.userPreferencesRepository.setAssistantEnabled(enabled)
                            }
                        }
                    )
                }
            }
        }
    }
}
