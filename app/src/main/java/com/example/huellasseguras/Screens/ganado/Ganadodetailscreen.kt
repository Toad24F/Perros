package com.example.huellasseguras.Screens.ganado

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.huellasseguras.R
import com.example.huellasseguras.data.GanadoRepository
import com.example.huellasseguras.data.medicHistoryRepository
import com.example.huellasseguras.model.Ganado
import com.example.huellasseguras.model.MedicalRecord
import kotlinx.coroutines.launch

// ── Tipos de registro para ganado ─────────────────────────────────────────────
val TIPOS_REGISTRO_GANADO = listOf(
    "Todos", "Vacuna", "Desparasitacion", "Antibiotico",
    "Vitamina", "Parto", "Peso", "Diagnostico", "Cirugia", "Consulta", "Otro"
)

// ── Color por tipo ────────────────────────────────────────────────────────────
fun colorPorTipoGanado(tipo: String): Color = when (tipo) {
    "Vacuna"         -> Color(0xFF4CAF50)
    "Desparasitacion"-> Color(0xFF2196F3)
    "Antibiotico"    -> Color(0xFFF44336)
    "Vitamina"       -> Color(0xFFFF9800)
    "Parto"          -> Color(0xFFE91E63)
    "Peso"           -> Color(0xFF9C27B0)
    "Diagnostico"    -> Color(0xFF00BCD4)
    "Cirugia"        -> Color(0xFFFF5722)
    "Consulta"       -> Color(0xFF607D8B)
    else             -> Color(0xFF795548)
}

