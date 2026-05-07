package com.example.huellasseguras.model

import kotlinx.serialization.Serializable

@Serializable
data class Ganado(
    val id: String,
    val arete: String,
    val nombre: String? = null,
    val tipo: String,
    val raza: String,
    val edad: Int? = null,
    val peso: Double? = null,
    val fecha_nacimiento: String? = null,
    val madre_id: String? = null,
    val padre_id: String? = null,
    val foto_url: String? = null,
    val user_id: String,
    val sexo: String? = null,
    val bateria: Int? = null
)
@Serializable
data class GanadoConBateriaRaw(
    val id: String,
    val arete: String,
    val nombre: String? = null,
    val tipo: String,
    val raza: String,
    val user_id: String,
    val foto_url: String? = null,
    // Agregamos la lista de ubicaciones para extraer la batería del registro más reciente
    val ubicaciones: List<UbicacionBateria> = emptyList()
)

@Serializable
data class UbicacionBateria(
    val Bateria: Int? = null
)

// Modelo para insertar (sin id — Supabase lo genera o usamos UUID)
@Serializable
data class NewGanado(
    val arete: String,
    val nombre: String? = null,
    val tipo: String,
    val raza: String,
    val edad: Int? = null,
    val peso: Double? = null,
    val fecha_nacimiento: String? = null,
    val madre_id: String? = null,   // UUID del registro madre (resuelto en el repo)
    val padre_id: String? = null,    // UUID del registro padre (resuelto en el repo)}
    val user_id: String? = null,
)
@Serializable
data class ganadoLocation(
    val id: String,
    val nombre: String,
    val tipo: String,
    val lat: Double,
    val lng: Double,
    val foto_url: String? = null
)
@Serializable
data class ganadoLocationRaw(
    val id: String,
    val nombre: String,
    val tipo: String,
    val foto_url: String? = null,
    val ubicaciones: List<Ubicacion>
)
@Serializable
data class Ubicacion(
    val lat: Double,
    val lng: Double
)