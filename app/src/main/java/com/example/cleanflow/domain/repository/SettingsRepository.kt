package com.example.cleanflow.domain.repository

import com.example.cleanflow.domain.model.ContentScaleMode
import com.example.cleanflow.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Interface for settings/preferences operations.
 * Follows Clean Architecture - domain layer abstraction.
 */
interface SettingsRepository {
    val userPreferences: Flow<UserPreferences>
    suspend fun toggleAutoPlay(currentValue: Boolean)
    suspend fun setContentScale(mode: ContentScaleMode)
}
