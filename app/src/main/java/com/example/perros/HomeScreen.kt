package com.example.perros

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController


// --- Pantalla Principal (Home) ---
@Composable
fun HomeScreen(navController: NavController) {
    val tabs = listOf(
        TabItem("Mapa", R.drawable.ic_map),
        TabItem("Mascotas", R.drawable.ic_pets),
        TabItem("Comunidad", R.drawable.ic_community),
        TabItem("Perfil", R.drawable.ic_profile),
    )
    var selectedTab by remember { mutableIntStateOf(1) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                painter = painterResource(id = tab.iconRes), // Solución aquí
                                contentDescription = tab.title,
                                modifier = Modifier.size(24.dp),
                                tint = Color.Unspecified // 👈 Esto evita que Compose aplique un tint
                            )
                        },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Contenido de cada pestaña
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (selectedTab) {
                0 -> MapScreen()
                1 -> Mascotas(navController)
                2 -> Text("Proximamente")
                3 -> ProfileScreen(navController)
            }
        }
    }
}
// Data class debe usar Int para los recursos
data class TabItem(val title: String, val iconRes: Int)



