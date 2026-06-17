plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.dualcal.widget"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dualcal.widget"
        minSdk = 26          // Android 8.0: java.time nativo, sin "desugaring"
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // Keystore de firma fija (versionada). La contraseña "android" es la
    // estándar de depuración y no es secreta: solo sirve para que todas las
    // compilaciones (Mac o GitHub Actions) firmen igual y el móvil reconozca
    // las actualizaciones como la misma app.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
