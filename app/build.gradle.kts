plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localDeepSeekToken = providers.environmentVariable("DEEPSEEK_TOKEN").orNull
    ?: rootProject.file(".deepseek_token").takeIf { it.exists() }?.readText()?.trim().orEmpty()

android { namespace = "com.smali.fox"; compileSdk = 35
    defaultConfig {
        applicationId = "com.smali.fox"; minSdk = 23; targetSdk = 35; versionCode = 1; versionName = "1.0"
        buildConfigField("String", "DEEPSEEK_TOKEN", "\"${localDeepSeekToken.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Debug signing keeps this release APK directly installable for testing.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }

android.buildFeatures { buildConfig = true }

dependencies {
    implementation("org.smali:dexlib2:2.5.2")
}
