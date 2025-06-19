package com.senseauto.nui_service.network

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.sse.*
import io.ktor.serialization.kotlinx.json.json

val agentClient = HttpClient() {
    install(SSE)

    install(ContentNegotiation) {
        json()
    }

//    install(Logging) {
//        logger = Logger.SIMPLE
//        level = LogLevel.ALL
//    }

    engine {
        //这里需要根据平台选择不同的引擎配置
        configurePlatformEngine()
    }
}

val nluClient = HttpClient {
    install(SSE)

    install(ContentNegotiation) {
        json()
    }

//    install(Logging) {
//        logger = Logger.SIMPLE
//        level = LogLevel.ALL
//    }

    engine {
        //这里需要根据平台选择不同的引擎配置
        configurePlatformEngine()
    }
}

expect fun HttpClientEngineConfig.configurePlatformEngine()
