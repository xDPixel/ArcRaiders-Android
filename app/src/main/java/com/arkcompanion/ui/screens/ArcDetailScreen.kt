package com.arkcompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.text.style.TextAlign

data class CombatTactics(
    val weakPoints: String,
    val bestWayToKill: String,
    val mostEffectiveWeapon: String
)

val ArcTacticsMap = mapOf(
    "bastion" to CombatTactics(
        weakPoints = "Cooling vents on the back and legs",
        bestWayToKill = "Coordinate with your squad to distract it while flanking to hit the rear vents. Use EMPs to stun it when it prepares its minigun.",
        mostEffectiveWeapon = "High-Caliber Sniper, Explosives, EMP Grenades"
    ),
    "bombardier" to CombatTactics(
        weakPoints = "Missile pods, rear exhaust",
        bestWayToKill = "Close the distance quickly while it reloads its rocket barrage. Attack from above or behind. Do not stay in open areas.",
        mostEffectiveWeapon = "Assault Rifle, Shotgun, Grenades"
    ),
    "comet" to CombatTactics(
        weakPoints = "Glowing center core",
        bestWayToKill = "Engage at long range and shoot the core. Do not let it get close as it detonates on impact.",
        mostEffectiveWeapon = "Assault Rifle, SMG"
    ),
    "hornet" to CombatTactics(
        weakPoints = "Underbelly core, thrusters",
        bestWayToKill = "Use cover to avoid its lasers. Focus fire on its underbelly when it hovers to attack.",
        mostEffectiveWeapon = "Assault Rifle, Sniper"
    ),
    "wasp" to CombatTactics(
        weakPoints = "Central eye/core",
        bestWayToKill = "Track its fast movements and use hit-scan or fast-firing weapons. Swarms can be deadly.",
        mostEffectiveWeapon = "SMG, Assault Rifle"
    ),
    "rollbot" to CombatTactics(
        weakPoints = "Center eye/sensor",
        bestWayToKill = "Stealth takedown before it alerts others, or hit its sensor immediately with a high-damage weapon.",
        mostEffectiveWeapon = "Silenced Sniper, Shotgun (Close Range)"
    ),
    "tick" to CombatTactics(
        weakPoints = "Center mass",
        bestWayToKill = "Keep your distance and pick them off before they swarm. Explosives work great for groups.",
        mostEffectiveWeapon = "Shotgun, SMG, Explosives"
    ),
    "bison" to CombatTactics(
        weakPoints = "Head/Sensors",
        bestWayToKill = "Dodge its leap attack and shoot its back/head while it recovers.",
        mostEffectiveWeapon = "Shotgun, Assault Rifle"
    ),
    "turret" to CombatTactics(
        weakPoints = "Power cell on back/bottom",
        bestWayToKill = "Flank out of its line of sight and destroy the power cell, or use long-range explosives.",
        mostEffectiveWeapon = "Sniper, Explosives"
    )
)

@Composable
fun ArcDetailScreen(
    arcId: String,
    onBackClick: () -> Unit,
    onItemClick: (String) -> Unit = {},
    viewModel: ArcsViewModel = viewModel()
) {
    val arcs by viewModel.arcs.collectAsState()
    val arc = arcs.firstOrNull { it.id == arcId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackClick) {
                Text("< Back", color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "ARC Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hero Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = arc?.imageUrl?.ifBlank { arc.iconUrl },
                contentDescription = arc?.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = arc?.name ?: "Unknown ARC",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        // Description Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = arc?.description?.ifBlank { "No description available from MetaForge API." } ?: "Unknown",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        val tactics = ArcTacticsMap[arcId.lowercase()]
        if (tactics != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Combat Tactics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TacticRow(icon = Icons.Default.Warning, label = "Weak Points", value = tactics.weakPoints)
                    Spacer(modifier = Modifier.height(12.dp))
                    TacticRow(icon = Icons.Default.Info, label = "Best Way to Kill", value = tactics.bestWayToKill)
                    Spacer(modifier = Modifier.height(12.dp))
                    TacticRow(icon = Icons.Default.CheckCircle, label = "Effective Weapon", value = tactics.mostEffectiveWeapon)
                }
            }
        }

        if (arc?.loot?.isNotEmpty() == true) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Drops & Loot",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(arc!!.loot) { lootItem ->
                            com.arkcompanion.ui.components.HideoutItemView(
                                req = com.arkcompanion.ui.screens.HideoutItemUiModel(
                                    itemId = lootItem.id,
                                    itemName = lootItem.name,
                                    itemIconUrl = lootItem.iconUrl,
                                    quantity = 0
                                ),
                                onItemClick = { onItemClick(lootItem.id) }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun TacticRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}