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

// OpenRouter API key, also from local.properties (gitignored). Falls back to empty
// string so the app builds without it — AI features then no-op with a clear toast.
//   OPENROUTER_API_KEY=sk-or-v1-...
// .trim() — a stray newline or space in local.properties would silently produce
// an invalid Bearer token at runtime. Properties.load() already strips trailing
// whitespace per spec, but trimming again costs nothing and removes one class of
// "I added the key but it still 401s" failures.
val openrouterApiKey: String = cloudinaryProps.getProperty("OPENROUTER_API_KEY", "").trim()

// Verified-share Cloud Functions base URL. Defaults to the emulator
// host so a debug build runs without further config — flip to your
// production URL via local.properties:
//   SHARE_FUNCTIONS_BASE_URL=https://us-central1-<project>.cloudfunctions.net/
// The trailing slash is required (Retrofit appends path segments).
val shareFunctionsBaseUrl: String = cloudinaryProps
    .getProperty("SHARE_FUNCTIONS_BASE_URL", "http://10.0.2.2:5001/betterme/us-central1/")
    .trim()

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
        buildConfigField("String", "OPENROUTER_API_KEY", "\"$openrouterApiKey\"")
        buildConfigField("String", "SHARE_FUNCTIONS_BASE_URL", "\"$shareFunctionsBaseUrl\"")
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