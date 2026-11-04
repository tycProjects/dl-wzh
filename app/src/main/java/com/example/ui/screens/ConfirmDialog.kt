package com.example.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalTumbaColors
import com.example.ui.theme.LocalTumbaStrings

@Composable
fun ConfirmDialog(
    isOpen: Boolean,
    title: String,
    message: String,
    confirmLabel: String? = null,
    isDestructive: Boolean = true,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    if (!isOpen) return

    val colors = LocalTumbaColors.current
    val strings = LocalTumbaStrings.current

    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = if (isDestructive) colors.danger else colors.accent
            )
        },
        title = {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 13.sp,
                color = colors.textSecondary,
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) colors.danger else colors.textPrimary,
                    contentColor = if (isDestructive) colors.textPrimary else colors.bgMain
                ),
                modifier = Modifier.testTag("confirm_dialog_action_button")
            ) {
                Text(confirmLabel ?: strings.confirm, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(strings.cancel, color = colors.textSecondary, fontSize = 12.sp)
            }
        },
        containerColor = colors.bgCard
    )
}
