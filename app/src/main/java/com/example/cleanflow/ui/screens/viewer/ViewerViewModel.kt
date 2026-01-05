package com.example.cleanflow.ui.screens.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cleanflow.domain.model.MediaFile
import com.example.cleanflow.domain.repository.MediaRepository
import com.example.cleanflow.data.repository.SettingsRepository
import com.example.cleanflow.domain.usecase.DeleteFileUseCase
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

    // Delete UseCase
    private val deleteFileUseCase = DeleteFileUseCase(repository)
    
    // Commands regarding navigation
    private val _navigationEvent = MutableStateFlow<Int?>(null)
    val navigationEvent: StateFlow<Int?> = _navigationEvent.asStateFlow()

    // Safe Delete State
    private var pendingDeleteFile: MediaFile? = null
    private var pendingDeleteIndex: Int = -1

    // Events
    private val _onShowSnackbar = MutableStateFlow<Boolean>(false)
    val onShowSnackbar: StateFlow<Boolean> = _onShowSnackbar.asStateFlow()

    init {
        loadFiles()
    }

    private fun loadFiles() {
        viewModelScope.launch {
            repository.getFilesByCollection(collectionId).collectLatest { files ->
                // If we have a pending delete, we should filter it out from the "source of truth"
                // until it's confirmed. Or, we use a local UI state entirely.
                // Simpler approach: Rely on local list modification for "Pending" state if possible, 
                // but since we collectLatest from Repo, treating the Repo as source of truth is redundant IF we don't modify repo.
                // BETTER: We manage a local list merge.
                // For this iteration, let's keep it simple: 
                // 1. Optimistic: Remove from UI list copy. 
                // 2. Repo update triggers overwrite, so we must be careful. 
                // If we remove from UI list locally, and then Repo emits same list, our removal is lost?
                // NO, because we update _uiState. To make this persist across repo updates, 
                // we'd need to filter the incoming repo list against pending IDs.
                
                val currentPendingId = pendingDeleteFile?.id ?: -1L
                val visibleFiles = files.filter { it.id != currentPendingId }
                
                _uiState.update {
                    it.copy(files = visibleFiles)
                }
            }
        }
    }

    // Called when user taps Delete
    fun prepareDelete(file: MediaFile, index: Int) {
        pendingDeleteFile = file
        pendingDeleteIndex = index
        
        // Optimistic Remove
        _uiState.update { state ->
            val mutableList = state.files.toMutableList()
            mutableList.remove(file)
            state.copy(files = mutableList)
        }
        
        // Show Snackbar
        _onShowSnackbar.value = true
    }

    fun restoreDeletedFile() {
        val file = pendingDeleteFile ?: return
        
        // Restore to UI Logic
        // We trigger a reload effectively, or insert back at index.
        // Easiest: clear pending, reload from repo will auto-add it (since it wasn't deleted from disk).
        // BUT we need immediate UI reaction.
        
        _uiState.update { state ->
            val mutableList = state.files.toMutableList()
            // Safety check for index bounds
            val index = pendingDeleteIndex.coerceIn(0, mutableList.size)
            mutableList.add(index, file)
            state.copy(files = mutableList)
        }
        
        pendingDeleteFile = null
        pendingDeleteIndex = -1
        _onShowSnackbar.value = false
    }

    fun confirmDelete() {
        val file = pendingDeleteFile ?: return
        val uri = file.uri
        
        viewModelScope.launch {
            val success = deleteFileUseCase(uri)
            if (success) {
                println("Permanently deleted ${file.id}")
            } else {
                 // Restore if failed?
                 restoreDeletedFile()
                 // Maybe show error toast
            }
            // Clear pending regardless (transaction done)
            pendingDeleteFile = null
            pendingDeleteIndex = -1
            _onShowSnackbar.value = false
        }
    }
    
    fun dismissSnackbar() {
        // If dismissed without Action (Undo), we confirm delete
        if (pendingDeleteFile != null) {
            confirmDelete()
        }
        _onShowSnackbar.value = false
    }

    fun keepCurrentFile(currentIndex: Int) {
         _navigationEvent.update { currentIndex + 1 }
    }
    
    fun resetNavigation() {
        _navigationEvent.update { null }
    }
    
    fun consumeSnackbar() {
        _onShowSnackbar.value = false
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
