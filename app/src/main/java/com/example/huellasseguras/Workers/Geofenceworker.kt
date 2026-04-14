package com.example.huellasseguras.Workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.huellasseguras.Notifications.NotificationHelper
import com.example.huellasseguras.Supabase.Supabase
import com.example.huellasseguras.model.Geofence
import com.example.huellasseguras.model.PetLocationRaw
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class GeofenceWorker(
    context    : Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Obtener el usuario actual
            val user = Supabase.client.auth.currentUserOrNull()
                ?: return Result.success() // No hay sesión, nada que hacer

            val userId = user.id

            // 2. Obtener geofences activos del usuario
            val geofences = Supabase.client.from("geofences")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("activo", true)
                    }
                }
                .decodeList<Geofence>()

            if (geofences.isEmpty()) return Result.success()

            // 3. Obtener ubicaciones actuales de las mascotas
            val ubicaciones = Supabase.client
                .from("mascotas")
                .select(
                    columns = Columns.raw("id, nombre, tipo, foto_url, ubicaciones!inner(lat, lng)")
                ) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<PetLocationRaw>()

            // 4. Chequear cada geofence contra la ubicación de su mascota
            geofences.forEach { gf ->
                val petRaw = ubicaciones.firstOrNull { it.id == gf.mascota_id }
                    ?: return@forEach

                val ultimaUbicacion = petRaw.ubicaciones.firstOrNull()
                    ?: return@forEach

                val distancia = distanciaMetros(
                    ultimaUbicacion.lat, ultimaUbicacion.lng,
                    gf.lat, gf.lng
                )

                if (distancia > gf.radio_metros) {
                    NotificationHelper.sendGeofenceAlert(applicationContext, petRaw.nombre)
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Reintentar si hay error de red
            Result.retry()
        }
    }

    // ── Utilidad de distancia ─────────────────────────────────────────────────
    private fun distanciaMetros(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val r    = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a    = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    companion object {
        const val WORK_NAME = "geofence_check"

        // Llamar esto desde MainActivity para programar el worker
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // Solo con internet
                .build()

            val request = PeriodicWorkRequestBuilder<GeofenceWorker>(
                repeatInterval    = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    10, TimeUnit.MINUTES  // Si falla, reintenta en 10 min
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // No reprogramar si ya existe
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}