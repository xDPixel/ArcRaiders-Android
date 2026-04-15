package com.arkcompanion.data

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

@Serializable
data class ArcLootItemEntity(
    val id: String,
    val name: String,
    val iconUrl: String,
    val rarity: String,
    val category: String
)

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromLootItemList(value: List<ArcLootItemEntity>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toLootItemList(value: String): List<ArcLootItemEntity> {
        if (value.isBlank()) return emptyList()
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
