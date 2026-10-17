// The "template" is a tiny offline WebView app. The builder app embeds its APK and, for every
// project the user creates, swaps in their website, name, package, version and icon.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.template.webapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.template.webapp"
        minSdk = 21
        targetSdk = 34
        versionCode = 1234567 // sentinel, patched by the builder
        versionName = "__VERSION_NAME__" // placeholder, patched by the builder
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
// Deliberately no androidx dependencies: they add permissions/providers named after the
// package, which would break installing two generated apps side by side.
