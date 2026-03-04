package com.example.perros.Models

import android.R
import android.content.Context
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
            // Nota: En algunas configuraciones de Supabase, si se requiere confirmación de email,
            // la sesión no se inicia inmediatamente.
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
}