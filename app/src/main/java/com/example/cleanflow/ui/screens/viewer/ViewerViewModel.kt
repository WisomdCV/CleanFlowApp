package com.example.cleanflow.ui.screens.viewer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cleanflow.domain.model.MediaFile
import com.example.cleanflow.domain.repository.MediaRepository
import com.example.cleanflow.data.repository.SettingsRepository
import com.example.cleanflow.data.repository.TrashRepository
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
    private val trashRepository: TrashRepository,
    private val collectionId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    // Expose preferences
    val userPreferences = settingsRepository.userPreferences
    
    // Commands regarding navigation
    private val _navigationEvent = MutableStateFlow<Int?>(null)
    val navigationEvent: StateFlow<Int?> = _navigationEvent.asStateFlow()

    // Snackbar message event
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

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

    /**
     * Move file to trash. This is immediate and reliable.
     * The file will be filtered out from the grid automatically.
     */
    fun moveToTrash(file: MediaFile) {
        viewModelScope.launch {
            try {
                trashRepository.addToTrash(file, collectionId)
                Log.d("CleanFlow", "Moved to trash: ${file.id} - ${file.displayName}")
                _snackbarMessage.value = "Enviado a papelera"
            } catch (e: Exception) {
                Log.e("CleanFlow", "Failed to move to trash: ${e.message}", e)
                _snackbarMessage.value = "Error al mover a papelera"
            }
        }
    }

    fun keepCurrentFile(currentIndex: Int) {
         _navigationEvent.update { currentIndex + 1 }
    }
    
    fun resetNavigation() {
        _navigationEvent.update { null }
    }
    
    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val repository: MediaRepository,
        private val settingsRepository: SettingsRepository,
        private val trashRepository: TrashRepository,
        private val collectionId: String
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ViewerViewModel(repository, settingsRepository, trashRepository, collectionId) as T
        }
    }
}
