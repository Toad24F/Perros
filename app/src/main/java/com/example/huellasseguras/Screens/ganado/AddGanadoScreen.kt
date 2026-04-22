package com.example.huellasseguras.Screens.ganado

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.example.huellasseguras.data.GanadoRepository
import com.example.huellasseguras.model.NewGanado
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FECHA_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGanadoScreen(navController: NavController, userId: String) {
    val scope = rememberCoroutineScope()
    val ganadoRepository = remember { GanadoRepository() }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    // Launcher para la galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

    // ── Estado del formulario ────────────────────────────────────────────────
    var arete           by remember { mutableStateOf("") }
    var nombre          by remember { mutableStateOf("") }
    var tipoSeleccionado by remember { mutableStateOf("") }
    var razaSeleccionada by remember { mutableStateOf("") }
    var edad            by remember { mutableStateOf("") }
    var peso            by remember { mutableStateOf("") }
    var madreId         by remember { mutableStateOf("") }
    var showDatePickerFecha        by remember { mutableStateOf(false) }
    val datePickerStateFecha       = rememberDatePickerState()
    var fechaNacimiento             by remember { mutableStateOf(LocalDate.now().format(FECHA_FORMATTER)) }
    var padreId         by remember { mutableStateOf("") }

    // ── Estado UI ────────────────────────────────────────────────────────────
    var isLoading    by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ── Catálogos ─────────────────────────────────────────────────────────────
    val tiposGanado = mapOf(
        "Bovino" to listOf(
            "Angus", "Hereford", "Charolais", "Simmental", "Brahman",
            "Limousin", "Gelbvieh", "Holstein", "Jersey", "Pardo Suizo",
            "Zebu", "Brangus", "Beefmaster", "Charbray", "Shorthorn"
        ),
        "Porcino" to listOf(
            "Yorkshire", "Landrace", "Duroc", "Hampshire", "Pietrain",
            "Berkshire", "Chester White", "Poland China", "Spotted", "Tamworth"
        ),
        "Ovino" to listOf(
            "Merino", "Suffolk", "Dorper", "Rambouillet", "Pelibuey",
            "Blackbelly", "Hampshire Down", "Corridale", "Columbia", "Katahdin"
        ),
        "Caprino" to listOf(
            "Saanen", "Nubia", "Boer", "Alpina", "Toggenburg",
            "LaMancha", "Angora", "Kiko", "Oberhasli", "Nigerian Dwarf"
        ),
        "Equino" to listOf(
            "Cuarto de Milla", "Thoroughbred", "Appaloosa", "Paint",
            "Palomino", "Arabian", "Morgan", "Tennessee Walker", "Andaluz", "Azteca"
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Registrar Animal") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Atrás"
                        )
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

            // ── Error ─────────────────────────────────────────────────────────
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Número de arete (obligatorio, único) ──────────────────────────
            OutlinedTextField(
                value = arete,
                onValueChange = { arete = it },
                label = { Text("Número de arete *") },
                placeholder = { Text("Ej: MX-001-2024") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = errorMessage != null && arete.isBlank()
            )

            // ── Nombre (opcional) ─────────────────────────────────────────────
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre (opcional)") },
                placeholder = { Text("Ej: Lupita") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ── Tipo de ganado ─────────────────────────────────────────────────
            var expandedTipo by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedTipo,
                onExpandedChange = { expandedTipo = !expandedTipo }
            ) {
                OutlinedTextField(
                    value = tipoSeleccionado,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de ganado *") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    isError = errorMessage != null && tipoSeleccionado.isBlank()
                )
                ExposedDropdownMenu(
                    expanded = expandedTipo,
                    onDismissRequest = { expandedTipo = false }
                ) {
                    tiposGanado.keys.forEach { tipo ->
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

            // ── Raza (depende del tipo) ────────────────────────────────────────
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
                        label = { Text("Raza *") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRaza)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        isError = errorMessage != null && razaSeleccionada.isBlank()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedRaza,
                        onDismissRequest = { expandedRaza = false }
                    ) {
                        tiposGanado[tipoSeleccionado]?.forEach { raza ->
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

            // ── Edad y Peso ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = edad,
                    onValueChange = { if (it.all { c -> c.isDigit() }) edad = it },
                    label = { Text("Edad (años)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = peso,
                    onValueChange = {
                        if (it.matches(Regex("^\\d*\\.?\\d*$"))) peso = it
                    },
                    label = { Text("Peso (kg)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

//            // ── Fecha de nacimiento ────────────────────────────────────────────
//            OutlinedTextField(
//                value = fechaNacimiento,
//                onValueChange = { fechaNacimiento = it },
//                label = { Text("Fecha de nacimiento") },
//                placeholder = { Text("YYYY-MM-DD") },
//                modifier = Modifier.fillMaxWidth(),
//                singleLine = true,
//                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
//            )
            OutlinedTextField(
                value         = fechaNacimiento,
                onValueChange = {},
                readOnly      = true,
                label         = { Text("Fecha de nacimiento *") },
                trailingIcon  = {
                    IconButton(onClick = { showDatePickerFecha = true }) {
                        Icon(Icons.Default.DateRange, "Seleccionar fecha")
                    }
                },
                modifier      = Modifier.fillMaxWidth()
            )


            // ── Genealogía (IDs de arete de madre/padre) ──────────────────────
            Text(
                "Genealogía (opcional)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = madreId,
                    onValueChange = { madreId = it },
                    label = { Text("Arete de la madre") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = padreId,
                    onValueChange = { padreId = it },
                    label = { Text("Arete del padre") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Botón guardar ─────────────────────────────────────────────────
            Button(
                onClick = {
                    // Validación
                    if (arete.isBlank() || tipoSeleccionado.isBlank() || razaSeleccionada.isBlank()) {
                        errorMessage = "El arete, tipo y raza son obligatorios"
                        return@Button
                    }
                    errorMessage = null
                    isLoading = true

                    scope.launch {
                        val newGanado = NewGanado(
                            arete           = arete.trim(),
                            nombre          = nombre.ifBlank { null },
                            tipo            = tipoSeleccionado,
                            raza            = razaSeleccionada,
                            edad            = edad.toIntOrNull(),
                            peso            = peso.toDoubleOrNull(),
                            fecha_nacimiento = fechaNacimiento.ifBlank { null },
                            madre_id        = null, // se resolverá por arete en el repo
                            padre_id        = null,
                            user_id = userId,
                        )

                        val result = ganadoRepository.agregarGanado(
                            newGanado   = newGanado,
                            aretesMadre = madreId.ifBlank { null },
                            aretesPadre = padreId.ifBlank { null }
                        )

                        result.onSuccess { createdGanado ->
                            // 3. Si hay foto, subirla usando el ID de la ganado creada
                            if (imageUri != null) {
                                val uploadResult = ganadoRepository.uploadGanadoPhoto(createdGanado.id, imageUri!!, context)
                                uploadResult.onSuccess { photoUrl ->
                                    // 4. Actualizar la mascota con su nueva URL de foto
                                    ganadoRepository.updateGanadoPhotoUrl(createdGanado.id, photoUrl)
                                }
                            }
                            navController.popBackStack()
                        }.onFailure { e ->
                            errorMessage = when {
                                e.message?.contains("unique", ignoreCase = true) == true ||
                                        e.message?.contains("duplicate", ignoreCase = true) == true ->
                                    "El número de arete \"$arete\" ya existe. Usa uno diferente."
                                else -> "Error al registrar: ${e.message}"
                            }
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Registrar Animal")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    // ── Date Picker dialogs ───────────────────────────────────────────────────
    if (showDatePickerFecha) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerFecha = false },
            confirmButton    = {
                TextButton(onClick = {
                    datePickerStateFecha.selectedDateMillis?.let { millis ->
                        fechaNacimiento = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                            .format(FECHA_FORMATTER)
                    }
                    showDatePickerFecha = false
                }) { Text("Aceptar") }
            },
            dismissButton    = {
                TextButton(onClick = { showDatePickerFecha = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerStateFecha)
        }
    }
}