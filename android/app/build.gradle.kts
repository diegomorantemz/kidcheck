import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// URL de la API por defecto de la app. Se puede sobreescribir en local.properties
// (kidcheck.api.url=http://IP-DEL-PC:8080) o con la variable KIDCHECK_API_URL,
// sin tocar el código ni el repositorio.
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val apiUrlPorDefecto: String = propiedadesLocales.getProperty("kidcheck.api.url")
    ?: System.getenv("KIDCHECK_API_URL")
    ?: "http://10.0.2.2:8080"

android {
    namespace = "pe.edu.kidcheck.porteria"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "pe.edu.kidcheck.porteria"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "API_URL_POR_DEFECTO", "\"$apiUrlPorDefecto\"")
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**", "pe.edu.kidcheck.porteria.**")
            }
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

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.androidx.security.crypto)
    implementation(libs.sqlcipher)
    implementation(libs.androidx.sqlite.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
