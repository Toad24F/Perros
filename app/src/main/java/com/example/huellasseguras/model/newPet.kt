package com.example.huellasseguras.model

import kotlinx.serialization.Serializable

@Serializable
data class NewPet(
    val nombre: String,
    val tipo: String,
    val raza: String? = null,
    val edad: Int?,
    val peso: Float?,
    val user_id: String
)