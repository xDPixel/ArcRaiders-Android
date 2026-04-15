package com.arkcompanion.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive

object QuantityAsStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("QuantityAsString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String {
        return if (decoder is JsonDecoder) {
            decoder.decodeJsonElement().jsonPrimitive.content
        } else {
            decoder.decodeString()
        }
    }
}

@Serializable
data class QuestsResponseDto(
    val data: List<QuestDto>,
    val pagination: PaginationDto? = null
)

@Serializable
data class QuestDto(
    val id: String,
    val name: String,
    @SerialName("trader_name") val traderName: String? = null,
    val description: String? = null,
    val objectives: List<String> = emptyList(),
    @SerialName("granted_items") val grantedItems: List<QuestRewardDto> = emptyList(),
    val rewards: List<QuestRewardDto> = emptyList(),
    val image: String? = null
)

@Serializable
data class QuestRewardDto(
    val id: String,
    @SerialName("item_id") val itemId: String,
    @Serializable(with = QuantityAsStringSerializer::class) val quantity: String = "1",
    val item: QuestRewardItemDto? = null
)

@Serializable
data class QuestRewardItemDto(
    val id: String,
    val name: String,
    val icon: String = "",
    val rarity: String = "",
    @SerialName("item_type") val itemType: String = ""
)
