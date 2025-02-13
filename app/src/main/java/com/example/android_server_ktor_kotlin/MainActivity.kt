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
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.android_server_ktor_kotlin.data.MyScreen
import com.example.android_server_ktor_kotlin.data.ScreenWebsocket
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "MyScreen"){
                    composable(
                        route = "web"
                    ) {
                        ScreenWebsocket(innerPadding)
                    }

                    composable(
                        route = "MyScreen"
                    ) {
                        MyScreen()
                    }
                }
            }
//            GetLocationAndCompassScreen()
        }
    }
}

@Composable
fun GetLocationAndCompassScreen() {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var location by remember { mutableStateOf<Location?>(null) }
    var compassDirection by remember { mutableStateOf("Aguardando...") }
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val client = remember {
        HttpClient(CIO) {
            install(WebSockets)
        }
    }
    Text("Martinho")

    // Verificação de permissões
    val locationPermissionGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    if (!locationPermissionGranted.value) {
        LaunchedEffect(Unit) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    // Listener para o sensor de orientação
    DisposableEffect(Unit) {
        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ORIENTATION) {
                    val azimuth = event.values[0]
                    compassDirection = azimuth.toInt().toString()
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

    // Solicitar atualizações constantes de localização
    LaunchedEffect(locationPermissionGranted.value) {
        if (locationPermissionGranted.value) {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                100 //
            ).build()

            // Obter última localização conhecida antes de iniciar atualizações
            fusedLocationClient.lastLocation.addOnSuccessListener { lastKnownLocation ->
                if (lastKnownLocation != null) {
                    location = lastKnownLocation
                }
            }

            // Iniciar atualizações de localização
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        val newLocation = locationResult.lastLocation
                        if (newLocation != null) {
                            location = newLocation // Atualizar a localização com a nova
                        }
                    }

                    override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                        if (!locationAvailability.isLocationAvailable) {
                            Log.e("Location", "Localização não disponível")
                        }
                    }
                },
                context.mainLooper
            )
        }
    }

    // Comunicação constante com o servidor
    LaunchedEffect(client) {
        client.webSocket(host = "192.168.221.249", port = 8080, path = "/chat") {
            while (true) {
                val lat = location?.latitude?.toString() ?: "Aguardando localização..."
                val lng = location?.longitude?.toString() ?: "Aguardando localização..."
                val azimuth = compassDirection

                try {
                    send(Frame.Text("latitude:$lat"))
                    send(Frame.Text("longitude:$lng"))
                    send(Frame.Text("azimuth:$azimuth"))
                } catch (e: Exception) {
                    Log.e("WebSocket", "Erro ao enviar dados: ${e.message}")
                }

                delay(500) // Enviar dados a cada 500ms
            }
        }
    }

    // Exibir informações na tela
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Latitude: ${location?.latitude ?: "Aguardando..."}")
        Text("Longitude: ${location?.longitude ?: "Aguardando..."}")
        Text("Azimuth: $compassDirection")
    }
}

private const val LOCATION_PERMISSION_REQUEST_CODE = 1






