package com.example.huellasseguras.data

import android.content.Context
import android.net.Uri
import com.example.huellasseguras.Supabase.Supabase
import com.example.huellasseguras.model.NewPet
import com.example.huellasseguras.model.Pet
import com.example.huellasseguras.model.PetLocation
import com.example.huellasseguras.model.PetLocationRaw


import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage


class PetsRepository {

    suspend fun agregarMascota(newPet: NewPet): Result<Pet> {
        return try {
            // Al insertar, le pedimos a Supabase que nos devuelva el registro creado
            val pet = Supabase.client.from("mascotas")
                .insert(newPet) {
                    select()
                }
                .decodeSingle<Pet>()
            Result.success(pet)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    // Función para cargar mascotas del usuario
    suspend fun loadPets(userId: String): Result<List<Pet>> {
            return try {
                val pets = Supabase.client
                    .from("mascotas")
                    .select()
                    .decodeList<Pet>()
                Result.success(pets)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    //funcion para cargar la ubicacion de las mascotas
    suspend fun loadPetsLocation(userId: String): Result<List<PetLocation>> {
        Supabase.client
        return try {
            val response = Supabase.client
                .from("mascotas")
                .select(
                    columns = Columns.raw("id, nombre, tipo, foto_url, ubicaciones!inner(lat, lng)")
                ) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<PetLocationRaw>()

            val pets = response.map { raw ->
                // Tomamos la primera ubicación de la lista anidada
                val ultimaPos = raw.ubicaciones.firstOrNull()
                PetLocation(
                    id = raw.id,
                    nombre = raw.nombre,
                    tipo = raw.tipo,
                    lat = ultimaPos?.lat ?: 0.0,
                    lng = ultimaPos?.lng ?: 0.0,
                    foto_url = raw.foto_url
                )
            }

            Result.success(pets)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    //Funcion para cargar los datos de una mascota en especifico
    suspend fun loadPetData(petId: String): Result<Pet> {
        return try {
            // 1. Usamos .select() pero agregamos un filtro .eq()
            val pet = Supabase.client // Asegúrate de usar tu objeto cliente
                .from("mascotas")
                .select {
                    filter {
                        eq("id", petId) // Filtramos por el ID que recibes
                    }
                }
                .decodeSingle<Pet>() // Usamos decodeSingle porque esperamos una sola mascota

            Result.success(pet)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }

    }
    // Función para subir la foto (asegúrate de tener 'import io.github.jan_tennert.supabase.storage.storage')
    suspend fun uploadPetPhoto(petId: String, imageUri: Uri, context: Context): Result<String> {
        return try {
            val bucket = Supabase.client.storage.from("mascotas_fotos")
            val fileName = "pet_$petId.jpg"

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
    suspend fun updatePetPhotoUrl(petId: String, photoUrl: String): Boolean {
        return try {
            Supabase.client.from("mascotas").update(
                {
                    set("foto_url", photoUrl)
                }
            ) {
                filter { eq("id", petId) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

}
