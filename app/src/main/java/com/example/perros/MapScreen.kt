package com.example.perros

import android.Manifest
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler() {
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedPref = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val userId by remember { mutableStateOf(sharedPref.getString("user_id", "") ?: "") }
    var pets by remember { mutableStateOf<List<PetLocation>>(emptyList()) }
    var selectedPet by remember { mutableStateOf<PetLocation?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Configuración del mapa
    val isDarkTheme = isSystemInDarkTheme()
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val cameraPositionState = rememberCameraPositionState()

    // Función para cargar mascotas
    fun loadPets() {
        scope.launch {
            try {
                val client = HttpClient(Android) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }

                val response = client.get("http://192.168.100.25:5000/api/v1/mascotas?user_id=$userId")
                val petsResponse = response.body<PetsLocationResponse>()
                pets = petsResponse.data.map { pet ->
                    PetLocation(
                        id = pet.id,
                        nombre = pet.nombre,
                        tipo = pet.tipo,
                        lat = pet.lat,
                        lng = pet.lng
                    )
                }

                // Seleccionar la primera mascota por defecto
                if (pets.isNotEmpty() && selectedPet == null) {
                    selectedPet = pets[0]
                }
            } catch (e: Exception) {
                errorMessage = "Error al cargar mascotas: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Cargar mascotas al inicio y cada 20 segundos
    LaunchedEffect(userId) {
        loadPets()
        while (true) {
            delay(20000)
            loadPets()
        }
    }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var hasMovedCamera by remember { mutableStateOf(false) }

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    userLocation = latLng

                    if (!hasMovedCamera) {
                        scope.launch {
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngZoom(latLng, 15f),
                                durationMs = 1000
                            )
                            hasMovedCamera = true
                        }
                    }
                }
            }
        }
    }

    // Mover cámara cuando se selecciona una mascota
    LaunchedEffect(selectedPet) {
        selectedPet?.let { pet ->
            val latLng = LatLng(pet.lat.toDouble(), pet.lng.toDouble())
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(latLng, 15f),
                durationMs = 1000
            )
        }
    }

    // Diseño de la pantalla
    Scaffold(

    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Mapa (70% de la pantalla)
            Box(modifier = Modifier.weight(0.8f)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        mapStyleOptions = MapStyleOptions.loadRawResourceStyle(
                            context,
                            if (isDarkTheme) R.raw.dark_map_style else R.raw.light_map_style
                        ),
                        isMyLocationEnabled = locationPermissionState.status.isGranted
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        myLocationButtonEnabled = true
                    )
                ) {

                    // Marcadores de mascotas con ventana de información (nombre visible)
                    pets.forEach { pet ->
                        val position = LatLng(pet.lat.toDouble(), pet.lng.toDouble())
                        Marker(
                            state = MarkerState(position = position),
                            title = pet.nombre,
                            snippet = "Tipo: ${pet.tipo}",
                            onClick = {
                                selectedPet = pet
                                false // ← muestra automáticamente el InfoWindow
                            }
                        )
                    }
                }

            }

            // Lista de mascotas (30% de la pantalla)
            Box(modifier = Modifier.weight(0.2f)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (errorMessage != null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { loadPets() }) {
                            Text("Reintentar")
                        }
                    }
                } else {
                    LazyColumn {
                        items(pets) { pet ->
                            PetMapItem(
                                pet = pet,
                                isSelected = selectedPet?.id == pet.id,
                                onClick = { selectedPet = pet }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PetMapItem(pet: PetLocation, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono según el tipo de mascota
            val iconRes = when(pet.tipo.lowercase()) {
                "perro" -> R.drawable.ic_dog
                "gato" -> R.drawable.ic_cat
                else -> R.drawable.ic_pet
            }

            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = pet.tipo,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = pet.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Última ubicación: ${pet.lat}, ${pet.lng}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
@Serializable
data class PetLocation(
    val id: String,
    val nombre: String,
    val tipo: String,
    val lat: String,
    val lng: String
)

@Serializable
data class PetsLocationResponse(
    val message: String,
    val data: List<PetLocation>
)
