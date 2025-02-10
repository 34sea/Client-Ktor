package com.example.android_server_ktor_kotlin.data

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class coordenadas(
    val latitude: Float,
    val longitude: Float
)

@Serializable
data class dadoscrane(
    val latitude: String,
    val longitude: String,
    val machine_code: String
)

@Composable
fun ScreenWebsocket(innerPading: PaddingValues) {
    var isClicked by remember { mutableStateOf(false) }
    var itemsClicked by remember { mutableStateOf("") }
    var enviado by remember { mutableStateOf(false) }
    val coordenadas = listOf(
        coordenadas(-19.4444F, 34.90545F),
        coordenadas(-29.4444F, 44.90545F)
    )
    val dadoslo = listOf(
        dadoscrane("-19.331272019072015", "34.127201809072011", "2025-02-06T10:49:22.955710Z"),
        dadoscrane("-19.331207809072015", "34.122407809072011", "2025-02-06T10:00:01.653445700Z")
    )

//    val jsonDados = Json.encodeToString(dadoslo[0])
//    Log.d("dadoslo: ", jsonDados)
    val serverUrl = "wss://ws.postman-echo.com/raw"
    val messageState = remember { mutableStateOf("") }
    val receivedMessage = remember { mutableStateOf("Aguardando resposta...") }
    val coroutineScope = rememberCoroutineScope()

    val client = remember {
        HttpClient(CIO) {
            install(WebSockets)
        }
    }

    fun connectAndSendMessage(message: String) {
        enviado = false
        coroutineScope.launch {
            client.webSocket(urlString = serverUrl) {
                send(message)
                receivedMessage.value = "Enviando mensagem..."

                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    receivedMessage.value = "Recebido: ${frame.readText()}"
                    enviado = true
                    break
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextField(
            value = messageState.value,
            onValueChange = { messageState.value = it },
            label = { Text("Digite sua mensagem") }
        )

        Button(onClick = {
            connectAndSendMessage(messageState.value)
        }) {
            Text("Enviar Mensagem")
        }

        Text(text = receivedMessage.value)

        LazyColumn {
            items(dadoslo){items ->
                Column(
                    modifier = Modifier
                        .clickable {
                            isClicked = true
                            itemsClicked = items.latitude
                            Log.d("Items: ", items.toString())
                            connectAndSendMessage(Json.encodeToString(items))
                        }
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .height(40.dp)
                        .background(
                            color = if(isClicked && items.latitude == itemsClicked) Color.Red.copy(alpha = 0.5f) else Color.Red
                        )
                ) {
                    Text(text = items.longitude)
                }
            }
        }
        connectAndSendMessage("Teste")

        LaunchedEffect(Unit) {

            while (true){
                Log.d("Enviado: ", enviado.toString())

                if(enviado){ connectAndSendMessage("Teste")}

                delay(5000)

            }
        }
    }

}
