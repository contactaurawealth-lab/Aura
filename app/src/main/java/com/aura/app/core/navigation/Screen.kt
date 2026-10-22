package com.aura.app.core.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Explore : Screen("explore")
    object MusicLibrary : Screen("music_library")
    object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist/$playlistId"
    }
    object Player : Screen("player/{trackId}") {
        fun createRoute(trackId: String) = "player/$trackId"
    }
    object Meditation : Screen("meditation/{trackId}") {
        fun createRoute(trackId: String) = "meditation/$trackId"
    }
    object Breathing : Screen("breathing")
    object VoiceJournal : Screen("journal")
    object VoiceLibrary : Screen("voice_library")
    object JournalDetail : Screen("journal/{noteId}") {
        fun createRoute(noteId: String) = "journal/$noteId"
    }
    object Focus : Screen("focus")
    object AppBlocker : Screen("app_blocker")
    object MoodCheckIn : Screen("mood_checkin")
    object History : Screen("history")
    object Settings : Screen("settings")
    object PermissionCenter : Screen("permission_center")
}
