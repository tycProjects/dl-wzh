package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.JnmfStrings
import com.example.ui.i18n.LocalAppLanguage

enum class TaskType {
    STARTUP,
    MUSIC_SCAN,
    VIDEO_SCAN,
    DUPLICATES,
    PLAYLIST_LOAD,
    PLAYLIST_SAVE
}

data class TaskProgressState(
    val taskType: TaskType = TaskType.MUSIC_SCAN,
    val title: String = "Loading",
    val progress: Float? = null, // null = indeterminate, 0.0f..1.0f = measurable
    val current: Int = 0,
    val total: Int = 0,
    val statusMessage: String? = null,
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val error: String? = null
) {
    val progressPercent: Int
        get() = progress?.let { (it * 100).toInt().coerceIn(0, 100) } ?: 0

    companion object {
        val IDLE = TaskProgressState()
    }
}

/**
 * Reusable neon-green loading progress banner matching JNMF dark styling.
 * Displays measurable progress (1% - 100%) or indeterminate loading when total is unknown.
 */
@Composable
fun JnmfTaskProgressBar(
    state: TaskProgressState,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val isVisible = state.isRunning || state.isCompleted || state.error != null

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 })
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .border(
                    width = 1.dp,
                    color = when {
                        state.error != null -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                        state.isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                .testTag("task_progress_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        state.error != null -> {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = JnmfStrings.errorLabel(lang),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        state.isCompleted -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = JnmfStrings.completedLabel(lang),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        else -> {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.title == "Loading") JnmfStrings.loadingDefault(lang) else state.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (!state.statusMessage.isNullOrBlank()) {
                            Text(
                                text = state.statusMessage,
                                fontSize = 11.sp,
                                color = if (state.error != null) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Measurable Percentage or Item Counts
                    if (state.isRunning && state.progress != null) {
                        Text(
                            text = "${state.progressPercent}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }

                    if (onDismiss != null && (state.isCompleted || state.error != null)) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = JnmfStrings.dismiss(lang),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Progress Indicator Bar
                if (state.isRunning) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.progress != null) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                    } else {
                        // Indeterminate when total is unknown
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }
        }
    }
}
