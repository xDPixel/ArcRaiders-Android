package com.arkcompanion.network

import kotlinx.serialization.Serializable

@Serializable
data class EventsScheduleResponseDto(
    val data: List<EventScheduleDto> = emptyList()
)

@Serializable
data class EventScheduleDto(
    val name: String,
    val map: String,
    val icon: String = "",
    val startTime: Long,
    val endTime: Long
)
