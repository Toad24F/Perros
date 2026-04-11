package com.example.huellasseguras.Screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.huellasseguras.R
import com.example.huellasseguras.data.PetsRepository
import com.example.huellasseguras.model.Pet
import kotlinx.coroutines.launch
import androidx.navigation.NavController as NavController1

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PetProfileScreen(petId: String, navController: NavController1) {

    val pet1 = remember { mutableStateOf<Pet?>(null) }
    var currentPet by remember { mutableStateOf<Pet?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val petsRepository = remember { PetsRepository() }
    var isBleConnected by remember { mutableStateOf(false) }

    // Launcher para seleccionar la imagen
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            scope.launch {
                // 3. Si hay foto, subirla usando el ID de la mascota creada
                if (currentPet != null) {
                    isLoading = true
                    val uploadResult = petsRepository.uploadPetPhoto(currentPet!!.id,
                        selectedUri, context)
                    uploadResult.onSuccess { photoUrl ->
                        // 4. Actualizar la mascota con su nueva URL de foto
                        petsRepository.updatePetPhotoUrl(currentPet!!.id, photoUrl)
                        currentPet = currentPet?.copy(foto_url = photoUrl)
                        isLoading = false
                    }.onFailure {
                        errorMessage = "Error al subir la imagen"
                        isLoading = false
                    }
                }
            }
        }
    }

    // Cargar datos al iniciar o cuando cambia el ID
    LaunchedEffect(petId) {
        scope.launch {
            val result = petsRepository.loadPetData(petId)
            result.onSuccess { petData ->
                currentPet = petData
                isLoading = false
            }.onFailure { error ->
                errorMessage = "No se pudo cargar la información: ${error.message}"
                isLoading = false
            }

        }
    }

    // Diseño de la pantalla
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Perfil de ${pet1.value?.nombre ?: "Mascota"}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Regresar")
                    }
                },
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
                    LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
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
                        Button(onClick = { scope.launch {
                            val result = petsRepository.loadPetData(petId)
                            result.onSuccess { petData ->
                                currentPet = petData
                                isLoading = false
                            }.onFailure { error ->
                                errorMessage = "No se pudo cargar la información: ${error.message}"
                                isLoading = false
                            }

                        } }) {
                            Text("Reintentar")
                        }
                    }
                }

                currentPet != null -> {
                    val currentPet = currentPet
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Icono según tipo
                        val iconRes = when (currentPet?.tipo?.lowercase()) {
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
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable { galleryLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                        ) {
                            // Lógica para decidir si mostrar FOTO o ICONO
                            if (!currentPet?.foto_url.isNullOrBlank()) {
                                CircularWavyProgressIndicator()
                                AsyncImage(
                                    model = "${currentPet.foto_url}?t=${System.currentTimeMillis()}",
                                    contentDescription = currentPet.nombre,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = currentPet?.nombre,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.BottomCenter
                            ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Cambiar foto",
                                tint = Color.White,
                                modifier = Modifier.padding(bottom = 8.dp).size(20.dp)
                            )
                        }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Toca para Cambiar la foto", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(24.dp))
                        // Nombre
                        Text(
                            text = currentPet?.nombre?: "Mascota",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

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
                                    // Tipo de mascota
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Tipo de mascota",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = currentPet?.tipo ?: "N/A",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    // Sexo
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Sexo",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = currentPet?.sexo?.toString() ?: "N/A",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
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
                                            text = (currentPet?.edad?.toString() + " Años")
                                                ?: "N/A",
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
                                            text = currentPet?.raza ?: "No especificada",
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
                                            text = currentPet?.peso?.let { "$it kg" } ?: "N/A",
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
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
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