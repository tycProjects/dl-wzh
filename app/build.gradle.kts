plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.example.infinitestairsauto"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.infinitestairsauto"
        minSdk = 29
        targetSdk = 34
        versionCode = 2
        versionName = "0.2-vision-experimental"
    }
}
