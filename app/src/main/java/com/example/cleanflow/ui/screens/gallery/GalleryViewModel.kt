package com.example.cleanflow.ui.screens.gallery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleanflow.domain.model.GalleryItem
import com.example.cleanflow.domain.model.MediaFile
import com.example.cleanflow.domain.repository.MediaRepository
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
    val totalFiles: Int = 0
)

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val collectionId: String = savedStateHandle.get<String>("collectionId") ?: ""

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

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
