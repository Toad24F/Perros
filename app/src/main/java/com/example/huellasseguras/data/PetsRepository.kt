package com.example.huellasseguras.data

import android.util.Log

import com.example.huellasseguras.Supabase.Supabase
import com.example.huellasseguras.model.NewPet
import com.example.huellasseguras.model.Pet

import io.github.jan.supabase.postgrest.from


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


    // Función para cargar mascotas desde el servidor

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

    }
