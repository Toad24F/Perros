package com.example.huellasseguras.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.huellasseguras.R
import com.example.huellasseguras.data.PetsRepository
import com.example.huellasseguras.data.medicHistoryRepository
import com.example.huellasseguras.model.MedicalRecord
import com.example.huellasseguras.model.Pet
import androidx.navigation.NavController as NavController1

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PetProfileScreen(petId: String, navController: NavController1) {
    val scope = rememberCoroutineScope()
    val petsRepository = remember { PetsRepository() }
    val medicHistoryRepository = remember { medicHistoryRepository() }

    var pet by remember { mutableStateOf<Pet?>(null) }
    var medicalRecords by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Carga de datos inicial
    LaunchedEffect(petId) {
        val petResult = petsRepository.loadPetData(petId)
        val recordsResult = medicHistoryRepository.loadMedicalRecords(petId)

        petResult.onSuccess { pet = it }
        recordsResult.onSuccess { medicalRecords = it }

        isLoading = false
    }

    Scaffold(
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
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cabecera: Foto y Nombre
                PetHeaderSection(pet)

                // Alerta de Alergias (Solo si existen)
                val alergias = medicalRecords.filter { it.tipo_registro == "Alergia" }
                if (alergias.isNotEmpty()) {
                    AlertSection(titulo = "Alergias Detectadas", items = alergias.map { it.titulo })
                }

                // Recordatorio de Próxima Cita (Desparasitación/Vacuna)
                val proximoRecordatorio = medicalRecords
                    .filter { !it.proxima_cita.isNullOrEmpty() }
                    .minByOrNull { it.proxima_cita!! }
                if (proximoRecordatorio != null) {
                    ReminderBanner(proximoRecordatorio)
                }

                // Información Básica
                InfoBasicCard(pet)

                // Historial Médico Categorizado
                Text(
                    text = "Historial Médico",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Bold
                )

                if (medicalRecords.isEmpty()) {
                    Text("No hay registros médicos aún.", color = Color.Gray)
                } else {
                    medicalRecords.forEach { record ->
                        MedicalRecordItem(record)
                    }
                }
            }
        }
    }
}

@Composable
fun PetHeaderSection(pet: Pet?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!pet?.foto_url.isNullOrEmpty()) {
                AsyncImage(
                    model = pet?.foto_url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_dog),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(pet?.nombre ?: "", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(pet?.tipo ?: "", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
    }
}

@Composable
fun AlertSection(titulo: String, items: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(titulo, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            items.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
fun ReminderBanner(record: MedicalRecord) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_location), contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(8.dp))
            Text(
                "Próxima ${record.tipo_registro}: ${record.proxima_cita}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
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
                Text("Sexo: ${pet?.sexo ?: "No especificada"}", Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun MedicalRecordItem(record: MedicalRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when(record.tipo_registro) {
                "Vacuna" -> R.drawable.ic_pets
                "Desparasitacion" -> R.drawable.ic_leash
                "Peso" -> R.drawable.ic_profile
                else -> R.drawable.ic_pet
            }
            Icon(painterResource(icon), contentDescription = null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(record.titulo, fontWeight = FontWeight.Bold)
                Text(record.fecha ?: "", style = MaterialTheme.typography.labelSmall)
                if (!record.descripcion.isNullOrEmpty()) {
                    Text(record.descripcion, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!record.documento_url.isNullOrEmpty()) {
                IconButton(onClick = { /* Abrir URL */ }) {
                    Icon(painterResource(R.drawable.ic_community), "Documento")
                }
            }
        }
    }
}