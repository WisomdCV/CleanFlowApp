package com.example.cleanflow.ui.screens.trash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleanflow.data.local.TrashedFileEntity
import com.example.cleanflow.data.repository.TrashRepository
import com.example.cleanflow.domain.repository.MediaRepository
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
            try {
                trashRepository.removeFromTrash(file.mediaId)
                Log.d("CleanFlow", "Restored from trash: ${file.mediaId} - ${file.displayName}")
                _snackbarMessage.value = "Archivo restaurado"
            } catch (e: Exception) {
                Log.e("CleanFlow", "Failed to restore: ${e.message}", e)
                _snackbarMessage.value = "Error al restaurar"
            }
        }
    }

    fun permanentlyDelete(file: TrashedFileEntity) {
        viewModelScope.launch {
            try {
                trashRepository.removeFromTrash(file.mediaId)
                val success = mediaRepository.deleteFile(file.uri)
                if (success) {
                    Log.d("CleanFlow", "Permanently deleted: ${file.mediaId}")
                    _snackbarMessage.value = "Eliminado permanentemente"
                } else {
                    Log.e("CleanFlow", "Failed to delete from disk: ${file.mediaId}")
                    _snackbarMessage.value = "Error al eliminar"
                }
            } catch (e: Exception) {
                Log.e("CleanFlow", "Exception during permanent delete: ${e.message}", e)
                _snackbarMessage.value = "Error al eliminar"
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            val files = _uiState.value.trashedFiles
            if (files.isEmpty()) return@launch
            
            _uiState.update { it.copy(isProcessing = true, processingMessage = "Eliminando...") }
            
            var successCount = 0
            val total = files.size
            
            for ((index, file) in files.withIndex()) {
                try {
                    _uiState.update { 
                        it.copy(processingMessage = "Eliminando ${index + 1} de $total...") 
                    }
                    val deleted = mediaRepository.deleteFileWithoutRefresh(file.uri)
                    if (deleted) successCount++
                } catch (e: Exception) {
                    Log.e("CleanFlow", "Failed to delete ${file.mediaId}: ${e.message}")
                }
            }
            
            trashRepository.clearTrash()
            mediaRepository.refreshCache()
            
            _uiState.update { it.copy(isProcessing = false, processingMessage = "") }
            _snackbarMessage.value = "Se eliminaron $successCount de $total archivos"
        }
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }
}
