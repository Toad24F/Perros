package com.example.perros.data

import android.content.Context
import com.example.perros.Supabase.Supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class authRespository {

    // Función para realizar el registro en supabase
    suspend fun registerUser(
        context: Context,
        emailInput: String,
        passwordInput: String,
        nombre: String,
        apellido: String): String?{
        if (emailInput.isBlank() || passwordInput.isBlank()) {
            return "Email y contraseña no pueden estar vacíos"
        }
        return try {
            Supabase.client.auth.signUpWith(Email) {
                this.email = emailInput
                this.password = passwordInput
                data = buildJsonObject {
                    put("Nombre", nombre)
                    put("Apellido", apellido)
                }
            }
            // 2. Si el registro es exitoso, Supabase nos devuelve el objeto User
            val user = Supabase.client.auth.currentUserOrNull()

            if (user != null) {
                // 3. Guardar en tus SharedPreferences (sin el token, ya que el SDK lo maneja)
                val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("user_id", user.id)
                    putString("user_name", nombre)
                    putString("user_surname", apellido)
                    putString("user_email", user.email)
                    apply()
                }
                }
            null//exito
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error al registrar"
        }
    }
    //Funcion para iniciar sesión
    // 1. Debe ser suspend porque Supabase.auth es una operación de red
    suspend fun loginUser(
        context: Context,
        emailInput: String,
        passwordInput: String
    ): String? {
        if (emailInput.isBlank() || passwordInput.isBlank()) {
            return "Email y contraseña no pueden estar vacíos"
        }

        return try {
            // 2. Intento de inicio de sesión
            Supabase.client.auth.signInWith(Email) {
                email = emailInput
                password = passwordInput
            }

            // 3. Obtener el usuario actual después del login exitoso
            val user = Supabase.client.auth.currentUserOrNull()
            val nameElement = user?.userMetadata?.get("Nombre")
            val surnameElement = user?.userMetadata?.get("Apellido")

            // usar .jsonPrimitive.contentOrNull para limiar las comillas
            val nombreLimpio = nameElement?.jsonPrimitive?.contentOrNull ?: "Sin nombre"
            val apellidoLimpio = surnameElement?.jsonPrimitive?.contentOrNull ?: ""
            if (user != null) {
                val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("user_id", user.id)
                    // Usamos accessor seguro para evitar el error de Cast
                    putString("user_name", nombreLimpio)
                    putString("user_surname", apellidoLimpio)
                    putString("user_email", user.email)
                    apply()
                }
                null // Éxito: no devolvemos mensaje de error
            } else {
                "No se pudo recuperar la información del usuario"
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // 4. Mejorar el mensaje de error según la excepción
            when (e) {
                is io.github.jan.supabase.exceptions.RestException -> "Credenciales incorrectas"
                else -> "Error de conexión: ${e.message}"
            }
        }
    }

}