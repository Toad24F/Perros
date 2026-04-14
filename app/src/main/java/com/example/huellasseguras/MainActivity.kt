package com.example.huellasseguras

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.WorkManager
import com.example.huellasseguras.Screens.AddMedicalRecordScreen
import com.example.huellasseguras.Screens.AddPetScreen
import com.example.huellasseguras.Screens.HomeScreen
import com.example.huellasseguras.Screens.LoginScreen
import com.example.huellasseguras.Screens.PetProfileScreen
import com.example.huellasseguras.Screens.RegisterScreen
import com.example.huellasseguras.Supabase.Supabase
import com.example.huellasseguras.Workers.GeofenceWorker
import com.example.huellasseguras.ui.theme.PerrosTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Supabase.client
        super.onCreate(savedInstanceState)
        // Programar el chequeo de geofence en segundo plano
        GeofenceWorker.schedule(this)
        WorkManager.getInstance(this)
            .getWorkInfosForUniqueWorkLiveData("geofence_check")
            .observe(this) { workInfos ->
                workInfos.forEach {
                    Log.d("WorkManager", "Estado: ${it.state}")
                }
            }

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")
            )
        }
            startActivity(intent)
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
// --- Navegación entre pantallas ---
@OptIn(ExperimentalPermissionsApi::class)
@Composable
public fun AppNavigation() {

    val context = LocalContext.current
    PermissionHandler()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notifPermission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) {
            if (!notifPermission.status.isGranted) notifPermission.launchPermissionRequest()
        }
    }
    val navController = rememberNavController()
    val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    val userName = sharedPref.getString("user_name", null)
    val userEmail = sharedPref.getString("user_email", null)
    val startDestination = if (userName != null && userEmail != null) {
        "home"
    } else {
        "login"
    }
    NavHost(
        navController = navController,
        startDestination = startDestination
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
        composable("addPet") {
            val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val userId = sharedPref.getString("user_id", "") ?: ""
            AddPetScreen(navController, userId)
        }
        composable("addMedicalRecord/{petId}") { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            AddMedicalRecordScreen(petId = petId, navController = navController)
        }
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
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler() {
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }
}
