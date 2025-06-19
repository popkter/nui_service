package com.senseauto.nui_service.dialogue


import com.senseauto.nui_service.entity.ActionData
import com.senseauto.nui_service.entity.ChatData
import com.senseauto.nui_service.entity.PositionType
import com.senseauto.nui_service.entity.Role
import com.senseauto.nui_service.nlu.IntentType
import com.senseauto.nui_service.nlu.NLU
import com.senseauto.nui_service.nlu.SimpleNLU
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlin.coroutines.cancellation.CancellationException

/**
 * 多用户对话上下文管理类
 * 用于管理和存储多个用户的对话历史记录和上下文信息
 */
class MultiUserDialogContext {

    /**
     * 聊天内容数据类
     * @param role 说话者角色
     * @param content 对话内容
     * @param intent 意图
     * @param timestamp 时间戳
     */
    @Serializable
    data class ChatContent(
        val role: Role,
        val content: String,
        val intent: String = "",
        val timestamp: Long = 10L
    )

    /**
     * 单个用户的对话上下文类
     * 管理用户特定的上下文信息和对话历史
     */
    class DialogContext {
        // 存储键值对形式的上下文信息
        private val context = mutableMapOf<String, String>()

        fun getContext(key: String): String {
            return context.getOrElse(key) { "" }
        }

        fun putContext(key: String, value: String) {
            context[key] = value
        }

        fun removeContext(key: String) {
            context.remove(key)
        }

        // 存储对话历史记录
        private val history: MutableSet<ChatContent> = mutableSetOf()

        fun addRecord(chat: ChatData) {
            history.removeAll { it.role == chat.role && it.timestamp == chat.timestamp && it.intent == chat.intent }
            history.add(ChatContent(chat.role, chat.content, chat.intent, timestamp = chat.timestamp))
        }

        fun snapshotRecord() = history.toList()
    }

    // 存储所有用户的上下文信息
    private val _userContext = mutableMapOf<String, DialogContext>()

    // 用于并发控制的互斥锁
    private val mutex = Mutex()

    suspend fun putContext(userId: String, key: String, value: String) {
        mutex.withLock { _userContext.getOrPut(userId) { DialogContext() }.putContext(key, value) }
    }

    suspend fun putChatRecord(chat: ChatData) {
        mutex.withLock { _userContext.getOrPut(chat.userId) { DialogContext() }.addRecord(chat) }
    }

    /**
     * delete single context
     */
    suspend fun delContext(userId: String, key: String) {
        mutex.withLock { _userContext.getOrPut(userId) { DialogContext() }.removeContext(key) }
    }

    suspend fun getContext(userId: String, key: String): String {
        return mutex.withLock { _userContext.getOrPut(userId) { DialogContext() }.getContext(key) }
    }

    /**
     * delete all context of user
     */
    suspend fun delContext(userId: String) {
        mutex.withLock { _userContext.remove(userId) }
    }

    suspend fun getContext(userId: String): DialogContext {
        return mutex.withLock { _userContext.getOrPut(userId) { DialogContext() } }
    }

}


/**
 * 对话核心管理类
 * 负责处理对话流程、意图识别和响应
 *
 * @param nlu 自然语言理解模块
 */
