package com.stela.agent.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stela_settings")

data class AppSettings(
    val themeMode: String = "system",
    val aiProvider: String = "gemini",
    val defaultModel: String = "gemini-2.5-flash",
    val voiceEnabled: Boolean = false,
    val accessibilityEnabled: Boolean = false
)

class SettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val providerKey = stringPreferencesKey("ai_provider")
    private val modelKey = stringPreferencesKey("default_model")
    private val voiceKey = booleanPreferencesKey("voice_enabled")
    private val accessibilityKey = booleanPreferencesKey("accessibility_enabled")

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[themeKey] ?: "system",
            aiProvider = prefs[providerKey] ?: "gemini",
            defaultModel = prefs[modelKey] ?: "gemini-2.5-flash",
            voiceEnabled = prefs[voiceKey] ?: false,
            accessibilityEnabled = prefs[accessibilityKey] ?: false
        )
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[themeKey] = mode }
    }

    suspend fun setAiProvider(value: String) {
        context.dataStore.edit { it[providerKey] = value }
    }

    suspend fun setDefaultModel(value: String) {
        context.dataStore.edit { it[modelKey] = value }
    }

    suspend fun setVoiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[voiceKey] = enabled }
    }

    suspend fun setAccessibilityEnabled(enabled: Boolean) {
        context.dataStore.edit { it[accessibilityKey] = enabled }
    }
}
