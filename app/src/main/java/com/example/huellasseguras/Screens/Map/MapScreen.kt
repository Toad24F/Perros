package com.example.huellasseguras.Screens.Map

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.huellasseguras.R
import com.example.huellasseguras.data.PetsRepository
import com.example.huellasseguras.model.PetLocation
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.toColorInt


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

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedPref = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val userId by remember { mutableStateOf(sharedPref.getString("user_id", "") ?: "") }
    val userPetLocation = remember { mutableStateListOf<PetLocation>() }
    var selectedPet by remember { mutableStateOf<PetLocation?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val petsRepository = remember { PetsRepository() }
    // Configuración del mapa
    val isDarkTheme = isSystemInDarkTheme()
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val cameraPositionState = rememberCameraPositionState()

    // Cargar mascotas al inicio y cada 20 segundos
    LaunchedEffect(userId) {
        if (userId.isBlank()) return@LaunchedEffect

        while (true) {
            isLoading = true // Solo la primera vez o si quieres mostrar el indicador
            val result = petsRepository.loadPetsLocation(userId)

            result.onSuccess { pets ->
                userPetLocation.clear()
                userPetLocation.addAll(pets)

                // Seleccionar la primera por defecto si no hay ninguna seleccionada
                if (selectedPet == null && pets.isNotEmpty()) {
                    selectedPet = pets[0]
                }
            }.onFailure {
                errorMessage = "Error: ${it.message}"
            }

            isLoading = false
            delay(5000) // Espera 5 segundos antes de la siguiente actualización
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
            val latLng = LatLng(pet.lat, pet.lng)
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
                    userPetLocation.forEach { pet ->
                        val position = LatLng(pet.lat.toDouble(), pet.lng.toDouble())
                        val icon = rememberPetMarkerIcon(
                            context = context,
                            photoUrl = pet.foto_url,
                            borderColor = "#FF8CE2".toColorInt(), // ← cambia aquí
                            anchorColor = "#FF8CE2".toColorInt(),
                        )

                        // Solo renderiza cuando el ícono ya está listo
                        icon?.let {
                            Marker(
                                state = MarkerState(position = position),
                                title = pet.nombre,
                                snippet = "Tipo: ${pet.tipo}",
                                icon = it,
                                anchor = Offset(0.5f, 1f),
                                onClick = {
                                    selectedPet = pet
                                    false
                                }
                            )
                        }
                    }
                }

            }

            // Lista de mascotas (30% de la pantalla)
            Box(modifier = Modifier.weight(0.2f)) {
                if (isLoading) {
                    LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(20.dp))

                } else if (errorMessage != null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = {
                            scope.launch {
                                val result = petsRepository.loadPetsLocation(userId)
                                result.onSuccess { pets ->
                                    userPetLocation.clear()
                                    userPetLocation.addAll(pets)
                                    isLoading = false
                                }
                                result.onFailure {
                                    errorMessage = it.message
                                    isLoading = false
                                }

                            }
                        }) {
                            Text("Reintentar")
                        }
                    }
                } else {
                    LazyColumn {
                        items(userPetLocation) { pet ->
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
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
            // Lógica para decidir si mostrar FOTO o ICONO
            if (!pet.foto_url.isNullOrBlank()) {
                AsyncImage(
                    model = "${pet.foto_url}?",
                    contentDescription = pet.nombre,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = pet.nombre,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
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

