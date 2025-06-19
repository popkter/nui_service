package com.senseauto.nui_service.entity

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class BaseAgentResponse(
    val requestId: String,
    val type: AgentResponseType,
    val data: String
)

@Serializable(with = AgentResponseTypeSerializer::class)
sealed class AgentResponseType(val type: String) {
    data object Stream : AgentResponseType("stream")
    data object Finish : AgentResponseType("finish")
    data object Action : AgentResponseType("action")
    data object Ext : AgentResponseType("ext")
    data object Error : AgentResponseType("error")
    data object Unknown : AgentResponseType("unknown")

    companion object {
        fun from(type: String): AgentResponseType = when (type) {
            "stream" -> Stream
            "finish" -> Finish
            "error" -> Error
            "action" -> Action
            "ext" -> Ext
            else -> Unknown
        }
    }
}

object AgentResponseTypeSerializer : KSerializer<AgentResponseType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ChatResponseType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AgentResponseType) {
        encoder.encodeString(value.type)
    }

    override fun deserialize(decoder: Decoder): AgentResponseType {
        val value = decoder.decodeString()
        return AgentResponseType.from(value)
    }
}
