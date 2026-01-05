package com.example.cleanflow.ui.screens.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cleanflow.domain.model.MediaFile
import com.example.cleanflow.domain.repository.MediaRepository
import com.example.cleanflow.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ViewerUiState(
    val files: List<MediaFile> = emptyList(),
    val initialIndex: Int = 0
)

class ViewerViewModel(
    private val repository: MediaRepository,
    private val settingsRepository: SettingsRepository,
    private val collectionId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    // Expose preferences
    val userPreferences = settingsRepository.userPreferences

    init {
        loadFiles()
    }

    private fun loadFiles() {
        viewModelScope.launch {
            repository.getFilesByCollection(collectionId).collectLatest { files ->
                _uiState.update {
                    it.copy(files = files)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val repository: MediaRepository,
        private val settingsRepository: SettingsRepository,
        private val collectionId: String
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ViewerViewModel(repository, settingsRepository, collectionId) as T
        }
    }
}
