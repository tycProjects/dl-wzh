plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.cybervshack.game"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.cybervshack.game"
        minSdk = 26; targetSdk = 34; versionCode = 1; versionName = "0.1"
        // GANTI sebelum rilis: host backend + pin SHA-256 (leaf & backup)
        buildConfigField("String", "API_HOST", "\"api.example.com\"")
        buildConfigField("String", "PIN_1", "\"sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=\"")
        buildConfigField("String", "PIN_2", "\"sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=\"")
    }
    buildTypes {
        release {
            isMinifyEnabled = true      // R8 obfuscation + shrink
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.8.1")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.android.play:integrity:1.3.0")
}
