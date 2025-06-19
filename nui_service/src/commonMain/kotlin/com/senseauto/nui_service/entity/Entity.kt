package com.senseauto.nui_service.entity

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * [conversationId] current session id for track.
 * [role] chat response from
 * [content] if this::role is User, it means user input. if this::role is Bot, it means agent response
 * [intent] this::content intent from nlu
 * [userId] current request from user id.
 * [position] current request from position
 * [stream] whether the current content is streamed or not
 * [finish] if this::content is stream. signal it is finished
 * [speakType] if this::role is Bot, it means the type of tts content
 * [timestamp] the request starts from.
 *
 */
@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
@Serializable
data class ChatData(
    val conversationId: String = Uuid.random().toHexString().replace("-", ""),
    val role: Role,
    val content: String,
    val intent: String = "",
    val userId: String,
    val position: PositionType = PositionType.Unknown,
    val stream: Boolean = false,
    val finish: Boolean = false,
    val speakType: SpeakType = SpeakType.Tts,
//    val chatStatusType: ChatStatusType = ChatStatusType.Normal,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
) {
    fun fromBot(): Boolean {
        return role == Role.Bot
    }

    fun fromUser(): Boolean {
        return role == Role.User
    }
}


@Serializable(with = PositionTypeSerializer::class)
sealed class PositionType(val type: Int) {

    data object Driver : PositionType(1)
    data object Copilot : PositionType(2)
    data object MiddleLeft : PositionType(3)
    data object MiddleRight : PositionType(4)
    data object MiddleCenter : PositionType(5)
    data object RearLeft : PositionType(6)
    data object RearRight : PositionType(7)
    data object RearCenter : PositionType(8)
    data object Unknown : PositionType(0)

    override fun equals(other: Any?): Boolean {
        println("equals: $this other: $other")
        return (this === other) || (other is PositionType && type == other.type)
    }

    override fun hashCode(): Int {
        return type
    }

    companion object Companion {

        val allPositionTypes: List<PositionType>
            get() = listOf(
                Driver,
                Copilot,
                MiddleLeft,
                MiddleRight,
                MiddleCenter,
                RearLeft,
                RearRight,
                RearCenter,
                Unknown
            )
        fun from(type: Int): PositionType = when (type) {
            0 -> Unknown
            1 -> Driver
            2 -> Copilot
            3 -> MiddleLeft
            4 -> MiddleRight
            5 -> MiddleCenter
            6 -> RearLeft
            7 -> RearRight
            8 -> RearCenter
            else -> Unknown
        }
    }
}

object PositionTypeSerializer : KSerializer<PositionType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("PositionType", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: PositionType) {
        encoder.encodeInt(value.type)
    }

    override fun deserialize(decoder: Decoder): PositionType {
        val value = decoder.decodeInt()
        return PositionType.from(value)
    }
}

@Serializable(with = SpeakTypeSerializer::class)
sealed class SpeakType(val type: Int) {

    data object Asr : SpeakType(0)

    //normal chat
    data object Tts : SpeakType(1)

    //active recommend scene
    data object Recommend : SpeakType(2)

    //emergency scene
    data object Emergency : SpeakType(3)

    //phone call
    data object Dialogue : SpeakType(4)

    //wake up
    data object Wakeup : SpeakType(5)

    //interim broadcast, for those which spend too long
    data object Interim : SpeakType(6)

    companion object Companion {
        fun from(type: Int): SpeakType = when (type) {
            0 -> Asr
            1 -> Tts
            2 -> Recommend
            3 -> Emergency
            4 -> Dialogue
            5 -> Wakeup
            6 -> Interim
            else -> Tts
        }
    }
}

object SpeakTypeSerializer : KSerializer<SpeakType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SpeakType", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: SpeakType) {
        encoder.encodeInt(value.type)
    }

    override fun deserialize(decoder: Decoder): SpeakType {
        val value = decoder.decodeInt()
        return SpeakType.from(value)
    }
}

@Serializable(with = ChatStatusTypeSerializer::class)
sealed class ChatStatusType(val type: Int) {

    data object Normal : ChatStatusType(1)
    data object Interrupt : ChatStatusType(2)
    data object Unknown : ChatStatusType(0)

    companion object Companion {
        fun from(type: Int): ChatStatusType = when (type) {
            1 -> Normal
            2 -> Interrupt
            else -> Unknown
        }
    }
}

object ChatStatusTypeSerializer : KSerializer<ChatStatusType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ChatStatusType", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: ChatStatusType) {
        encoder.encodeInt(value.type)
    }

    override fun deserialize(decoder: Decoder): ChatStatusType {
        val value = decoder.decodeInt()
        return ChatStatusType.from(value)
    }
}


@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
@Serializable
data class ActionData(
    val conversationId: String,
    val action: String,
    val params: String = "",
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
) {
    companion object {
        const val ACTION_WEATHER_SHOW_DAYS = "weather_show_days"
        const val ACTION_WEATHER_SHOW_HOURS = "weather_show_hours"

        const val ACTION_DEVICE_CONTROL = "device_control"
    }
}

@Serializable
enum class Role { User, Bot }
