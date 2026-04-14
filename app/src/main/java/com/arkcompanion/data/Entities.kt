package com.arkcompanion.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imageUrl: String,
    val price: Int,
    val rarity: String,
    val category: String
)

@Entity(tableName = "arcs")
data class ArcEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconUrl: String,
    val imageUrl: String,
    val description: String
)
