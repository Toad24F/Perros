
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization")

}
// Cargar las variables de local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.example.huellasseguras"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.huellasseguras.app"
        minSdk = 32
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // CREAR LAS VARIABLES PARA EL BUILDCONFIG
        buildConfigField("String", "SUPABASE_URL", "\"${localProperties.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${localProperties.getProperty("SUPABASE_KEY", "")}\"")
        //manifestPlaceholders["MAPS_API_KEY"] = project.properties["MAPS_API_KEY"] as String
        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY", "")

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.transport.runtime)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.androidx.navigation.common.android)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.ui.unit)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.navigation.compose) // Navegación
    implementation(libs.androidx.animation) // Para animaciones
    implementation(libs.play.services.location)
    implementation(libs.maps.compose)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.security.crypto) // Para encriptar SharedPreferences
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.serialization.json)
    // SDK principal de Supabase y módulo de Auth (GoTrue)
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.4.1")
    implementation("io.github.jan-tennert.supabase:auth-kt:3.4.1")
    // Serialización necesaria para los datos
    implementation("io.ktor:ktor-client-android:3.4.1")
    // Para mostrar imágenes desde una URL (Coil)
    implementation("io.coil-kt:coil-compose:2.7.0")
// Para el almacenamiento de Supabase (verifica que la versión coincida con las que ya tienes)
    implementation("io.github.jan-tennert.supabase:storage-kt:3.4.1")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.4.0")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.5.0-alpha15")



}