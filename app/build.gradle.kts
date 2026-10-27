plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.azuro.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.azuro.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        // Tuỳ chọn: nhúng sẵn API key (gradle -PAZURO_API_KEY=sk-ant-...). Key trong APK có thể bị trích xuất.
        val k = (project.findProperty("AZURO_API_KEY") as String?) ?: ""
        buildConfigField("String", "DEFAULT_KEY", "\"$k\"")
    }
    buildFeatures { buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildTypes { release { signingConfig = signingConfigs.getByName("debug") } }
}
