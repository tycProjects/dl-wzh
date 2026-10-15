package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val TajawalFontFamily = FontFamily(
  Font(R.font.tajawal, FontWeight.Normal),
  Font(R.font.tajawal, FontWeight.Medium),
  Font(R.font.tajawal, FontWeight.SemiBold),
  Font(R.font.tajawal, FontWeight.Bold),
  Font(R.font.tajawal, FontWeight.ExtraBold)
)

val Typography = Typography(
  headlineLarge = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 36.sp
  ),
  headlineMedium = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 30.sp
  ),
  titleLarge = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    lineHeight = 26.sp
  ),
  titleMedium = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    lineHeight = 24.sp
  ),
  bodyLarge = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp
  ),
  bodyMedium = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp
  ),
  bodySmall = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp
  ),
  labelLarge = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 20.sp
  ),
  labelSmall = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 16.sp
  )
)
