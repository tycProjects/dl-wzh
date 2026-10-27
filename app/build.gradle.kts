plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "vn.example.khoamanhinh"
    compileSdk = 35
    defaultConfig {
        applicationId = "vn.example.khoamanhinh"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
