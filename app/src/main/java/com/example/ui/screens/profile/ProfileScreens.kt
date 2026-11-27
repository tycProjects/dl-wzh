package com.example.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.ReferralMember
import com.example.domain.model.UserProfile
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    onUpdateAvatar: (String) -> Unit,
    onCopyReferral: () -> Unit,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onUpdateAvatar(it.toString()) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraBlack)
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Profile", style = MaterialTheme.typography.headlineMedium, color = SoftWhite)
                IconButton(
                    onClick = { onNavigate("settings") },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(NexoraSurface)
                        .border(1.dp, DividerDark, CircleShape)
                        .testTag("profile_settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = SoftWhite)
                }
            }
        }

        // Hero Profile Card
        item {
            NexoraGlassCard(borderColor = VioletBorder) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        NexoraAvatar(
                            avatarUri = userProfile.avatarUri,
                            contentDescription = userProfile.displayName,
                            size = 88.dp,
                            borderColor = MetallicGold
                        )
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(NeonPurple)
                                .border(2.dp, NexoraBlack, CircleShape)
                                .clickable {
                                    avatarPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Upload avatar", tint = SoftWhite, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(userProfile.displayName, style = MaterialTheme.typography.headlineSmall, color = SoftWhite)
                    Text("NexoraHub ID: ${userProfile.nexoraUserId}", style = MaterialTheme.typography.labelLarge, color = ElectricViolet)
                    Text("Member since ${userProfile.createdAt}", style = MaterialTheme.typography.bodySmall, color = CoolGrey)

                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NexoraSurface,
                        border = BorderStroke(1.dp, DividerDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Saved Free Fire Identity", style = MaterialTheme.typography.labelSmall, color = CoolGrey)
                                Text(
                                    text = if (userProfile.savedIgn.isNotBlank()) "${userProfile.savedIgn} • UID: ${userProfile.savedUid}" else "Not set yet",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = BrightGold
                                )
                            }
                            Text(
                                text = "Edit",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricViolet,
                                modifier = Modifier.clickable { onNavigate("player_identity") }
                            )
                        }
                    }
                }
            }
        }

        // Referral Code Card
        item {
            NexoraClayCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Referral Code", style = MaterialTheme.typography.labelSmall, color = CoolGrey)
                            Text(userProfile.referralCode, style = MaterialTheme.typography.titleLarge, color = BrightGold)
                        }
                        Surface(
                            onClick = {
                                clipboard.setText(AnnotatedString(userProfile.referralCode))
                                onCopyReferral()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = NeonPurple
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = SoftWhite, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Copy", style = MaterialTheme.typography.labelLarge, color = SoftWhite)
                            }
                        }
                    }
                    HorizontalDivider(color = DividerDark)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("referral_team") },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = ElectricViolet)
                            Spacer(Modifier.width(10.dp))
                            Text("Referral Team (${userProfile.referralTeamCount} Members)", style = MaterialTheme.typography.titleSmall, color = SoftWhite)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = CoolGrey)
                    }
                }
            }
        }

        // All 15 Profile Options grouped neatly
        item {
            NexoraGlassCard(contentPadding = PaddingValues(vertical = 6.dp)) {
                ProfileMenuRow("Edit Profile", Icons.Default.Edit) { onNavigate("edit_profile") }
                ProfileMenuRow("My Tournaments", Icons.Default.EmojiEvents) { onNavigate("my_tournaments") }
                ProfileMenuRow("Player Details (Free Fire IGN & UID)", Icons.Default.SportsEsports) { onNavigate("player_identity") }
                ProfileMenuRow("Referral Code & Team", Icons.Default.Group) { onNavigate("referral_team") }
                ProfileMenuRow("Redeem Promo Code", Icons.Default.CardGiftcard) { onNavigate("redeem_code") }
                ProfileMenuRow("Leaderboard", Icons.Default.Leaderboard) { onNavigate("leaderboard") }
                ProfileMenuRow("Feedback", Icons.Default.Feedback) { onNavigate("feedback") }
                ProfileMenuRow("Support & Complaints", Icons.Default.HeadsetMic) { onNavigate("support") }
                ProfileMenuRow("Privacy Policy", Icons.Default.Security) { onNavigate("legal/privacy") }
                ProfileMenuRow("Terms and Conditions", Icons.Default.Description) { onNavigate("legal/terms") }
                ProfileMenuRow("Settings", Icons.Default.Settings) { onNavigate("settings") }
                ProfileMenuRow("App Version v1.0.0", Icons.Default.Info) { onNavigate("legal/about") }
                ProfileMenuRow("Delete Account", Icons.Default.DeleteForever, tint = ErrorRed) { onNavigate("delete_account") }
            }
        }

        item {
            NexoraPurpleButton(
                text = "Logout",
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                testTag = "profile_logout_button"
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ProfileMenuRow(
    title: String,
    icon: ImageVector,
    tint: Color = ElectricViolet,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (tint == ErrorRed) ErrorRed else SoftWhite,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = CoolGrey)
    }
}

