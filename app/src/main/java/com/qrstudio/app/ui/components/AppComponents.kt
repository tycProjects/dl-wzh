package com.qrstudio.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.qrstudio.app.ui.theme.BorderColor
import com.qrstudio.app.ui.theme.CardBg
import com.qrstudio.app.ui.theme.CardBgAlt
import com.qrstudio.app.ui.theme.Emerald
import com.qrstudio.app.ui.theme.TextMain
import com.qrstudio.app.ui.theme.TextMuted

@Composable
fun SectionCard(
    highlight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = if (highlight) CardBgAlt else CardBg,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            if (highlight) Emerald.copy(alpha = 0.5f) else BorderColor,
        ),
    ) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = Emerald,
        modifier = modifier.padding(bottom = 10.dp),
    )
}

@Composable
fun MutedLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TextMuted,
        modifier = modifier.padding(top = 10.dp, bottom = 6.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipsRow(
    items: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { (key, label) ->
            FilterChip(
                selected = selectedKey == key,
                onClick = { onSelect(key) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Emerald,
                    selectedLabelColor = Color(0xFF06281f),
                ),
            )
        }
    }
}

@Composable
fun Swatches(
    colors: List<Long>,
    selected: Long,
    onPick: (Long) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { color ->
            val isSelected = color == selected
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) Emerald else BorderColor,
                        shape = CircleShape,
                    )
                    .clickable { onPick(color) },
            )
        }
    }
}

@Composable
fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: String,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextMain)
            Spacer(Modifier.weight(1f))
            Text(valueText, style = MaterialTheme.typography.labelMedium, color = Emerald)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

@Composable
fun EmptyState(icon: @Composable () -> Unit, message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
    ) {
        icon()
        Text(message, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}
