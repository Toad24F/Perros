package com.example.huellasseguras.Screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.huellasseguras.R
import com.example.huellasseguras.data.PetsRepository
import com.example.huellasseguras.model.Pet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Mascotas(navController: NavController) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val userId = remember { sharedPref.getString("user_id", "") ?: "" }
    val userName by remember { mutableStateOf(sharedPref.getString("user_name", "Usuario") ?: "Usuario") }
    var searchText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // Estado para las mascotas del usuario
    val userPets = remember { mutableStateListOf<Pet>() }
    val scope = rememberCoroutineScope()
    val petsRepository = remember { PetsRepository() }

    // Cargar mascotas al iniciar
    LaunchedEffect( true) {
        if (userId.isNotEmpty()) {
            isLoading = true
                val result = petsRepository.loadPets(userId)
                result.onSuccess { pets ->
                    userPets.clear()
                    userPets.addAll(pets)
                    isLoading = false
                }
                result.onFailure {
                    errorMessage = it.message
                    isLoading = false
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Foto de perfil del usuario
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_person),
                contentDescription = "Foto de perfil",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Saludo al usuario
        Text(
            text = "Hola, $userName!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Barra de búsqueda
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Buscar mascota") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Mostrar error si hay
        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
            Button(
                onClick = {
                    scope.launch {
                        val result = petsRepository.loadPets(userId)
                        result.onSuccess { pets ->
                            userPets.clear()
                            userPets.addAll(pets)
                        }
                        result.onFailure {
                            errorMessage = it.message
                        }

                    }
                }
            ) {
                Text("Cargar mascotas")
            }
        }

        // Mostrar loading
        if (isLoading) {
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Lista de mascotas
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(userPets.filter {
                it.nombre.contains(searchText, ignoreCase = true) ||
                        it.tipo.contains(searchText, ignoreCase = true)
            }) { pet ->
                PetItem(pet = pet) {
                    navController.navigate("petDetail/${pet.id}")
                    println(pet.id)
                }
            }

            item {
                AddPetButton {
                        navController.navigate("addPet")
                }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PetItem(pet: Pet, onClick: () -> Unit) {
    // Mostrar loading
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        // Icono según el tipo de mascota
        val iconRes = when(pet.tipo.lowercase()) {
            "perro" -> R.drawable.ic_dog
            "gato" -> R.drawable.ic_cat
            "ave" -> R.drawable.ic_bird
            "pez" -> R.drawable.ic_fish
            "reptil" -> R.drawable.ic_reptile
            else -> R.drawable.ic_pet
        }

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            // Lógica para decidir si mostrar FOTO o ICONO
            if (!pet.foto_url.isNullOrBlank()) {
                CircularWavyProgressIndicator()
                AsyncImage(
                    model = "${pet.foto_url}?t=${System.currentTimeMillis()}",
                    contentDescription = pet.nombre,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = pet.nombre,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = pet.nombre,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = pet.tipo,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

    }
}

@Composable
fun AddPetButton(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick )
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = "Agregar mascota",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Agregar",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}



