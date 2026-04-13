package com.example.huellasseguras.data

import com.example.huellasseguras.Supabase.Supabase
import com.example.huellasseguras.model.Geofence
import com.example.huellasseguras.model.NewGeofence
import com.example.huellasseguras.model.UbicacionHistorial
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class GeofenceRepository {

    // ── Obtener geofence activo de una mascota ────────────────────────────────
    suspend fun getGeofence(mascotaId: String): Result<Geofence?> {
        return try {
            val result = Supabase.client.from("geofences")
                .select {
                    filter {
                        eq("mascota_id", mascotaId)
                        eq("activo", true)
                    }
                }
                .decodeList<Geofence>()
            Result.success(result.firstOrNull())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Guardar o actualizar geofence ─────────────────────────────────────────
    suspend fun saveGeofence(geofence: NewGeofence): Result<Geofence> {
        return try {
            // Primero desactivar cualquier geofence anterior de esta mascota
            Supabase.client.from("geofences").update(
                { set("activo", false) }
            ) {
                filter { eq("mascota_id", geofence.mascota_id) }
            }

            // Insertar el nuevo
            val saved = Supabase.client.from("geofences")
                .insert(geofence) { select() }
                .decodeSingle<Geofence>()

            Result.success(saved)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // ── Eliminar geofence de una mascota ──────────────────────────────────────
    suspend fun deleteGeofence(mascotaId: String): Result<Unit> {
        return try {
            Supabase.client.from("geofences").delete {
                filter { eq("mascota_id", mascotaId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Cargar historial de ubicaciones para heatmap ──────────────────────────
    suspend fun getUbicacionesHistorial(
        mascotaId: String,
        limite: Int = 500
    ): Result<List<UbicacionHistorial>> {
        return try {
            val result = Supabase.client.from("ubicaciones_historial")
                .select {
                    filter { eq("mascota_id", mascotaId) }
                    order("timestamp", Order.DESCENDING)
                    limit(limite.toLong())
                }
                .decodeList<UbicacionHistorial>()
            Result.success(result)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}