// ── Icono por tipo ────────────────────────────────────────────────────────────
fun iconoPorTipoGanado(tipo: String): Int = when (tipo) {
    "Vacuna"         -> R.drawable.ic_syringe
    "Desparasitacion"-> R.drawable.ic_deworming
    "Peso"           -> R.drawable.ic_scales
    "Antibiotico"    -> R.drawable.ic_syringe
    else             -> R.drawable.ic_pet
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GanadoDetailScreen(ganadoId: String, navController: NavController) {
    val ganadoRepository       = remember { GanadoRepository() }
    val historialRepository    = remember { medicHistoryRepository() }

    var ganado         by remember { mutableStateOf<Ganado?>(null) }
    var historial      by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var isLoading      by remember { mutableStateOf(true) }
    var errorMessage   by remember { mutableStateOf<String?>(null) }

    // Filtros
    var searchQuery          by remember { mutableStateOf("") }
    var tipoSeleccionado     by remember { mutableStateOf("Todos") }
    var ordenFecha           by remember { mutableStateOf("Reciente") }
    var showRemindersDialog  by remember { mutableStateOf(false) }

    LaunchedEffect(ganadoId) {
        ganadoRepository.loadGanadoById(ganadoId)
            .onSuccess { ganado = it }
            .onFailure { errorMessage = it.message }

        historialRepository.loadMedicalRecords(ganadoId)
            .onSuccess { historial = it }
            .onFailure { if (errorMessage == null) errorMessage = it.message }

        isLoading = false
    }

    // ── Registros filtrados ───────────────────────────────────────────────────
    val registrosFiltrados = remember(historial, searchQuery, tipoSeleccionado, ordenFecha) {
        historial
            .filter { record ->
                val coincideTipo = tipoSeleccionado == "Todos" || record.tipo_registro == tipoSeleccionado
                val coincideBusq = searchQuery.isBlank() ||
                        record.titulo.contains(searchQuery, ignoreCase = true) ||
                        record.descripcion?.contains(searchQuery, ignoreCase = true) == true ||
                        record.producto_aplicado?.contains(searchQuery, ignoreCase = true) == true
                coincideTipo && coincideBusq
            }
            .let { lista ->
                if (ordenFecha == "Reciente")
                    lista.sortedByDescending { it.fecha ?: "" }
                else
                    lista.sortedBy { it.fecha ?: "" }
            }
    }

    val registrosAgrupados = remember(registrosFiltrados) {
        registrosFiltrados.groupBy { it.tipo_registro }
    }

    val recordatorios = remember(historial) {
        historial.filter { !it.proxima_cita.isNullOrEmpty() }.sortedBy { it.proxima_cita }
    }

    // ── Alertas de periodo de retiro ──────────────────────────────────────────
    val animalesEnRetiro = remember(historial) {
        historial.filter { (it.periodo_retiro_dias ?: 0) > 0 }
    }

    Scaffold(
        floatingActionButton = {
            // TODO: Navegar a AddMedicalRecordGanadoScreen cuando esté lista
            FloatingActionButton(onClick = { /* navController.navigate("addMedicalRecord/$ganadoId") */ }) {
                Icon(Icons.Default.Add, "Agregar registro")
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Ficha: ${ganado?.nombre ?: ganado?.arete ?: "Animal"}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Regresar")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator()
            }

            errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Cabecera con foto ─────────────────────────────────────────
                GanadoHeaderSection(ganado = ganado, ganadoRepository = ganadoRepository)

                // ── Info básica ───────────────────────────────────────────────
                GanadoInfoCard(ganado)

                // ── Alerta periodo de retiro ──────────────────────────────────
                if (animalesEnRetiro.isNotEmpty()) {
                    RetiroAlertCard(animalesEnRetiro)
                }

                // ── Banner recordatorio ───────────────────────────────────────
                recordatorios.firstOrNull()?.let { prox ->
                    ReminderBannerWithButtonGanado(
                        proxRecord    = prox,
                        cantidadTotal = recordatorios.size,
                        onVerTodos    = { showRemindersDialog = true }
                    )
                }

                // ── Historial médico ──────────────────────────────────────────
                Text(
                    "Historial Médico",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Búsqueda
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder   = { Text("Buscar por título, producto…") },
                    leadingIcon   = { Icon(Icons.Default.Search, null) },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    singleLine    = true
                )

                // Chips de tipo
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TIPOS_REGISTRO_GANADO) { tipo ->
                        FilterChip(
                            selected = tipo == tipoSeleccionado,
                            onClick  = { tipoSeleccionado = tipo },
                            label    = { Text(tipo) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (tipo == "Todos")
                                    MaterialTheme.colorScheme.secondary
                                else
                                    colorPorTipoGanado(tipo).copy(alpha = 0.85f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Orden fecha
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Ordenar:", style = MaterialTheme.typography.labelMedium)
                    listOf("Reciente", "Antiguo").forEach { orden ->
                        FilterChip(
                            selected = orden == ordenFecha,
                            onClick  = { ordenFecha = orden },
                            label    = { Text(orden) }
                        )
                    }
                }

                // Contenido agrupado
                if (registrosAgrupados.isEmpty()) {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (searchQuery.isBlank() && tipoSeleccionado == "Todos")
                                "Sin registros médicos aún."
                            else
                                "Sin resultados para los filtros aplicados.",
                            color = Color.Gray
                        )
                    }
                } else {
                    registrosAgrupados.forEach { (tipo, registros) ->
                        GanadoMedicalRecordGroup(tipo = tipo, registros = registros)
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showRemindersDialog) {
        RemindersDialogGanado(
            recordatorios = recordatorios,
            onDismiss     = { showRemindersDialog = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cabecera con foto editable
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GanadoHeaderSection(ganado: Ganado?, ganadoRepository: GanadoRepository) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }

    val iconRes = when (ganado?.tipo?.lowercase()) {
        "bovino"  -> R.drawable.ic_bovino
        "porcino" -> R.drawable.ic_porcino
        "ovino"   -> R.drawable.ic_ovino
        "caprino" -> R.drawable.ic_caprino
        "equino"  -> R.drawable.ic_equino
        else      -> R.drawable.ic_pet
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            if (ganado != null) {
                scope.launch {
                    isUploading = true
                    ganadoRepository.uploadGanadoPhoto(ganado.id, selectedUri, context)
                        .onSuccess { url -> ganadoRepository.updateGanadoPhotoUrl(ganado.id, url) }
                    isUploading = false
                }
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable { galleryLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (!ganado?.foto_url.isNullOrBlank()) {
                if (isUploading) CircularWavyProgressIndicator()
                AsyncImage(
                    model            = "${ganado!!.foto_url}?t=${System.currentTimeMillis()}",
                    contentDescription = ganado.nombre ?: ganado.arete,
                    modifier         = Modifier.fillMaxSize(),
                    contentScale     = ContentScale.Crop
                )
            } else {
                Icon(
                    painter          = painterResource(iconRes),
                    contentDescription = null,
                    modifier         = Modifier.size(60.dp),
                    tint             = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            // Overlay editar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Icon(
                    imageVector      = Icons.Default.Edit,
                    contentDescription = "Cambiar foto",
                    tint             = Color.White,
                    modifier         = Modifier.padding(bottom = 8.dp).size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text       = ganado?.nombre ?: "Sin nombre",
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text  = "Arete: ${ganado?.arete ?: "—"}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text  = "${ganado?.tipo ?: ""} · ${ganado?.raza ?: ""}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tarjeta de información básica del animal
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GanadoInfoCard(ganado: Ganado?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Datos del Animal", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                InfoChip(label = "Edad", value = if (ganado?.edad != null) "${ganado.edad} años" else "N/D")
                InfoChip(label = "Peso", value = if (ganado?.peso != null) "${ganado.peso} kg" else "N/D")
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                InfoChip(label = "Raza", value = ganado?.raza ?: "N/D")
                InfoChip(
                    label = "Nac.",
                    value = ganado?.fecha_nacimiento?.take(10) ?: "N/D"
                )
            }
            // Genealogía (solo si existe)
            if (ganado?.madre_id != null || ganado?.padre_id != null) {
                HorizontalDivider()
                Text("Genealogía", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                if (ganado.madre_id != null) {
                    Text("Madre ID: ${ganado.madre_id.take(8)}…",
                        style = MaterialTheme.typography.bodySmall)
                }
                if (ganado.padre_id != null) {
                    Text("Padre ID: ${ganado.padre_id.take(8)}…",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Alerta de periodo de retiro
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RetiroAlertCard(registros: List<MedicalRecord>) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "⚠️ Periodo de retiro activo",
                fontWeight = FontWeight.Bold,
                color      = Color(0xFFE65100),
                style      = MaterialTheme.typography.titleSmall
            )
            registros.forEach { r ->
                Text(
                    "• ${r.titulo}: ${r.periodo_retiro_dias} días — ${r.producto_aplicado ?: "sin producto"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFBF360C)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Banner próximo recordatorio
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ReminderBannerWithButtonGanado(
    proxRecord    : MedicalRecord,
    cantidadTotal : Int,
    onVerTodos    : () -> Unit
) {
    Surface(
        color    = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier              = Modifier.padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier              = Modifier.weight(1f),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter            = painterResource(R.drawable.ic_location),
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.tertiary
                )
                Column {
                    Text(
                        "Próx. ${proxRecord.tipo_registro}",
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        proxRecord.proxima_cita ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            TextButton(onClick = onVerTodos) {
                Text("Ver todos ($cantidadTotal)")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Grupo colapsable por tipo
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GanadoMedicalRecordGroup(tipo: String, registros: List<MedicalRecord>) {
    var expandido by remember { mutableStateOf(true) }
    val color = colorPorTipoGanado(tipo)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(color.copy(alpha = 0.15f))
                .clickable { expandido = !expandido }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment          = Alignment.CenterVertically,
            horizontalArrangement      = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(10.dp).clip(CircleShape).background(color)
                )
                Text(tipo, fontWeight = FontWeight.Bold, color = color,
                    style = MaterialTheme.typography.titleSmall)
                Text("(${registros.size})", style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.7f))
            }
            Icon(
                imageVector = if (expandido) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null, tint = color
            )
        }

        AnimatedVisibility(visible = expandido, enter = expandVertically(), exit = shrinkVertically()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                registros.forEach { record -> GanadoMedicalRecordItem(record) }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Item individual del historial (con campos veterinarios)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GanadoMedicalRecordItem(record: MedicalRecord) {
    val context = LocalContext.current
    val color   = colorPorTipoGanado(record.tipo_registro)

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier         = Modifier.size(40.dp).clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(iconoPorTipoGanado(record.tipo_registro)),
                    contentDescription = null,
                    modifier           = Modifier.size(22.dp),
                    tint               = color
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(record.titulo, fontWeight = FontWeight.Bold)

                if (!record.fecha.isNullOrEmpty()) {
                    Text(record.fecha, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                if (!record.descripcion.isNullOrEmpty()) {
                    Text(record.descripcion, style = MaterialTheme.typography.bodySmall)
                }

                // ── Campos veterinarios ────────────────────────────────────
                if (!record.producto_aplicado.isNullOrEmpty()) {
                    VetInfoRow("Producto", record.producto_aplicado, color)
                }
                if (record.dosis != null && !record.unidad_dosis.isNullOrEmpty()) {
                    VetInfoRow("Dosis", "${record.dosis} ${record.unidad_dosis}", color)
                }
                if (!record.via_administracion.isNullOrEmpty()) {
                    VetInfoRow("Vía", record.via_administracion, color)
                }
                if (!record.lote_medicamento.isNullOrEmpty()) {
                    VetInfoRow("Lote", record.lote_medicamento, color)
                }
                if (!record.veterinario_nombre.isNullOrEmpty()) {
                    VetInfoRow("Vet.", record.veterinario_nombre, color)
                }
                if ((record.periodo_retiro_dias ?: 0) > 0) {
                    Surface(
                        color    = Color(0xFFFFF3E0),
                        shape    = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            "⚠️ Retiro: ${record.periodo_retiro_dias} días",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = Color(0xFFE65100),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (!record.proxima_cita.isNullOrEmpty()) {
                    Text(
                        "Próxima cita: ${record.proxima_cita}",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = color,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            // Botón de documento
            if (!record.documento_url.isNullOrEmpty()) {
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(record.documento_url))
                    context.startActivity(intent)
                }) {
                    Icon(painterResource(R.drawable.ic_document), "Ver documento",
                        tint = color, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
private fun VetInfoRow(label: String, value: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "$label:",
            style  = MaterialTheme.typography.labelSmall,
            color  = color.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
        Text(value, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialog de recordatorios
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RemindersDialogGanado(recordatorios: List<MedicalRecord>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Próximas citas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier            = Modifier.heightIn(max = 380.dp)
                ) {
                    items(recordatorios.mapIndexed { i, r -> i to r }) { (index, record) ->
                        val color = colorPorTipoGanado(record.tipo_registro)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (index == 0)
                                    color.copy(alpha = 0.12f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape)
                                        .background(color.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(painterResource(iconoPorTipoGanado(record.tipo_registro)),
                                        contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(record.titulo, fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium)
                                        if (index == 0) {
                                            Surface(color = color, shape = RoundedCornerShape(4.dp)) {
                                                Text("Próxima", style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    Text("${record.tipo_registro} · ${record.proxima_cita}",
                                        style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cerrar") }
            }
        }
    }
}
