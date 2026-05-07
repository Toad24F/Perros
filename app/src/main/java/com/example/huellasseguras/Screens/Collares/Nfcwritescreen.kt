package com.example.huellasseguras.Screens.Collares

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.huellasseguras.Nfc.NfcManager
import com.example.huellasseguras.R
import com.example.huellasseguras.model.Ganado

private enum class NfcWriteState { WAITING, SUCCESS, ERROR, NO_NFC }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NfcWriteScreen(ganado: Ganado, navController: NavController) {
    val context = LocalContext.current
    val activity = context as? Activity

    var writeState by remember { mutableStateOf(NfcWriteState.WAITING) }
    var errorMsg by remember { mutableStateOf("") }

    // Verificar si el dispositivo tiene NFC
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }

    // Animación de pulso para el ícono NFC
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Activar el foreground dispatch para detectar la etiqueta NFC
    DisposableEffect(writeState) {
        if (writeState != NfcWriteState.WAITING || nfcAdapter == null || activity == null) {
            return@DisposableEffect onDispose {}
        }
        if (!nfcAdapter.isEnabled) {
            writeState = NfcWriteState.NO_NFC
            return@DisposableEffect onDispose {}
        }

        // Intent para que el sistema nos avise cuando detecte una etiqueta
        val intent = Intent(activity, activity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("NFC_WRITE_GANADO_ID", ganado.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            activity, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, null, null)

        onDispose {
            try { nfcAdapter.disableForegroundDispatch(activity) } catch (_: Exception) {}
        }
    }

    // Callback para procesar la etiqueta detectada — llamado desde MainActivity
    // Se registra en el LocalActivity a través del intent extra "NFC_TAG"
    DisposableEffect(Unit) {
        val listener: (Tag) -> Unit = { tag ->
            val result = NfcManager.writeToTag(tag, ganado.id)
            writeState = if (result.isSuccess) {
                NfcWriteState.SUCCESS
            } else {
                errorMsg = result.exceptionOrNull()?.message ?: "Error desconocido"
                NfcWriteState.ERROR
            }
        }
        NfcWriteRegistry.register(ganado.id, listener)
        onDispose { NfcWriteRegistry.unregister(ganado.id) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Configurar NFC") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (writeState) {
                NfcWriteState.WAITING -> {
                    // Círculo animado con ícono NFC
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .scale(scale)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nfc),
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Text(
                        text = "Acerca el teléfono a la etiqueta NFC del collar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Se configurará para mostrar los datos de ${ganado.nombre}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar")
                    }
                }

                NfcWriteState.SUCCESS -> {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(Color(0xFF4CAF50).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nfc),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color(0xFF4CAF50)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "¡Collar configurado!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "El collar de ${ganado.nombre} ya puede ser escaneado.\n" +
                                "Cualquier usuario con la app puede acercar su teléfono " +
                                "al collar para ver su información.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Listo")
                    }
                }

                NfcWriteState.ERROR -> {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nfc),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Error al escribir",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = { writeState = NfcWriteState.WAITING },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reintentar")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar")
                    }
                }

                NfcWriteState.NO_NFC -> {
                    Text(
                        text = "Este dispositivo no tiene NFC o está desactivado.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { navController.popBackStack() }) { Text("Regresar") }
                }
            }
        }
    }
}

/**
 * Registro global para callbacks de escritura NFC.
 * MainActivity lo llama cuando detecta una etiqueta con el petId correcto.
 */
object NfcWriteRegistry {
    private val listeners = mutableMapOf<String, (Tag) -> Unit>()

    fun register(ganadoId: String, listener: (Tag) -> Unit) {
        listeners[ganadoId] = listener
    }

    fun unregister(ganadiId: String) {
        listeners.remove(ganadiId)
    }

    fun dispatch(tag: Tag) {
        listeners.values.forEach { it(tag) }
    }
}