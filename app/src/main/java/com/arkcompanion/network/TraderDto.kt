package com.arkcompanion.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TraderResponseDto(
    val success: Boolean,
    val data: Map<String, List<TraderItemDto>>
)

@Serializable
data class TraderItemDto(
    val id: String,
    val name: String,
    @SerialName("item_type") val category: String = "",
    val rarity: String = "",
    val value: Int = 0,
    @SerialName("trader_price") val traderPrice: Int = 0,
    val icon: String = "",
    val description: String = ""
)