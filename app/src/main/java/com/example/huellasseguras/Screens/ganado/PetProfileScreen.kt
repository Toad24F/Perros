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
import coil.compose.AsyncImage
import com.example.huellasseguras.R
import com.example.huellasseguras.data.PetsRepository
import com.example.huellasseguras.data.medicHistoryRepository
import com.example.huellasseguras.model.MedicalRecord
import com.example.huellasseguras.model.Pet
import kotlinx.coroutines.launch
import androidx.navigation.NavController as NavController1

//Tipos disponibles (ajusta si añades más en Supabase)
val TIPOS_REGISTRO = listOf("Todos", "Vacuna", "Desparasitacion", "Alergia", "Peso", "Consulta", "Otro")

// Colores por tipo
fun colorPorTipo(tipo: String): Color = when (tipo) {
    "Vacuna"          -> Color(0xFF4CAF50)
    "Desparasitacion" -> Color(0xFF2196F3)
    "Alergia"         -> Color(0xFFF44336)
    "Peso"            -> Color(0xFFFF9800)
    "Consulta"        -> Color(0xFF9C27B0)
    else              -> Color(0xFF607D8B)
}

// Iconos por tipo
fun iconoPorTipo(tipo: String): Int = when (tipo) {
    "Vacuna"          -> R.drawable.ic_syringe
    "Desparasitacion" -> R.drawable.ic_deworming
    "Peso"            -> R.drawable.ic_scales
    "Alergia"         -> R.drawable.ic_allergy
    else              -> R.drawable.ic_pet
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PetProfileScreen(petId: String, navController: NavController1) {
    val petsRepository        = remember { PetsRepository() }
    val medicHistoryRepository = remember { medicHistoryRepository() }

    var pet            by remember { mutableStateOf<Pet?>(null) }
    var medicalRecords by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var isLoading      by remember { mutableStateOf(true) }
    var errorMessage   by remember { mutableStateOf<String?>(null) }

    // Filtros
    var searchQuery        by remember { mutableStateOf("") }
    var tipoSeleccionado   by remember { mutableStateOf("Todos") }
    var ordenFecha         by remember { mutableStateOf("Reciente") }  // "Reciente" | "Antiguo"
    var showRemindersDialog by remember { mutableStateOf(false) }

    LaunchedEffect(petId) {
        val petResult     = petsRepository.loadPetData(petId)
        val recordsResult = medicHistoryRepository.loadMedicalRecords(petId)

        petResult.onSuccess     { pet = it }
            .onFailure          { errorMessage = it.message }
        recordsResult.onSuccess { medicalRecords = it }
            .onFailure          { if (errorMessage == null) errorMessage = it.message }

        isLoading = false
    }

    // Registros filtrados
    val registrosFiltrados = remember(medicalRecords, searchQuery, tipoSeleccionado, ordenFecha) {
        medicalRecords
            .filter { record ->
                val coincideTipo  = tipoSeleccionado == "Todos" || record.tipo_registro == tipoSeleccionado
                val coincideBusq  = searchQuery.isBlank() ||
                        record.titulo.contains(searchQuery, ignoreCase = true) ||
                        record.descripcion?.contains(searchQuery, ignoreCase = true) == true
                coincideTipo && coincideBusq
            }
            .sortedWith(compareBy {
                val fecha = it.fecha ?: ""
                if (ordenFecha == "Reciente") -fecha.hashCode() else fecha.hashCode()
            })
    }

    // Agrupados por tipo (para la vista de secciones)
    val registrosAgrupados = remember(registrosFiltrados) {
        registrosFiltrados.groupBy { it.tipo_registro }
    }

    // Recordatorios (todos los que tienen proxima_cita), ordenados por fecha
    val recordatorios = remember(medicalRecords) {
        medicalRecords
            .filter { !it.proxima_cita.isNullOrEmpty() }
            .sortedBy { it.proxima_cita }
    }
    val proximoRecordatorio = recordatorios.firstOrNull()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("addMedicalRecord/$petId") }) {
                Icon(Icons.Default.Add, "Agregar registro")
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Perfil de ${pet?.nombre ?: "Mascota"}") },
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
                // Cabecera
                PetHeaderSection(pet)

                // Info básica
                InfoBasicCard(pet)

                // Banner recordatorio + botón
                if (proximoRecordatorio != null) {
                    ReminderBannerWithButton(
                        proxRecord      = proximoRecordatorio,
                        cantidadTotal   = recordatorios.size,
                        onVerTodos      = { showRemindersDialog = true }
                    )
                }

                //Alergias
                val alergias = medicalRecords.filter { it.tipo_registro == "Alergia" }
                if (alergias.isNotEmpty()) {
                    AlertSection(titulo = "Alergias Detectadas", items = alergias.map { it.titulo })
                }

                //Historial médico
                Text(
                    "Historial Médico",
                    style    = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                // Búsqueda
                OutlinedTextField(
                    value           = searchQuery,
                    onValueChange   = { searchQuery = it },
                    placeholder     = { Text("Buscar en historial…") },
                    leadingIcon     = { Icon(Icons.Default.Search, null) },
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(12.dp),
                    singleLine      = true
                )

                // Filtro por tipo (chips)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TIPOS_REGISTRO) { tipo ->
                        val seleccionado = tipo == tipoSeleccionado
                        FilterChip(
                            selected  = seleccionado,
                            onClick   = { tipoSeleccionado = tipo },
                            label     = { Text(tipo) },
                            colors    = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (tipo == "Todos")
                                    MaterialTheme.colorScheme.secondary
                                else
                                    colorPorTipo(tipo).copy(alpha = 0.85f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Filtro orden fecha
                Row(
                    verticalAlignment    = Alignment.CenterVertically,
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

                //Contenido: agrupado
                if (registrosAgrupados.isEmpty()) {
                    Box(
                        modifier            = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment    = Alignment.Center
                    ) {
                        Text(
                            if (searchQuery.isBlank() && tipoSeleccionado == "Todos")
                                "No hay registros médicos aún."
                            else
                                "Sin resultados para los filtros aplicados.",
                            color = Color.Gray
                        )
                    }
                } else {
                    registrosAgrupados.forEach { (tipo, registros) ->
                        MedicalRecordGroup(tipo = tipo, registros = registros)
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Popup de recordatorios
    if (showRemindersDialog) {
        RemindersDialog(
            recordatorios = recordatorios,
            onDismiss     = { showRemindersDialog = false }
        )
    }
}

// Sección de grupo colapsable por tipo
@Composable
fun MedicalRecordGroup(tipo: String, registros: List<MedicalRecord>) {
    var expandido by remember { mutableStateOf(true) }
    val color = colorPorTipo(tipo)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Encabezado del grupo (tap para colapsar)
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    tipo,
                    fontWeight = FontWeight.Bold,
                    color      = color,
                    style      = MaterialTheme.typography.titleSmall
                )
                Text(
                    "(${registros.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = if (expandido) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = color
            )
        }

        // Registros del grupo
        AnimatedVisibility(
            visible = expandido,
            enter   = expandVertically(),
            exit    = shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                registros.forEach { record ->
                    MedicalRecordItem(record)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
// Banner recordatorio con botón "Ver todos"
@Composable
fun ReminderBannerWithButton(
    proxRecord    : MedicalRecord,
    cantidadTotal : Int,
    onVerTodos    : () -> Unit
) {
    Surface(
        color    = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier              = Modifier.padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painterResource(R.drawable.ic_location),
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.tertiary
                )
                Column {
                    Text(
                        "Próxima ${proxRecord.tipo_registro}",
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
// Dialog con todos los recordatorios
@Composable
fun RemindersDialog(
    recordatorios : List<MedicalRecord>,
    onDismiss     : () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape    = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Próximas citas",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier            = Modifier.heightIn(max = 380.dp)
                ) {
                    items(recordatorios.mapIndexed { i, r -> i to r }) { (index, record) ->
                        ReminderDialogItem(record = record, isProximo = index == 0)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick  = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}

@Composable
fun ReminderDialogItem(record: MedicalRecord, isProximo: Boolean) {
    val color = colorPorTipo(record.tipo_registro)
    Card(
        colors   = CardDefaults.cardColors(
            containerColor = if (isProximo)
                color.copy(alpha = 0.12f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(iconoPorTipo(record.tipo_registro)),
                    contentDescription = null,
                    tint               = color,
                    modifier           = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(record.titulo, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    if (isProximo) {
                        Surface(
                            color = color,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "Próxima",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    "${record.tipo_registro} · ${record.proxima_cita}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}

// Componentes existentes
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PetHeaderSection(pet: Pet?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    val petsRepository = remember { PetsRepository() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // Launcher para seleccionar la imagen
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            scope.launch {
                // 3. Si hay foto, subirla usando el ID de la mascota creada
                if (pet != null) {
                    isLoading = true
                    val uploadResult = petsRepository.uploadPetPhoto(pet!!.id,
                        selectedUri, context)
                    uploadResult.onSuccess { photoUrl ->
                        // 4. Actualizar la mascota con su nueva URL de foto
                        petsRepository.updatePetPhotoUrl(pet!!.id, photoUrl)
                        //pet = pet.copy(foto_url = photoUrl)
                        isLoading = false
                    }.onFailure {
                        errorMessage = "Error al subir la imagen"
                        isLoading = false
                    }
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
            if (!pet?.foto_url.isNullOrEmpty()) {
                CircularWavyProgressIndicator()
                AsyncImage(
                    model = "${pet.foto_url}?t=${System.currentTimeMillis()}",
                    contentDescription = null,
                    modifier         = Modifier.fillMaxSize(),
                    contentScale     = ContentScale.Crop
                )
            } else {
                Icon(
                    painter          = painterResource(id = R.drawable.ic_dog),
                    contentDescription = null,
                    modifier         = Modifier.size(60.dp),
                    tint             = MaterialTheme.colorScheme.onPrimaryContainer
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
        Spacer(Modifier.height(8.dp))
        Text(pet?.nombre ?: "", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(pet?.tipo   ?: "", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
    }
}

@Composable
fun AlertSection(titulo: String, items: List<String>) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(titulo, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            items.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
fun InfoBasicCard(pet: Pet?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Información de Salud", fontWeight = FontWeight.Bold)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Edad: ${pet?.edad ?: "N/A"} años")
                Text("Peso: ${pet?.peso ?: "N/A"} kg")
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Raza: ${pet?.raza ?: "No especificada"}", Modifier.padding(top = 4.dp))
                Text("Sexo: ${pet?.sexo ?: "No especificado"}", Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun MedicalRecordItem(record: MedicalRecord) {
    val context = LocalContext.current
    val color = colorPorTipo(record.tipo_registro)
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(iconoPorTipo(record.tipo_registro)),
                    contentDescription = null,
                    modifier           = Modifier.size(22.dp),
                    tint               = color
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(record.titulo, fontWeight = FontWeight.Bold)
                if (!record.fecha.isNullOrEmpty()) {
                    Text(record.fecha, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                if (!record.descripcion.isNullOrEmpty()) {
                    Text(record.descripcion, style = MaterialTheme.typography.bodySmall)
                }
                if (!record.proxima_cita.isNullOrEmpty()) {
                    Text(
                        "Próxima cita: ${record.proxima_cita}",
                        style  = MaterialTheme.typography.labelSmall,
                        color  = color,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (!record.documento_url.isNullOrEmpty()) {
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(record.documento_url))
                    context.startActivity(intent)
                }) {
                    Icon(painterResource(R.drawable.ic_document),
                        "Ver documento", tint = color,
                        modifier = Modifier.size(33.dp))
                }
            }
        }
    }
}