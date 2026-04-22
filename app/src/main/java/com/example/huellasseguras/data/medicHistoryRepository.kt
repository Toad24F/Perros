package com.example.huellasseguras.data

import android.content.Context
import android.net.Uri
import com.example.huellasseguras.Supabase.Supabase
import com.example.huellasseguras.model.MedicalRecord
import com.example.huellasseguras.model.NewMedicalRecord
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage

class medicHistoryRepository {

    // ── Cargar registros de un animal ─────────────────────────────────────────
    suspend fun loadMedicalRecords(ganadoId: String): Result<List<MedicalRecord>> {
        return try {
            val records = Supabase.client.from("historial_medico")
                .select {
                    filter { eq("ganado_id", ganadoId) }
                }
                .decodeList<MedicalRecord>()
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Agregar nuevo registro (con documento opcional) ───────────────────────
    suspend fun addMedicalRecord(
        record: NewMedicalRecord,
        documentoUri: Uri?,
        context: Context
    ): Result<MedicalRecord> {
        return try {
            val created = Supabase.client.from("historial_medico")
                .insert(record) { select() }
                .decodeSingle<MedicalRecord>()

            if (documentoUri != null && created.id != null) {
                val uploadResult = uploadDocument(created.id, documentoUri, context)
                uploadResult.onSuccess { url -> updateDocumentUrl(created.id, url) }
                Result.success(created.copy(documento_url = uploadResult.getOrNull()))
            } else {
                Result.success(created)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // ── Subir documento a Supabase Storage ────────────────────────────────────
    private suspend fun uploadDocument(
        recordId: String,
        documentoUri: Uri,
        context: Context
    ): Result<String> {
        return try {
            val bucket = Supabase.client.storage.from("historial_documentos")
            val mimeType = context.contentResolver.getType(documentoUri) ?: "application/octet-stream"
            val extension = when {
                mimeType.contains("pdf")  -> "pdf"
                mimeType.contains("png")  -> "png"
                mimeType.contains("jpeg") -> "jpg"
                mimeType.contains("jpg")  -> "jpg"
                else -> "bin"
            }
            val fileName = "record_${recordId}.$extension"
            val bytes = context.contentResolver
                .openInputStream(documentoUri)
                ?.readBytes()
                ?: throw Exception("No se pudo leer el archivo")

            bucket.upload(path = fileName, data = bytes) { upsert = true }
            Result.success(bucket.publicUrl(fileName))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // ── Actualizar URL del documento ─────────────────────────────────────────
    private suspend fun updateDocumentUrl(recordId: String, url: String) {
        try {
            Supabase.client.from("historial_medico").update(
                { set("documento_url", url) }
            ) { filter { eq("id", recordId) } }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}