@Composable
fun EditProfileScreen(
    userProfile: UserProfile,
    onBack: () -> Unit,
    onSaveProfile: (displayName: String, avatarUri: String?) -> Unit
) {
    var displayName by remember { mutableStateOf(userProfile.displayName) }
    var avatarUri by remember { mutableStateOf(userProfile.avatarUri) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { avatarUri = it.toString() }
    }

    Scaffold(
        containerColor = NexoraBlack,
        topBar = { NexoraTopBar(title = "Edit Profile", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NexoraAvatar(avatarUri = avatarUri, contentDescription = "Avatar", size = 96.dp, borderColor = MetallicGold)
            NexoraOutlinedButton(
                text = "Choose Photo from Gallery",
                icon = Icons.Default.CameraAlt,
                onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            )
            NexoraTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = "NexoraHub Display Name"
            )
            NexoraTextField(
                value = userProfile.nexoraUserId,
                onValueChange = {},
                label = "NexoraHub User ID (Fixed)"
            )
            Spacer(Modifier.weight(1f))
            NexoraPurpleButton(
                text = "Save Changes",
                onClick = {
                    onSaveProfile(displayName, avatarUri)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PlayerIdentityScreen(
    userProfile: UserProfile,
    onBack: () -> Unit,
    onSaveIdentity: (ign: String, uid: String) -> Unit
) {
    var ign by remember { mutableStateOf(userProfile.savedIgn) }
    var uid by remember { mutableStateOf(userProfile.savedUid) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = NexoraBlack,
        topBar = { NexoraTopBar(title = "Free Fire Player Identity", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NexoraGlassCard(borderColor = GoldBorder) {
                Text("Identity Separation", style = MaterialTheme.typography.titleSmall, color = BrightGold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Your NexoraHub User ID (${userProfile.nexoraUserId}) is separate from your Free Fire UID. Saving your IGN and UID lets you auto-fill tournament registrations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoolGrey
                )
            }
            NexoraTextField(
                value = ign,
                onValueChange = { ign = it },
                label = "Free Fire In-Game Name (IGN)",
                leadingIcon = Icons.Default.SportsEsports
            )
            NexoraTextField(
                value = uid,
                onValueChange = { uid = it.filter(Char::isDigit) },
                label = "Free Fire UID",
                leadingIcon = Icons.Default.Badge,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                errorText = error
            )
            Spacer(Modifier.weight(1f))
            NexoraPurpleButton(
                text = "Save Free Fire Identity",
                onClick = {
                    if (ign.isBlank() || uid.length < 6) {
                        error = "Enter a valid Free Fire IGN and numeric UID (6+ digits)."
                    } else {
                        onSaveIdentity(ign, uid)
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ReferralTeamScreen(
    userProfile: UserProfile,
    referrals: List<ReferralMember>,
    onBack: () -> Unit,
    onCopyCode: () -> Unit
) {
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraBlack)
    ) {
        NexoraTopBar(title = "Referral Team", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                NexoraGlassCard(borderColor = GoldBorder) {
                    Text("Your Invite Code", style = MaterialTheme.typography.labelMedium, color = CoolGrey)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(userProfile.referralCode, style = MaterialTheme.typography.displaySmall, color = BrightGold)
                        NexoraPurpleButton(
                            text = "Copy Code",
                            onClick = {
                                clipboard.setText(AnnotatedString(userProfile.referralCode))
                                onCopyCode()
                            },
                            modifier = Modifier.width(140.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Earn promotional platform points when invited friends verify their account and play their first tournament.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CoolGrey
                    )
                }
            }

            item {
                Text("Invited Players (${referrals.size})", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
            }

            items(referrals, key = { it.nexoraUserId }) { member ->
                NexoraClayCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(member.username, style = MaterialTheme.typography.titleSmall, color = SoftWhite)
                            Text("${member.nexoraUserId} • Joined ${member.joinedDate}", style = MaterialTheme.typography.bodySmall, color = CoolGrey)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            NexoraStatusBadge(
                                text = member.status,
                                containerColor = if (member.bonusPoints > 0) SuccessContainer else WarningContainer,
                                contentColor = if (member.bonusPoints > 0) SuccessGreen else WarningAmber
                            )
                            if (member.bonusPoints > 0) {
                                Spacer(Modifier.height(4.dp))
                                Text("+${member.bonusPoints} Pts", style = MaterialTheme.typography.labelMedium, color = BrightGold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RedeemCodeScreen(
    onBack: () -> Unit,
    onRedeem: (
        code: String,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var redeemedBonus by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        containerColor = NexoraBlack,
        topBar = { NexoraTopBar(title = "Redeem Promo Code", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NexoraGlassCard(borderColor = VioletBorder) {
                Text("Enter Promotional Code", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Admin-issued promotional codes grant free non-cash platform points. Demo preview codes: NEXORA2026 or BOOYAH500.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoolGrey
                )
                Spacer(Modifier.height(14.dp))
                NexoraTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = "Promo Code",
                    placeholder = "e.g. NEXORA2026",
                    leadingIcon = Icons.Default.ConfirmationNumber,
                    errorText = errorText,
                    testTag = "redeem_code_input"
                )
                Spacer(Modifier.height(14.dp))
                NexoraPurpleButton(
                    text = "Redeem Code",
                    onClick = {
                        if (code.isBlank()) {
                            errorText = "Enter a promotional code."
                        } else {
                            errorText = null
                            onRedeem(
                                code,
                                { bonus -> redeemedBonus = bonus },
                                { err -> errorText = err }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "redeem_code_submit"
                )
            }

            if (redeemedBonus != null) {
                NexoraGlassCard(borderColor = GoldBorder) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrightGold, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Congratulations!", style = MaterialTheme.typography.headlineSmall, color = BrightGold)
                        Text(
                            "You have received $redeemedBonus Points in your wallet!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SoftWhite
                        )
                    }
                }
            }
        }
    }
}
