package com.senseauto.nui_service.agent

import com.senseauto.nui_service.entity.ChatData
import com.senseauto.nui_service.entity.BaseAgentRequest
import kotlinx.coroutines.flow.Flow

fun interface BaseAgent<T> {

    fun requestRemote(request: BaseAgentRequest): Flow<T>

    fun requestAgent(chatData: ChatData): Flow<T> {
        return requestRemote(
            BaseAgentRequest(
                requestId = chatData.conversationId,
                query = chatData.content,
                intent = chatData.intent
            )
        )
    }
}