package com.senseauto.nui_service.network

import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

fun createUnsafeOkHttpClient(): OkHttpClient {

    val cert = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }
    // 创建一个空的 TrustManager，忽略所有证书校验
    val trustAllCerts = arrayOf(cert)

    val sslContext = SSLContext.getInstance("SSL").apply {
        init(null, trustAllCerts, SecureRandom())
    }

    val sslSocketFactory = sslContext.socketFactory

    return OkHttpClient.Builder()
        .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .build()
}


actual fun HttpClientEngineConfig.configurePlatformEngine() {
    (this as OkHttpConfig).config {
        // 注入自定义的 OkHttpClient（忽略证书）
        val unsafeClient = createUnsafeOkHttpClient()
        followRedirects(true)
        followSslRedirects(true)
        sslSocketFactory(unsafeClient.sslSocketFactory, unsafeClient.x509TrustManager!!)
    }
}