package com.example.cleanflow.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cleanflow.domain.model.MediaCollection
import com.example.cleanflow.domain.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val collections: List<MediaCollection> = emptyList(),
    val totalSpaceUsed: Long = 0
)

class HomeViewModel(
    private val repository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCollections()
    }

    private fun loadCollections() {
        viewModelScope.launch {
            repository.getAllCollections().collectLatest { collections ->
                val totalSize = collections.sumOf { it.totalSize }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        collections = collections,
                        totalSpaceUsed = totalSize
                    )
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val repository: MediaRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
