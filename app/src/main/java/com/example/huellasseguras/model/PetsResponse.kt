package com.example.huellasseguras.model

import kotlinx.serialization.Serializable

@Serializable
data class PetsResponse(
    val message: String,
    val data: List<Pet>
)