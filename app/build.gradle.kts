plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.droidstore.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.droidstore.app"
        minSdk = 29          // Android 10
        targetSdk = 36       // Android 16
        versionCode = 1
        versionName = "0.2.0"

        // Адрес backend: 10.0.2.2 — это localhost компьютера для эмулятора Android.
        buildConfigField("String", "API_BASE_URL", "\"${project.findProperty("DS_API_BASE_URL") ?: "http://10.0.2.2:8080"}\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation("androidx.security:security-crypto:1.0.0")
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")
}
