package com.example.perros

import android.Manifest
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.google.maps.android.compose.rememberCameraPositionState


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
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    var deviceLocation by remember { mutableStateOf<LatLng?>(null) }
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val cameraPositionState = rememberCameraPositionState()
    // ✅ Estilo del mapa dinámico según el tema del sistema
    val mapStyle by remember {
        mutableStateOf(
            MapStyleOptions.loadRawResourceStyle(
                context,
                if (isDarkTheme) R.raw.dark_map_style else R.raw.light_map_style
            )
        )
    }
    // Manejo de permisos y ubicación
    LaunchedEffect(locationPermissionState.status) {
        when {
            locationPermissionState.status.isGranted -> {
                if (isLocationEnabled(context)) {
                    try {
                        locationClient.lastLocation.addOnSuccessListener { location ->
                            location?.let {
                                deviceLocation = LatLng(it.latitude, it.longitude)
                            }
                        }
                    } catch (e: SecurityException) {
                        e.printStackTrace()
                    }
                }
            }
            else -> {
                locationPermissionState.launchPermissionRequest()
            }
        }
    }
    // 🔄 Mover la cámara cuando se obtenga la ubicación
    LaunchedEffect(deviceLocation) {
        deviceLocation?.let { latLng ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(latLng, 15f),
                durationMs = 1000
            )
        }
    }
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            mapStyleOptions = mapStyle,
            isMyLocationEnabled = locationPermissionState.status.isGranted
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            myLocationButtonEnabled = locationPermissionState.status.isGranted
        )
    )
}
