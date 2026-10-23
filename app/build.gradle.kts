plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose"); id("com.google.devtools.ksp") }
android {
 namespace="com.mat.tradehelper"; compileSdk=37
 defaultConfig { applicationId="com.mat.tradehelper"; minSdk=26; targetSdk=36; versionCode=100000; versionName="10.0.0" }
 buildFeatures { compose=true }
 buildTypes { release { isMinifyEnabled=true; isShrinkResources=true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro") } }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2026.09.00"))
 implementation("androidx.activity:activity-compose:1.12.1")
 implementation("androidx.compose.ui:ui"); implementation("androidx.compose.material3:material3"); implementation("androidx.compose.material:material-icons-extended")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
 implementation("androidx.room:room-runtime:2.8.3"); implementation("androidx.room:room-ktx:2.8.3"); ksp("androidx.room:room-compiler:2.8.3")
}
