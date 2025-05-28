plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example.perros"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.perros"
        minSdk = 32
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
}

dependencies {
    implementation (libs.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.transport.runtime)
    implementation(libs.firebase.crashlytics.buildtools)
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
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    implementation (libs.postgrest.kt)
    implementation(libs.gotrue.kt)
    implementation(libs.androidx.security.crypto) // Para encriptar SharedPreferences
    // Motor HTTP para Ktor (elige uno):
    implementation(libs.ktor.client.android) // Para Android
    // O alternativamente:
    implementation(libs.ktor.client.cio) // Motor basado en coroutines
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    // Si usas inyección de dependencias (opcional)
    implementation(libs.koin.android)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}