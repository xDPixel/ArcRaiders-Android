package com.arkcompanion.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkcompanion.network.TraderItemDto
import com.arkcompanion.network.QuestDto
import com.arkcompanion.repository.DataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TradersUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class TradersViewModel : ViewModel() {
    companion object {
        private const val TAG = "TradersViewModel"
    }

    // Map of TraderName -> List of items they sell
    private val _traders = MutableStateFlow<Map<String, List<TraderItemDto>>>(emptyMap())
    val traders: StateFlow<Map<String, List<TraderItemDto>>> = _traders

    // Map of TraderName -> List of quests they offer
    private val _quests = MutableStateFlow<Map<String, List<QuestDto>>>(emptyMap())
    val quests: StateFlow<Map<String, List<QuestDto>>> = _quests

    private val _uiState = MutableStateFlow(TradersUiState())
    val uiState: StateFlow<TradersUiState> = _uiState

    init {
        refresh(forceRefresh = false)
    }

    fun refresh(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = TradersUiState(isLoading = true, errorMessage = null)
            val result = DataRepository.getTraders(forceRefresh = forceRefresh)
            val questsResult = DataRepository.getQuests(forceRefresh = forceRefresh)
            
            result.onSuccess { response ->
                _traders.value = response.data
            }.onFailure { error ->
                Log.e(TAG, "Failed to fetch traders", error)
                _traders.value = emptyMap()
                _uiState.value = TradersUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Unable to load traders data."
                )
                return@launch
            }

            questsResult.onSuccess { allQuests ->
                _quests.value = allQuests.filter { !it.traderName.isNullOrBlank() }
                    .groupBy { it.traderName!! }
            }.onFailure { error ->
                Log.e(TAG, "Failed to fetch quests", error)
                _quests.value = emptyMap()
            }

            _uiState.value = TradersUiState(isLoading = false, errorMessage = null)
        }
    }
}