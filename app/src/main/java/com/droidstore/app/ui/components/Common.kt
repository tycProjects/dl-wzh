package com.droidstore.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.droidstore.app.data.BADGE_HINTS
import com.droidstore.app.data.BADGE_LABELS

/** Бейдж с пояснением по нажатию. Бейджи выдаются только администраторами. */
@Composable
fun BadgeChip(badge: String) {
    var open by remember { mutableStateOf(false) }
    Column {
        AssistChip(onClick = { open = !open }, label = { Text(BADGE_LABELS[badge] ?: badge) })
        if (open) {
            Text(
                text = BADGE_HINTS[badge].orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
            )
        }
    }
}

@Composable
fun Stars(value: Double?, count: Int? = null) {
    val text = if (value == null) "Нет оценок" else "★ ${"%.1f".format(value)}" + (count?.let { " ($it)" } ?: "")
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}

/** Выбор оценки звёздами с пружинной анимацией. */
@Composable
fun StarPicker(selected: Int, onSelect: (Int) -> Unit) {
    Row {
        for (i in 1..5) {
            val scale by animateFloatAsState(
                targetValue = if (i <= selected) 1.15f else 1f,
                animationSpec = spring(dampingRatio = 0.5f),
                label = "star$i",
            )
            Text(
                text = if (i <= selected) "★" else "☆",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .scale(scale)
                    .clickable { onSelect(i) },
            )
        }
    }
}

@Composable
fun AppIcon(url: String?, size: Int) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 4).dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(url).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size.dp),
            )
        }
    }
}

/** Результат проверки. Без проверки показываем Unknown, без зелёной галочки. */
@Composable
fun SecurityNotice(status: String?, score: Int?, limitations: List<String>) {
    val title = when (status) {
        "high_risk" -> "Высокий риск по результатам проверки"
        "warning" -> "Обнаружены предупреждения"
        "needs_review" -> "Требуется дополнительная проверка"
        "partial" -> "Проверка выполнена частично"
        else -> "Статус: Unknown (проверка не выполнена)"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (score != null) Text("Оценка: $score из 100", style = MaterialTheme.typography.bodySmall)
            limitations.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun ErrorText(message: String?) {
    if (!message.isNullOrBlank()) {
        Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 4.dp))
    }
}
