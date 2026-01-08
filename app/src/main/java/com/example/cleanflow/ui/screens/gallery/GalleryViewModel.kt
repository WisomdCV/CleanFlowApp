package com.example.cleanflow.ui.screens.gallery

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleanflow.data.repository.TrashRepository
import com.example.cleanflow.domain.model.GalleryItem
import com.example.cleanflow.domain.model.MediaFile
import com.example.cleanflow.domain.repository.MediaRepository
import com.example.cleanflow.domain.util.Result
import com.example.cleanflow.util.DateHeaderUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GalleryUiState(
    val collectionName: String = "",
    val items: List<GalleryItem> = emptyList(),
    val isLoading: Boolean = true,
    val totalFiles: Int = 0,
    // Selection state
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
) {
    val selectedCount: Int get() = selectedIds.size
}

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val trashRepository: TrashRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val collectionId: String = savedStateHandle.get<String>("collectionId") ?: ""

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()
    
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        loadFiles()
    }

    private fun loadFiles() {
        viewModelScope.launch {
            repository.getFilesByCollection(collectionId).collectLatest { files ->
                val galleryItems = transformToGalleryItems(files)
                _uiState.update {
                    it.copy(
                        collectionName = collectionId,
                        items = galleryItems,
                        isLoading = false,
                        totalFiles = files.size
                    )
                }
            }
        }
    }

    // --- Selection Mode ---
    
    fun activateSelectionMode(initialItemId: Long) {
        _uiState.update {
            it.copy(
                isSelectionMode = true,
                selectedIds = setOf(initialItemId)
            )
        }
    }
    
    fun toggleItemSelection(itemId: Long) {
        _uiState.update { state ->
            val newSelection = if (state.selectedIds.contains(itemId)) {
                state.selectedIds - itemId
            } else {
                state.selectedIds + itemId
            }
            
            // If no items selected, exit selection mode
            if (newSelection.isEmpty()) {
                state.copy(isSelectionMode = false, selectedIds = emptySet())
            } else {
                state.copy(selectedIds = newSelection)
            }
        }
    }
    
    fun selectAll() {
        val allMediaIds = _uiState.value.items
            .filterIsInstance<GalleryItem.Media>()
            .map { it.file.id }
            .toSet()
        
        _uiState.update {
            it.copy(selectedIds = allMediaIds)
        }
    }
    
    fun clearSelection() {
        _uiState.update {
            it.copy(isSelectionMode = false, selectedIds = emptySet())
        }
    }
    
    fun deleteSelected() {
        val selectedIds = _uiState.value.selectedIds
        if (selectedIds.isEmpty()) return
        
        val filesToDelete = _uiState.value.items
            .filterIsInstance<GalleryItem.Media>()
            .filter { it.file.id in selectedIds }
            .map { it.file }
        
        viewModelScope.launch {
            var successCount = 0
            
            for (file in filesToDelete) {
                when (val result = trashRepository.addToTrash(file, collectionId)) {
                    is Result.Success -> {
                        successCount++
                        Log.d("CleanFlow", "Moved to trash: ${file.id}")
                    }
                    is Result.Error -> {
                        Log.e("CleanFlow", "Failed to trash: ${result.exception.message}")
                    }
                    is Result.Loading -> { /* ignore */ }
                }
            }
            
            _snackbarMessage.value = "Movidos $successCount archivos a papelera"
            clearSelection()
        }
    }
    
    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    /**
     * Transforms a flat list of MediaFiles into a list of GalleryItems
     * with date headers interspersed.
     */
    private fun transformToGalleryItems(files: List<MediaFile>): List<GalleryItem> {
        if (files.isEmpty()) return emptyList()

        // Sort by date descending (most recent first)
        val sortedFiles = files.sortedByDescending { it.dateAdded }
        
        val result = mutableListOf<GalleryItem>()
        var lastCategory: String? = null

        sortedFiles.forEachIndexed { index, file ->
            val category = DateHeaderUtils.getDateCategory(file.dateAdded)
            
            // Add header if category changed
            if (category != lastCategory) {
                result.add(GalleryItem.Header(category))
                lastCategory = category
            }
            
            // Add media item with original index for navigation
            // Original index is based on sorted list position
            result.add(GalleryItem.Media(file, index))
        }

        return result
    }
}
