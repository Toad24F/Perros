package com.example.huellasseguras.data
import com.example.huellasseguras.Supabase.Supabase
import com.example.huellasseguras.model.MedicalRecord
import io.github.jan.supabase.postgrest.from

class medicHistoryRepository {

    suspend fun loadMedicalRecords(petId: String): Result<List<MedicalRecord>> {
        return try {
            val records = Supabase.client.from("historial_medico")
                .select {
                    filter { eq("pet_id", petId) }
                }
                .decodeList<MedicalRecord>()
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}