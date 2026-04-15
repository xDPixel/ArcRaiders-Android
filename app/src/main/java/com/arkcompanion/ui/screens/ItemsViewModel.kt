package com.arkcompanion.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkcompanion.data.ItemEntity
import com.arkcompanion.repository.DataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

data class ItemsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ItemsViewModel : ViewModel() {
    companion object {
        private const val TAG = "ItemsViewModel"
    }

    private val _allItems = MutableStateFlow<List<ItemEntity>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _uiState = MutableStateFlow(ItemsUiState())
    val uiState: StateFlow<ItemsUiState> = _uiState

    val items: StateFlow<List<ItemEntity>> = combine(
        _allItems,
        _searchQuery,
        _selectedCategory
    ) { allItems, query, category ->
        allItems.filter { item ->
            val matchesCategory = category == "All" || item.category == category
            val matchesQuery = query.isBlank() || item.name.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = _allItems
        .combine(_selectedCategory) { allItems, _ ->
            listOf("All") + allItems.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    init {
        refresh(forceRefresh = false)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun refreshAllAppContent() {
        viewModelScope.launch {
            _uiState.value = ItemsUiState(isLoading = true, errorMessage = null)
            val itemsJob = async { DataRepository.getItems(forceRefresh = true) }
            val arcsJob = async { DataRepository.getArcs(forceRefresh = true) }
            val hideoutJob = async { DataRepository.getHideout(forceRefresh = true) }

            val itemsResult = itemsJob.await()
            arcsJob.await()
            hideoutJob.await()

            itemsResult.onSuccess { liveItems ->
                _allItems.value = liveItems
                _uiState.value = ItemsUiState(isLoading = false, errorMessage = null)
            }.onFailure { error ->
                Log.e(TAG, "Live API fetch failed for items", error)
                _allItems.value = emptyList()
                _uiState.value = ItemsUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Unable to load live item data."
                )
            }
        }
    }

    fun refresh(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = ItemsUiState(isLoading = true, errorMessage = null)
            val result = DataRepository.getItems(forceRefresh = forceRefresh)
            result.onSuccess { liveItems ->
                _allItems.value = liveItems
                _uiState.value = ItemsUiState(isLoading = false, errorMessage = null)
            }.onFailure { error ->
                Log.e(TAG, "Live API fetch failed for items", error)
                _allItems.value = emptyList()
                _uiState.value = ItemsUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Unable to load live item data."
                )
            }
        }
    }
}
