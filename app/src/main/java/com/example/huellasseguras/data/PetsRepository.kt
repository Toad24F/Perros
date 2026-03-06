package com.example.huellasseguras.data

import com.example.huellasseguras.Supabase.Supabase
import com.example.huellasseguras.model.NewPet
import com.example.huellasseguras.model.Pet
import com.example.huellasseguras.model.PetLocation
import com.example.huellasseguras.model.PetLocationRaw

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns


class PetsRepository {

    suspend fun agregarMascota(newPet: NewPet): String? {
        return try{
            Supabase.client.from("mascotas").insert(newPet)
            null
        }catch (e: Exception) {
            val error = e.message
            e.printStackTrace()
            return error
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
        return try {
            val response = Supabase.client
                .from("mascotas")
                .select(
                    columns = Columns.raw("id, nombre, tipo, ubicaciones!inner(lat, lng)")
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
                    lng = ultimaPos?.lng ?: 0.0
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

}
