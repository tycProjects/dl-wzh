plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.android.island"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.android.island"
        // Android 8.0 (API 26) is the supported floor; newer-only APIs are runtime-guarded.
        minSdk = 26
        targetSdk = 35
        versionCode = 162
        versionName = "1.17.2"

        // APK size: ML Kit text recognition ships native engines for four ABIs, but every real
        // phone is ARM. Dropping x86/x86_64 removes roughly half of the bundled native payload
        // (several MB) with zero effect on any phone. Remove the filter temporarily if the app
        // must ever run on an x86 emulator.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
        // APK size: AndroidX/Compose libraries carry translations for ~100 locales that this
        // app never shows (its own UI is a single default strings.xml). Keeping only English
        // and Indonesian strips the rest at packaging time.
        resourceConfigurations += listOf("en", "id")
    }

    // APK size: AGP embeds a protobuf blob listing every dependency inside the APK. It is
    // only used by tooling (Play developer service detection) and is dead weight for a
    // side-loaded APK.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildTypes {
        release {
            // Installable without a private keystore (signed with the debug key). Use `assembleRelease` to measure
            // real RAM: the debug build is not minified, not optimised and is debuggable, so it uses more memory.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // The "debug" variant is configured exactly like release (minified, shrunk, not debuggable, same
        // ProGuard rules). APK builders usually run assembleDebug; this way the APK is lean whichever task runs.
        // Cost: no debugger, obfuscated stack traces (mapping file in build/outputs/mapping).
        debug {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // APK size: every AndroidX jar embeds a META-INF/*.version marker file. They are
            // tooling metadata only (never read at runtime); excluding them removes ~40 dead
            // entries from the APK with zero functional impact.
            excludes += "/META-INF/*.version"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { test ->
            // Robolectric's android-all artifacts resolve through the same throttled mirror as the build.
            (test as org.gradle.api.tasks.testing.Test).systemProperty(
                "robolectric.dependency.repo.url", "https://maven.aliyun.com/repository/public"
            )
        }
    }
}

dependencies {
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.savedstate:savedstate-ktx:1.2.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.runtime:runtime")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.media:media:1.7.0")
    implementation("androidx.palette:palette-ktx:1.0.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.google.android.material:material:1.12.0")
    // Safe YAML reader/writer for the Lyricsfile 1.0 draft format.
    implementation("org.yaml:snakeyaml:2.3")

    // Optional shell bridge for status-bar notification icon control.
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    implementation("com.google.mlkit:text-recognition:16.0.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    // Real org.json implementation for JVM unit tests; android.jar only carries method stubs.
    testImplementation("org.json:json:20240303")
}
