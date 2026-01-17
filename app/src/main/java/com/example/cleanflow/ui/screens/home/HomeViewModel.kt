package com.example.cleanflow.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleanflow.domain.model.MediaCollection
import com.example.cleanflow.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.cleanflow.domain.model.SmartDashboardStats
import com.example.cleanflow.domain.usecase.GetSmartStatsUseCase
import kotlinx.coroutines.flow.combine

data class HomeUiState(
    val isLoading: Boolean = true,
    val collections: List<MediaCollection> = emptyList(),
    val totalSize: Long = 0,
    val stats: SmartDashboardStats = SmartDashboardStats()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val getSmartStatsUseCase: GetSmartStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getAllCollections(),
                getSmartStatsUseCase()
            ) { collections, stats ->
                val totalSize = collections.sumOf { it.totalSize }
                HomeUiState(
                    isLoading = false,
                    collections = collections,
                    totalSize = totalSize,
                    stats = stats
                )
            }.collectLatest { newState ->
                _uiState.value = newState
            }
        }
    }
}
