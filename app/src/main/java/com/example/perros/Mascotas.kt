package com.example.perros

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
@Serializable
data class PetWithLocation(
    val id: String,
    val nombre: String,
    val edad: Int?,
    val raza: String?,
    val peso: String?,
    val tipo: String,
    val user_id: String,
    val created_at: String?,
    val updated_at: String?,
    val lat: String?,
    val lng: String?,
    val ultima_actualizacion: String?
)
@Composable
fun Mascotas(navController: NavController) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val userId by remember { mutableStateOf(sharedPref.getString("user_id", "") ?: "") }
    val userName by remember { mutableStateOf(sharedPref.getString("user_name", "Usuario") ?: "Usuario") }
    val token by remember { mutableStateOf(sharedPref.getString("user_token", "") ?: "") }

    var searchText by remember { mutableStateOf("") }
    var showAddPetDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Estado para las mascotas del usuario
    val userPets = remember { mutableStateListOf<PetWithLocation>() }
    val scope = rememberCoroutineScope()

    // Modelos de datos
    @Serializable
    data class PetsResponse(
        val message: String,
        val data: List<PetWithLocation>
    )



    // Función para cargar mascotas desde el servidor
    fun loadPets() {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null

                val client = HttpClient(Android) {
                    install(ContentNegotiation) {
                        json(Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                        })
                    }
                }

                // Usar parámetros de consulta en la URL
                val response = client.get("http://192.168.100.25:5000/api/v1/mascotas?user_id=$userId") {
                    contentType(ContentType.Application.Json)
                }

                when (response.status) {
                    HttpStatusCode.OK -> {
                        val petsResponse = response.body<PetsResponse>()
                        userPets.clear()
                        userPets.addAll(petsResponse.data)
                    }
                    else -> {
                        errorMessage = try {
                            val errorResponse = response.body<Map<String, String>>()
                            errorResponse["message"] ?: "Error al cargar mascotas"
                        } catch (e: Exception) {
                            "Error al procesar la respuesta"
                        }
                    }
                }
            } catch (e: Exception) {
                errorMessage = when {
                    e.message?.contains("Unable to resolve host") == true ->
                        "No se puede conectar al servidor"
                    e.message?.contains("timed out") == true ->
                        "Tiempo de espera agotado"
                    else -> "Error: ${e.message ?: "Error desconocido"}"
                }
                Log.e("Mascotas", "Error al cargar", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Cargar mascotas al iniciar
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            loadPets()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Foto de perfil del usuario
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_person),
                contentDescription = "Foto de perfil",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Saludo al usuario
        Text(
            text = "Hola, $userName!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Barra de búsqueda
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Buscar mascota") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Mostrar error si hay
        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
            Button(onClick = { loadPets() }) {
                Text("Reintentar")
            }
        }

        // Mostrar loading
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(24.dp))
        }

        // Lista de mascotas
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(userPets.filter {
                it.nombre.contains(searchText, ignoreCase = true) ||
                        it.tipo.contains(searchText, ignoreCase = true)
            }) { pet ->
                PetItem(pet = pet) {
                    navController.navigate("petDetail/${pet.id}")
                    println(pet.id)
                }
            }

            item {
                AddPetButton {
                    showAddPetDialog = true
                }
            }
        }
    }

    // Diálogo para agregar mascota
    if (showAddPetDialog) {
        AddPetForm(
            onDismiss = { showAddPetDialog = false },
            onSave = { newPet ->
                scope.launch {
                    try {
                        val client = HttpClient(Android) {
                            install(ContentNegotiation) {
                                json(Json {
                                    ignoreUnknownKeys = true
                                    isLenient = true
                                })
                            }
                        }

                        val response = client.post("http://192.168.100.25:5000/api/v1/mascotas/") {
                            contentType(ContentType.Application.Json)
                            setBody(newPet) // Enviamos el objeto NewPet directamente
                        }

                        when (response.status) {
                            HttpStatusCode.Created -> {
                                loadPets() // Recargamos la lista
                                // Opcional: Mostrar mensaje de éxito
                                errorMessage = "Mascota agregada exitosamente"
                            }
                            else -> {
                                errorMessage = try {
                                    val errorResponse = response.body<Map<String, String>>()
                                    errorResponse["message"] ?: "Error al agregar mascota"
                                } catch (e: Exception) {
                                    "Error al procesar la respuesta"
                                }
                            }
                        }
                    } catch (e: Exception) {
                        errorMessage = when {
                            e.message?.contains("Unable to resolve host") == true ->
                                "No se puede conectar al servidor"
                            else -> "Error: ${e.message ?: "Error desconocido"}"
                        }
                        Log.e("Mascotas", "Error al agregar", e)
                    }
                }
            },
            userId = userId
        )
    }
}

