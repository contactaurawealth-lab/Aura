package com.aura.app.core.navigation

import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aura.app.core.audio.AmbientMixerState
import com.aura.app.core.audio.PlaybackState
import com.aura.app.core.common.ProceduralAudioGenerator
import com.aura.app.core.database.*
import com.aura.app.core.recording.SpeechTranscriptionEngine
import com.aura.app.core.recording.VoiceRecorder
import com.aura.app.feature.appblocker.AppBlockerScreen
import com.aura.app.feature.breathing.BreathingScreen
import kotlinx.coroutines.flow.Flow
import com.aura.app.feature.explore.ExploreScreen
import com.aura.app.feature.focus.FocusScreen
import com.aura.app.feature.history.HistoryScreen
import com.aura.app.feature.home.HomeScreen
import com.aura.app.feature.journal.JournalDetailScreen
import com.aura.app.feature.journal.VoiceJournalScreen
import com.aura.app.feature.journal.VoiceLibraryScreen
import com.aura.app.feature.library.MusicLibraryScreen
import com.aura.app.feature.library.PlaylistDetailScreen
import com.aura.app.feature.meditation.MeditationScreen
import com.aura.app.feature.mood.MoodCheckInScreen
import com.aura.app.feature.onboarding.OnboardingScreen
import com.aura.app.feature.permissions.PermissionCenterScreen
import com.aura.app.feature.player.PlayerScreen
import com.aura.app.feature.settings.SettingsScreen

