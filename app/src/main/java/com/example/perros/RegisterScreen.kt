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
import io.github.jan.supabase.gotrue.SessionManager
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import java.util.Base64

// --- Patalla de registro ---
@Composable
fun RegisterScreen(navController: NavController) {
    // Estados para los campos del formulario
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // CoroutineScope para manejar operaciones asíncronas
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Función para validar email
    fun isValidEmail(email: String): Boolean {
        val pattern = Patterns.EMAIL_ADDRESS
        return pattern.matcher(email).matches()
    }

    // Función para validar contraseña
    fun isValidPassword(password: String): Boolean {
        return password.length >= 8
    }

    // Función simple para "encriptar" (solo para prototipo)
    fun simpleEncrypt(password: String): String {
        return Base64.getEncoder().encodeToString(password.toByteArray())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Logo y Título con fondo azul ---
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
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Campo de Nombre
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Campo de Apellido
            OutlinedTextField(
                value = apellido,
                onValueChange = { apellido = it },
                label = { Text("Apellido") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

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

            // Campo de Confirmar Contraseña con validación visual
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirmar Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                trailingIcon = {
                    if (confirmPassword.isNotEmpty()) {
                        Icon(
                            imageVector = if (confirmPassword == password) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (confirmPassword == password) Color.Green else Color.Red
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            )

            // Botón de Registro
            Button(
                onClick = {
                    // Validaciones
                    when {
                        nombre.isEmpty() || apellido.isEmpty() || email.isEmpty() || password.isEmpty() -> {
                            errorMessage = "Por favor complete todos los campos"
                            return@Button
                        }

                        !isValidEmail(email) -> {
                            errorMessage = "Por favor ingrese un email válido"
                            return@Button
                        }

                        !isValidPassword(password) -> {
                            errorMessage = "La contraseña debe tener al menos 8 caracteres"
                            return@Button
                        }

                        password != confirmPassword -> {
                            errorMessage = "Las contraseñas no coinciden"
                            return@Button
                        }
                    }

                    isLoading = true
                    errorMessage = ""

                    scope.launch {
                        try {
                            // "Encriptar" la contraseña (solo para prototipo)
                            val encryptedPassword = simpleEncrypt(password)

                            // Insertar usuario en Supabase
                            supabase.postgrest["users"].insert(
                                mapOf(
                                    "nombre" to nombre,
                                    "apellido" to apellido,
                                    "email" to email,
                                    "contraseña" to encryptedPassword
                                )
                            )

                            val userData = supabase.postgrest["users"]
                                .select(columns = Columns.list("id","nombre", "apellido", "email")) {
                                    eq("email", email)
                                    eq("contraseña", encryptedPassword)
                                }
                                .decodeSingleOrNull<User>()

                            // Guardar datos en sesión local
                            val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                if (userData != null) {
                                    putInt("user_id", userData.id)
                                }
                                if (userData != null) {
                                    putString("user_name", userData.nombre)
                                }
                                if (userData != null) {
                                    putString("user_surname", userData.apellido)
                                }
                                if (userData != null) {
                                    putString("user_email", userData.email)
                                }
                                apply()
                            }

                            // Navegar a la pantalla de inicio
                            navController.navigate("home") {
                                popUpTo("register") { inclusive = true }
                            }
                        } catch (e: Exception) {
                            errorMessage = when {
                                e.message?.contains("duplicate key value") == true ->
                                    "El email ya está registrado"

                                else -> "Error al registrar: ${e.message}"
                            }
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF58A5D7),
                    contentColor = MaterialTheme.colorScheme.background,
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.background)
                } else {
                    Text("Registrar cuenta", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Enlace "¿Ya tienes cuenta?"
            Row {
                Text("¿Ya tienes cuenta? ")
                Text(
                    "Iniciar sesión",
                    color = Color(0xFF58A5D7),
                    modifier = Modifier.clickable { navController.navigate("login") }
                )
            }
        }
    }
}
