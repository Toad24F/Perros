package com.example.huellasseguras.Screens.ganado

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.huellasseguras.R
import com.example.huellasseguras.data.PetsRepository
import com.example.huellasseguras.model.NewPet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPetScreen(navController: NavController, userId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedPref = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val petsRepository = remember { PetsRepository() }
    // Estados del formulario
    var nombre by remember { mutableStateOf("") }
    var sexo by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var peso by remember { mutableStateOf("") }
    var tipoSeleccionado by remember { mutableStateOf("") }
    var razaSeleccionada by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Launcher para la galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

    // Tipos de mascotas y sus razas
    val tiposMascotas = mapOf(
        "Perro" to listOf("Labrador", "Golden Retriever", "Bulldog", "Poodle", "Beagle",
            "Chihuahua", "Pastor Alemán", "Boxer", "Dálmata", "Husky",
            "Pug", "Rottweiler", "Shih Tzu", "Doberman", "Gran Danés"),
        "Gato" to listOf("Siamés", "Persa", "Maine Coon", "Bengalí", "Esfinge",
            "Ragdoll", "British Shorthair", "Scottish Fold", "Siberiano",
            "Azul Ruso", "Abisinio", "Birmano", "Angora", "Bombay", "Savannah"),
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nueva Mascota") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Sección de Foto ---
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { galleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = "Añadir foto",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Text("Toca para añadir foto", style = MaterialTheme.typography.labelMedium)

            // --- Campos de Texto ---
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
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
            var expanded by remember { mutableStateOf(false) }

            val opcionesSexo = listOf("Macho", "Hembra")

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {

                OutlinedTextField(
                    value = sexo,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sexo") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {

                    opcionesSexo.forEach { opcion ->
                        DropdownMenuItem(
                            text = { Text(opcion) },
                            onClick = {
                                sexo = opcion
                                expanded = false
                            }
                        )
                    }
                }
            }
            // Campos adicionales (Edad y Peso)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = edad,
                    onValueChange = { if (it.all { c -> c.isDigit() }) edad = it },
                    label = { Text("Edad") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = peso,
                    onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) peso = it },
                    label = { Text("Peso (kg)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Botón de Guardar ---
            Button(
                onClick = {
                    isLoading = true
                    scope.launch {
                        // 1. Crear el objeto para enviar
                        val newPet = NewPet(
                            nombre = nombre,
                            tipo = tipoSeleccionado,
                            raza = razaSeleccionada,
                            edad = edad.toIntOrNull() ?: 0,
                            peso = peso.toFloatOrNull() ?: 0f,
                            user_id = userId,
                            sexo = sexo
                        )

                        // 2. Guardar en la base de datos
                        val petResult = petsRepository.agregarMascota(newPet)

                        petResult.onSuccess { createdPet ->
                            // 3. Si hay foto, subirla usando el ID de la mascota creada
                            if (imageUri != null) {
                                val uploadResult = petsRepository.uploadPetPhoto(createdPet.id, imageUri!!, context)
                                uploadResult.onSuccess { photoUrl ->
                                    // 4. Actualizar la mascota con su nueva URL de foto
                                    petsRepository.updatePetPhotoUrl(createdPet.id, photoUrl)
                                }
                            }
                            isLoading = false
                            navController.popBackStack() // Regresar al éxito
                        }.onFailure {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = nombre.isNotBlank() && tipoSeleccionado.isNotBlank() && !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White)
                else Text("Registrar Mascota")
            }
        }
    }
}