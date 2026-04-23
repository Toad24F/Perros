package com.example.huellasseguras.model

import kotlinx.serialization.Serializable

@Serializable
data class MedicalRecord(
    val id: String? = null,
    val ganado_id: String,               // ahora apunta a ganado
    val tipo_registro: String,
    val titulo: String,
    val descripcion: String? = null,
    val fecha: String? = null,
    val valor_numerico: Double? = null,
    val documento_url: String? = null,
    val proxima_cita: String? = null,
    // Campos veterinarios nuevos
    val producto_aplicado: String? = null,   // nombre comercial del med/vacuna
    val lote_medicamento: String? = null,    // trazabilidad SENASICA
    val via_administracion: String? = null,  // IM, IV, SC, oral, tópica
    val periodo_retiro_dias: Int? = 0,       // días sin vender carne/leche
    val dosis: Double? = null,               // cantidad aplicada
    val unidad_dosis: String? = null,        // ml, mg, cc, tabletas
    val veterinario_nombre: String? = null,  // quién lo aplicó
    val numero_animales: Int? = 1            // para tratamientos por lote
)

@Serializable
data class NewMedicalRecord(
    val ganado_id: String,
    val tipo_registro: String,
    val titulo: String,
    val descripcion: String? = null,
    val fecha: String? = null,
    val valor_numerico: Double? = null,
    val documento_url: String? = null,
    val proxima_cita: String? = null,
    val producto_aplicado: String? = null,
    val lote_medicamento: String? = null,
    val via_administracion: String? = null,
    val periodo_retiro_dias: Int? = 0,
    val dosis: Double? = null,
    val unidad_dosis: String? = null,
    val veterinario_nombre: String? = null,
    val numero_animales: Int? = 1
)