package com.example.huellasseguras.model

import kotlinx.serialization.Serializable

@Serializable
data class PetLocation(
    val id: String,
    val nombre: String,
    val tipo: String,
    val lat: Double,
    val lng: Double
)
@Serializable
data class PetLocationRaw(
    val id: String,
    val nombre: String,
    val tipo: String,
    val ubicaciones: List<Ubicacion>
)
@Serializable
data class Ubicacion(
    val lat: Double,
    val lng: Double
)