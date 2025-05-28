package com.example.perros

import android.content.Context
import android.util.Log
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Composable
fun Mascotas(navController: NavController) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val userId by remember { mutableStateOf(sharedPref.getInt("user_id", 0)) }
    val userName by remember { mutableStateOf(sharedPref.getString("user_name", "Nombre")) }
    var searchText by remember { mutableStateOf("") }
    var showAddPetDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Estado para las mascotas del usuario
    val userPets = remember { mutableStateListOf<Pet>() }
    val scope = rememberCoroutineScope()

    // Función para cargar mascotas
    fun loadPets() {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null

                val pets = supabase.postgrest["mascotas"]
                    .select(columns = Columns.list("id","user_id","nombre","tipo","raza","edad","peso")) {
                        eq("user_id", userId)
                    }
                    .decodeList<Pet>()

                userPets.clear()
                userPets.addAll(pets)
            } catch (e: Exception) {
                errorMessage = "Error al cargar mascotas: ${e.message}"
                Log.e("Mascotas", "Error al cargar", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Cargar mascotas al iniciar o cuando se agrega una nueva
    LaunchedEffect(userId) {
        loadPets()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ... (código existente del perfil y saludo)
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
                painter = painterResource(id = R.drawable.ic_person), // Reemplaza con tu ícono
                contentDescription = "Foto de perfil",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(90.dp)
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
            Button(onClick = { loadPets() }) {
                Text("Reintentar")
            }
        }

        // Mostrar loading
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(24.dp))
        }

        // Lista de mascotas
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(userPets) { pet ->
                PetItem(pet = pet) {
                    // Acción al hacer clic en una mascota
                    navController.navigate("petDetail/${pet.id}")
                }
            }

            item {
                AddPetButton {
                    showAddPetDialog = true
                }
            }
        }
    }

    // Diálogo para agregar mascota
    if (showAddPetDialog) {
        AddPetForm(
            onDismiss = { showAddPetDialog = false },
            onSave = { newPet ->
                scope.launch {
                    try {
                        // Insertar en Supabase
                        val insertedPet = supabase.postgrest["mascotas"]
                            .insert(newPet)
                        loadPets()
                    } catch (e: Exception) {
                        errorMessage = "Error al agregar mascota: ${e.message}"
                        Log.e("Mascotas", "Error al agregar", e)
                    }
                }
            },
            userId = userId
        )
    }
}
@Composable
fun PetItem(pet: Pet, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        // Icono diferente según el tipo de mascota
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
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = pet.nombre,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = pet.nombre,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // Mostrar tipo debajo
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
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add), // Reemplaza con tu ícono
                contentDescription = "Agregar mascota",
                modifier = Modifier.size(90.dp),
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPetForm(
    onDismiss: () -> Unit,
    onSave: (Pet) -> Unit,
    userId: Int
) {
    var nombre by remember { mutableStateOf("") }
    val userId = userId
    var edad by remember { mutableStateOf("") }
    var peso by remember { mutableStateOf("") }
    var tipoSeleccionado by remember { mutableStateOf("") }
    var razaSeleccionada by remember { mutableStateOf("") }

    // Tipos de mascotas y sus razas
    val tiposMascotas = mapOf(
        "Perro" to listOf("Labrador", "Golden Retriever", "Bulldog", "Poodle", "Beagle",
            "Chihuahua", "Pastor Alemán", "Boxer", "Dálmata", "Husky",
            "Pug", "Rottweiler", "Shih Tzu", "Doberman", "Gran Danés"),
        "Gato" to listOf("Siamés", "Persa", "Maine Coon", "Bengalí", "Esfinge",
            "Ragdoll", "British Shorthair", "Scottish Fold", "Siberiano",
            "Azul Ruso", "Abisinio", "Birmano", "Angora", "Bombay", "Savannah"),
        "Ave" to listOf("Canario", "Periquito", "Loro", "Cacatúa", "Agapornis",
            "Guacamayo", "Ninfa", "Diamante Mandarín", "Jilguero", "Cotorra",
            "Tucán", "Pinzón", "Cardenal", "Mirlo", "Cisne"),
        "Roedor" to listOf("Hámster", "Cobaya", "Ratón", "Rata", "Jerbo",
            "Chinchilla", "Degú", "Ardilla", "Conejo", "Huron",
            "Erizo", "Marmota", "Castor", "Nutria", "Capibara"),
        "Reptil" to listOf("Iguana", "Tortuga", "Serpiente", "Camaleón", "Gecko",
            "Dragón Barbudo", "Tegu", "Anolis", "Cocodrilo", "Tuatara",
            "Basilisco", "Monstruo de Gila", "Varano", "Boa", "Pitón"),
        "Pez" to listOf("Goldfish", "Betta", "Guppy", "Tetra", "Ángel",
            "Disco", "Cíclido", "Pez Gato", "Payaso", "Tiburón Bala",
            "Molly", "Platy", "Piraña", "Pez Globo", "Caballito de Mar"),
        "Anfibio" to listOf("Rana", "Salamandra", "Tritón", "Ajolote", "Cecilia",
            "Sapo", "Rana Arborícola", "Rana Dardo", "Rana Toro", "Rana Vidrio",
            "Rana Flecha", "Salamandra Gigante", "Tritón Pigmeo", "Sapo de Espuelas", "Rana de Cristal"),
        "Arácnido" to listOf("Tarántula", "Viuda Negra", "Escorpión", "Araña Saltarina", "Araña Lobo",
            "Araña Tigre", "Araña de Seda Dorada", "Araña de Jardín", "Araña Pescadora", "Araña Patona",
            "Araña de Rincón", "Araña Cangrejo", "Araña Saltadora", "Araña Pavo Real", "Araña Trampera"),
        "Insecto" to listOf("Mantis", "Escarabajo", "Mariposa", "Grillo", "Saltamontes",
            "Abeja", "Hormiga", "Libélula", "Cigarra", "Mariquita",
            "Cucaracha", "Polilla", "Termita", "Mosca", "Mosquito"),
        "Otro" to listOf("Hurón", "Conejo", "Cerdo Vietnamita", "Zorro", "Mapache",
            "Canguro", "Panda Rojo", "Suricata", "Zorrillo", "Armadillo",
            "Puercoespín", "Mofeta", "Tejón", "Nutria", "Erizo")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar nueva mascota") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la mascota") },
                    modifier = Modifier.fillMaxWidth()
                )

                var expandedTipo by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedTipo,
                    onExpandedChange = { expandedTipo = !expandedTipo }
                ) {
                    OutlinedTextField(
                        value = tipoSeleccionado,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de mascota") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTipo,
                        onDismissRequest = { expandedTipo = false }
                    ) {
                        tiposMascotas.keys.forEach { tipo ->
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
                            label = { Text("Raza") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRaza) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedRaza,
                            onDismissRequest = { expandedRaza = false }
                        ) {
                            tiposMascotas[tipoSeleccionado]?.forEach { raza ->
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

                OutlinedTextField(
                    value = edad,
                    onValueChange = { if (it.all { char -> char.isDigit() }) edad = it },
                    label = { Text("Edad (años)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = peso,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            peso = it
                        }
                    },
                    label = { Text("Peso (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newPet = Pet(
                        nombre = nombre,
                        user_id = userId,
                        edad = edad.toIntOrNull(),
                        raza = razaSeleccionada.ifEmpty { null },
                        peso = peso.toFloatOrNull(),
                        tipo = tipoSeleccionado,
                    )
                    onSave(newPet)
                    onDismiss()
                },
                enabled = nombre.isNotBlank() && tipoSeleccionado.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
@Serializable
data class Pet(
    val id: Int? = null,
    val nombre: String,
    val user_id: Int,
    val edad: Int?,
    val raza: String?,
    val peso: Float?,
    val tipo: String,
)
