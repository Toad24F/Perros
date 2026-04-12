package com.example.huellasseguras.model

import kotlinx.serialization.Serializable

@Serializable
data class MedicalRecord(
    val id: String? = null,
    val pet_id: String,
    val tipo_registro: String,
    val titulo: String,
    val descripcion: String? = null,
    val fecha: String? = null,
    val valor_numerico: Float? = null,
    val documento_url: String? = null,
    val proxima_cita: String? = null
)
// Modelo para insertar (sin id ni created_at)
@Serializable
data class NewMedicalRecord(
    val pet_id: String,
    val tipo_registro: String,
    val titulo: String,
    val descripcion: String? = null,
    val fecha: String? = null,
    val valor_numerico: Float? = null,
    val documento_url: String? = null,
    val proxima_cita: String? = null
)