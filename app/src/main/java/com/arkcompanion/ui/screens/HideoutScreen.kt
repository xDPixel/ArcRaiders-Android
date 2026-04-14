package com.arkcompanion.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arkcompanion.data.HideoutTableEntity
import com.arkcompanion.ui.components.HideoutStationCard

@Composable
fun HideoutScreen(viewModel: HideoutViewModel = viewModel()) {
    val tables by viewModel.tables.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Hideout",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (uiState.errorMessage != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Could not load Hideout data.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn {
                items(tables) { table ->
                    HideoutStationCard(
                        stationName = table.name,
                        currentLevel = table.currentLevel,
                        onLevelIncrease = { viewModel.incrementLevel(table.id, table.currentLevel, table.maxLevel) },
                        onLevelDecrease = { viewModel.decrementLevel(table.id, table.currentLevel) }
                    )
                }
            }
        }
    }
}
