package com.example.android_server_ktor_kotlin

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GetLocationScreen()
        }
    }

}

@Composable
fun GetLocationScreen() {

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var location by remember { mutableStateOf<Location?>(null) }
    var permissionGranted by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(" ") }
    var localizacao by remember { mutableStateOf(" ") }

    val client  by lazy {
        HttpClient(CIO) {
            install(WebSockets)
        }
    }

    suspend fun sendMessage(mensagem: String): String  {
        lateinit var income : String

        client.webSocket(host = "localhost", port = 8080, path = "/chat") {


            send(Frame.Text(mensagem))

            for (message in incoming) {
                message as? Frame.Text ?: continue
                income = "Received: ${message.readText()}"
                return@webSocket
            }
        }
        return income
    }


    LaunchedEffect(Unit) {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            permissionGranted = true
        } else {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(permission),
                1
            )
        }
    }


    if (permissionGranted) {
        LaunchedEffect(Unit) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                location = loc
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        location?.let { loc ->
            val lat = loc.latitude
            val lng = loc.longitude
            localizacao = "Latitude: $lat e longitude: $lng"
            Text("Latitude: $lat, Longitude: $lng")
        } ?: run {
            Text("Localização não encontrada ou permissão negada.")
        }
        Column {

//                Button(onClick = {
//                    CoroutineScope(Dispatchers.IO).launch {
//                        text += sendMessage(localizacao) + "/n"
//                    }
//                }) {
//                    Text("Send")
//                }
//                LazyColumn {
//                    items(text.split("/n")){
//                        Text(it)
//                    }
//                }

            LaunchedEffect(key1 = Unit) {
                while (true) {
                    CoroutineScope(Dispatchers.IO).launch {
                        text += sendMessage(localizacao) + "/n"
                    }

                    delay(1000L)

                }
                    println("O loop está rodando!")
                }
            }

            LazyColumn {
                items(text.split("/n")){
                    Text(it)
                }
            }
    }
}