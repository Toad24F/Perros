package com.example.perros

import android.content.Context
import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64

// --- Pantalla de Login ---
@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Función simple para "encriptar" (debe coincidir con RegisterScreen)
    fun simpleEncrypt(password: String): String {
        return Base64.getEncoder().encodeToString(password.toByteArray())
    }


    // Función para validar email
    fun isValidEmail(email: String): Boolean {
        val pattern = Patterns.EMAIL_ADDRESS
        return pattern.matcher(email).matches()
    }

    // Función para validar contraseña
    fun isValidPassword(password: String): Boolean {
        return password.length >= 8
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- Logo y Título ---
        Column(
            modifier = Modifier.padding(vertical = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.dog_logo),
                contentDescription = "Logo Petsense",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF58A5D7))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Huellas Seguras",
                    color = MaterialTheme.colorScheme.background,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Mostrar mensaje de error si existe
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Campo de Email con validación visual
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = email.isNotEmpty() && !isValidEmail(email),
            trailingIcon = {
                if (email.isNotEmpty()) {
                    Icon(
                        imageVector = if (isValidEmail(email)) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (isValidEmail(email)) Color.Green else Color.Red
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Campo de Contraseña con validación visual
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña (mínimo 8 caracteres)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = password.isNotEmpty() && !isValidPassword(password),
            trailingIcon = {
                if (password.isNotEmpty()) {
                    Icon(
                        imageVector = if (isValidPassword(password)) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (isValidPassword(password)) Color.Green else Color.Red
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Enlace "Olvidé mi contraseña"
        TextButton(
            onClick = { /* TODO: Implementar recuperación */ },
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color(0xFF58A5D7)
            )
        ) {
            Text("¿Olvidaste tu contraseña?")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botón de Iniciar Sesión
        Button(
            onClick = {
                if (email.isEmpty() || password.isEmpty()) {
                    errorMessage = "Por favor completa todos los campos"
                    return@Button
                }

                isLoading = true
                errorMessage = ""

                scope.launch {
                    try {
                        // 1. Encriptar la contraseña (igual que en registro)
                        val encryptedPassword = simpleEncrypt(password)
                        val userData = supabase.postgrest["users"]
                            .select(columns = Columns.list("id","nombre", "apellido", "email")) {
                                eq("email", email)
                                eq("contraseña", encryptedPassword)
                            }
                            .decodeSingleOrNull<User>()
                        if (userData == null) {
                            errorMessage = "Email o contraseña incorrectos"
                            return@launch
                        }

                        // Guardar datos en sesión local
                        val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putInt("user_id", userData.id)
                            putString("user_name", userData.nombre)
                            putString("user_surname", userData.apellido)
                            putString("user_email", userData.email)
                            apply()
                        }

                        // 4. Navegar a home
                        navController.navigate("home") {
                            popUpTo(0)
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error al iniciar sesión: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF58A5D7),
                contentColor = MaterialTheme.colorScheme.background,
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.background)
            } else {
                Text("Iniciar sesión")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Texto "¿No tienes cuenta?"
        Row {
            Text("¿No tienes cuenta? ")
            Text(
                "Crear cuenta",
                color = Color(0xFF58A5D7),
                modifier = Modifier.clickable { navController.navigate("registro") }
            )
        }
    }
}
@Serializable
data class User(
    val id: Int,
    val nombre: String,
    val apellido: String,
    val email: String,
)