package com.example.huellasseguras.model

import kotlinx.serialization.Serializable
@Serializable
data class NewPet(
    val nombre: String,
    val tipo: String,
    val raza: String? = null,
    val edad: Int?,
    val peso: Float?,
    val user_id: String,
    val sexo: String
)
@Serializable
data class Pet(
    val id: String,
    val nombre: String,
    val tipo: String,
    val raza: String?,
    val edad: Int?,
    val peso: Float?,
    val user_id: String,
    val foto_url: String? = null,
    val sexo: String
)

@Serializable
data class PetLocation(
    val id: String,
    val nombre: String,
    val tipo: String,
    val lat: Double,
    val lng: Double,
    val foto_url: String? = null
)
@Serializable
data class PetLocationRaw(
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