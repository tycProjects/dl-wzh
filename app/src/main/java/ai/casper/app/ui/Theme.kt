package ai.casper.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Цвета из дизайн-токенов Motion Studies (тёмная тема, графит и чёрный, фиолетовый акцент).
object Casper {
    val Stage = Color(0xFF000000)
    val Card = Color(0xFF171717)
    val Panel = Color(0xFF262626)
    val Fill1 = Color(0x0DFFFFFF)
    val Fill2 = Color(0x17FFFFFF)
    val Fill3 = Color(0x24FFFFFF)
    val Ink = Color(0xFFFFFFFF)
    val Muted = Color(0xFF9A9A9A)
    val Accent = Color(0xFFA78BFA)
    val Inverse = Color(0xFFFFFFFF)
    val OnInverse = Color(0xFF000000)
}

private val scheme = darkColorScheme(
    primary = Casper.Accent,
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF3B2A6B),
    onPrimaryContainer = Color(0xFFFFFFFF),
    background = Casper.Card,
    onBackground = Casper.Ink,
    surface = Casper.Panel,
    onSurface = Casper.Ink,
    surfaceVariant = Casper.Panel,
    onSurfaceVariant = Casper.Muted,
    error = Color(0xFFFF5A5F),
)

@Composable
fun CasperTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
