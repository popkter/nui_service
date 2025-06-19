package com.senseauto.nui_service.network

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.apache5.Apache5EngineConfig
import org.apache.hc.client5.http.ssl.TrustAllStrategy
import org.apache.hc.core5.ssl.SSLContextBuilder

actual fun HttpClientEngineConfig.configurePlatformEngine() {
    (this as Apache5EngineConfig).sslContext = SSLContextBuilder.create()
        .loadTrustMaterial(TrustAllStrategy.INSTANCE)
        .build()
}