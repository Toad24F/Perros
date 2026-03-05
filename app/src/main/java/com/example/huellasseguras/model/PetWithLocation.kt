package com.example.huellasseguras.model

import kotlinx.serialization.Serializable

@Serializable
data class Pet(
    val id: String,
    val nombre: String,
    val tipo: String,
    val raza: String?,
    val edad: Int?,
    val peso: Float?,
    val user_id: String,
//    val lat: String?,
//    val lng: String?,
//    val ultima_actualizacion: String?
)