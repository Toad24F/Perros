
// ── NfcWriteScreenWrapper.kt ─────────────────────────────────────────────
// Wrapper que carga la mascota antes de mostrar NfcWriteScreen
package com.example.huellasseguras.Screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.huellasseguras.data.PetsRepository
import com.example.huellasseguras.model.Pet

@Composable
fun NfcWriteScreenWrapper(petId: String, navController: NavController) {
    val petsRepository = remember { PetsRepository() }
    var pet by remember { mutableStateOf<Pet?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(petId) {
        petsRepository.loadPetData(petId)
            .onSuccess { pet = it }
            .onFailure { error = it.message }
        isLoading = false
    }

    when {
        isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }
        error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("Error: $error")
        }
        pet != null -> NfcWriteScreen(pet = pet!!, navController = navController)
    }
}