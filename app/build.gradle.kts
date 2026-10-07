plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.androidserverhub"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.androidserverhub"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.4"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.webkit:webkit:1.12.1")
}
