package com.example.cleanflow.ui.screens.viewer

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleanflow.data.repository.TrashRepository
import com.example.cleanflow.domain.model.MediaFile
import com.example.cleanflow.domain.repository.MediaRepository
import com.example.cleanflow.domain.repository.SettingsRepository
import com.example.cleanflow.domain.util.AppException
import com.example.cleanflow.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ViewerUiState(
    val files: List<MediaFile> = emptyList(),
    val initialIndex: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val settingsRepository: SettingsRepository,
    private val trashRepository: TrashRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val collectionId: String = savedStateHandle.get<String>("collectionId") ?: ""
    private val initialIndex: Int = savedStateHandle.get<Int>("initialIndex") ?: 0

    private val _uiState = MutableStateFlow(ViewerUiState(initialIndex = initialIndex))
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    val userPreferences = settingsRepository.userPreferences
    
    private val _navigationEvent = MutableStateFlow<Int?>(null)
    val navigationEvent: StateFlow<Int?> = _navigationEvent.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()
    
    private val _error = MutableStateFlow<AppException?>(null)
    val error: StateFlow<AppException?> = _error.asStateFlow()

    init {
        loadFiles()
    }

    private fun loadFiles() {
        viewModelScope.launch {
            repository.getFilesByCollection(collectionId).collectLatest { files ->
                val sortedFiles = files.sortedByDescending { it.dateAdded }
                _uiState.update {
                    it.copy(
                        files = sortedFiles,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun moveToTrash(file: MediaFile) {
        viewModelScope.launch {
            when (val result = trashRepository.addToTrash(file, collectionId)) {
                is Result.Success -> {
                    Log.d("CleanFlow", "Moved to trash: ${file.id} - ${file.displayName}")
                    _snackbarMessage.value = "Enviado a papelera"
                }
                is Result.Error -> {
                    Log.e("CleanFlow", "Failed to move to trash: ${result.exception.message}")
                    _error.value = result.exception
                }
                is Result.Loading -> { /* ignore */ }
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
    
    fun consumeError() {
        _error.value = null
    }
}
