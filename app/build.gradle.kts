plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Firebase login only activates when you add app/google-services.json (see README).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.glassdex.analyzer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.glassdex.analyzer"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Put your Web client ID (from Firebase > Auth > Google) in local.properties
        // as GOOGLE_WEB_CLIENT_ID=xxxx.apps.googleusercontent.com
        val props = java.util.Properties()
        val lp = rootProject.file("local.properties")
        if (lp.exists()) lp.inputStream().use { props.load(it) }
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${props.getProperty("GOOGLE_WEB_CLIENT_ID", "")}\"")
        buildConfigField("boolean", "FIREBASE_ENABLED", file("google-services.json").exists().toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // Auth (Google / email)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
}
