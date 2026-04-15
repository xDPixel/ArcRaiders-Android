package com.arkcompanion.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val pagination: PaginationDto? = null
)

@Serializable
data class PaginationDto(
    val page: Int? = null,
    val total: Int? = null,
    val totalPages: Int? = null,
    val hasNextPage: Boolean = false
)

@Serializable
data class ItemDto(
    val id: String,
    val name: String,
    @SerialName("item_type") val category: String = "",
    val rarity: String = "",
    @SerialName("value") val price: Int = 0,
    @SerialName("icon") val imageUrl: String = ""
)

@Serializable
data class ArcDto(
    val id: String,
    val name: String,
    val icon: String = "",
    val image: String = "",
    val description: String = ""
)

@Serializable
data class HideoutRequirementDto(
    val id: String,
    val quantity: Int
)

@Serializable
data class HideoutLevelDto(
    val unlockRequirements: List<HideoutRequirementDto> = emptyList(),
    val requiredItems: List<HideoutRequirementDto> = emptyList(),
    val crafts: List<String> = emptyList()
)

@Serializable
data class HideoutDto(
    val id: String,
    val name: String,
    val description: String = "",
    val maxLevel: Int = 1,
    val levels: Map<String, HideoutLevelDto> = emptyMap()
)

interface MetaForgeService {
    @GET("items")
    suspend fun getItems(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): PaginatedResponse<ItemDto>

    @GET("arcs")
    suspend fun getArcs(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): PaginatedResponse<ArcDto>

    @GET("hideout")
    suspend fun getHideout(): List<HideoutDto>

    @GET("traders")
    suspend fun getTraders(): TraderResponseDto

    @GET("quests")
    suspend fun getQuests(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): QuestsResponseDto

    @GET("events-schedule")
    suspend fun getEventsSchedule(): EventsScheduleResponseDto
}

val metaForgeService: MetaForgeService by lazy {
    ApiClient.retrofit.create(MetaForgeService::class.java)
}
