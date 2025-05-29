package com.example.perros

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController as NavController1
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetProfileScreen(petId: String?, navController: NavController1) {
    @Serializable
    data class PetDetail(
        val id: String,
        val nombre: String,
        val edad: Int?,
        val raza: String?,
        val peso: String?,
        val tipo: String,
        val user_id: String,
        val created_at: String?,
        val updated_at: String?
        // Solo incluye los campos que realmente devuelve tu API
    )

    val pet = remember { mutableStateOf<PetDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current


    // Modelo para la respuesta de la API
    @Serializable
    data class PetDetailResponse(
        val message: String,
        val data: List<PetDetail>
    )

    // Función para cargar los datos de la mascota
    fun loadPetData() {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null

                if (petId == null) {
                    errorMessage = "ID de mascota no válido"
                    isLoading = false
                    return@launch
                }

                val client = HttpClient(Android) {
                    install(ContentNegotiation) {
                        json(Json {
                            ignoreUnknownKeys = true  // Esto permite ignorar campos extraños
                            isLenient = true
                        })
                    }
                }

                val response = client.get("http://192.168.100.25:5000/api/v1/mascotas/$petId") {
                    contentType(ContentType.Application.Json)
                }

                when (response.status) {
                    HttpStatusCode.OK -> {
                        val apiResponse = response.body<PetDetailResponse>()
                        if (apiResponse.data.isNotEmpty()) {
                            // Mapeamos a los campos que necesitas mostrar
                            pet.value = apiResponse.data[0]
                        } else {
                            errorMessage = "No se encontraron datos"
                        }
                    }

                    else -> {
                        errorMessage = "Error: ${response.status}"
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Error al cargar: ${e.message}"
                Log.e("PetProfile", "Error", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Cargar datos al iniciar o cuando cambia el ID
    LaunchedEffect(petId) {
        loadPetData()
    }

    // Diseño de la pantalla
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Perfil de ${pet.value?.nombre ?: "Mascota"}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Regresar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(onClick = { loadPetData() }) {
                            Text("Reintentar")
                        }
                    }
                }

                pet.value != null -> {
                    val currentPet = pet.value!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Icono según tipo
                        val iconRes = when (currentPet.tipo.lowercase()) {
                            "perro" -> R.drawable.ic_dog
                            "gato" -> R.drawable.ic_cat
                            "ave" -> R.drawable.ic_bird
                            "pez" -> R.drawable.ic_fish
                            "reptil" -> R.drawable.ic_reptile
                            else -> R.drawable.ic_pet
                        }

                        // Foto/icono de la mascota
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = "Tipo ${currentPet.tipo}",
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Nombre
                        Text(
                            text = currentPet.nombre,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tipo
                        Text(
                            text = currentPet.tipo,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Detalles en cards
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Card de información básica
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Información Básica",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    Divider()

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Edad
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Edad",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = currentPet.edad?.toString() ?: "N/A",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    // Raza
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Raza",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = currentPet.raza ?: "No especificada",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    // Peso
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Peso",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = currentPet.peso?.let { "$it kg" } ?: "N/A",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            // Sección de Historial Médico
                            Text(
                                text = "Historial Médico",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )

                            // Tabla de 3 columnas (vacía por ahora)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Column {
                                    // Encabezados de la tabla
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(vertical = 12.dp, horizontal = 16.dp)
                                    ) {
                                        Text(
                                            text = "Fecha",
                                            modifier = Modifier.weight(1f),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Procedimiento",
                                            modifier = Modifier.weight(2f),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Notas",
                                            modifier = Modifier.weight(1f),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }


                                    // Contenido vacío
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No se puede Registrar aun",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                }else -> {
                Text(
                    text = "No se encontraron datos de la mascota",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            }
        }
    }
}