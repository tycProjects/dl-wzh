package ai.casper.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

// Spring Dropdown: меню выпадает из-под кнопки на пружине, стрелка поворачивается.
@Composable
fun SpringDropdown(
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val openness by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = springFloat(280f, 22f),
        label = "dropdown"
    )
    Box {
        Surface(
            onClick = { onExpandedChange(!expanded) },
            shape = RoundedCornerShape(16.dp),
            color = Casper.Fill2
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, maxLines = 1, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer { rotationZ = openness * 180f }
                )
            }
        }
        if (openness > 0.01f) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Casper.Panel,
                modifier = Modifier
                    .offset(y = 48.dp)
                    .width(300.dp)
                    .graphicsLayer {
                        scaleY = openness
                        alpha = openness
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
            ) {
                Column(Modifier.padding(10.dp), content = content)
            }
        }
    }
}
