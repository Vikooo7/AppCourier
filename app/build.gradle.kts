plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)   // lee google-services.json (proyecto rutalog-peru)
}

android {
    namespace = "com.example.rutalogcourier"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.rutalogcourier"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        // URL de la API: la de Vercel, o otra al compilar con -PurlApi=http://localhost:3100/ (pruebas en la PC).
        buildConfigField("String", "URL_API", "\"" + (providers.gradleProperty("urlApi").orNull ?: "https://app-api-rutalog.vercel.app/") + "\"")
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Room guarda el esquema de cada versión en app/schemas: sirve para revisar las migraciones.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Inicio con Google: Credential Manager entrega la cuenta y Firebase emite el token que valida la API
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Room: ruta del día y cola de operaciones hechas sin señal
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // API REST: Retrofit + Gson
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // Sincronización en segundo plano y sesión cifrada
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.security.crypto)

    // Ruta del día en el mapa (OpenStreetMap, sin clave)
    implementation(libs.osmdroid.android)
    // GPS de cada evento (RF-C03, RN-06)
    implementation(libs.play.services.location)
    // Escaneo de la guía con la cámara, sin pedir permiso de cámara (RF-C03)
    implementation(libs.play.services.code.scanner)
    // QR del cobro contraentrega que el destinatario paga con Banco Andino (RF-C05)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
