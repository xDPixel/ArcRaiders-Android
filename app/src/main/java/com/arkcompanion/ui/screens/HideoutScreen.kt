package com.arkcompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arkcompanion.ui.components.HideoutProgressionNode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HideoutScreen(
    onItemClick: (String) -> Unit,
    viewModel: HideoutViewModel = viewModel()
) {
    val stations by viewModel.stations.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var selectedStationIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Hideout",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh(forceRefresh = true) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Hideout",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.errorMessage != null) {
                ErrorState(
                    message = uiState.errorMessage.orEmpty(),
                    onRetry = { viewModel.refresh(forceRefresh = true) },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (stations.isEmpty()) {
                Text(
                    text = "No hideout stations found.",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val safeIndex = selectedStationIndex.coerceIn(0, (stations.size - 1).coerceAtLeast(0))
                val selectedStation = stations.getOrNull(safeIndex)

                Column(modifier = Modifier.fillMaxSize()) {
                    // Custom polished Tab Row
                    ScrollableTabRow(
                        selectedTabIndex = safeIndex,
                        edgePadding = 16.dp,
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant) }
                    ) {
                        stations.forEachIndexed { index, station ->
                            Tab(
                                selected = safeIndex == index,
                                onClick = { selectedStationIndex = index },
                                text = { 
                                    Text(
                                        text = station.name, 
                                        fontWeight = if (safeIndex == index) FontWeight.Bold else FontWeight.Medium
                                    ) 
                                },
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContentColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Content area
                    if (selectedStation != null) {
                        val sortedLevels = selectedStation.levels.entries
                            .mapNotNull { entry -> entry.key.toIntOrNull()?.let { it to entry.value } }
                            .sortedBy { it.first }

                        if (sortedLevels.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No progression data available.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 32.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(
                                    items = sortedLevels,
                                    key = { _, item -> item.first }
                                ) { index, pair ->
                                    val (levelNum, levelData) = pair
                                    val isLast = index == sortedLevels.lastIndex
                                    HideoutProgressionNode(
                                        levelNumber = levelNum,
                                        levelData = levelData,
                                        isLast = isLast,
                                        onItemClick = onItemClick
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}