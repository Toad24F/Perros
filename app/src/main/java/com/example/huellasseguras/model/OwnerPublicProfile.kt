package com.example.huellasseguras.model

import kotlinx.serialization.Serializable

@Serializable
data class OwnerPublicProfile(
    val user_id: String,
    val owner_name: String,
    val owner_phone: String? = null
)