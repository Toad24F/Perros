package com.example.huellasseguras.Screens

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.huellasseguras.Bluetooth.BluetoothManager
import com.example.huellasseguras.R
import com.example.huellasseguras.data.PetsRepository
import com.example.huellasseguras.model.Pet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CollaresAdminScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showDeviceDialog by remember { mutableStateOf(false) }
    var selectedPetForLinking by remember { mutableStateOf<Pet?>(null) }

    // Estados de la UI
    var isScanning by remember { mutableStateOf(false) }
    var dispositivosEncontrados = remember { mutableStateListOf<BluetoothDevice>() } // Necesitarás importar android.bluetooth.BluetoothDevice
    var mascotas = remember { mutableStateListOf<Pet>() }
    var isLoadingMascotas by remember { mutableStateOf(true) }

    // Cargar mascotas al iniciar (usando tu repositorio existente)
    val petsRepository = remember { PetsRepository() }
    val sharedPref = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val userId = sharedPref.getString("user_id", "") ?: ""
    val bleManager = remember { BluetoothManager(context) }
    val dispositivos = bleManager.dispositivosEncontrados

    LaunchedEffect(Unit) {
        val result = petsRepository.loadPets(userId) // Asumiendo que tienes esta función en el repo
        result.onSuccess {
            mascotas.clear()
            mascotas.addAll(it)
            isLoadingMascotas = false
        }
    }
    // Control del Escaneo
    LaunchedEffect(isScanning) {
        if (isScanning) {
            bleManager.startScanning()
            delay(10000)
            isScanning = false
            bleManager.stopScanning()
        } else {
            bleManager.stopScanning()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Administrar Collares", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    // Botón para refrescar/escanear
                    IconButton(onClick = { isScanning = !isScanning }) {
                        Icon(
                            painter = painterResource(id = if (isScanning) R.drawable.ic_stop else R.drawable.ic_refresh),
                            contentDescription = "Escanear",
                            tint = if (isScanning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- SECCIÓN 1: ESTADO DEL ESCANER ---
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isScanning) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Buscando collares cercanos...")
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_bluetooth), // Solución aquí
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.Unspecified // 👈 Esto evita que Compose aplique un tint
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Escáner detenido")
                        }
                    }
                }
            }

            // --- SECCIÓN 2: LISTA DE MASCOTAS ---
            item {
                Text("Tus Mascotas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (isLoadingMascotas) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            items(mascotas) { pet ->
                CollarPetCard(
                    pet = pet,
                    isScanning = isScanning,
                    onLinkClick = {
                        // 👈 ACTUALIZADO: Guardamos la mascota y abrimos el diálogo
                        selectedPetForLinking = pet
                        showDeviceDialog = true
                        isScanning = true
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
        if (showDeviceDialog) {
            AlertDialog(
                onDismissRequest = { showDeviceDialog = false },
                title = { Text("Selecciona el Collar para ${selectedPetForLinking?.nombre}") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (dispositivos.isEmpty()) {
                            Text("Buscando collares cercanos...", style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                                items(dispositivos) { dispositivo ->
                                    Text(
                                        text = dispositivo.name ?: "Collar Desconocido",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                // PRÓXIMO PASO: Vincular selectedPetForLinking con este dispositivo
                                                val petId = selectedPetForLinking?.id ?: ""

                                                // Llamamos a la conexión
                                                bleManager.conectarYEnviarId(dispositivo.address, petId) { success ->
                                                    scope.launch {
                                                        if (success) {
                                                            // Aquí podrías mostrar un aviso de "Vinculado con éxito"
                                                            showDeviceDialog = false
                                                        } else {
                                                            // Aviso de error
                                                        }
                                                    }
                                                }
                                                showDeviceDialog = false
                                                isScanning = false
                                            }
                                            .padding(16.dp),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDeviceDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}


@Composable
fun CollarPetCard(pet: Pet, isScanning: Boolean, onLinkClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto de la mascota (usando tu lógica de Coil)
            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.LightGray)) {
                AsyncImage(
                    model = pet.foto_url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_dog) // Icono por defecto
                )
            }

            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(text = pet.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = if (false) "Conectado al collar" else "Sin collar vinculado",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (false) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline
                )
            }

            Button(
                onClick = onLinkClick,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("Vincular")
            }
        }
    }
}