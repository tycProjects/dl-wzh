package com.qrstudio.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GFont
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.qrstudio.app.R

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val TajawalFamily = androidx.compose.ui.text.font.FontFamily(
    GFont(googleFont = GoogleFont("Tajawal"), fontProvider = fontProvider, weight = FontWeight.Light),
    GFont(googleFont = GoogleFont("Tajawal"), fontProvider = fontProvider, weight = FontWeight.Normal),
    GFont(googleFont = GoogleFont("Tajawal"), fontProvider = fontProvider, weight = FontWeight.Medium),
    GFont(googleFont = GoogleFont("Tajawal"), fontProvider = fontProvider, weight = FontWeight.Bold),
    GFont(googleFont = GoogleFont("Tajawal"), fontProvider = fontProvider, weight = FontWeight.Black),
)

val Typography = Typography(
    displayLarge = Typography().displayLarge.copy(fontFamily = TajawalFamily),
    displayMedium = Typography().displayMedium.copy(fontFamily = TajawalFamily),
    displaySmall = Typography().displaySmall.copy(fontFamily = TajawalFamily),
    headlineLarge = Typography().headlineLarge.copy(fontFamily = TajawalFamily),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = TajawalFamily),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = TajawalFamily),
    titleLarge = Typography().titleLarge.copy(fontFamily = TajawalFamily),
    titleMedium = Typography().titleMedium.copy(fontFamily = TajawalFamily),
    titleSmall = Typography().titleSmall.copy(fontFamily = TajawalFamily),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = TajawalFamily),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = TajawalFamily),
    bodySmall = Typography().bodySmall.copy(fontFamily = TajawalFamily),
    labelLarge = Typography().labelLarge.copy(fontFamily = TajawalFamily),
    labelMedium = Typography().labelMedium.copy(fontFamily = TajawalFamily),
    labelSmall = Typography().labelSmall.copy(fontFamily = TajawalFamily),
)
