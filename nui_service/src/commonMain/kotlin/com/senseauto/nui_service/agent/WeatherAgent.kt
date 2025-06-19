package com.senseauto.nui_service.agent


import com.senseauto.nui_service.entity.AgentResponseType
import com.senseauto.nui_service.entity.BaseAgentResponse
import com.senseauto.nui_service.network.agentClient
import com.senseauto.nui_service.token.TokenFactory
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

val WeatherAgent = BaseAgent { request ->
    flow {
        agentClient.sse(
            urlString = weatherUrl, request = {
                contentType(ContentType.Application.Json)
                accept(ContentType.Text.EventStream)
                header(HttpHeaders.Authorization, TokenFactory.weatherToken())
                method = HttpMethod.Post
                setBody(WeatherRequest(request.query))
            }) {
            val ctx = currentCoroutineContext()  // 当前上下文

            incoming.cancellable().collect { serverSentEvent ->
                ctx.ensureActive()
                // TODO:
                emit(BaseAgentResponse(requestId = request.requestId, type = AgentResponseType.Finish, data = ""))
            }
        }
    }.flowOn(Dispatchers.IO)
        .catch {
            println("error: ${it.message}")
            emit(
                BaseAgentResponse(
                    requestId = request.requestId,
                    type = AgentResponseType.Error,
                    data = it.message ?: ""
                )
            )
        }.cancellable()

}

@Serializable
data class WeatherRequest(
    val user_query: String
)

@Serializable
data class WeatherResponse(
    val requestId: String,
    val type: WeatherResponseType,
    val data: String
)


@Serializable(with = WeatherResponseTypeSerializer::class)
sealed class WeatherResponseType(val type: String) {
    data object Summary : WeatherResponseType("summary")
    data object Finish : WeatherResponseType("finish")
    data object Error : WeatherResponseType("error")
    data object DaysDetail : WeatherResponseType("days_detail")
    data object HoursDetail : WeatherResponseType("hours_detail")

    data object Unknown : WeatherResponseType("unknown")
    companion object {
        fun from(type: String): WeatherResponseType = when (type) {
            "summary" -> Summary
            "finish" -> Finish
            "days_detail" -> DaysDetail
            "hours_detail" -> HoursDetail
            "error" -> Error
            else -> Unknown
        }
    }
}

object WeatherResponseTypeSerializer : KSerializer<WeatherResponseType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("WeatherResponseType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: WeatherResponseType) {
        encoder.encodeString(value.type)
    }

    override fun deserialize(decoder: Decoder): WeatherResponseType {
        val value = decoder.decodeString()
        return WeatherResponseType.from(value)
    }
}
