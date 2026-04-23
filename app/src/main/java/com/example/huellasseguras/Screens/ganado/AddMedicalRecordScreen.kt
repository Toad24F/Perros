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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
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
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

// ── Tipos de registro para ganado ─────────────────────────────────────────────
val TIPOS_DISPONIBLES_GANADO = listOf(
    "Vacuna",
    "Desparasitacion",
    "Antibiotico",
    "Vitamina/Suplemento",
    "Cirugia",
    "Consulta",
    "Parto",
    "Peso",
    "Alergia",
    "Otro"
)

// Tipos que requieren datos de medicamento/producto
private val TIPOS_MEDICAMENTO = setOf(
    "Vacuna", "Desparasitacion", "Antibiotico", "Vitamina/Suplemento"
)

// Vías de administración comunes en ganadería
private val VIAS_ADMINISTRACION = listOf(
    "Intramuscular (IM)",
    "Subcutánea (SC)",
    "Intravenosa (IV)",
    "Oral",
    "Intranasal",
    "Tópica",
    "Pour-on",
    "Intramastitis"
)

// Unidades de dosis
private val UNIDADES_DOSIS = listOf(
    "mL", "cc", "mg", "g", "UI", "dosis"
)

private val FECHA_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicalRecordScreen(ganadoId: String, navController: NavController) {
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            // Bloquea cualquier fecha mayor a la actual (en milisegundos)
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }

            // Bloquea también los años futuros en el selector de años
            override fun isSelectableYear(year: Int): Boolean {
                return year <= java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            }
        }
    )
    fun colorPorTipo(tipo: String): Color = when (tipo) {
        "Vacuna"         -> Color(0xFF4CAF50)
        "Desparasitacion"-> Color(0xFF2196F3)
        "Antibiotico"    -> Color(0xFFF44336)
        "Vitamina"       -> Color(0xFFFF9800)
        "Parto"          -> Color(0xFFE91E63)
        "Peso"           -> Color(0xFF9C27B0)
        "Diagnostico"    -> Color(0xFF00BCD4)
        "Cirugia"        -> Color(0xFFFF5722)
        ""       -> Color(0xFF607D8B)
        else             -> Color(0xFF795548)
    }
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()
    val repository = remember { medicHistoryRepository() }

    // ── Estado del formulario ─────────────────────────────────────────────────
    var tipoSeleccionado   by remember { mutableStateOf("") }
    var titulo             by remember { mutableStateOf("") }
    var descripcion        by remember { mutableStateOf("") }
    var fecha              by remember { mutableStateOf(LocalDate.now().format(FECHA_FORMATTER)) }
    var valorNumerico      by remember { mutableStateOf("") }
    var proximaCita        by remember { mutableStateOf("") }
    var documentoUri       by remember { mutableStateOf<Uri?>(null) }
    var documentoNombre    by remember { mutableStateOf<String?>(null) }

    // Campos veterinarios / medicamento
    var productoAplicado   by remember { mutableStateOf("") }
    var loteMedicamento    by remember { mutableStateOf("") }
    var viaAdministracion  by remember { mutableStateOf("") }
    var periodoRetiro      by remember { mutableStateOf("0") }
    var dosis              by remember { mutableStateOf("") }
    var unidadDosis        by remember { mutableStateOf("") }
    var veterinarioNombre  by remember { mutableStateOf("") }
    var numeroAnimales     by remember { mutableStateOf("1") }

    // ── Estado UI ─────────────────────────────────────────────────────────────
    var isLoading          by remember { mutableStateOf(false) }
    var errorMessage       by remember { mutableStateOf<String?>(null) }
    var expandedTipo       by remember { mutableStateOf(false) }
    var expandedVia        by remember { mutableStateOf(false) }
    var expandedUnidad     by remember { mutableStateOf(false) }

    // ── Date Pickers ──────────────────────────────────────────────────────────
    var showDatePickerFecha       by remember { mutableStateOf(false) }
    var showDatePickerProximaCita by remember { mutableStateOf(false) }
    val datePickerStateFecha       = rememberDatePickerState()
    val datePickerStateProximaCita = rememberDatePickerState()

    // ── File picker ───────────────────────────────────────────────────────────
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            documentoUri = it
            documentoNombre = context.contentResolver
                .query(it, null, null, null, null)
                ?.use { cursor ->
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    if (idx >= 0) cursor.getString(idx) else "documento"
                } ?: "documento"
        }
    }

    // Flags de sección condicional
    val esMedicamento  = tipoSeleccionado in TIPOS_MEDICAMENTO
    val requierePeso   = tipoSeleccionado == "Peso"
    val tieneCita      = tipoSeleccionado in listOf("Vacuna", "Desparasitacion", "Consulta", "Antibiotico")

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
                    TIPOS_DISPONIBLES_GANADO.forEach { tipo ->
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
                placeholder   = { Text("Ej: Vacuna contra fiebre aftosa") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                isError       = errorMessage != null && titulo.isBlank()
            )

            // ── Descripción ───────────────────────────────────────────────────
            OutlinedTextField(
                value         = descripcion,
                onValueChange = { descripcion = it },
                label         = { Text("Descripción (opcional)") },
                placeholder   = { Text("Observaciones adicionales…") },
                modifier      = Modifier.fillMaxWidth().height(110.dp),
                maxLines      = 4
            )

            OutlinedTextField(
                value = fecha,
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha del registro *") },
                trailingIcon = {
                    IconButton(onClick = { showDatePickerFecha = true }) {
                        Icon(Icons.Default.DateRange, "Seleccionar fecha")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (showDatePickerFecha) {
                DatePickerDialog(
                    onDismissRequest = { showDatePickerFecha = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                // Convertimos los millis a formato legible (YYYY-MM-DD)
                                fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    .format(Date(millis))
                            }
                            showDatePickerFecha = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePickerFecha = false }) { Text("Cancelar") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // ── SECCIÓN: Datos del medicamento/producto ───────────────────────
            if (esMedicamento) {
                SectionTitle("Datos del producto / medicamento")

                OutlinedTextField(
                    value         = productoAplicado,
                    onValueChange = { productoAplicado = it },
                    label         = { Text("Producto aplicado *") },
                    placeholder   = { Text("Ej: Ivermectina 1%") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    isError       = errorMessage != null && productoAplicado.isBlank()
                )

                OutlinedTextField(
                    value         = loteMedicamento,
                    onValueChange = { loteMedicamento = it },
                    label         = { Text("Lote del medicamento") },
                    placeholder   = { Text("Ej: LOT-2024-001") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true
                )

                // Dosis + Unidad en la misma fila
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value         = dosis,
                        onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) dosis = it },
                        label         = { Text("Dosis *") },
                        placeholder   = { Text("Ej: 5") },
                        modifier      = Modifier.weight(1f),
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError       = errorMessage != null && dosis.isBlank()
                    )

                    // Dropdown unidad de dosis
                    ExposedDropdownMenuBox(
                        expanded        = expandedUnidad,
                        onExpandedChange = { expandedUnidad = !expandedUnidad },
                        modifier        = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value        = unidadDosis,
                            onValueChange = {},
                            readOnly     = true,
                            label        = { Text("Unidad *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUnidad) },
                            modifier     = Modifier.menuAnchor().fillMaxWidth(),
                            isError      = errorMessage != null && unidadDosis.isBlank()
                        )
                        ExposedDropdownMenu(
                            expanded        = expandedUnidad,
                            onDismissRequest = { expandedUnidad = false }
                        ) {
                            UNIDADES_DOSIS.forEach { u ->
                                DropdownMenuItem(
                                    text    = { Text(u) },
                                    onClick = { unidadDosis = u; expandedUnidad = false }
                                )
                            }
                        }
                    }
                }

                // Vía de administración
                ExposedDropdownMenuBox(
                    expanded        = expandedVia,
                    onExpandedChange = { expandedVia = !expandedVia }
                ) {
                    OutlinedTextField(
                        value        = viaAdministracion,
                        onValueChange = {},
                        readOnly     = true,
                        label        = { Text("Vía de administración *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVia) },
                        modifier     = Modifier.menuAnchor().fillMaxWidth(),
                        isError      = errorMessage != null && viaAdministracion.isBlank()
                    )
                    ExposedDropdownMenu(
                        expanded        = expandedVia,
                        onDismissRequest = { expandedVia = false }
                    ) {
                        VIAS_ADMINISTRACION.forEach { via ->
                            DropdownMenuItem(
                                text    = { Text(via) },
                                onClick = { viaAdministracion = via; expandedVia = false }
                            )
                        }
                    }
                }

                // Período de retiro
                OutlinedTextField(
                    value         = periodoRetiro,
                    onValueChange = { if (it.all { c -> c.isDigit() }) periodoRetiro = it },
                    label         = { Text("Período de retiro (días)") },
                    placeholder   = { Text("0 si no aplica") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("Días que no se puede consumir leche/carne") }
                )
            }

            // ── SECCIÓN: Valor numérico (solo Peso) ──────────────────────────
            if (requierePeso) {
                OutlinedTextField(
                    value         = valorNumerico,
                    onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) valorNumerico = it },
                    label         = { Text("Peso (kg) *") },
                    placeholder   = { Text("Ej: 380.5") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError       = errorMessage != null && valorNumerico.isBlank()
                )
            }

            // ── SECCIÓN: Datos generales veterinarios ─────────────────────────
            SectionTitle("Datos generales")

            // Número de animales tratados
            OutlinedTextField(
                value         = numeroAnimales,
                onValueChange = { if (it.all { c -> c.isDigit() } && it.isNotEmpty()) numeroAnimales = it },
                label         = { Text("Número de animales tratados") },
                placeholder   = { Text("1") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText  = { Text("Útil para tratamientos colectivos") }
            )

            OutlinedTextField(
                value         = veterinarioNombre,
                onValueChange = { veterinarioNombre = it },
                label         = { Text("Nombre del veterinario (opcional)") },
                placeholder   = { Text("Ej: Dr. Juan Pérez") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )

            // ── Próxima cita ──────────────────────────────────────────────────
            if (tieneCita) {
                OutlinedTextField(
                    value         = proximaCita,
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("Próxima aplicación / cita (opcional)") },
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

            // ── Documento adjunto ─────────────────────────────────────────────
            Text(
                "Documento adjunto (opcional)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = if (documentoUri != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
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
                        tint = if (documentoUri != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Column {
                        Text(
                            if (documentoUri != null) documentoNombre ?: "Archivo seleccionado"
                            else "Toca para adjuntar PDF o imagen",
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (documentoUri != null) FontWeight.Medium else FontWeight.Normal,
                            color      = if (documentoUri != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
                    // ── Validaciones ──────────────────────────────────────────
                    val validationError = when {
                        tipoSeleccionado.isBlank() ->
                            "Selecciona el tipo de registro"
                        titulo.isBlank() ->
                            "El título es obligatorio"
                        requierePeso && valorNumerico.isBlank() ->
                            "Ingresa el peso del animal en kg"
                        esMedicamento && productoAplicado.isBlank() ->
                            "Ingresa el nombre del producto aplicado"
                        esMedicamento && dosis.isBlank() ->
                            "Ingresa la dosis aplicada"
                        esMedicamento && unidadDosis.isBlank() ->
                            "Selecciona la unidad de la dosis"
                        esMedicamento && viaAdministracion.isBlank() ->
                            "Selecciona la vía de administración"
                        numeroAnimales.isBlank() || (numeroAnimales.toIntOrNull() ?: 0) < 1 ->
                            "El número de animales debe ser al menos 1"
                        else -> null
                    }

                    if (validationError != null) {
                        errorMessage = validationError
                        return@Button
                    }

                    errorMessage = null
                    isLoading    = true

                    scope.launch {
                        val newRecord = NewMedicalRecord(
                            ganado_id          = ganadoId,
                            tipo_registro      = tipoSeleccionado,
                            titulo             = titulo,
                            descripcion        = descripcion.ifBlank { null },
                            fecha              = fecha,
                            valor_numerico     = if (requierePeso) valorNumerico.toDoubleOrNull() else null,
                            proxima_cita       = proximaCita.ifBlank { null },
                            documento_url      = null,   // se actualizará tras subir
                            // Medicamento
                            producto_aplicado  = productoAplicado.ifBlank { null },
                            lote_medicamento   = loteMedicamento.ifBlank { null },
                            via_administracion = viaAdministracion.ifBlank { null },
                            periodo_retiro_dias = periodoRetiro.toIntOrNull() ?: 0,
                            dosis              = dosis.toDoubleOrNull(),
                            unidad_dosis       = unidadDosis.ifBlank { null },
                            // General
                            veterinario_nombre = veterinarioNombre.ifBlank { null },
                            numero_animales    = numeroAnimales.toIntOrNull() ?: 1
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
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
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

// ── Componente auxiliar para títulos de sección ───────────────────────────────
@Composable
private fun SectionTitle(text: String) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth()
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
}