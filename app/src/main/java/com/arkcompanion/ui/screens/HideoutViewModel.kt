package com.arkcompanion.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkcompanion.data.HideoutTableEntity
import com.arkcompanion.repository.DataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HideoutUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class HideoutViewModel : ViewModel() {
    private val _tables = MutableStateFlow<List<HideoutTableEntity>>(emptyList())
    val tables: StateFlow<List<HideoutTableEntity>> = _tables

    private val _uiState = MutableStateFlow(HideoutUiState())
    val uiState: StateFlow<HideoutUiState> = _uiState

    init {
        refresh()
    }

    fun refresh(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = HideoutUiState(isLoading = true, errorMessage = null)
            val result = DataRepository.getHideout(forceRefresh = forceRefresh)
            
            result.onSuccess { liveHideout ->
                _tables.value = liveHideout.map { dto ->
                    HideoutTableEntity(
                        id = dto.id,
                        name = dto.name,
                        imageUrl = "", // API doesn't currently provide an image url
                        currentLevel = 0,
                        maxLevel = dto.maxLevel
                    )
                }
                _uiState.value = HideoutUiState(isLoading = false, errorMessage = null)
            }.onFailure { error ->
                Log.e("HideoutViewModel", "Failed to fetch hideout", error)
                _uiState.value = HideoutUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to fetch live Hideout data."
                )
            }
        }
    }

    fun incrementLevel(id: String, currentLevel: Int, maxLevel: Int) {
        if (currentLevel < maxLevel) {
            _tables.value = _tables.value.map {
                if (it.id == id) it.copy(currentLevel = currentLevel + 1) else it
            }
        }
    }

    fun decrementLevel(id: String, currentLevel: Int) {
        if (currentLevel > 0) {
            _tables.value = _tables.value.map {
                if (it.id == id) it.copy(currentLevel = currentLevel - 1) else it
            }
        }
    }
}
