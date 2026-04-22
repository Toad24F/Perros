package com.example.huellasseguras.data

import android.content.Context
import android.net.Uri
import com.example.huellasseguras.Supabase.Supabase
import com.example.huellasseguras.model.Ganado
import com.example.huellasseguras.model.NewGanado
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage

class GanadoRepository {

    // ── Registrar un animal nuevo ─────────────────────────────────────────────
    // aretesMadre / aretesPadre: el usuario ingresa el ARETE (string) y aquí
    // resolvemos el UUID correspondiente antes de insertar.
    suspend fun agregarGanado(
        newGanado: NewGanado,
        aretesMadre: String? = null,
        aretesPadre: String? = null
    ): Result<Ganado> {
        return try {
            // Resolver IDs por arete si el usuario los proporcionó
            val madreId = aretesMadre?.let { resolverIdPorArete(it) }
            val padreId = aretesPadre?.let { resolverIdPorArete(it) }

            val ganadoConId = newGanado.copy(
                // Supabase usará el DEFAULT gen_random_uuid() si la columna tiene ese default.
                // Si no, generamos uno aquí:
                madre_id = madreId,
                padre_id = padreId
            )

            val created = Supabase.client.from("ganado")
                .insert(ganadoConId) { select() }
                .decodeSingle<Ganado>()

            Result.success(created)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // ── Cargar todo el ganado ─────────────────────────────────────────────────
    suspend fun loadGanado(userId: String): Result<List<Ganado>> {
        return try {
            val lista = Supabase.client.from("ganado")
                .select(){
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<Ganado>()
            Result.success(lista)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // ── Cargar un animal por ID ───────────────────────────────────────────────
    suspend fun loadGanadoById(id: String): Result<Ganado> {
        return try {
            val animal = Supabase.client.from("ganado")
                .select { filter { eq("id", id) } }
                .decodeSingle<Ganado>()
            Result.success(animal)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // ── Buscar ID a partir del arete ─────────────────────────────────────────
    private suspend fun resolverIdPorArete(arete: String): String? {
        return try {
            val result = Supabase.client.from("ganado")
                .select { filter { eq("arete", arete.trim()) } }
                .decodeList<Ganado>()
            result.firstOrNull()?.id
        } catch (e: Exception) {
            null
        }
    }

    // ── Actualizar peso (registro rápido desde historial) ─────────────────────
    suspend fun actualizarPeso(id: String, nuevoPeso: Double): Result<Unit> {
        return try {
            Supabase.client.from("ganado").update(
                { set("peso", nuevoPeso) }
            ) {
                filter { eq("id", id) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    suspend fun uploadGanadoPhoto(ganadoId: String, imageUri: Uri, context: Context): Result<String> {
        return try {
            val bucket = Supabase.client.storage.from("ganado_fotos")
            val fileName = "ganado_$ganadoId.jpg"

            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bytes = inputStream?.readBytes() ?: throw Exception("No se pudo leer la imagen")

            bucket.upload(path = fileName, data = bytes) {
                upsert = true
            }
            val url = bucket.publicUrl(fileName)
            Result.success(url)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    // Función para actualizar la URL de la foto en la tabla
    suspend fun updateGanadoPhotoUrl(ganadoId: String, photoUrl: String): Boolean {
        return try {
            Supabase.client.from("ganado").update(
                {
                    set("foto_url", photoUrl)
                }
            ) {
                filter { eq("id", ganadoId) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}