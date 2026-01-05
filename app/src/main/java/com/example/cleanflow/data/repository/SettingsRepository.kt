package com.example.cleanflow.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ContentScaleMode {
    FIT, CROP
}

data class UserPreferences(
    val autoPlayVideo: Boolean = true,
    val defaultContentScale: ContentScaleMode = ContentScaleMode.CROP
)

class SettingsRepository(private val context: Context) {

    private val AUTO_PLAY_KEY = booleanPreferencesKey("auto_play")
    private val CONTENT_SCALE_KEY = stringPreferencesKey("content_scale")

    val userPreferences: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            val autoPlay = preferences[AUTO_PLAY_KEY] ?: true
            val scaleName = preferences[CONTENT_SCALE_KEY] ?: ContentScaleMode.CROP.name
            val scale = try {
                ContentScaleMode.valueOf(scaleName)
            } catch (e: IllegalArgumentException) {
                ContentScaleMode.CROP
            }
            UserPreferences(autoPlay, scale)
        }

    suspend fun toggleAutoPlay(currentValue: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_PLAY_KEY] = !currentValue
        }
    }

    suspend fun setContentScale(mode: ContentScaleMode) {
        context.dataStore.edit { preferences ->
            preferences[CONTENT_SCALE_KEY] = mode.name
        }
    }
}
