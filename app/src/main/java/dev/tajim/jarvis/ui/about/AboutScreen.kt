package dev.tajim.jarvis.ui.about

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import dev.tajim.jarvis.BuildConfig
import dev.tajim.jarvis.R
import dev.tajim.jarvis.core.Branding
import dev.tajim.jarvis.core.ExternalLinks
import dev.tajim.jarvis.ui.components.GlassCard
import dev.tajim.jarvis.ui.components.HoloButton
import dev.tajim.jarvis.ui.components.JarvisScreenScaffold
import dev.tajim.jarvis.ui.theme.LocalJarvisColors
import dev.tajim.jarvis.ui.theme.Spacing
import dev.tajim.jarvis.ui.theme.WordmarkStyle

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val c = LocalJarvisColors.current
    val context = LocalContext.current
    val linkError = stringResource(R.string.open_link_failed)
    fun open(url: String) {
        if (!ExternalLinks.open(context, url)) Toast.makeText(context, linkError, Toast.LENGTH_SHORT).show()
    }

    JarvisScreenScaffold(title = stringResource(R.string.about_title), onBack = onBack) {
        GlassCard(Modifier.fillMaxWidth().padding(top = Spacing.lg)) {
            Column(
                Modifier.fillMaxWidth().padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    Branding.APP_NAME,
                    style = WordmarkStyle.merge(TextStyle(brush = Brush.linearGradient(listOf(c.cyan, c.violet, c.magenta)))),
                )
                Text(stringResource(R.string.about_created_by), style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Text(stringResource(R.string.about_credit), style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
                // Version comes straight from the Gradle build configuration.
                Text(
                    stringResource(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                    style = MaterialTheme.typography.labelMedium, color = c.textSecondary,
                )
            }
        }
        Text(
            stringResource(R.string.about_links),
            modifier = Modifier.padding(top = Spacing.xl, bottom = Spacing.sm),
            style = MaterialTheme.typography.titleMedium, color = c.textSecondary,
        )
        Column(Modifier.padding(bottom = Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            HoloButton(stringResource(R.string.link_youtube), { open(Branding.YOUTUBE_URL) }, Modifier.fillMaxWidth())
            HoloButton(stringResource(R.string.link_tiktok), { open(Branding.TIKTOK_URL) }, Modifier.fillMaxWidth())
            HoloButton(stringResource(R.string.link_whatsapp), { open(Branding.WHATSAPP_URL) }, Modifier.fillMaxWidth())
        }
    }
}
