package com.example.android_server_ktor_kotlin.data

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*

fun Cliente() {
    val client = HttpClient(CIO) {
        install(WebSockets)
    }

    runBlocking {
        client.webSocket(host = "localhost", port = 8080, path = "/chat") {
            send("Hello, WebSocket!")
            for (message in incoming) {
                message as? Frame.Text ?: continue
                println("Received: ${message.readText()}")
            }
        }
    }
    client.close()
}