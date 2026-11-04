package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteTemplate
import com.example.ui.theme.LocalTumbaColors
import com.example.ui.theme.LocalTumbaStrings

@Composable
fun TemplatesScreen(
    templates: List<NoteTemplate>,
    defaultTemplateId: String?,
    onCreateNoteFromTemplate: (NoteTemplate) -> Unit,
    onNewTemplate: () -> Unit,
    onEditTemplate: (NoteTemplate) -> Unit,
    onDeleteTemplate: (NoteTemplate) -> Unit,
    onSetDefaultTemplate: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val colors = LocalTumbaColors.current
    val strings = LocalTumbaStrings.current

    val builtIn = remember(templates) { templates.filter { it.isBuiltIn } }
    val custom = remember(templates) { templates.filter { !it.isBuiltIn } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = colors.textPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.templatesLibrary,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Text(
                        text = "${templates.size} available formats",
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Custom templates section (if any)
                if (custom.isNotEmpty()) {
                    item {
                        Text(
                            text = strings.customTemplates.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(custom, key = { it.id }) { tpl ->
                        TemplateCard(
                            template = tpl,
                            isDefault = tpl.id == defaultTemplateId,
                            onUse = { onCreateNoteFromTemplate(tpl) },
                            onEdit = { onEditTemplate(tpl) },
                            onDelete = { onDeleteTemplate(tpl) },
                            onSetDefault = { onSetDefaultTemplate(tpl.id) }
                        )
                    }
                }

                // Built-in standard templates
                item {
                    Text(
                        text = strings.builtInTemplates.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                items(builtIn, key = { it.id }) { tpl ->
                    TemplateCard(
                        template = tpl,
                        isDefault = tpl.id == defaultTemplateId,
                        onUse = { onCreateNoteFromTemplate(tpl) },
                        onEdit = null,
                        onDelete = null,
                        onSetDefault = { onSetDefaultTemplate(tpl.id) }
                    )
                }
            }
        }

        // FAB to create custom template
        FloatingActionButton(
            onClick = onNewTemplate,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = colors.accent,
            contentColor = colors.background,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = strings.createCustomTemplate)
        }
    }
}

@Composable
fun TemplateCard(
    template: NoteTemplate,
    isDefault: Boolean,
    onUse: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onSetDefault: () -> Unit
) {
    val colors = LocalTumbaColors.current
    val strings = LocalTumbaStrings.current

    val iconVector = getTemplateIconVector(template.icon)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUse() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isDefault) colors.accent.copy(alpha = 0.6f) else colors.border
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.panelBackground, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        iconVector,
                        contentDescription = null,
                        tint = if (isDefault) colors.accent else colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = template.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isDefault) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = colors.accent.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = strings.defaultBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = colors.accent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = template.description.ifBlank { template.category },
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Actions: Set Default, Edit, Delete
                IconButton(
                    onClick = onSetDefault,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = strings.setAsDefaultTemplate,
                        tint = if (isDefault) colors.accent else colors.textSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (onEdit != null) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.background
                ) {
                    Text(
                        text = template.category,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.accent,
                    modifier = Modifier.clickable { onUse() }
                ) {
                    Text(
                        text = strings.useTemplate,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.background,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

fun getTemplateIconVector(iconName: String): ImageVector {
    return when (iconName) {
        "book-open", "book" -> Icons.Default.Book
        "bookmark" -> Icons.Default.Bookmark
        "users", "briefcase", "work" -> Icons.Default.Work
        "layers" -> Icons.Default.Layers
        "code" -> Icons.Default.Code
        "graduation-cap", "school" -> Icons.Default.School
        "lightbulb" -> Icons.Default.Lightbulb
        "check-square", "checkbox" -> Icons.Default.CheckBox
        else -> Icons.Default.Description
    }
}
