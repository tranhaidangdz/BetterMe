import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}

// Cloudinary credentials live in local.properties (gitignored) so secrets stay off the
// remote. If the file is missing or the keys aren't set, the app still builds — it just
// falls back to keeping the on-device URI for check-in photos. To enable cloud uploads,
// add to local.properties:
//   CLOUDINARY_CLOUD_NAME=your_cloud_name
//   CLOUDINARY_UPLOAD_PRESET=your_unsigned_preset
val cloudinaryProps: Properties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}
val cloudinaryCloudName: String = cloudinaryProps.getProperty("CLOUDINARY_CLOUD_NAME", "")
val cloudinaryUploadPreset: String = cloudinaryProps.getProperty("CLOUDINARY_UPLOAD_PRESET", "")

// AI provider keys from local.properties (gitignored). Comma-separated lists let
// the runtime ApiKeyPool rotate across multiple keys from different Google /
// OpenRouter accounts — one expired or rate-limited key never stalls the chain.
//
//   GEMINI_API_KEYS=AIza...key1,AIza...key2
//   OPENROUTER_API_KEYS=sk-or-v1-key1,sk-or-v1-key2
//
// Backward compatibility: the legacy single `OPENROUTER_API_KEY=...` form is
// still accepted as a 1-element list when `OPENROUTER_API_KEYS` is absent. Both
// fields fall back to empty strings so the app still builds without secrets —
// AI features then surface a clear error category at runtime.
//
// .trim() on each entry catches stray whitespace that would silently produce an
// invalid Bearer token.
fun csv(raw: String): String =
    raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(",")

val geminiApiKeysCsv: String = csv(cloudinaryProps.getProperty("GEMINI_API_KEYS", ""))

val openrouterApiKeysCsv: String = run {
    val multi = csv(cloudinaryProps.getProperty("OPENROUTER_API_KEYS", ""))
    if (multi.isNotEmpty()) multi
    else csv(cloudinaryProps.getProperty("OPENROUTER_API_KEY", "")) // legacy single-key form
}

android {
    namespace = "com.example.betterme"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.betterme"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"$cloudinaryCloudName\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"$cloudinaryUploadPreset\"")
        buildConfigField("String", "GEMINI_API_KEYS", "\"$geminiApiKeysCsv\"")
        buildConfigField("String", "OPENROUTER_API_KEYS", "\"$openrouterApiKeysCsv\"")
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    testOptions {
        // Make `android.util.Log` (and other Android-stub classes) return defaults
        // instead of throwing in JVM unit tests. Lets pure-logic tests cover code
        // paths that touch Log.d / Log.w without requiring Robolectric.
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material.icons.extended) // Icons.Default.*, Icons.Filled.*
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.googleid)
    implementation(libs.androidx.compose.foundation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.core)

    // Coil 3
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Splash screen
    implementation(libs.core.splashscreen)

    // Extra UI support
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.ui.text.google.fonts)

    // Koin (Dependency Injection)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.core)

    // Datastore
    implementation(libs.androidx.datastore.preferences)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Work Manager
    implementation(libs.androidx.work.runtime.ktx)

    // Firebase (BOM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.functions.ktx)

    //LottieAnimation
    implementation(libs.lottie.compose)

    // Location (GPS for check-in anti-cheat)
    implementation(libs.play.services.location)

    // Cloudinary (image upload + delivery)
    implementation(libs.cloudinary.android)

    // AI / OpenRouter (Retrofit + OkHttp + kotlinx-serialization JSON)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
}