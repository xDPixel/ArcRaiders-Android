package com.arkcompanion

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkcompanion.network.ItemDto
import com.arkcompanion.repository.DataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UiState {
    object Loading : UiState()
    data class Success(val items: List<ItemDto>) : UiState()
    data class Error(val message: String) : UiState()
}

class MainViewModel : ViewModel() {
    companion object {
        private const val TAG = "MainViewModel"
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        fetchItems()
    }

    fun fetchItems(forceRefresh: Boolean = false) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val result = DataRepository.getItems(forceRefresh)
            result.onSuccess { data ->
                _uiState.value = UiState.Success(data)
            }.onFailure { error ->
                Log.e(TAG, "Live item fetch failed", error)
                _uiState.value = UiState.Error(
                    error.message ?: "Unable to load live items. Please check your API settings and connection."
                )
            }
        }
    }
}
