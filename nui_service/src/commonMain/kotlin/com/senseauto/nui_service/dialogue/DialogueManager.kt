package com.senseauto.nui_service.dialogue


import com.senseauto.nui_service.agent.ChatAgent
import com.senseauto.nui_service.agent.WeatherAgent
import com.senseauto.nui_service.entity.*
import com.senseauto.nui_service.ext.OutputColor
import com.senseauto.nui_service.ext.output
import com.senseauto.nui_service.nlu.IntentType
import com.senseauto.nui_service.nlu.NLU
import kotlinx.coroutines.flow.*

object DialogueManager {

    private lateinit var bot: DialogueCore

    fun init(nlu: NLU) {
        bot = dialogueBuilder(
            nlu = nlu,
        ) {
            onWakeup { position, userId, conversationId ->
                val text = "你好，${userContext.getContext(userId, "userName")}"
                speak(
                    ChatData(
                        conversationId = conversationId,
                        userId = userId,
                        speakType = SpeakType.Wakeup,
                        role = Role.Bot,
                        content = text,
                        position = position,
                    )
                )

                // TODO: Start Asr Here
            }

            //设备控制
            onIntent(IntentType.DEVICE_CONTROL) { chat, ctx ->
                action(
                    ActionData(
                        conversationId = chat.conversationId,
                        action = ActionData.ACTION_DEVICE_CONTROL,
                        params = chat.content,
                        timestamp = chat.timestamp,
                    )
                )
                null
            }

            //媒体控制
//            onIntent(IntentType.MEDIA_CONTROL) { chat, ctx ->
//                chat
//            }
//
//            //应用控制
//            onIntent(IntentType.APP_CONTROL) { chat, ctx ->
//                chat
//            }
//
//            //导航
//            onIntent(IntentType.NAVIGATION) { chat, ctx ->
//                chat
//            }

            //天气
            onIntent(IntentType.WEATHER) { chat, ctx ->
                WeatherAgent.requestAgent(chat).onEach { response ->
                    when (response.type) {
                        AgentResponseType.Action -> {
                            action(
                                ActionData(
                                    conversationId = chat.conversationId,
                                    action = ActionData.Companion.ACTION_WEATHER_SHOW_DAYS,
                                    params = response.data,
                                    timestamp = chat.timestamp
                                )
                            )
                        }

//                        AgentResponseType.Action -> {
//                            action(
//                                ActionData(
//                                    conversationId = chat.conversationId,
//                                    action = ActionData.Companion.ACTION_WEATHER_SHOW_HOURS,
//                                    params = response.data,
//                                    timestamp = chat.timestamp
//                                )
//                            )
//                        }

                        AgentResponseType.Stream -> {
                            speak(
                                chat.copy(
                                    role = Role.Bot,
                                    content = response.data,
                                    stream = true,
                                    finish = false
                                )
                            )
                        }

                        AgentResponseType.Finish -> {
                            speak(
                                chat.copy(
                                    role = Role.Bot,
                                    content = response.data,
                                    stream = true,
                                    finish = true
                                )
                            )
                        }

                        AgentResponseType.Unknown -> {
                            println("Unknown weather data: $response")
                        }

                        AgentResponseType.Error -> {
                            println("Error weather data: $response")
                        }

                        AgentResponseType.Ext -> {
                        }
                    }
                }
                    .filter { it.type == AgentResponseType.Stream || it.type == AgentResponseType.Finish }
                    .map { it.data }
                    .runningReduce { acc, value -> acc + value }
                    .map { chat.copy(role = Role.Bot, content = it) }
                    .cancellable()

            }

            //任务或提醒
//            onIntent(IntentType.TASK) { chat, ctx ->
//                flow { chat }
//            }

            //闲聊
            onIntent(IntentType.CHAT) { chat, ctx ->
                ChatAgent.requestAgent(chat).onEach { response ->
                    when (response.type) {
                        AgentResponseType.Error -> {

                        }

                        AgentResponseType.Finish -> {
                            speak(
                                chat.copy(
                                    role = Role.Bot,
                                    content = response.data,
                                    stream = true,
                                    finish = true
                                )
                            )
                        }

                        AgentResponseType.Stream -> {
                            speak(
                                chat.copy(
                                    role = Role.Bot,
                                    content = response.data,
                                    stream = true,
                                    finish = false
                                )
                            )
                        }

                        AgentResponseType.Unknown -> {

                        }

                        else -> {}
                    }
                }
                    .filter { it.type == AgentResponseType.Stream || it.type == AgentResponseType.Finish }
                    .map { it.data }
                    .runningReduce { acc, value -> acc + value }
                    .map { chat.copy(role = Role.Bot, content = it) }
                    .cancellable()
            }

            onIntent(IntentType.REJECT) { chat, ctx ->
                println("onIntent IntentType.REJECT")
                null
            }

            //记忆问答
//            onIntent(IntentType.MQA) { chat, ctx ->
//                chat
//            }
//
//            //视觉问答
//            onIntent(IntentType.VQA) { chat, ctx ->
//                chat
//            }
//
//            //默认
//            onIntent(IntentType.DEFAULT) { chat, ctx ->
//                chat
//            }

            onAction {
                println("action: $it")
            }

            onSpeak {
                when (it.position) {
                    PositionType.Driver -> output(it.content, OutputColor.Blue)
                    PositionType.Copilot -> output(it.content, OutputColor.Green)
                    PositionType.RearLeft -> output(it.content, OutputColor.Cyan)
                    PositionType.RearRight -> output(it.content, OutputColor.Pink)
                    else -> output(it.content, OutputColor.Red)
                }
            }


            onFallback {
                println("FALLBACK")
//                speak("抱歉，我不明白你的意思")
            }
        }
    }


    suspend fun handleInput(chat: ChatData) {
        bot.handleInput(chat, individualPositionInput = false)
    }


    suspend fun wakeUp(position: Int, userId: String, conversationId: String) {
        bot.wakeup(position, userId, conversationId)
    }

    suspend fun snapshotRecord(userId: String) {
        val history = bot.userContext.getContext(userId).snapshotRecord()
        println("history: $history")
    }

}