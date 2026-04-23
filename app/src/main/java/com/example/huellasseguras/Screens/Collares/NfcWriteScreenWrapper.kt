
// ── NfcWriteScreenWrapper.kt ─────────────────────────────────────────────
// Wrapper que carga la mascota antes de mostrar NfcWriteScreen
package com.example.huellasseguras.Screens.Collares

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
import com.example.huellasseguras.data.GanadoRepository
import com.example.huellasseguras.model.Ganado

@Composable
fun NfcWriteScreenWrapper(ganadoId: String, navController: NavController) {
    val GanadoRepository = remember { GanadoRepository() }
    var ganado by remember { mutableStateOf<Ganado?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(ganadoId) {
        GanadoRepository.loadGanadoById(ganadoId)
            .onSuccess { ganado = it }
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
        ganado != null -> NfcWriteScreen(ganado = ganado!!, navController = navController)
    }
}