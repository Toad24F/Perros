package com.example.huellasseguras.Screens.Map

import android.Manifest
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.example.huellasseguras.Notifications.NotificationHelper
import com.example.huellasseguras.R
import com.example.huellasseguras.data.GeofenceRepository
import com.example.huellasseguras.data.PetsRepository
import com.example.huellasseguras.model.Geofence
import com.example.huellasseguras.model.NewGeofence
import com.example.huellasseguras.model.PetLocation
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.maps.android.compose.*
import com.google.maps.android.heatmaps.HeatmapTileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

// ── Utilidad: distancia en metros entre dos coords ────────────────────────────
fun distanciaMetros(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MapScreen() {
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()
    val sharedPref = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val userId     = remember { sharedPref.getString("user_id", "") ?: "" }

    val petsRepository     = remember { PetsRepository() }
    val geofenceRepository = remember { GeofenceRepository() }

    // ── Estado principal ──────────────────────────────────────────────────────
    val userPetLocation  = remember { mutableStateListOf<PetLocation>() }
    var selectedPet      by remember { mutableStateOf<PetLocation?>(null) }
    var isLoading        by remember { mutableStateOf(true) }
    var errorMessage     by remember { mutableStateOf<String?>(null) }

    // ── Modos de vista ────────────────────────────────────────────────────────
    var modoHeatmap      by remember { mutableStateOf(false) }
    val heatmapPoints    = remember { mutableStateListOf<LatLng>() }

    // ── Geofence ──────────────────────────────────────────────────────────────
    var geofenceActivo        by remember { mutableStateOf<Geofence?>(null) }
    var showGeofenceSheet     by remember { mutableStateOf(false) }
    // Centro temporal mientras el usuario configura
    var geofenceCenterTemp    by remember { mutableStateOf<LatLng?>(null) }
    var geofenceRadioTemp     by remember { mutableStateOf(100f) }   // metros
    // Track de alertas ya enviadas para no repetir
    val alertasEnviadas       = remember { mutableSetOf<String>() }

    // ── Mapa ──────────────────────────────────────────────────────────────────
    val isDarkTheme            = isSystemInDarkTheme()
    val locationPermission     = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val cameraPositionState    = rememberCameraPositionState()
    var hasMovedCamera         by remember { mutableStateOf(false) }
    var userLocation           by remember { mutableStateOf<LatLng?>(null) }

    // ── Inicializar canal de notificaciones ───────────────────────────────────
    LaunchedEffect(Unit) {
        NotificationHelper.createChannel(context)
    }

    // ── Loop de actualización de mascotas (cada 5s) ───────────────────────────
    LaunchedEffect(userId) {
        if (userId.isBlank()) return@LaunchedEffect
        while (true) {
            val result = petsRepository.loadPetsLocation(userId)
            result.onSuccess { pets ->
                userPetLocation.clear()
                userPetLocation.addAll(pets)
                if (selectedPet == null && pets.isNotEmpty()) selectedPet = pets[0]

                // ── Chequeo de geofence ───────────────────────────────────────
                geofenceActivo?.let { gf ->
                    pets.forEach { pet ->
                        if (pet.id == gf.mascota_id) {
                            val distancia = distanciaMetros(pet.lat, pet.lng, gf.lat, gf.lng)
                            val fueraDeZona = distancia > gf.radio_metros
                            val yaNotificado = alertasEnviadas.contains(pet.id)

                            if (fueraDeZona && !yaNotificado) {
                                NotificationHelper.sendGeofenceAlert(context, pet.nombre)
                                alertasEnviadas.add(pet.id)
                            } else if (!fueraDeZona && yaNotificado) {
                                // Volvió a la zona, resetear para notificar de nuevo si sale
                                alertasEnviadas.remove(pet.id)
                                NotificationHelper.cancelGeofenceAlert(context, pet.nombre)
                            }
                        }
                    }
                }
            }.onFailure { errorMessage = it.message }

            isLoading = false
            delay(5000)
        }
    }

    // ── Cargar geofence cuando cambia la mascota seleccionada ─────────────────
    LaunchedEffect(selectedPet) {
        selectedPet?.let { pet ->
            // Mover cámara
            val latLng = LatLng(pet.lat, pet.lng)
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f), 1000)

            // Cargar geofence
            val gfResult = geofenceRepository.getGeofence(pet.id)
            gfResult.onSuccess { gf ->
                geofenceActivo    = gf
                geofenceCenterTemp = gf?.let { LatLng(it.lat, it.lng) } ?: latLng
                geofenceRadioTemp  = gf?.radio_metros?.toFloat() ?: 100f
            }

            // Cargar historial para heatmap
            if (modoHeatmap) {
                val histResult = geofenceRepository.getUbicacionesHistorial(pet.id)
                histResult.onSuccess { puntos ->
                    heatmapPoints.clear()
                    heatmapPoints.addAll(puntos.map { LatLng(it.lat, it.lng) })
                }
            }
        }
    }

    // ── Cargar heatmap al activar el modo ─────────────────────────────────────
    LaunchedEffect(modoHeatmap) {
        if (modoHeatmap) {
            selectedPet?.let { pet ->
                val histResult = geofenceRepository.getUbicacionesHistorial(pet.id)
                histResult.onSuccess { puntos ->
                    heatmapPoints.clear()
                    heatmapPoints.addAll(puntos.map { LatLng(it.lat, it.lng) })
                }
            }
        }
    }

    // ── Ubicación del usuario ─────────────────────────────────────────────────
    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted) {
            LocationServices.getFusedLocationProviderClient(context)
                .lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val latLng = LatLng(it.latitude, it.longitude)
                        userLocation = latLng
                        if (!hasMovedCamera) {
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(latLng, 15f), 1000
                                )
                                hasMovedCamera = true
                            }
                        }
                    }
                }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────────────────────────────
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Mapa (80%) ────────────────────────────────────────────────────
            Box(modifier = Modifier.weight(0.8f)) {
                GoogleMap(
                    modifier            = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties          = MapProperties(
                        mapStyleOptions      = MapStyleOptions.loadRawResourceStyle(
                            context,
                            if (isDarkTheme) R.raw.dark_map_style else R.raw.light_map_style
                        ),
                        isMyLocationEnabled  = locationPermission.status.isGranted
                    ),
                    uiSettings          = MapUiSettings(
                        zoomControlsEnabled    = false,
                        myLocationButtonEnabled = false
                    ),
                    onMapClick          = { latLng ->
                        // Si el sheet está abierto, mover el centro del geofence
                        if (showGeofenceSheet) {
                            geofenceCenterTemp = latLng
                        }
                    }
                ) {
                    // ── Marcadores de mascotas ────────────────────────────────
                    userPetLocation.forEach { pet ->
                        val position = LatLng(pet.lat, pet.lng)
                        val icon = rememberPetMarkerIcon(
                            context      = context,
                            photoUrl     = pet.foto_url,
                            borderColor  = "#FF8CE2".toColorInt(),
                            anchorColor  = "#FF8CE2".toColorInt()
                        )
                        icon?.let {
                            Marker(
                                state   = MarkerState(position = position),
                                title   = pet.nombre,
                                snippet = "Tipo: ${pet.tipo}",
                                icon    = it,
                                anchor  = Offset(0.5f, 1f),
                                onClick = { selectedPet = pet; false }
                            )
                        }
                    }

                    // ── Círculo de geofence guardado ──────────────────────────
                    geofenceActivo?.let { gf ->
                        val center = LatLng(gf.lat, gf.lng)
                        Circle(
                            center      = center,
                            radius      = gf.radio_metros,
                            fillColor   = Color(0x2200C853),
                            strokeColor = Color(0xFF00C853),
                            strokeWidth = 3f
                        )
                        // Punto central
                        Marker(
                            state   = MarkerState(position = center),
                            title   = "Centro de zona segura",
                            snippet = "Radio: ${gf.radio_metros.toInt()} m"
                        )
                    }

                    // ── Círculo temporal mientras configura ───────────────────
                    if (showGeofenceSheet) {
                        geofenceCenterTemp?.let { center ->
                            Circle(
                                center      = center,
                                radius      = geofenceRadioTemp.toDouble(),
                                fillColor   = Color(0x220091EA),
                                strokeColor = Color(0xFF0091EA),
                                strokeWidth = 3f,
                                tag         = "temp"
                            )
                        }
                    }

                    // ── Heatmap ───────────────────────────────────────────────
                    if (modoHeatmap && heatmapPoints.size >= 2) {
                        val provider = remember(heatmapPoints.toList()) {
                            HeatmapTileProvider.Builder()
                                .data(heatmapPoints)
                                .radius(40)
                                .build()
                        }
                        TileOverlay(tileProvider = provider)
                    }
                }

                // ── Controles personalizados (abajo derecha) ──────────────────────────
                Column(
                    modifier              = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp),
                    horizontalAlignment   = Alignment.CenterHorizontally
                ) {
                    // Botón Geovalla
                    selectedPet?.let {
                        SmallFloatingActionButton(
                            onClick        = {
                                geofenceCenterTemp = LatLng(it.lat, it.lng)
                                showGeofenceSheet  = true
                            },
                            containerColor = if (geofenceActivo != null)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape          = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_fence),
                                contentDescription = "Zona segura",
                                modifier           = Modifier.size(18.dp),
                                tint               = if (geofenceActivo != null)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Botón Heatmap
                    SmallFloatingActionButton(
                        onClick        = { modoHeatmap = !modoHeatmap },
                        containerColor = if (modoHeatmap)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        shape          = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_heatmap),
                            contentDescription = "Mapa de calor",
                            modifier           = Modifier.size(18.dp)
                        )
                    }

                    // Separador visual
                    HorizontalDivider(
                        modifier  = Modifier.width(40.dp),
                        thickness = 1.dp,
                        color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )

                    // Botón de centrar ubicación del usuario
                    FloatingActionButton(
                        onClick        = {
                            scope.launch {
                                userLocation?.let { loc ->
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(loc, 15f), 800
                                    )
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor   = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape          = RoundedCornerShape(16.dp),
                        modifier       = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_location),
                            contentDescription = "Mi ubicación",
                            modifier           = Modifier.size(22.dp)
                        )
                    }
                }

                // ── Indicador heatmap ─────────────────────────────────────────
                if (modoHeatmap) {
                    Surface(
                        modifier       = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        color          = MaterialTheme.colorScheme.primaryContainer,
                        shape          = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Mapa de calor activo · ${heatmapPoints.size} puntos",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style    = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // ── Lista de mascotas (20%) ───────────────────────────────────────
            Box(modifier = Modifier.weight(0.2f)) {
                if (isLoading) {
                    LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if (errorMessage != null) {
                    Column(
                        modifier              = Modifier.align(Alignment.Center),
                        horizontalAlignment   = Alignment.CenterHorizontally
                    ) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { scope.launch {
                            petsRepository.loadPetsLocation(userId).onSuccess {
                                userPetLocation.clear()
                                userPetLocation.addAll(it)
                            }
                        }}) { Text("Reintentar") }
                    }
                } else {
                    LazyColumn {
                        items(userPetLocation) { pet ->
                            PetMapItem(
                                pet        = pet,
                                isSelected = selectedPet?.id == pet.id,
                                hasGeofence = geofenceActivo?.mascota_id == pet.id,
                                onClick    = { selectedPet = pet }
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Bottom Sheet de configuración de geofence ─────────────────────────────
    if (showGeofenceSheet) {
        GeofenceBottomSheet(
            petName          = selectedPet?.nombre ?: "",
            centerTemp       = geofenceCenterTemp,
            radioTemp        = geofenceRadioTemp,
            geofenceExistente = geofenceActivo,
            onRadioChange    = { geofenceRadioTemp = it },
            onSave           = {
                scope.launch {
                    val center = geofenceCenterTemp ?: return@launch
                    val newGf  = NewGeofence(
                        mascota_id   = selectedPet!!.id,
                        user_id      = userId,
                        lat          = center.latitude,
                        lng          = center.longitude,
                        radio_metros = geofenceRadioTemp.toDouble()
                    )
                    geofenceRepository.saveGeofence(newGf).onSuccess {
                        geofenceActivo   = it
                        showGeofenceSheet = false
                    }
                }
            },
            onDelete         = {
                scope.launch {
                    selectedPet?.let { pet ->
                        geofenceRepository.deleteGeofence(pet.id).onSuccess {
                            geofenceActivo   = null
                            showGeofenceSheet = false
                        }
                    }
                }
            },
            onDismiss        = { showGeofenceSheet = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom Sheet de Geofence
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofenceBottomSheet(
    petName           : String,
    centerTemp        : LatLng?,
    radioTemp         : Float,
    geofenceExistente : Geofence?,
    onRadioChange     : (Float) -> Unit,
    onSave            : () -> Unit,
    onDelete          : () -> Unit,
    onDismiss         : () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState
    ) {
        Column(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement   = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Zona segura · $petName",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Instrucción
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier          = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painterResource(R.drawable.ic_map),
                        contentDescription = null,
                        modifier           = Modifier.size(18.dp),
                        tint               = Color.Unspecified
                    )
                    Text(
                        "Zona asignada a la mascota, si la sobrepasa se le avisara con una notificacion",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Coordenadas actuales
            centerTemp?.let {
                Text(
                    "Centro: ${"%.5f".format(it.latitude)}, ${"%.5f".format(it.longitude)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Slider de radio
            Column {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Radio de la zona", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${radioTemp.toInt()} m",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value        = radioTemp,
                    onValueChange = onRadioChange,
                    valueRange   = 20f..200f,
                    steps        = 20,     // pasos de ~50m
                    modifier     = Modifier.fillMaxWidth()
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("50 m", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("250 m", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }

            HorizontalDivider()

            // Botones
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Eliminar (solo si ya existe)
                if (geofenceExistente != null) {
                    OutlinedButton(
                        onClick  = onDelete,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text("Eliminar zona")
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Button(
                    onClick  = onSave,
                    modifier = Modifier.weight(1f),
                    enabled  = centerTemp != null
                ) {
                    Text(if (geofenceExistente != null) "Actualizar" else "Guardar zona")
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Item de mascota en la lista (actualizado con indicador de geofence)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PetMapItem(
    pet         : PetLocation,
    isSelected  : Boolean,
    hasGeofence : Boolean = false,
    onClick     : () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        shape  = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconRes = when (pet.tipo.lowercase()) {
                "perro" -> R.drawable.ic_dog
                "gato"  -> R.drawable.ic_cat
                else    -> R.drawable.ic_pet
            }
            if (!pet.foto_url.isNullOrBlank()) {
                AsyncImage(
                    model          = "${pet.foto_url}?",
                    contentDescription = pet.nombre,
                    modifier       = Modifier.size(56.dp).clip(CircleShape),
                    contentScale   = ContentScale.Crop
                )
            } else {
                Icon(
                    painterResource(iconRes),
                    contentDescription = pet.nombre,
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                    tint     = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(pet.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Última ubicación: ${"%.4f".format(pet.lat)}, ${"%.4f".format(pet.lng)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            // Indicador de geofence activo
            if (hasGeofence) {
                Surface(
                    color  = Color(0x2200C853),
                    shape  = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "🛡 Zona activa",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = Color(0xFF00C853),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}