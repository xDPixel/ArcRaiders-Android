package com.arkcompanion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.arkcompanion.ui.screens.HideoutItemUiModel
import com.arkcompanion.ui.screens.HideoutLevelUiModel

@Composable
fun HideoutProgressionNode(
    levelNumber: Int,
    levelData: HideoutLevelUiModel,
    isLast: Boolean
) {
    val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    
    // Removing IntrinsicSize.Min because it crashes Compose when paired with a nested LazyRow (SubcomposeLayout).
    // Instead, we use drawBehind to draw the vertical connecting line seamlessly down the entire height of the Row.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (!isLast) {
                    val strokeWidth = 3.dp.toPx()
                    val startX = 24.dp.toPx() // Dead center of the 48dp wide left column
                    val startY = 36.dp.toPx() // Starts just below the 36dp bubble
                    drawLine(
                        color = lineColor,
                        start = Offset(startX, startY),
                        end = Offset(startX, size.height),
                        strokeWidth = strokeWidth
                    )
                }
            }
    ) {
        // Timeline Graphics (Left Column)
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Node Bubble
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$levelNumber",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Content Card (Right Column)
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 32.dp), // Extra padding to stretch the line to next node
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (levelData.requiredItems.isNotEmpty()) {
                    Text(
                        text = "Upgrade Cost",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(levelData.requiredItems) { req ->
                            HideoutItemView(req)
                        }
                    }
                }

                if (levelData.requiredItems.isNotEmpty() && levelData.crafts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (levelData.crafts.isNotEmpty()) {
                    Text(
                        text = "Unlocks Crafting",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(levelData.crafts) { craft ->
                            HideoutItemView(craft)
                        }
                    }
                }

                if (levelData.requiredItems.isEmpty() && levelData.crafts.isEmpty()) {
                    Text(
                        text = "No cost or unlocks for this level.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HideoutItemView(req: HideoutItemUiModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (req.itemIconUrl.isNotBlank()) {
                AsyncImage(
                    model = req.itemIconUrl,
                    contentDescription = req.itemName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.padding(6.dp).fillMaxSize()
                )
            } else {
                // Fallback icon
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            }
            
            // Quantity Badge
            if (req.quantity > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 6.dp, y = 6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "x${req.quantity}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = req.itemName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2
        )
    }
}