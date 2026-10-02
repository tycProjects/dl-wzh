plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
// Địa chỉ API máy chủ của bạn, ví dụ "https://your-domain.com/api". Để trống = tài khoản cục bộ.
val apiBase = ""

android {
    namespace = "com.example.mapphim"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.mapphim"
        minSdk = 26
        targetSdk = 34
        versionCode = 5
        versionName = "1.4"
        buildConfigField("String", "API_BASE", "\"$apiBase\"")
        manifestPlaceholders["cleartext"] = apiBase.startsWith("http://").toString()
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
