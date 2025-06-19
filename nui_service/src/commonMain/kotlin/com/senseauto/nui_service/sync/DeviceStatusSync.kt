package com.senseauto.nui_service.sync

import io.github.davidepianca98.MQTTClient
import io.github.davidepianca98.mqtt.MQTTVersion
import io.github.davidepianca98.mqtt.Subscription
import io.github.davidepianca98.mqtt.packets.Qos
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Suback
import io.github.davidepianca98.mqtt.packets.mqttv5.SubscriptionOptions
import io.github.davidepianca98.socket.tls.TLSClientSettings


/**
 * Car Status Sync With Server
 */
class DeviceStatusSync {

    private lateinit var poiRecommendClient: MQTTClient

    companion object Companion {
        private const val POI_BROKER_ADDRESS = "demo.com"
        private const val POI_BROKET_PORT = 21883
        private const val POI_MQTT_USER_NAME = "user"
        private const val POI_MQTT_PASSWORD = "123456"
        const val SN = "122"
        private const val POI_PUBLISH_TOPIC = "device_info_sync"
        private const val POI_SUBSCRIBE_TOPIC = "device_info_sync/${SN}"

    }

    /**
     * Kmqtt Sample
     * Connects to the MQTT broker and sets up a subscription to a topic.
     * When a message is received, it invokes the provided callback function.
     *
     * @param onMessageReceive Callback function to handle received messages.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun connect(onMessageReceive: (String?) -> Unit) {
        poiRecommendClient = MQTTClient(
            mqttVersion = MQTTVersion.MQTT5,
            address = POI_BROKER_ADDRESS,
            port = POI_BROKET_PORT,
            tls = TLSClientSettings(
                checkServerCertificate = true,
                serverCertificate = """
                    -----BEGIN CERTIFICATE-----
                    MIIDyzCCArOgAwIBAgIULp8OsVrd+t9OrXlMS6JcxMRRxpUwDQYJKoZIhvcNAQEL
                    BQAwdTELMAkGA1UEBhMCQ04xETAPBgNVBAgMCFNoYW5naGFpMREwDwYDVQQHDAhT
                    -----END CERTIFICATE-----
                """.trimIndent(),
                clientCertificateKey = """
                    -----BEGIN PRIVATE KEY-----
                    MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCKMuJMzdC/CSt0
                    -----END PRIVATE KEY-----
                """.trimIndent(),
                clientCertificate = """
                    -----BEGIN CERTIFICATE-----
                    MIIDjjCCAnagAwIBAgIUH58X9pUHlvRoB4NmUKQTbFAfraEwDQYJKoZIhvcNAQEL
                    BQAwdTELMAkGA1UEBhMCQ04xETAPBgNVBAgMCFNoYW5naGFpMREwDwYDVQQHDAhT
                    -----END CERTIFICATE-----
                """.trimIndent(),

                ),
            userName = POI_MQTT_USER_NAME,
            password = POI_MQTT_PASSWORD.encodeToByteArray().toUByteArray(),
            onSubscribed = {
                println("Subscribed to topic: ${(it as MQTT5Suback).reasonCodes.toList()}")
            }
        ) {
            println("publishReceived" + it.payload?.toByteArray()?.decodeToString())
            onMessageReceive(it.payload?.toByteArray()?.decodeToString())
        }

        poiRecommendClient.subscribe(
            listOf(Subscription(POI_PUBLISH_TOPIC, SubscriptionOptions(Qos.EXACTLY_ONCE)))
        )

        poiRecommendClient.runSuspend()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun publish(str: String) {
        poiRecommendClient.publish(
            true, Qos.EXACTLY_ONCE, POI_PUBLISH_TOPIC, str.trimIndent().encodeToByteArray().toUByteArray()
        )
    }

}