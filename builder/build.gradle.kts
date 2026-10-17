plugins {
    id("com.android.application")
}

android {
    namespace = "com.htmltoapk.builder"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.htmltoapk.builder"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Build the offline WebView template first, then embed its APK as assets/template.apk.
val templateAssets = layout.buildDirectory.dir("generated/templateAssets")

val copyTemplate by tasks.registering(Copy::class) {
    dependsOn(":template:assembleRelease")
    from(rootProject.layout.projectDirectory.file("template/build/outputs/apk/release/template-release-unsigned.apk"))
    rename { "template.apk" }
    into(templateAssets)
}

android.sourceSets.getByName("main").assets.srcDir(templateAssets)

tasks.named("preBuild") { dependsOn(copyTemplate) }

dependencies {
    implementation("com.android.tools.build:apksig:8.5.2")
    implementation("androidx.core:core:1.13.1")
}
