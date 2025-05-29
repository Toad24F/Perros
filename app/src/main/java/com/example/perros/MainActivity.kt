package com.example.perros

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.perros.ui.theme.PerrosTheme
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import androidx.navigation.NavType


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PerrosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
@Composable
fun CheckAuthScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Verificar tanto en SharedPreferences como en Supabase Auth para mayor robustez
        val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userName = sharedPref.getString("user_name", null)
        val userEmail = sharedPref.getString("user_email", null)

        scope.launch {
            try {
                if (userEmail != null && userName != null) {
                    // Usuario autenticado - ir a home
                    navController.navigate("home") {
                        popUpTo(0) // Limpiar back stack
                    }
                } else {
                    // No autenticado - ir a login
                    navController.navigate("login") {
                        popUpTo(0) // Limpiar back stack
                    }
                }
            } catch (e: Exception) {
                // En caso de error al verificar sesión, ir a login
                navController.navigate("login") {
                    popUpTo(0)
                }
            }
        }
    }
}
// --- Navegación entre pantallas ---
@Composable
public fun AppNavigation() {
    PermissionHandler() // Añade esto al inicio
    val navController = rememberNavController()
    CheckAuthScreen(navController)
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable(
            route = "login",
            enterTransition = {
                slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut(animationSpec = tween(300))
            }
        ) { LoginScreen(navController) }

        composable(
            route = "home",
            enterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            }
        ) { HomeScreen(navController) }

        composable(
            route = "registro",
            enterTransition = {
                slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut(animationSpec = tween(300))
            }
        ) { RegisterScreen(navController) }
        composable(
            route = "petDetail/{petId}",
            arguments = listOf(
                navArgument("petId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId")
            if (petId != null) {
                PetProfileScreen(petId = petId, navController = navController)
            } else {
                // Manejo de error si petId es null
                Text("Error: ID de mascota no válido")
            }
        }
    }
}
// Función para verificar si el GPS está activado
public fun isLocationEnabled(context: Context): Boolean {
    val locationManager = ContextCompat.getSystemService(
        context,
        LocationManager::class.java
    )
    return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
}
// --- Previews ---
@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    PerrosTheme {
        LoginScreen(navController = rememberNavController())
    }
}
