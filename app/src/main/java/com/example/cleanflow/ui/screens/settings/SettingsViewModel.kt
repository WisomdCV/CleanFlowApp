package com.example.cleanflow.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.example.cleanflow.domain.model.ContentScaleMode
import com.example.cleanflow.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val userPreferences = settingsRepository.userPreferences

    suspend fun toggleAutoPlay(currentValue: Boolean) {
        settingsRepository.toggleAutoPlay(currentValue)
    }

    suspend fun setContentScale(mode: ContentScaleMode) {
        settingsRepository.setContentScale(mode)
    }
}
