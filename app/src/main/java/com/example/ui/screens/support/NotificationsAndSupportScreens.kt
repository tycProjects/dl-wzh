package com.example.ui.screens.support

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.*
import com.example.ui.theme.*

@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    onMarkRead: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraBlack)
    ) {
        NexoraTopBar(
            title = "Notifications",
            onBack = onBack,
            actions = {
                TextButton(onClick = onMarkAllRead, modifier = Modifier.testTag("mark_all_read_button")) {
                    Text("Mark All Read", style = MaterialTheme.typography.labelLarge, color = ElectricViolet)
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (notifications.isEmpty()) {
                item {
                    NexoraEmptyState(
                        icon = Icons.Default.Notifications,
                        title = "You're All Caught Up",
                        description = "Tournament reminders, room availability alerts, and admin notices will appear here."
                    )
                }
            } else {
                items(notifications, key = { it.id }) { item ->
                    val icon = when (item.category) {
                        NotificationCategory.ROOM -> Icons.Default.Key
                        NotificationCategory.TOURNAMENT, NotificationCategory.MATCH_RESULT -> Icons.Default.EmojiEvents
                        NotificationCategory.STORE -> Icons.Default.Storefront
                        NotificationCategory.SUPPORT -> Icons.Default.HeadsetMic
                        else -> Icons.Default.Campaign
                    }
                    NexoraGlassCard(
                        onClick = { onMarkRead(item.id) },
                        borderColor = if (item.isRead) DividerDark else ElectricViolet,
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (item.isRead) NexoraSurface else NeonPurple.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = if (item.isRead) CoolGrey else BrightGold, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall, color = SoftWhite)
                                    if (!item.isRead) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(ElectricViolet)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(item.description, style = MaterialTheme.typography.bodySmall, color = CoolGrey)
                                Spacer(Modifier.height(6.dp))
                                Text("${item.dateLabel} • ${item.timeLabel}", style = MaterialTheme.typography.labelSmall, color = MutedGrey)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupportCenterScreen(
    tickets: List<SupportTicket>,
    onBack: () -> Unit,
    onCreateTicket: (
        category: TicketCategory,
        subject: String,
        description: String,
        attachmentUri: String?,
        onSuccess: (SupportTicket) -> Unit
    ) -> Unit,
    onReplyToTicket: (ticketId: String, message: String) -> Unit
) {
    var showCreateForm by remember { mutableStateOf(false) }
    var selectedTicket by remember { mutableStateOf<SupportTicket?>(null) }

    var category by remember { mutableStateOf(TicketCategory.TOURNAMENT_PROBLEM) }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val screenshotPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> attachmentUri = uri }

    Scaffold(
        containerColor = NexoraBlack,
        topBar = { NexoraTopBar(title = "Support & Complaints", onBack = onBack) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                NexoraGlassCard(borderColor = VioletBorder) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(VioletSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.HeadsetMic, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(30.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Need Help?", style = MaterialTheme.typography.titleLarge, color = SoftWhite)
                        Text("Our NexoraHub team is here to assist you.", style = MaterialTheme.typography.bodySmall, color = CoolGrey)
                        Spacer(Modifier.height(14.dp))
                        NexoraPurpleButton(
                            text = if (showCreateForm) "Hide Ticket Form" else "Create Ticket",
                            icon = Icons.Default.Add,
                            onClick = { showCreateForm = !showCreateForm },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "toggle_create_ticket_button"
                        )
                    }
                }
            }

            if (showCreateForm) {
                item {
                    NexoraClayCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("New Support Ticket", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TicketCategory.entries.forEach { cat ->
                                    FilterChip(
                                        selected = category == cat,
                                        onClick = { category = cat },
                                        label = { Text(cat.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonPurple,
                                            selectedLabelColor = SoftWhite,
                                            containerColor = NexoraSurface,
                                            labelColor = CoolGrey
                                        )
                                    )
                                }
                            }
                            NexoraTextField(
                                value = subject,
                                onValueChange = { subject = it },
                                label = "Subject",
                                placeholder = "Brief summary of your issue"
                            )
                            NexoraTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = "Describe your problem",
                                singleLine = false,
                                minLines = 3
                            )
                            NexoraOutlinedButton(
                                text = if (attachmentUri != null) "Screenshot Attached ✓" else "Attach Optional Screenshot",
                                icon = Icons.Default.AttachFile,
                                onClick = {
                                    screenshotPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (errorMsg != null) {
                                Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                            }
                            NexoraPurpleButton(
                                text = "Submit Ticket",
                                onClick = {
                                    if (subject.isBlank() || description.isBlank()) {
                                        errorMsg = "Subject and description are required."
                                    } else {
                                        errorMsg = null
                                        onCreateTicket(category, subject, description, attachmentUri?.toString()) {
                                            subject = ""
                                            description = ""
                                            attachmentUri = null
                                            showCreateForm = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item {
                Text("Your Tickets (${tickets.size})", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
            }

            items(tickets, key = { it.ticketId }) { ticket ->
                val badgeColors = when (ticket.status) {
                    TicketStatus.RESOLVED -> SuccessContainer to SuccessGreen
                    TicketStatus.UNDER_REVIEW, TicketStatus.WAITING_FOR_USER -> WarningContainer to WarningAmber
                    TicketStatus.CLOSED -> VioletSubtle to CoolGrey
                    TicketStatus.SUBMITTED -> VioletSubtle to ElectricViolet
                }
                NexoraGlassCard(
                    onClick = { selectedTicket = ticket },
                    borderColor = DividerDark
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(ticket.ticketId, style = MaterialTheme.typography.labelLarge, color = BrightGold)
                        NexoraStatusBadge(ticket.status.label, badgeColors.first, badgeColors.second)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(ticket.subject, style = MaterialTheme.typography.titleSmall, color = SoftWhite)
                    Text("${ticket.category.label} • ${ticket.createdAt}", style = MaterialTheme.typography.bodySmall, color = CoolGrey)
                }
            }
        }
    }

    selectedTicket?.let { active ->
        val latest = tickets.find { it.ticketId == active.ticketId } ?: active
        var replyText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { selectedTicket = null },
            containerColor = NexoraCharcoal,
            title = { Text("${latest.ticketId} • ${latest.status.label}", color = BrightGold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(latest.subject, style = MaterialTheme.typography.titleSmall, color = SoftWhite)
                    Text(latest.description, style = MaterialTheme.typography.bodySmall, color = CoolGrey)
                    HorizontalDivider(color = DividerDark)
                    latest.replies.forEach { reply ->
                        Surface(
                            color = if (reply.isAdmin) VioletSubtle else NexoraSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, DividerDark)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "${reply.senderName} • ${reply.timestamp}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (reply.isAdmin) ElectricViolet else BrightGold
                                )
                                Text(reply.message, style = MaterialTheme.typography.bodySmall, color = SoftWhite)
                            }
                        }
                    }
                    NexoraTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = "Reply to support"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (replyText.isNotBlank()) {
                            onReplyToTicket(latest.ticketId, replyText)
                            replyText = ""
                        }
                    }
                ) { Text("Send Reply", color = ElectricViolet) }
            },
            dismissButton = {
                TextButton(onClick = { selectedTicket = null }) {
                    Text("Close", color = CoolGrey)
                }
            }
        )
    }
}

@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    onSubmitFeedback: (Int, String) -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    Scaffold(
        containerColor = NexoraBlack,
        topBar = { NexoraTopBar(title = "Feedback", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NexoraGlassCard {
                Text("Rate Your NexoraHub Experience", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (star in 1..5) {
                        IconButton(onClick = { rating = star }) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$star stars",
                                tint = BrightGold,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                NexoraTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = "Share your thoughts or feature ideas",
                    singleLine = false,
                    minLines = 4
                )
                Spacer(Modifier.height(14.dp))
                NexoraPurpleButton(
                    text = "Submit Feedback",
                    onClick = {
                        onSubmitFeedback(rating, comment)
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
