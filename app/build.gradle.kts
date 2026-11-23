plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android { namespace="com.lenqin.lenai"; compileSdk=35
 defaultConfig { applicationId="com.lenqin.lenai"; minSdk=26; targetSdk=35; versionCode=3; versionName="3.0" } }
dependencies {
 implementation("androidx.core:core-ktx:1.15.0")
 implementation("androidx.appcompat:appcompat:1.7.0")
 implementation("androidx.security:security-crypto:1.1.0-alpha06")
 implementation("com.squareup.okhttp3:okhttp:4.12.0")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
 implementation("com.google.mlkit:text-recognition:16.0.1")
}