@Composable
fun PetItem(pet: PetWithLocation, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        // Icono según el tipo de mascota
        val iconRes = when(pet.tipo.lowercase()) {
            "perro" -> R.drawable.ic_dog
            "gato" -> R.drawable.ic_cat
            "ave" -> R.drawable.ic_bird
            "pez" -> R.drawable.ic_fish
            "reptil" -> R.drawable.ic_reptile
            else -> R.drawable.ic_pet
        }

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = pet.nombre,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = pet.nombre,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = pet.tipo,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

    }
}

@Composable
fun AddPetButton(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = "Agregar mascota",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Agregar",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPetForm(
    onDismiss: () -> Unit,
    onSave: (NewPet) -> Unit,
    userId: String
) {
    var nombre by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var peso by remember { mutableStateOf("") }
    var tipoSeleccionado by remember { mutableStateOf("") }
    var razaSeleccionada by remember { mutableStateOf("") }

    // Tipos de mascotas y sus razas
    val tiposMascotas = mapOf(
        "Perro" to listOf("Labrador", "Golden Retriever", "Bulldog", "Poodle", "Beagle",
            "Chihuahua", "Pastor Alemán", "Boxer", "Dálmata", "Husky",
            "Pug", "Rottweiler", "Shih Tzu", "Doberman", "Gran Danés"),
        "Gato" to listOf("Siamés", "Persa", "Maine Coon", "Bengalí", "Esfinge",
            "Ragdoll", "British Shorthair", "Scottish Fold", "Siberiano",
            "Azul Ruso", "Abisinio", "Birmano", "Angora", "Bombay", "Savannah"),
        // ... (otros tipos como en tu código original)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar nueva mascota") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la mascota") },
                    modifier = Modifier.fillMaxWidth()
                )

                var expandedTipo by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedTipo,
                    onExpandedChange = { expandedTipo = !expandedTipo }
                ) {
                    OutlinedTextField(
                        value = tipoSeleccionado,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de mascota") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTipo,
                        onDismissRequest = { expandedTipo = false }
                    ) {
                        tiposMascotas.keys.forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text(tipo) },
                                onClick = {
                                    tipoSeleccionado = tipo
                                    razaSeleccionada = ""
                                    expandedTipo = false
                                }
                            )
                        }
                    }
                }

                if (tipoSeleccionado.isNotEmpty()) {
                    var expandedRaza by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expandedRaza,
                        onExpandedChange = { expandedRaza = !expandedRaza }
                    ) {
                        OutlinedTextField(
                            value = razaSeleccionada,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Raza") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRaza) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedRaza,
                            onDismissRequest = { expandedRaza = false }
                        ) {
                            tiposMascotas[tipoSeleccionado]?.forEach { raza ->
                                DropdownMenuItem(
                                    text = { Text(raza) },
                                    onClick = {
                                        razaSeleccionada = raza
                                        expandedRaza = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = edad,
                    onValueChange = { if (it.all { char -> char.isDigit() }) edad = it },
                    label = { Text("Edad (años)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = peso,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            peso = it
                        }
                    },
                    label = { Text("Peso (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newPet = NewPet(
                        nombre = nombre,
                        tipo = tipoSeleccionado,
                        raza = razaSeleccionada.ifEmpty { null },
                        edad = edad.toIntOrNull(),
                        peso = peso.toFloatOrNull(),
                        user_id = userId
                    )
                    onSave(newPet)
                    onDismiss()
                },
                enabled = nombre.isNotBlank() && tipoSeleccionado.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Serializable
data class NewPet(
    val nombre: String,
    val tipo: String,
    val raza: String? = null,
    val edad: Int?,
    val peso: Float?,
    val user_id: String
)