package com.aura.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aura_user_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val USER_GOALS = stringSetPreferencesKey("user_goals")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "SYSTEM", "LIGHT", "DARK"
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val ASSISTANT_ENABLED = booleanPreferencesKey("assistant_enabled")
        val ASSISTANT_POS_X = floatPreferencesKey("assistant_pos_x")
        val ASSISTANT_POS_Y = floatPreferencesKey("assistant_pos_y")
        val DEFAULT_FOCUS_MINUTES = intPreferencesKey("default_focus_minutes")
        val LAST_SESSION_TITLE = stringPreferencesKey("last_session_title")
        val ACTIVE_FOCUS_END_TIME = longPreferencesKey("active_focus_end_time")
        val ACTIVE_FOCUS_INTENTION = stringPreferencesKey("active_focus_intention")
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
    }

    val userGoals: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.USER_GOALS] ?: emptySet()
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.THEME_MODE] ?: "SYSTEM"
    }

    val reducedMotion: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.REDUCED_MOTION] ?: false
    }

    val assistantEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.ASSISTANT_ENABLED] ?: false
    }

    val assistantCoordinates: Flow<Pair<Float, Float>> = context.dataStore.data.map { prefs ->
        val x = prefs[PreferencesKeys.ASSISTANT_POS_X] ?: 0.85f
        val y = prefs[PreferencesKeys.ASSISTANT_POS_Y] ?: 0.45f
        Pair(x, y)
    }

    val defaultFocusMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DEFAULT_FOCUS_MINUTES] ?: 25
    }

    val activeFocusSession: Flow<Pair<Long, String>?> = context.dataStore.data.map { prefs ->
        val endTime = prefs[PreferencesKeys.ACTIVE_FOCUS_END_TIME] ?: 0L
        val intention = prefs[PreferencesKeys.ACTIVE_FOCUS_INTENTION] ?: ""
        if (endTime > System.currentTimeMillis()) Pair(endTime, intention) else null
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setUserGoals(goals: Set<String>) {
        context.dataStore.edit { it[PreferencesKeys.USER_GOALS] = goals }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.REDUCED_MOTION] = enabled }
    }

    suspend fun setAssistantEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.ASSISTANT_ENABLED] = enabled }
    }

    suspend fun setAssistantPosition(xRatio: Float, yRatio: Float) {
        context.dataStore.edit {
            it[PreferencesKeys.ASSISTANT_POS_X] = xRatio
            it[PreferencesKeys.ASSISTANT_POS_Y] = yRatio
        }
    }

    suspend fun setDefaultFocusMinutes(minutes: Int) {
        context.dataStore.edit { it[PreferencesKeys.DEFAULT_FOCUS_MINUTES] = minutes }
    }

    suspend fun setActiveFocusSession(endTimeMs: Long, intention: String) {
        context.dataStore.edit {
            it[PreferencesKeys.ACTIVE_FOCUS_END_TIME] = endTimeMs
            it[PreferencesKeys.ACTIVE_FOCUS_INTENTION] = intention
        }
    }

    suspend fun clearActiveFocusSession() {
        context.dataStore.edit {
            it.remove(PreferencesKeys.ACTIVE_FOCUS_END_TIME)
            it.remove(PreferencesKeys.ACTIVE_FOCUS_INTENTION)
        }
    }
}
