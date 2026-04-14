package com.arkcompanion.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkcompanion.data.ArcEntity
import com.arkcompanion.repository.DataRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ArcsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ArcsViewModel : ViewModel() {
    companion object {
        private const val TAG = "ArcsViewModel"
    }

    private val _arcs = MutableStateFlow<List<ArcEntity>>(emptyList())
    val arcs: StateFlow<List<ArcEntity>> = _arcs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _uiState = MutableStateFlow(ArcsUiState())
    val uiState: StateFlow<ArcsUiState> = _uiState

    init {
        refresh()
    }

    fun refresh(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = ArcsUiState(isLoading = true, errorMessage = null)
            val result = DataRepository.getArcs(forceRefresh = forceRefresh)
            result.onSuccess { liveArcs ->
                _arcs.value = liveArcs.map { dto ->
                    ArcEntity(
                        id = dto.id,
                        name = dto.name,
                        iconUrl = dto.icon,
                        imageUrl = dto.image,
                        description = dto.description
                    )
                }
                _uiState.value = ArcsUiState(isLoading = false, errorMessage = null)
            }.onFailure { error ->
                Log.e(TAG, "Live API fetch failed for arcs", error)
                _arcs.value = emptyList()
                _uiState.value = ArcsUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Unable to load live ARC data."
                )
            }
        }
    }
}
