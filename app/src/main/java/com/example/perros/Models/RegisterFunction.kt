package com.example.perros.Models

import android.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.perros.Supabase.Supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RegisterFunction {
    // Función para realizar el registro en supabase
    suspend fun registerUser(emailInput: String, passwordInput: String, nombre: String, apellido: String): String?{
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
            null//exito
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error al registrar"
        }
    }
}