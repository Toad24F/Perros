package com.example.huellasseguras.Screens.Map

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage
import com.example.huellasseguras.Notifications.NotificationHelper
import com.example.huellasseguras.R
import com.example.huellasseguras.data.GanadoRepository
import com.example.huellasseguras.data.GeofenceRepository
import com.example.huellasseguras.model.Geofence
import com.example.huellasseguras.model.NewGeofence
import com.example.huellasseguras.model.UbicacionHistorial
import com.example.huellasseguras.model.ganadoLocation
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.heatmaps.HeatmapTileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class IntervaloHeatmap(val label: String, val horas: Int?) {
    TODO("Todo el historial", null),
    HORA_1("Última hora", 1),
    HORAS_6("Últimas 6 horas", 6),
    HORAS_24("Últimas 24 horas", 24),
    DIAS_7("Últimos 7 días", 168)
}

enum class PanelSize(val height: Dp) {
    COLLAPSED(80.dp),
    MEDIUM(220.dp),
    EXPANDED(380.dp)
}

fun distanciaMetros(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

fun filtrarPorIntervalo(historial: List<UbicacionHistorial>, intervalo: IntervaloHeatmap): List<LatLng> {
    if (historial.isEmpty()) return emptyList()
    val lista = if (intervalo.horas == null) historial else {
        val limiteMs = System.currentTimeMillis() - (intervalo.horas * 3600 * 1000L)
        historial.filter { punto ->
            try {
                val ts = punto.timestamp ?: return@filter true
                val norm = ts.replace(" ", "T").let { if (!it.endsWith("Z") && !it.contains("+")) "${it}Z" else it }
                java.time.Instant.parse(norm).toEpochMilli() >= limiteMs
            } catch (e: Exception) { true }
        }
    }
    return lista.map { LatLng(it.lat, it.lng) }
}

fun crearFlechaBitmap(anguloDeg: Float, color: Int, tamano: Int = 48): Bitmap {
    val bmp = Bitmap.createBitmap(tamano, tamano, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
    val cx = tamano / 2f; val cy = tamano / 2f
    val path = android.graphics.Path().apply {
        moveTo(cx, 0f)
        lineTo(cx - tamano * 0.3f, tamano.toFloat())
        lineTo(cx, tamano * 0.65f)
        lineTo(cx + tamano * 0.3f, tamano.toFloat())
        close()
    }
    val matrix = Matrix()
    matrix.postRotate(anguloDeg, cx, cy)
    path.transform(matrix)
    canvas.drawPath(path, paint)
    return bmp
}

fun anguloDesdePuntos(desde: LatLng, hacia: LatLng): Float {
    // Para voltear 180 grados, restamos el origen del destino (o viceversa si ya estaba así)
    val dLng = desde.longitude - hacia.longitude
    val dLat = desde.latitude - hacia.latitude
    return Math.toDegrees(atan2(dLng, dLat)).toFloat()
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedPref = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val userId = remember { sharedPref.getString("user_id", "") ?: "" }

    val ganadoRepository = remember { GanadoRepository() }
    val geofenceRepository = remember { GeofenceRepository() }

    val userGanadoLocation = remember { mutableStateListOf< ganadoLocation>() }
    var selectedGanado by remember { mutableStateOf< ganadoLocation?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var panelSize by remember { mutableStateOf(PanelSize.COLLAPSED) }
    val panelHeight by animateDpAsState(targetValue = panelSize.height, label = "panel")

    var modoHeatmap by remember { mutableStateOf(false) }
    var mostrarLineas by remember { mutableStateOf(true) }
    var intervalo by remember { mutableStateOf(IntervaloHeatmap.HORAS_24) }
    var showHeatmapSheet by remember { mutableStateOf(false) }
    var isLoadingHeatmap by remember { mutableStateOf(false) }
    val historialCompleto = remember { mutableStateListOf<UbicacionHistorial>() }
    val heatmapPoints = remember(historialCompleto.toList(), intervalo) {
        filtrarPorIntervalo(historialCompleto, intervalo)
    }

    var geofenceActivo by remember { mutableStateOf<Geofence?>(null) }
    var showGeofenceSheet by remember { mutableStateOf(false) }
    var geofenceCenterTemp by remember { mutableStateOf<LatLng?>(null) }
    var geofenceRadioTemp by remember { mutableStateOf(100f) }
    val alertasEnviadas = remember { mutableSetOf<String>() }

    val isDarkTheme = isSystemInDarkTheme()
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val cameraPositionState = rememberCameraPositionState()
    var hasMovedCamera by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    LaunchedEffect(Unit) { NotificationHelper.createChannel(context) }

    LaunchedEffect(userId) {
        if (userId.isBlank()) return@LaunchedEffect
        while (true) {
            ganadoRepository.loadGanadoLocation(userId).onSuccess { ganados ->
                userGanadoLocation.clear()
                userGanadoLocation.addAll(ganados)
                if (selectedGanado == null && ganados.isNotEmpty()) {
                    selectedGanado = ganados[0]
                    panelSize = PanelSize.MEDIUM
                }
                geofenceActivo?.let { gf ->
                    ganados.forEach { ganado ->
                        if (ganado.id == gf.ganado_id) {
                            val dist = distanciaMetros(ganado.lat, ganado.lng, gf.lat, gf.lng)
                            val fuera = dist > gf.radio_metros
                            val notificado = alertasEnviadas.contains(ganado.id)
                            if (fuera && !notificado) { NotificationHelper.sendGeofenceAlert(context, ganado.nombre); alertasEnviadas.add(ganado.id) }
                            else if (!fuera && notificado) { alertasEnviadas.remove(ganado.id); NotificationHelper.cancelGeofenceAlert(context, ganado.nombre) }
                        }
                    }
                }
            }.onFailure { errorMessage = it.message }
            isLoading = false
            delay(5000)
        }
    }

    LaunchedEffect(selectedGanado) {
        selectedGanado?.let { ganado ->
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(ganado.lat, ganado.lng), 15f), 1000)
            geofenceRepository.getGeofence(ganado.id).onSuccess { gf ->
                geofenceActivo = gf
                geofenceCenterTemp = gf?.let { LatLng(it.lat, it.lng) } ?: LatLng(ganado.lat, ganado.lng)
                geofenceRadioTemp = gf?.radio_metros?.toFloat() ?: 100f
            }
            if (modoHeatmap) {
                isLoadingHeatmap = true
                geofenceRepository.getUbicacionesHistorial(ganado.id).onSuccess { historialCompleto.clear(); historialCompleto.addAll(it) }
                isLoadingHeatmap = false
            }
        }
    }

    LaunchedEffect(modoHeatmap) {
        if (modoHeatmap) {
            selectedGanado?.let { ganado ->
                isLoadingHeatmap = true
                geofenceRepository.getUbicacionesHistorial(ganado.id).onSuccess { historialCompleto.clear(); historialCompleto.addAll(it) }
                isLoadingHeatmap = false
            }
        } else { historialCompleto.clear() }
    }

    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted) {
            LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { loc ->
                loc?.let {
                    userLocation = LatLng(it.latitude, it.longitude)
                    if (!hasMovedCamera) {
                        scope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLocation!!, 15f), 1000)
                            hasMovedCamera = true
                        }
                    }
                }
            }
        }
    }

    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, if (isDarkTheme) R.raw.dark_map_style else R.raw.light_map_style),
                    isMyLocationEnabled = locationPermission.status.isGranted
                ),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
                onMapClick = { latLng ->
                    if (showGeofenceSheet) geofenceCenterTemp = latLng
                    else panelSize = PanelSize.COLLAPSED
                }
            ) {
                userGanadoLocation.forEach { ganado ->
                    val icon = rememberPetMarkerIcon(context = context, photoUrl = ganado.foto_url, borderColor = "#FF8CE2".toColorInt(), anchorColor = "#FF8CE2".toColorInt())
                    icon?.let {
                        Marker(
                            state = MarkerState(LatLng(ganado.lat, ganado.lng)),
                            title = ganado.nombre, snippet = "Tipo: ${ganado.tipo}", icon = it,
                            anchor = Offset(0.5f, 1f),
                            onClick = { selectedGanado = ganado; panelSize = PanelSize.MEDIUM; false }
                        )
                    }
                }

                geofenceActivo?.let { gf ->
                    val center = LatLng(gf.lat, gf.lng)
                    Circle(center = center, radius = gf.radio_metros, fillColor = Color(0x2200C853), strokeColor = Color(0xFF00C853), strokeWidth = 3f)
                    Marker(state = MarkerState(center), title = "Centro zona segura", snippet = "Radio: ${gf.radio_metros.toInt()} m")
                }

                if (showGeofenceSheet) {
                    geofenceCenterTemp?.let { c ->
                        Circle(center = c, radius = geofenceRadioTemp.toDouble(), fillColor = Color(0x220091EA), strokeColor = Color(0xFF0091EA), strokeWidth = 3f)
                    }
                }

                if (modoHeatmap && heatmapPoints.size >= 2) {
                    val provider = remember(heatmapPoints) { HeatmapTileProvider.Builder().data(heatmapPoints).radius(40).build() }
                    TileOverlay(tileProvider = provider)
                }

                if (modoHeatmap && mostrarLineas && heatmapPoints.size >= 2) {
                    Polyline(points = heatmapPoints, color = Color(0xEEFF6B35), width = 6f, geodesic = true)

                    val paso = maxOf(1, heatmapPoints.size / 12)
                    heatmapPoints.windowed(2).forEachIndexed { index, (desde, hacia) ->
                        if (index % paso == 0) {
                            val angulo = anguloDesdePuntos(desde, hacia)
                            val midPoint = LatLng((desde.latitude + hacia.latitude) / 2, (desde.longitude + hacia.longitude) / 2)
                            val flechaBmp = crearFlechaBitmap(angulo, android.graphics.Color.parseColor("#FF6B35"), 52)
                            Marker(state = MarkerState(midPoint), icon = BitmapDescriptorFactory.fromBitmap(flechaBmp), anchor = Offset(0.5f, 0.5f), zIndex = 1f)
                        }
                    }
                    heatmapPoints.lastOrNull()?.let { Marker(state = MarkerState(it), title = "Inicio del recorrido") }
                    heatmapPoints.firstOrNull()?.let { Marker(state = MarkerState(it), title = "Posición más reciente") }
                }
            }

            // Indicador heatmap
            if (modoHeatmap) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(12.dp), shadowElevation = 4.dp
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("${heatmapPoints.size} puntos", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(intervalo.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        if (mostrarLineas) Text("↗ Recorrido activo", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF6B35))
                    }
                }
            }

            if (isLoadingHeatmap) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))

            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = panelHeight + 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                selectedGanado?.let {
                    SmallFloatingActionButton(
                        onClick = { geofenceCenterTemp = LatLng(it.lat, it.lng); showGeofenceSheet = true },
                        containerColor = if (geofenceActivo != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_fence), "Zona segura", Modifier.size(18.dp),
                            tint = if (geofenceActivo != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                SmallFloatingActionButton(
                    onClick = { if (selectedGanado == null) errorMessage = "Selecciona una mascota primero" else showHeatmapSheet = true },
                    containerColor = if (modoHeatmap) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_heatmap), "Mapa de calor", Modifier.size(18.dp),
                        tint = if (modoHeatmap) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }

                HorizontalDivider(modifier = Modifier.width(36.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                FloatingActionButton(
                    onClick = { scope.launch { userLocation?.let { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 15f), 800) } ?: run { errorMessage = "Ubicación no disponible aún" } } },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.size(48.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_location), "Mi ubicación", Modifier.size(22.dp))
                }
            }

            // PANEL INFERIOR ARRASTRABLE
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(panelHeight)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            panelSize = when {
                                dragAmount < -20 -> when (panelSize) {
                                    PanelSize.COLLAPSED -> PanelSize.MEDIUM
                                    PanelSize.MEDIUM -> PanelSize.EXPANDED
                                    else -> PanelSize.EXPANDED
                                }
                                dragAmount > 20 -> when (panelSize) {
                                    PanelSize.EXPANDED -> PanelSize.MEDIUM
                                    PanelSize.MEDIUM -> PanelSize.COLLAPSED
                                    else -> PanelSize.COLLAPSED
                                }
                                else -> panelSize
                            }
                        }
                    }
            ) {
                Column(Modifier.fillMaxSize()) {
                    // Handle visual
                    Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)))
                    }

                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (userGanadoLocation.isEmpty()) "Sin mascotas" else "${userGanadoLocation.size} mascota(s)",
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(PanelSize.COLLAPSED, PanelSize.MEDIUM, PanelSize.EXPANDED).forEach { size ->
                                Box(
                                    Modifier.size(if (panelSize == size) 10.dp else 7.dp).clip(CircleShape)
                                        .background(if (panelSize == size) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                                        .clickable { panelSize = size }
                                )
                            }
                        }
                    }

                    if (panelSize != PanelSize.COLLAPSED) {
                        if (isLoading) {
                            LinearWavyProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                        } else if (userGanadoLocation.isEmpty()) {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("Sin mascotas con ubicación activa", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(userGanadoLocation) { ganado ->
                                    ganadoMapItem(
                                        ganado = ganado,
                                        isSelected = selectedGanado?.id == ganado.id,
                                        hasGeofence = geofenceActivo?.ganado_id == ganado.id,
                                        onClick = { selectedGanado = ganado; panelSize = PanelSize.MEDIUM }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Snackbar
            errorMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = panelHeight + 8.dp, start = 16.dp, end = 80.dp),
                    action = { TextButton(onClick = { errorMessage = null }) { Text("OK") } }
                ) { Text(msg) }
            }
        }
    }

    if (showGeofenceSheet) {
        GeofenceBottomSheet(
            ganadoName = selectedGanado?.nombre ?: "", centerTemp = geofenceCenterTemp,
            radioTemp = geofenceRadioTemp, geofenceExistente = geofenceActivo,
            onRadioChange = { geofenceRadioTemp = it },
            onSave = {
                scope.launch {
                    val center = geofenceCenterTemp ?: return@launch
                    geofenceRepository.saveGeofence(NewGeofence(ganado_id = selectedGanado!!.id, user_id = userId, lat = center.latitude, lng = center.longitude, radio_metros = geofenceRadioTemp.toDouble()))
                        .onSuccess { geofenceActivo = it; showGeofenceSheet = false }
                        .onFailure { errorMessage = "Error al guardar: ${it.message}" }
                }
            },
            onDelete = {
                scope.launch {
                    selectedGanado?.let { ganado ->
                        geofenceRepository.deleteGeofence(ganado.id)
                            .onSuccess { geofenceActivo = null; showGeofenceSheet = false }
                            .onFailure { errorMessage = "Error al eliminar: ${it.message}" }
                    }
                }
            },
            onDismiss = { showGeofenceSheet = false }
        )
    }

    if (showHeatmapSheet) {
        HeatmapBottomSheet(
            ganadoName = selectedGanado?.nombre ?: "", modoHeatmap = modoHeatmap, mostrarLineas = mostrarLineas,
            intervalo = intervalo, totalPuntos = heatmapPoints.size, totalHistorial = historialCompleto.size,
            isLoading = isLoadingHeatmap, onToggleHeatmap = { modoHeatmap = it }, onToggleLineas = { mostrarLineas = it },
            onIntervaloChange = { intervalo = it },
            onRecargar = {
                scope.launch {
                    selectedGanado?.let { ganado ->
                        isLoadingHeatmap = true
                        geofenceRepository.getUbicacionesHistorial(ganado.id)
                            .onSuccess { historialCompleto.clear(); historialCompleto.addAll(it) }
                            .onFailure { errorMessage = "Error al cargar: ${it.message}" }
                        isLoadingHeatmap = false
                    }
                }
            },
            onDismiss = { showHeatmapSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapBottomSheet(
    ganadoName: String, modoHeatmap: Boolean, mostrarLineas: Boolean, intervalo: IntervaloHeatmap,
    totalPuntos: Int, totalHistorial: Int, isLoading: Boolean,
    onToggleHeatmap: (Boolean) -> Unit, onToggleLineas: (Boolean) -> Unit,
    onIntervaloChange: (IntervaloHeatmap) -> Unit, onRecargar: () -> Unit, onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Mapa de calor · $ganadoName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("Total historial", style = MaterialTheme.typography.labelMedium); Text("$totalHistorial registros", fontWeight = FontWeight.Bold) }
                    Column(horizontalAlignment = Alignment.End) { Text("Mostrando", style = MaterialTheme.typography.labelMedium); Text("$totalPuntos puntos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                }
            }

            if (totalHistorial == 0 && !isLoading && modoHeatmap) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text("Sin historial.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
                }
            }
            if (totalHistorial > 0 && totalPuntos < 2 && modoHeatmap) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Text("Solo $totalPuntos punto(s) en \"${intervalo.label}\". Prueba con un intervalo más amplio.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }

            HorizontalDivider()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Mapa de calor", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium); Text("Muestra densidad de movimiento", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) }
                Switch(checked = modoHeatmap, onCheckedChange = onToggleHeatmap)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Recorrido con flechas", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium); Text("Línea direccional entre puntos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) }
                Switch(checked = mostrarLineas, onCheckedChange = onToggleLineas, enabled = modoHeatmap)
            }

            HorizontalDivider()
            Text("Intervalo de tiempo", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                IntervaloHeatmap.entries.forEach { opcion ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(if (intervalo == opcion) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                            .clickable(enabled = modoHeatmap) { onIntervaloChange(opcion) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(opcion.label, style = MaterialTheme.typography.bodyMedium,
                            color = when { !modoHeatmap -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f); intervalo == opcion -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.onSurface })
                        if (intervalo == opcion) Icon(painterResource(R.drawable.ic_location), null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onRecargar, modifier = Modifier.weight(1f), enabled = !isLoading) {
                    if (isLoading) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
                    Text(if (isLoading) "Cargando…" else "Recargar")
                }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Listo") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofenceBottomSheet(
    ganadoName: String, centerTemp: LatLng?, radioTemp: Float, geofenceExistente: Geofence?,
    onRadioChange: (Float) -> Unit, onSave: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Zona segura · $ganadoName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_map), null, Modifier.size(18.dp), tint = Color.Unspecified)
                    Text("Si la mascota sale del radio se enviará una notificación.", style = MaterialTheme.typography.bodySmall)
                }
            }
            centerTemp?.let { Text("Centro: ${"%.5f".format(it.latitude)}, ${"%.5f".format(it.longitude)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) }
            Column {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Radio de la zona", style = MaterialTheme.typography.labelLarge)
                    Text("${radioTemp.toInt()} m", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(value = radioTemp, onValueChange = onRadioChange, valueRange = 20f..200f, steps = 20, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("20 m", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("200 m", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                if (geofenceExistente != null) {
                    OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)) { Text("Eliminar zona") }
                } else Spacer(Modifier.weight(1f))
                Button(onClick = onSave, modifier = Modifier.weight(1f), enabled = centerTemp != null) {
                    Text(if (geofenceExistente != null) "Actualizar" else "Guardar zona")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun ganadoMapItem(ganado: ganadoLocation, isSelected: Boolean, hasGeofence: Boolean = false, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val iconRes = when (ganado.tipo.lowercase()) { "perro" -> R.drawable.ic_dog; "gato" -> R.drawable.ic_cat; else -> R.drawable.ic_pet }
            Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                if (!ganado.foto_url.isNullOrBlank()) {
                    AsyncImage(model = "${ganado.foto_url}?", contentDescription = ganado.nombre, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(painterResource(iconRes), ganado.nombre, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(ganado.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("${"%.4f".format(ganado.lat)}, ${"%.4f".format(ganado.lng)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (hasGeofence) Surface(color = Color(0x2200C853), shape = RoundedCornerShape(4.dp)) { Text("🛡", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }
                if (isSelected) Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) { Text("Activa", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler() {
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    LaunchedEffect(Unit) { if (!locationPermissionState.status.isGranted) locationPermissionState.launchPermissionRequest() }
}