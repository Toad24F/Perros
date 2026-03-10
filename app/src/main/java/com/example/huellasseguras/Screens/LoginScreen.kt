package com.example.huellasseguras.Screens

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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.huellasseguras.R
import com.example.huellasseguras.data.authRespository
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController) {
    // Estados del formulario
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val authRespository = remember { authRespository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Validaciones
    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 8
    }

    // Interfaz de usuario
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo y título
        Column(
            modifier = Modifier.padding(vertical = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.dog_logo),
                contentDescription = "Logo de la app",
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
                    text = "HuellaLiza",
                    color = MaterialTheme.colorScheme.background,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Mensaje de error
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Campos del formulario
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next),
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

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña (mínimo 8 caracteres)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done // 4. En el último campo ponemos "Listo"
            ),
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

        // Enlace para recuperar contraseña
        TextButton(
            onClick = { /* TODO: Implementar recuperación */ },
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color(0xFF58A5D7))
        ) {
            Text("¿Olvidaste tu contraseña?")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botón de inicio de sesión
        Button(
            onClick = {
                // Validaciones
                when {
                    email.isEmpty() || password.isEmpty() -> {
                        errorMessage = "Por favor complete todos los campos"
                        return@Button
                    }
                    !isValidEmail(email) -> {
                        errorMessage = "Ingrese un email válido"
                        return@Button
                    }
                    !isValidPassword(password) -> {
                        errorMessage = "La contraseña debe tener al menos 8 caracteres"
                        return@Button
                    }
                }

                isLoading = true
                errorMessage = ""

                scope.launch {
                    val error = authRespository.loginUser(context,email, password)

                    if (error==null) {
                        navController.navigate("home") {
                            popUpTo(0)
                        }
                    }else{
                        errorMessage = error
                    }
                    isLoading = false
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

        // Enlace a registro
        Row {
            Text("¿No tienes cuenta? ")
            Text(
                "Crear cuenta",
                color = Color(0xFF58A5D7),
                modifier = Modifier.clickable { navController.navigate("registro"){
                    popUpTo("registro") { inclusive = true }
                } }
            )
        }
    }
}