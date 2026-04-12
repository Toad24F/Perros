package com.example.huellasseguras.Screens


import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.huellasseguras.R
import com.example.huellasseguras.data.medicHistoryRepository
import com.example.huellasseguras.model.NewMedicalRecord
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val TIPOS_DISPONIBLES = listOf("Vacuna", "Desparasitacion", "Alergia", "Peso", "Enfermedad", "Cirugia", "Consulta")
private val FECHA_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicalRecordScreen(petId: String, navController: NavController) {
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()
    val repository = remember { medicHistoryRepository() }

    // ── Estado del formulario ─────────────────────────────────────────────────
    var tipoSeleccionado  by remember { mutableStateOf("") }
    var titulo            by remember { mutableStateOf("") }
    var descripcion       by remember { mutableStateOf("") }
    var fecha             by remember { mutableStateOf(LocalDate.now().format(FECHA_FORMATTER)) }
    var valorNumerico     by remember { mutableStateOf("") }
    var proximaCita       by remember { mutableStateOf("") }
    var documentoUri      by remember { mutableStateOf<Uri?>(null) }
    var documentoNombre   by remember { mutableStateOf<String?>(null) }

    // ── Estado UI ─────────────────────────────────────────────────────────────
    var isLoading         by remember { mutableStateOf(false) }
    var errorMessage      by remember { mutableStateOf<String?>(null) }
    var expandedTipo      by remember { mutableStateOf(false) }

    // ── Date Pickers ──────────────────────────────────────────────────────────
    var showDatePickerFecha        by remember { mutableStateOf(false) }
    var showDatePickerProximaCita  by remember { mutableStateOf(false) }
    val datePickerStateFecha       = rememberDatePickerState()
    val datePickerStateProximaCita = rememberDatePickerState()

    // ── File picker (PDF, imágenes) ───────────────────────────────────────────
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            documentoUri    = it
            documentoNombre = context.contentResolver
                .query(it, null, null, null, null)
                ?.use { cursor ->
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    if (idx >= 0) cursor.getString(idx) else "documento"
                } ?: "documento"
        }
    }

    // ── Mostrar tipo requiere valor numérico ──────────────────────────────────
    val requiereValor = tipoSeleccionado == "Peso"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nuevo Registro") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Error ─────────────────────────────────────────────────────────
            errorMessage?.let {
                Card(
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        it,
                        color    = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // ── Tipo de registro ──────────────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded        = expandedTipo,
                onExpandedChange = { expandedTipo = !expandedTipo }
            ) {
                OutlinedTextField(
                    value        = tipoSeleccionado,
                    onValueChange = {},
                    readOnly     = true,
                    label        = { Text("Tipo de registro *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                    modifier     = Modifier.menuAnchor().fillMaxWidth(),
                    isError      = errorMessage != null && tipoSeleccionado.isBlank()
                )
                ExposedDropdownMenu(
                    expanded        = expandedTipo,
                    onDismissRequest = { expandedTipo = false }
                ) {
                    TIPOS_DISPONIBLES.forEach { tipo ->
                        DropdownMenuItem(
                            text    = {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(colorPorTipo(tipo))
                                    )
                                    Text(tipo)
                                }
                            },
                            onClick = {
                                tipoSeleccionado = tipo
                                // Auto-rellenar título si está vacío
                                if (titulo.isBlank()) titulo = tipo
                                expandedTipo = false
                            }
                        )
                    }
                }
            }

            // ── Título ────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = titulo,
                onValueChange = { titulo = it },
                label         = { Text("Título *") },
                placeholder   = { Text("Ej: Vacuna antirrábica") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                isError       = errorMessage != null && titulo.isBlank()
            )

            // ── Descripción ───────────────────────────────────────────────────
            OutlinedTextField(
                value         = descripcion,
                onValueChange = { descripcion = it },
                label         = { Text("Descripción (opcional)") },
                placeholder   = { Text("Notas adicionales…") },
                modifier      = Modifier.fillMaxWidth().height(110.dp),
                maxLines      = 4
            )

            // ── Fecha ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = fecha,
                onValueChange = {},
                readOnly      = true,
                label         = { Text("Fecha del registro *") },
                trailingIcon  = {
                    IconButton(onClick = { showDatePickerFecha = true }) {
                        Icon(Icons.Default.DateRange, "Seleccionar fecha")
                    }
                },
                modifier      = Modifier.fillMaxWidth()
            )

            // ── Valor numérico (solo para Peso) ───────────────────────────────
            if (requiereValor) {
                OutlinedTextField(
                    value         = valorNumerico,
                    onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) valorNumerico = it },
                    label         = { Text("Peso (kg) *") },
                    placeholder   = { Text("Ej: 12.5") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError       = errorMessage != null && requiereValor && valorNumerico.isBlank()
                )
            }

            // ── Próxima cita ──────────────────────────────────────────────────
            if (tipoSeleccionado in listOf("Vacuna", "Desparasitacion", "Consulta")) {
                OutlinedTextField(
                    value         = proximaCita,
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("Próxima cita (opcional)") },
                    placeholder   = { Text("Selecciona una fecha") },
                    trailingIcon  = {
                        Row {
                            if (proximaCita.isNotBlank()) {
                                IconButton(onClick = { proximaCita = "" }) {
                                    Icon(
                                        painterResource(R.drawable.ic_stop),
                                        contentDescription = "Limpiar",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(onClick = { showDatePickerProximaCita = true }) {
                                Icon(Icons.Default.DateRange, "Seleccionar fecha")
                            }
                        }
                    },
                    modifier      = Modifier.fillMaxWidth()
                )
            }

            // ── Documento ─────────────────────────────────────────────────────
            Text(
                "Documento adjunto (opcional)",
                style    = MaterialTheme.typography.labelMedium,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = if (documentoUri != null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { fileLauncher.launch("*/*") }
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painterResource(R.drawable.ic_add),
                        contentDescription = null,
                        tint               = if (documentoUri != null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Column {
                        Text(
                            if (documentoUri != null) documentoNombre ?: "Archivo seleccionado"
                            else "Toca para adjuntar PDF o imagen",
                            style     = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (documentoUri != null) FontWeight.Medium else FontWeight.Normal,
                            color     = if (documentoUri != null)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        if (documentoUri != null) {
                            Text(
                                "Toca para cambiar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Botón guardar ─────────────────────────────────────────────────
            Button(
                onClick = {
                    // Validación básica
                    if (tipoSeleccionado.isBlank() || titulo.isBlank()) {
                        errorMessage = "El tipo y el título son obligatorios"
                        return@Button
                    }
                    if (requiereValor && valorNumerico.isBlank()) {
                        errorMessage = "Ingresa el peso en kg"
                        return@Button
                    }
                    errorMessage = null
                    isLoading    = true

                    scope.launch {
                        val newRecord = NewMedicalRecord(
                            pet_id          = petId,
                            tipo_registro   = tipoSeleccionado,
                            titulo          = titulo,
                            descripcion     = descripcion.ifBlank { null },
                            fecha           = fecha,
                            valor_numerico  = valorNumerico.toFloatOrNull(),
                            proxima_cita    = proximaCita.ifBlank { null },
                            documento_url   = null   // se actualizará tras subir
                        )

                        val result = repository.addMedicalRecord(newRecord, documentoUri, context)

                        result.onSuccess {
                            navController.popBackStack()
                        }.onFailure {
                            errorMessage = "Error al guardar: ${it.message}"
                            isLoading    = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled  = !isLoading,
                shape    = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color    = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Guardar registro", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Date Picker dialogs ───────────────────────────────────────────────────
    if (showDatePickerFecha) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerFecha = false },
            confirmButton    = {
                TextButton(onClick = {
                    datePickerStateFecha.selectedDateMillis?.let { millis ->
                        fecha = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.of("UTC"))
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

    if (showDatePickerProximaCita) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerProximaCita = false },
            confirmButton    = {
                TextButton(onClick = {
                    datePickerStateProximaCita.selectedDateMillis?.let { millis ->
                        proximaCita = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.of("UTC"))
                            .toLocalDate()
                            .format(FECHA_FORMATTER)
                    }
                    showDatePickerProximaCita = false
                }) { Text("Aceptar") }
            },
            dismissButton    = {
                TextButton(onClick = { showDatePickerProximaCita = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerStateProximaCita)
        }
    }
}