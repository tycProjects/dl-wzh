plugins {
    id("com.android.application")
}

android {
    namespace = "com.ryomd.nativeapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ryomd.nativeapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.2.4"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

