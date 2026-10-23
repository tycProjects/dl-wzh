package com.mat.tradehelper.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
val Green=Color(0xFF00E676);val Red=Color(0xFFFF5252)
private val scheme=darkColorScheme(primary=Green,background=Color.Black,surface=Color(0xFF080808),surfaceVariant=Color(0xFF111111),onBackground=Color.White,onSurface=Color.White)
@Composable fun AppTheme(content:@Composable()->Unit)=MaterialTheme(colorScheme=scheme,content=content)
