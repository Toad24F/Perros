package com.example.huellasseguras.Screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.huellasseguras.R
import com.example.huellasseguras.data.PetsRepository
import com.example.huellasseguras.model.Pet

/**
 * Pantalla que se abre cuando alguien escanea el collar NFC de una mascota perdida.
 * Muestra los datos de la mascota y el contacto del dueño.
 * Accesible sin login — la información es pública por diseño.
 */
@Composable
fun PetFoundScreen(petId: String) {
    val context = LocalContext.current
    val petsRepository = remember { PetsRepository() }

    var pet by remember { mutableStateOf<Pet?>(null) }
    var ownerName by remember { mutableStateOf<String?>(null) }
    var ownerPhone by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(petId) {
        // Cargar datos de la mascota
        petsRepository.loadPetData(petId)
            .onSuccess { loadedPet ->
                pet = loadedPet
                // Cargar datos públicos del dueño usando el user_id de la mascota
                petsRepository.loadOwnerPublicData(loadedPet.user_id)
                    .onSuccess { ownerData ->
                        ownerName = ownerData.first
                        ownerPhone = ownerData.second
                    }
            }
            .onFailure { errorMessage = "No se pudo cargar la información: ${it.message}" }

        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(top = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(top = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "Error desconocido",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
            pet != null -> {
//                // Banner "¡MASCOTA ENCONTRADA!"
//                Card(
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(16.dp),
//                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
//                ) {
//                    Row(
//                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.Center
//                    ) {
//
//                        Text("🐾", style = MaterialTheme.typography.headlineMedium)
//                        Spacer(Modifier.width(8.dp))
//                        Column {
//                            Text(
//                                "¡Mascota encontrada!",
//                                style = MaterialTheme.typography.titleMedium,
//                                fontWeight = FontWeight.ExtraBold,
//                                color = Color(0xFFE65100)
//                            )
//                            Text(
//                                "Por favor contacta al dueño",
//                                style = MaterialTheme.typography.bodySmall,
//                                color = Color(0xFFBF360C)
//                            )
//                        }
//                    }
//                }

                Spacer(Modifier.height(28.dp))

                // Foto de la mascota
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!pet!!.foto_url.isNullOrBlank()) {
                        AsyncImage(
                            model = pet!!.foto_url,
                            contentDescription = pet!!.nombre,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val iconRes = when (pet!!.tipo.lowercase()) {
                            "perro" -> R.drawable.ic_dog
                            "gato" -> R.drawable.ic_cat
                            else -> R.drawable.ic_pet
                        }
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Nombre de la mascota
                Text(
                    text = pet!!.nombre,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${pet!!.tipo}${if (!pet!!.raza.isNullOrBlank()) " · ${pet!!.raza}" else ""}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(Modifier.height(28.dp))

                // Tarjeta de información de la mascota
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Información de la mascota",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))

                        InfoRow(
                            label = "Sexo",
                            value = pet!!.sexo,
                            icon = R.drawable.ic_pet
                        )
                        Spacer(Modifier.height(10.dp))
                        InfoRow(
                            label = "Edad",
                            value = "${pet!!.edad ?: "?"} años",
                            icon = R.drawable.ic_pet
                        )
                        if (pet!!.peso != null) {
                            Spacer(Modifier.height(10.dp))
                            InfoRow(
                                label = "Peso",
                                value = "${pet!!.peso} kg",
                                icon = R.drawable.ic_pet
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Tarjeta del dueño
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Contacto del dueño",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                        Spacer(Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_person),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = ownerName ?: "Dueño",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!ownerPhone.isNullOrBlank()) {
                                    Text(
                                        text = ownerPhone!!,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Botón de llamada
                if (!ownerPhone.isNullOrBlank()) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${ownerPhone}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_phone),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Llamar al dueño",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, icon: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}