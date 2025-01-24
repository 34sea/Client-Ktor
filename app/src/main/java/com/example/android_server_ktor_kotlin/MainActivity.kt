package com.example.android_server_ktor_kotlin

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.runtime.DisposableEffect
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GetLocationAndCompassScreen()
        }
    }
}

@Composable
fun GetLocationAndCompassScreen() {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var location by remember { mutableStateOf<Location?>(null) }
    var permissionGranted by remember { mutableStateOf(false) }
    var compassDirection by remember { mutableStateOf("Aguardando...") }
    var logMessages by remember { mutableStateOf(listOf<String>()) }

    val client by lazy {
        HttpClient(CIO) {
            install(WebSockets)
        }
    }

    val sensorManager = LocalContext.current.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Função para enviar mensagens via WebSocket
    suspend fun sendMessage(message: String) {
        client.webSocket(host = "192.168.221.222", port = 8080, path = "/chat") {
            send(Frame.Text(message))
            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                logMessages = logMessages + "Server: ${frame.readText()}"
            }
        }
    }

    // Obter permissão de localização
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

    // Atualizar localização
    if (permissionGranted) {
        LaunchedEffect(Unit) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                location = loc
            }
        }
    }

    // Monitorar direção da bússola
    DisposableEffect(Unit) {
        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ORIENTATION) {
                    val azimuth = event.values[0]
                    compassDirection = "Azimute: ${azimuth.toInt()}°"
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        sensorManager.registerListener(sensorListener, orientationSensor, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }

    // UI
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
            val locationMessage = "Latitude: $lat, Longitude: $lng"
            Text(locationMessage)
            LaunchedEffect(Unit) {
                sendMessage("Localização: $locationMessage")
            }
        } ?: Text("Localização não encontrada ou permissão negada.")

        Text(compassDirection)
        LaunchedEffect(compassDirection) {
            sendMessage(compassDirection)
        }

        LazyColumn {
            items(logMessages) { message ->
                Text(message)
            }
        }
    }
}