@Composable
fun AuraNavHost(
    navController: NavHostController,
    startDestination: String,
    contentPadding: PaddingValues,
    allTracks: List<AudioTrackEntity>,
    recentlyPlayed: List<AudioTrackEntity>,
    playlists: List<PlaylistEntity>,
    folders: List<VoiceFolderEntity>,
    latestMood: MoodEntryEntity?,
    latestVoiceNote: VoiceNoteEntity?,
    todayFocusedSeconds: Long,
    totalMeditationSeconds: Long,
    completedMeditationCount: Int,
    totalFocusSeconds: Long,
    completedFocusCount: Int,
    completedBreathingCount: Int,
    voiceNotesCount: Int,
    voiceNotes: List<VoiceNoteEntity>,
    restrictedRules: List<FocusRuleEntity>,
    activeFocusSession: Pair<Long, String>?,
    playbackState: PlaybackState,
    ambientMixerState: AmbientMixerState,
    recorder: VoiceRecorder,
    transcriptionEngine: SpeechTranscriptionEngine,
    currentTheme: String,
    isReducedMotion: Boolean,
    isAssistantEnabled: Boolean,
    onSaveGoals: suspend (Set<String>) -> Unit,
    onSetOnboardingCompleted: suspend (Boolean) -> Unit,
    onPlayTrack: (AudioTrackEntity) -> Unit,
    onPlayQueue: (List<AudioTrackEntity>, Int) -> Unit,
    onAddToQueue: (AudioTrackEntity) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSetSleepTimer: (Int, Boolean) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onSetAmbientVolume: (ProceduralAudioGenerator.SoundType, Float) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onImportUris: suspend (List<Uri>) -> List<AudioTrackEntity>,
    onCreatePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onAddTrackToPlaylist: (String, String) -> Unit,
    onRemoveTrackFromPlaylist: (String, String) -> Unit,
    onDeleteAudioTrack: (AudioTrackEntity) -> Unit,
    onCreateVoiceFolder: (String) -> Unit,
    onDeleteVoiceFolder: (String) -> Unit,
    onSaveMeditationSession: (String?, String, Long, Long, Boolean) -> Unit,
    onSaveBreathingSession: (String, Long, Boolean) -> Unit,
    onSaveVoiceNote: (VoiceNoteEntity) -> Unit,
    onDeleteVoiceNote: (String) -> Unit,
    onStartFocusSession: (Int, String, String?) -> Unit,
    onEndFocusSession: (Long, Long, Boolean) -> Unit,
    onToggleRule: (String, String, Boolean) -> Unit,
    onSaveMood: (String, String) -> Unit,
    onSetTheme: (String) -> Unit,
    onSetReducedMotion: (Boolean) -> Unit,
    onSetAssistantEnabled: (Boolean) -> Unit,
    getTracksForPlaylist: (String) -> Flow<List<AudioTrackEntity>> = { kotlinx.coroutines.flow.emptyFlow() }
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.padding(contentPadding)
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onSaveGoals = { goals ->
                    onSaveGoals(goals)
                    onSetOnboardingCompleted(true)
                }
            )
        }

        composable(Screen.Home.route) {
            val featured = allTracks.firstOrNull { it.id == "track_singing_bowl" } ?: allTracks.firstOrNull()
            HomeScreen(
                featuredTrack = featured,
                recentlyPlayed = recentlyPlayed,
                latestMood = latestMood,
                latestVoiceNote = latestVoiceNote,
                todayFocusedSeconds = todayFocusedSeconds,
                onNavigateToBreathing = { navController.navigate(Screen.Breathing.route) },
                onNavigateToMeditation = { trackId -> navController.navigate(Screen.Meditation.createRoute(trackId)) },
                onNavigateToExplore = { navController.navigate(Screen.Explore.route) },
                onNavigateToVoiceJournal = { navController.navigate(Screen.VoiceJournal.route) },
                onNavigateToFocus = { navController.navigate(Screen.Focus.route) },
                onNavigateToMoodCheckIn = { navController.navigate(Screen.MoodCheckIn.route) },
                onPlayTrack = { track ->
                    onPlayTrack(track)
                    navController.navigate(Screen.Player.createRoute(track.id))
                }
            )
        }

        composable(Screen.Explore.route) {
            ExploreScreen(
                tracks = allTracks,
                onTrackSelected = { track ->
                    onPlayTrack(track)
                    navController.navigate(Screen.Player.createRoute(track.id))
                },
                onToggleFavorite = onToggleFavorite
            )
        }

        composable(Screen.MusicLibrary.route) {
            MusicLibraryScreen(
                allTracks = allTracks,
                playlists = playlists,
                recentlyPlayed = recentlyPlayed,
                onPlayTrack = { track ->
                    onPlayTrack(track)
                    navController.navigate(Screen.Player.createRoute(track.id))
                },
                onPlayQueue = onPlayQueue,
                onAddToQueue = onAddToQueue,
                onToggleFavorite = onToggleFavorite,
                onImportUris = onImportUris,
                onCreatePlaylist = onCreatePlaylist,
                onDeletePlaylist = onDeletePlaylist,
                onDeleteTrack = onDeleteAudioTrack,
                onOpenPlaylist = { playlistId ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                },
                onAddTrackToPlaylist = onAddTrackToPlaylist,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PlaylistDetail.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
            val playlist = playlists.firstOrNull { it.id == playlistId }
            val playlistTracks by getTracksForPlaylist(playlistId).collectAsState(initial = emptyList())
            PlaylistDetailScreen(
                playlist = playlist,
                tracks = playlistTracks,
                availableTracks = allTracks,
                onPlayTrack = { track ->
                    onPlayTrack(track)
                    navController.navigate(Screen.Player.createRoute(track.id))
                },
                onPlayQueue = onPlayQueue,
                onRemoveTrackFromPlaylist = onRemoveTrackFromPlaylist,
                onAddTrackToPlaylist = onAddTrackToPlaylist,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(navArgument("trackId") { type = NavType.StringType })
        ) { backStackEntry ->
            val trackId = backStackEntry.arguments?.getString("trackId")
            val currentTrack = allTracks.firstOrNull { it.id == trackId } ?: playbackState.currentTrack
            PlayerScreen(
                track = currentTrack,
                playbackState = playbackState,
                ambientMixerState = ambientMixerState,
                onTogglePlayPause = onTogglePlayPause,
                onPlayNext = onPlayNext,
                onPlayPrevious = onPlayPrevious,
                onSeekTo = onSeekTo,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onSetSleepTimer = onSetSleepTimer,
                onCancelSleepTimer = onCancelSleepTimer,
                onToggleFavorite = onToggleFavorite,
                onSelectQueueItem = { index ->
                    playbackState.queue.getOrNull(index)?.let { onPlayTrack(it) }
                },
                onRemoveFromQueue = onRemoveFromQueue,
                onSetAmbientVolume = onSetAmbientVolume,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Meditation.route,
            arguments = listOf(navArgument("trackId") { type = NavType.StringType })
        ) { backStackEntry ->
            val trackId = backStackEntry.arguments?.getString("trackId")
            val track = allTracks.firstOrNull { it.id == trackId }
            MeditationScreen(
                track = track,
                onSaveSession = onSaveMeditationSession,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Breathing.route) {
            BreathingScreen(
                onSaveSession = onSaveBreathingSession,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.VoiceJournal.route) {
            VoiceJournalScreen(
                notes = voiceNotes,
                folders = folders,
                recorder = recorder,
                onSaveNote = onSaveVoiceNote,
                onDeleteNote = onDeleteVoiceNote,
                onNoteClick = { noteId ->
                    navController.navigate(Screen.JournalDetail.createRoute(noteId))
                },
                onNavigateToLibrary = {
                    navController.navigate(Screen.VoiceLibrary.route)
                }
            )
        }

        composable(Screen.VoiceLibrary.route) {
            VoiceLibraryScreen(
                notes = voiceNotes,
                folders = folders,
                onNoteClick = { noteId ->
                    navController.navigate(Screen.JournalDetail.createRoute(noteId))
                },
                onCreateFolder = onCreateVoiceFolder,
                onDeleteFolder = onDeleteVoiceFolder,
                onDeleteNote = onDeleteVoiceNote,
                onToggleFavorite = { noteId, isFav ->
                    voiceNotes.firstOrNull { it.id == noteId }?.let {
                        onSaveVoiceNote(it.copy(isFavorite = isFav))
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.JournalDetail.route,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")
            val note = voiceNotes.firstOrNull { it.id == noteId }
            JournalDetailScreen(
                note = note,
                transcriptionEngine = transcriptionEngine,
                onUpdateNote = onSaveVoiceNote,
                onDeleteNote = onDeleteVoiceNote,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Focus.route) {
            FocusScreen(
                activeSession = activeFocusSession,
                availableSounds = allTracks,
                onStartSession = onStartFocusSession,
                onEndSession = onEndFocusSession,
                onNavigateToAppBlocker = { navController.navigate(Screen.AppBlocker.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AppBlocker.route) {
            AppBlockerScreen(
                restrictedRules = restrictedRules,
                onToggleRule = onToggleRule,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MoodCheckIn.route) {
            MoodCheckInScreen(
                onSaveMood = onSaveMood,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                totalMeditationSeconds = totalMeditationSeconds,
                completedMeditationCount = completedMeditationCount,
                totalFocusSeconds = totalFocusSeconds,
                completedFocusCount = completedFocusCount,
                completedBreathingCount = completedBreathingCount,
                voiceNotesCount = voiceNotesCount,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                currentTheme = currentTheme,
                isReducedMotion = isReducedMotion,
                isAssistantEnabled = isAssistantEnabled,
                onSetTheme = onSetTheme,
                onSetReducedMotion = onSetReducedMotion,
                onSetAssistantEnabled = onSetAssistantEnabled,
                onNavigateToPermissions = { navController.navigate(Screen.PermissionCenter.route) },
                onResetOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PermissionCenter.route) {
            PermissionCenterScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
