package com.arkcompanion.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkcompanion.network.HideoutLevelDto
import com.arkcompanion.repository.DataRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HideoutItemUiModel(
    val itemId: String,
    val itemName: String,
    val itemIconUrl: String,
    val quantity: Int
)

data class HideoutLevelUiModel(
    val requiredItems: List<HideoutItemUiModel>,
    val crafts: List<HideoutItemUiModel>
)

data class HideoutStationUiModel(
    val id: String,
    val name: String,
    val description: String,
    val minLevel: Int,
    val maxLevel: Int,
    val currentLevel: Int = 1,
    val targetLevel: Int = 1,
    val levels: Map<String, HideoutLevelUiModel> = emptyMap(),
    val isExpanded: Boolean = false
)

data class HideoutUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class HideoutViewModel : ViewModel() {
    private val _stations = MutableStateFlow<List<HideoutStationUiModel>>(emptyList())
    val stations: StateFlow<List<HideoutStationUiModel>> = _stations

    private val _uiState = MutableStateFlow(HideoutUiState())
    val uiState: StateFlow<HideoutUiState> = _uiState

    init {
        refresh()
    }

    fun refresh(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = HideoutUiState(isLoading = true, errorMessage = null)
            
            val hideoutJob = async { DataRepository.getHideout(forceRefresh = forceRefresh) }
            val itemsJob = async { DataRepository.getItems(forceRefresh = false) }

            val hideoutResult = hideoutJob.await()
            val itemsResult = itemsJob.await()
            
            val itemsMap = itemsResult.getOrNull()?.associateBy { it.id } ?: emptyMap()

            hideoutResult.onSuccess { liveHideout ->
                Log.d("HideoutViewModel", "Parsed hideout size: ${liveHideout.size}")
                _stations.value = liveHideout.map { dto ->
                    Log.d("HideoutViewModel", "Station ${dto.id} has ${dto.levels.size} levels")
                    val uiLevels = dto.levels.mapValues { (_, levelDto) ->
                        Log.d("HideoutViewModel", "Level ${dto.id} has ${levelDto.requiredItems.size} required items and ${levelDto.crafts.size} crafts")
                        HideoutLevelUiModel(
                            requiredItems = levelDto.requiredItems.map { req ->
                                val searchId = if (req.id == "bandages") "bandage" else req.id
                                val matchedItem = itemsMap[searchId]
                                HideoutItemUiModel(
                                    itemId = req.id,
                                    itemName = matchedItem?.name ?: req.id.replace("-", " ").replaceFirstChar { it.uppercase() },
                                    itemIconUrl = matchedItem?.imageUrl?.ifBlank { null } ?: "https://cdn.metaforge.app/arc-raiders/icons/${searchId}.webp",
                                    quantity = req.quantity
                                )
                            },
                            crafts = levelDto.crafts.map { craftId ->
                                val searchId = if (craftId == "bandages") "bandage" else craftId
                                val matchedItem = itemsMap[searchId]
                                HideoutItemUiModel(
                                    itemId = craftId,
                                    itemName = matchedItem?.name ?: craftId.replace("-", " ").replaceFirstChar { it.uppercase() },
                                    itemIconUrl = matchedItem?.imageUrl?.ifBlank { null } ?: "https://cdn.metaforge.app/arc-raiders/icons/${searchId}.webp",
                                    quantity = 1 // Recipes unlock implicitly as 1
                                )
                            }
                        )
                    }

                    val actualMinLevel = dto.levels.entries
                        .filter { it.value.requiredItems.isNotEmpty() || it.value.crafts.isNotEmpty() }
                        .minOfOrNull { it.key.toIntOrNull() ?: 1 } ?: 1

                    val maxLevel = when (dto.id) {
                        "weapon-bench" -> 1
                        "scrappy" -> 5
                        else -> dto.maxLevel
                    }

                    val filteredUiLevels = uiLevels.filterKeys { 
                        val levelInt = it.toIntOrNull() ?: 1
                        levelInt <= maxLevel
                    }

                    HideoutStationUiModel(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description,
                        minLevel = actualMinLevel,
                        maxLevel = maxLevel,
                        currentLevel = 1,
                        targetLevel = actualMinLevel.coerceAtMost(maxLevel),
                        levels = filteredUiLevels,
                        isExpanded = false
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

    fun toggleExpanded(id: String) {
        _stations.value = _stations.value.map {
            if (it.id == id) it.copy(isExpanded = !it.isExpanded) else it
        }
    }

    fun setTargetLevel(id: String, level: Int) {
        _stations.value = _stations.value.map { station ->
            if (station.id == id && level in station.minLevel..station.maxLevel) {
                station.copy(targetLevel = level)
            } else station
        }
    }
}