class DialogueCore(
    val nlu: NLU = SimpleNLU
) {
    // 唤醒处理器
    private var wakeupHandler: (suspend (PositionType, String, String) -> Unit)? = null

    // 意图处理器映射表
    private val intentHandlers =
        mutableMapOf<String, suspend (ChatData, MultiUserDialogContext.DialogContext) -> Flow<ChatData>?>()

    private val speakHandlers = mutableSetOf<suspend (ChatData) -> Unit>()

    private val actionHandlers = mutableSetOf<suspend (ActionData) -> Unit>()

    // 默认回退处理器
    private var fallbackHandler: (() -> Unit)? = null

    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 用户上下文管理实例
    val userContext = MultiUserDialogContext()

    /**
     * 注册唤醒处理器
     * @param block 唤醒处理函数
     */
    fun onWakeup(block: suspend (position: PositionType, userId: String, conversationId: String) -> Unit) {
        wakeupHandler = block
    }

    /**
     * 注册意图处理器
     * @param intent 意图名称
     * @param block 意图处理函数
     */
    fun onIntent(
        intent: String,
        block: suspend (input: ChatData, ctx: MultiUserDialogContext.DialogContext) -> Flow<ChatData>?
    ) {
//        println("\n Register onIntent: $intent")
        intentHandlers[intent] = block
    }

    fun onSpeak(block: suspend (ChatData) -> Unit) {
        speakHandlers.add(block)
    }

    fun onAction(block: (ActionData) -> Unit) {
        actionHandlers.add(block)
    }

    fun onFallback(block: () -> Unit) {
        fallbackHandler = block
    }

    suspend fun wakeup(position: Int, userId: String, conversationId: String) {
        wakeupHandler?.invoke(PositionType.from(position), userId, conversationId)
    }

    suspend fun speak(chatData: ChatData) {
        if (chatData.fromBot()) {
            speakHandlers.map {
                supervisorScope {
                    async {
                        it.invoke(chatData)
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun action(action: ActionData) {
        actionHandlers.map {
            supervisorScope {
                async {
                    it.invoke(action)
                }
            }
        }.awaitAll()
    }


    private val individualInputFlowMap =
        mutableMapOf<PositionType, MutableSharedFlow<Pair<NLU.IntentResponse, ChatData>>>()
    private val commonInputFlow = MutableSharedFlow<Pair<NLU.IntentResponse, ChatData>>(1)
    private var commonInputJob: Job? = null

    /**
     * 处理用户输入
     * @param chatData 聊天数据
     * @param individualPositionInput 是否使用独立位置输入
     */
    suspend fun handleInput(chatData: ChatData, individualPositionInput: Boolean = true) {
        println("handleInput: $chatData individual: $individualPositionInput")
        if (individualPositionInput) {
            val flow = individualInputFlowMap.getOrPut(chatData.position) {
                MutableSharedFlow<Pair<NLU.IntentResponse, ChatData>>(1).also {
                    println("startCollectingPosition: ${chatData.position}")
                    startCollectingPosition(it)
                }
            }

            val intentResponse = nlu.recognize(chatData)

            println("handleInput intent = $intentResponse")

            flow.emit(intentResponse to chatData)
        } else {
            val intentResponse = nlu.recognize(chatData)

            if (intentResponse.intents.any { it.domain != IntentType.REJECT } && intentResponse.intents.isNotEmpty()) {
                commonInputJob?.cancelAndJoin()
                commonInputJob = startCollectingPosition(commonInputFlow)
                commonInputFlow.emit(intentResponse to chatData)
            } else {
                //直接处理，触发收集
                handleIntent(chatData, intentResponse)
            }
        }
    }


    private suspend fun startCollectingPosition(flow: SharedFlow<Pair<NLU.IntentResponse, ChatData>>): Job {
        return scope.launch {
            flow
                //全部是拒识
                .filter { (intentResponse, chat) ->
                    (intentResponse.intents.any { it.domain != IntentType.REJECT } && intentResponse.intents.isNotEmpty()).also { result ->
                        if (!result) {
                            //直接处理，触发收集
                            handleIntent(chat, intentResponse)
                        }
                    }
                }
                .catch {
                    println("startCollectingPosition Exception: ${it.message}")
                }
                .collectLatest { (intentResponse, chat) ->
                    try {
                        // 这里处理新的请求
                        handleIntent(chat, intentResponse)
                    } catch (e: CancellationException) {
                        println("🛑 [${chat.position}] Previous input cancelled.")
                    }
                }
        }
    }

    /**
     * 处理识别到的意图
     * @param chatData 聊天数据
     * @param intentResponse 意图响应
     */
    suspend fun handleIntent(chatData: ChatData, intentResponse: NLU.IntentResponse) {
        val inputs = intentResponse.intents.map { intent ->
            chatData.copy(content = intent.input, intent = intent.domain)
        }

        if (intentResponse.intents.isNotEmpty() && intentResponse.intents.any { it.domain != IntentType.REJECT }) {
            userContext.putChatRecord(chatData)
        }

        withContext(Dispatchers.IO) {
            //concurrent request agent
            if (intentResponse.async) {
                inputs.map { newChatData ->
                    async {
                        intentHandlers.getOrElse(newChatData.intent) {
                            fallbackHandler?.invoke()
                            null
                        }?.let { handler ->
                            handler(chatData, userContext.getContext(newChatData.userId))?.collectLatest {
                                userContext.putChatRecord(it)
                            }
                        }
                    }
                }.awaitAll()
            }
            //sync request agent
            else {
                inputs.forEach { newChatData ->
                    intentHandlers.getOrElse(newChatData.intent) {
                        fallbackHandler?.invoke()
                        null
                    }?.let { handler ->

                        handler(chatData, userContext.getContext(newChatData.userId))?.collectLatest {
                            userContext.putChatRecord(it)
                        }
                    }
                }
            }
        }
    }
}


/**
 * 对话系统构建器
 * 用于创建和配置对话核心实例
 *
 * @param nlu 自然语言理解模块
 * @param block 配置块
 * @return 配置好的对话核心实例
 */
fun dialogueBuilder(
    nlu: NLU = SimpleNLU,
    block: DialogueCore.() -> Unit
): DialogueCore {
    val bot = DialogueCore(nlu)
    bot.block()
    return bot
}