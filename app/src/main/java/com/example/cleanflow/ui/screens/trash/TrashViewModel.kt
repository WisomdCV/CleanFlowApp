package com.example.cleanflow.ui.screens.trash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleanflow.data.local.TrashedFileEntity
import com.example.cleanflow.data.repository.TrashRepository
import com.example.cleanflow.domain.repository.MediaRepository
import com.example.cleanflow.domain.util.AppException
import com.example.cleanflow.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrashUiState(
    val trashedFiles: List<TrashedFileEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val processingMessage: String = "",
    val totalSize: Long = 0
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val trashRepository: TrashRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()
    
    private val _error = MutableStateFlow<AppException?>(null)
    val error: StateFlow<AppException?> = _error.asStateFlow()

    init {
        loadTrashedFiles()
    }

    private fun loadTrashedFiles() {
        viewModelScope.launch {
            trashRepository.trashedFiles.collectLatest { files ->
                val totalSize = files.sumOf { it.size }
                _uiState.update {
                    it.copy(
                        trashedFiles = files,
                        isLoading = false,
                        totalSize = totalSize
                    )
                }
            }
        }
    }

    fun restoreFile(file: TrashedFileEntity) {
        viewModelScope.launch {
            when (val result = trashRepository.removeFromTrash(file.mediaId)) {
                is Result.Success -> {
                    Log.d("CleanFlow", "Restored from trash: ${file.mediaId}")
                    _snackbarMessage.value = "Archivo restaurado"
                }
                is Result.Error -> {
                    Log.e("CleanFlow", "Failed to restore: ${result.exception.message}")
                    _error.value = result.exception
                }
                is Result.Loading -> { /* ignore */ }
            }
        }
    }

    fun permanentlyDelete(file: TrashedFileEntity) {
        viewModelScope.launch {
            // First remove from trash DB
            when (val removeResult = trashRepository.removeFromTrash(file.mediaId)) {
                is Result.Error -> {
                    _error.value = removeResult.exception
                    return@launch
                }
                else -> { /* continue */ }
            }
            
            // Then delete from disk
            when (val deleteResult = mediaRepository.deleteFile(file.uri)) {
                is Result.Success -> {
                    Log.d("CleanFlow", "Permanently deleted: ${file.mediaId}")
                    _snackbarMessage.value = "Eliminado permanentemente"
                }
                is Result.Error -> {
                    Log.e("CleanFlow", "Failed to delete: ${deleteResult.exception.message}")
                    _error.value = deleteResult.exception
                }
                is Result.Loading -> { /* ignore */ }
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            val files = _uiState.value.trashedFiles
            if (files.isEmpty()) return@launch
            
            _uiState.update { it.copy(isProcessing = true, processingMessage = "Eliminando...") }
            
            var successCount = 0
            var errorCount = 0
            val total = files.size
            
            for ((index, file) in files.withIndex()) {
                _uiState.update { 
                    it.copy(processingMessage = "Eliminando ${index + 1} de $total...") 
                }
                
                when (val result = mediaRepository.deleteFileWithoutRefresh(file.uri)) {
                    is Result.Success -> successCount++
                    is Result.Error -> {
                        errorCount++
                        Log.e("CleanFlow", "Failed to delete ${file.mediaId}: ${result.exception.message}")
                    }
                    is Result.Loading -> { /* ignore */ }
                }
            }
            
            trashRepository.clearTrash()
            mediaRepository.refreshCache()
            
            _uiState.update { it.copy(isProcessing = false, processingMessage = "") }
            
            if (errorCount > 0) {
                _snackbarMessage.value = "Eliminados $successCount de $total ($errorCount errores)"
            } else {
                _snackbarMessage.value = "Se eliminaron $successCount archivos"
            }
        }
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }
    
    fun consumeError() {
        _error.value = null
    }
}
