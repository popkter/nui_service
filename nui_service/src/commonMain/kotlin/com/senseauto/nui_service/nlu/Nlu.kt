package com.senseauto.nui_service.nlu

import com.senseauto.nui_service.entity.ChatData
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


/**
 * 落域解析
 */
fun interface NLU {
    suspend fun recognize(chatData: ChatData): IntentResponse

    @Serializable
    data class IntentResponse(
        val intents: List<Intent>,
        @Transient
        val async: Boolean = false
    )

    @Serializable
    data class Intent(
        val domain: String,
        val confidence: Double,
        val input: String
    )
}

/**
 * 简单的NLU实现
 */
val SimpleNLU = NLU { chatData ->
    val intentMap = mapOf(
        "讲个笑话" to IntentType.CHAT,
        "讲个故事" to IntentType.CHAT,
        "上海市明天上午的天气怎么样" to IntentType.WEATHER,
        "今天天气怎么样" to IntentType.WEATHER,
        "无意义的字段" to IntentType.REJECT,
        "我叫*" to "SetName"
    )

    for ((pattern, intent) in intentMap) {
        val regex = Regex(pattern.replace("*", ".*"))
        if (regex.matches(chatData.content))
            return@NLU NLU.IntentResponse(
                intents = listOf(
                    NLU.Intent(
                        domain = intent,
                        confidence = 1.0,
                        input = chatData.content
                    )
                )
            )
    }


    return@NLU NLU.IntentResponse(
        intents = listOf(
            NLU.Intent(
                domain = "Unknown",
                confidence = 1.0,
                input = chatData.content
            )
        )
    )
}


/*class SenseAutoNlu : NLU {

    @OptIn(ExperimentalTime::class)
    override suspend fun recognize(chatData: com.senseauto.greetingkmp.dialogue.ChatData): String {

        val jsonData = """
            {
                "conversation_id": "${Clock.System.now().toEpochMilliseconds()}",
                "query_id": "",
                "user_query": "${chatData.chatContent.content}",
                "user_info": {
                    "faceid": "",
                    "nickname": "",
                    "gps_info": "121.399627,31.168164",
                    "human_info": [
                        {
                            "description": "灰色上衣衣服，无帽子，有眼镜",
                            "position": "办公室"
                        },
                        {
                            "description": "黑色外套衣服，有帽子，有眼镜",
                            "position": "办公室"
                        },
            					  {
                            "description": "绿色上衣衣服，无帽子，无眼镜",
                            "position": "办公室"
                        }
                    ],
                    "face_info": [
                        {"faceid":"7","name":"小王","nickname":""},
            						{"faceid":"ff","name":"","nickname":""}

                    ]
                },
                "context_info": [
                   
                ],
                "memory_info": [

                ]
            }
        """.trimIndent()

        val response = httpClient.post(_root_ide_package_.com.senseauto.greetingkmp.dialogue.intentUrl) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(jsonData)
        }.bodyAsText()

        response.run {
            val json = Json {
                ignoreUnknownKeys = true // 允许 JSON 中出现未在模型中声明的字段
            }
            val intent = json.decodeFromString<DMIntentResponse>(response)
            println("NLU Response: $intent")
        }
        return ""
    }

    // 封装的 POST 请求方法
    suspend inline fun <reified Req : Any, reified Res : Any> postJson(
        url: String,
        requestBody: Req,
        client: HttpClient
    ): Result<Res> {
        return try {
            val response: HttpResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            // 手动反序列化
            val body: Res = response.body()
            println("Response: $body")
            Result.success(body)
        } catch (e: ResponseException) {
            val errorBody = e.response.bodyAsText()
            println("Server returned error: $errorBody")
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}*/

@Serializable
data class DMIntentResponse(
    val code: Int,
    val message: String,
    val type: String,
    val domain: String,
    val conversation_id: String,
    val query_id: String,
    val timestamp: Double
)

