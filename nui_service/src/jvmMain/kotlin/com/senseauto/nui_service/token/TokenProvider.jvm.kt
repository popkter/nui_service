package com.senseauto.nui_service.token

actual fun createTokenProvider() = object : TokenProvider {
    override fun weatherToken(): String {
        return "dummy-weather-token"
    }
}