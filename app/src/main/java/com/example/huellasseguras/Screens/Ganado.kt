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
import com.example.huellasseguras.data.GanadoRepository
import com.example.huellasseguras.model.Ganado
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Ganado(navController: NavController) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val userId = remember { sharedPref.getString("user_id", "") ?: "" }
    val userName by remember { mutableStateOf(sharedPref.getString("user_name", "Usuario") ?: "Usuario") }
    var searchText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val userGanado = remember { mutableStateListOf<Ganado>() }
    val scope = rememberCoroutineScope()
    val ganadoRepository = remember { GanadoRepository() }

    // Cargar ganado al iniciar
    LaunchedEffect( true) {
        if (userId.isNotEmpty()) {
            isLoading = true
            val result = ganadoRepository.loadGanado(userId)
            result.onSuccess { ganado ->
                userGanado.clear()
                userGanado.addAll(ganado)
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
            label = { Text("Buscar ganado") },
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
                        val result = ganadoRepository.loadGanado(userId)
                        result.onSuccess { ganado ->
                            userGanado.clear()
                            userGanado.addAll(ganado)
                            isLoading = false
                        }
                        result.onFailure {
                            errorMessage = it.message
                            isLoading = false
                        }

                    }
                }
            ) {
                Text("Cargar ganado")
            }
        }

        // Mostrar loading
        if (isLoading) {
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Lista de ganado
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(userGanado.filter {
                it.nombre!!.contains(searchText, ignoreCase = true) ||
                        it.tipo.contains(searchText, ignoreCase = true)
            }) { ganado ->
                GanadoItem(ganado = ganado) {
                    navController.navigate("ganadoDetail/${ganado.id}")
                    println(ganado.id)
                }
            }

            item {
                AddPetButton {
                        navController.navigate("addGanado")
                }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GanadoItem(ganado: Ganado, onClick: () -> Unit) {
    // Mostrar loading
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        // Icono según el tipo de mascota
        val iconRes = when(ganado.tipo.lowercase()) {
            "Bovino" -> R.drawable.ic_bovino
            "Porcino" -> R.drawable.ic_porcino
            "Ovino" -> R.drawable.ic_ovino
            "Caprino" -> R.drawable.ic_caprino
            "equino" -> R.drawable.ic_equino
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
            if (!ganado.foto_url.isNullOrBlank()) {
                CircularWavyProgressIndicator()
                AsyncImage(
                    model = "${ganado.foto_url}?t=${System.currentTimeMillis()}",
                    contentDescription = ganado.nombre,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = ganado.nombre,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = ganado.nombre.toString(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = ganado.tipo,
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



