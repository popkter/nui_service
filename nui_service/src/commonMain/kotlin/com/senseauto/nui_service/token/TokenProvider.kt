package com.senseauto.nui_service.token

interface TokenProvider {
    fun weatherToken(): String
}

val TokenFactory = createTokenProvider()

expect fun createTokenProvider(): TokenProvider
