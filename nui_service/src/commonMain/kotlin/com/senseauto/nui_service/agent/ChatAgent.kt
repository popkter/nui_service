package com.senseauto.nui_service.agent

import com.senseauto.nui_service.entity.AgentResponseType
import com.senseauto.nui_service.entity.BaseAgentResponse
import com.senseauto.nui_service.network.agentClient
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val ChatAgent = BaseAgent { request ->
    flow {

        val jsonData = """ """.trimIndent()
        agentClient.sse(
            urlString = chatUrl,
            request = {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                method = HttpMethod.Post
                setBody(jsonData)
            }
        ) {
            val ctx = currentCoroutineContext()  // 当前上下文

            incoming.cancellable().collect { serverSentEvent ->
                ctx.ensureActive()
                // TODO:
            }
            emit(BaseAgentResponse(requestId = request.requestId, type = AgentResponseType.Finish, data = ""))
        }
    }.flowOn(Dispatchers.IO)
        .catch {
            println("loadChat Error: ${it.message}")
            emit(
                BaseAgentResponse(
                    requestId = request.requestId,
                    type = AgentResponseType.Error,
                    data = it.message ?: ""
                )
            )
        }
        .cancellable()
}