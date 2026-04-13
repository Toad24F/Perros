package com.example.huellasseguras.model

import kotlinx.serialization.Serializable

@Serializable
data class Geofence(
    val id: String? = null,
    val mascota_id: String,
    val user_id: String,
    val lat: Double,
    val lng: Double,
    val radio_metros: Double,
    val activo: Boolean = true
)

@Serializable
data class NewGeofence(
    val mascota_id: String,
    val user_id: String,
    val lat: Double,
    val lng: Double,
    val radio_metros: Double,
    val activo: Boolean = true
)

@Serializable
data class UbicacionHistorial(
    val id: String? = null,
    val mascota_id: String,
    val lat: Double,
    val lng: Double,
    val timestamp: String? = null
)