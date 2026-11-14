plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.example.gditroll"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.gditroll"
        minSdk = 26
        targetSdk = 34
        versionCode = 15
        versionName = "3.0.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
