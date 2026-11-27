package com.example.ui.screens.tournaments

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.*
import com.example.ui.theme.*

@Composable
fun TournamentRegistrationScreen(
    tournament: Tournament,
    userProfile: UserProfile,
    onBack: () -> Unit,
    onSubmitRegistration: (
        teamName: String?,
        players: List<FreeFirePlayerIdentity>,
        onSuccess: (TournamentRegistration) -> Unit,
        onError: (String) -> Unit
    ) -> Unit,
    onViewMyTournaments: () -> Unit
) {
    val playerCount = tournament.mode.requiredPlayers
    val requiresTeam = playerCount > 1

    var currentStep by remember { mutableIntStateOf(1) }
    var teamName by remember { mutableStateOf("") }
    val playerIgns = remember(playerCount) {
        mutableStateListOf<String>().apply {
            add(userProfile.savedIgn)
            repeat((playerCount - 1).coerceAtLeast(0)) { add("") }
        }
    }
    val playerUids = remember(playerCount) {
        mutableStateListOf<String>().apply {
            add(userProfile.savedUid)
            repeat((playerCount - 1).coerceAtLeast(0)) { add("") }
        }
    }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var confirmedReg by remember { mutableStateOf<TournamentRegistration?>(null) }

    BackHandler(enabled = currentStep in 2..4 && !isSubmitting) {
        currentStep -= 1
    }

    fun validateStep2(): Boolean {
        val ign = playerIgns[0].trim()
        val uid = playerUids[0].trim()
        return when {
            ign.isEmpty() -> {
                validationError = "Player 1 Free Fire IGN is required."
                false
            }
            uid.length < 6 || !uid.all { it.isDigit() } -> {
                validationError = "Enter a valid numeric Free Fire UID (at least 6 digits)."
                false
            }
            else -> {
                validationError = null
                true
            }
        }
    }

    fun validateStep3(): Boolean {
        if (!requiresTeam) return true
        if (teamName.trim().isEmpty()) {
            validationError = "Team Name is required for ${tournament.mode.label} tournaments."
            return false
        }
        val uidSet = mutableSetOf<String>()
        for (i in 0 until playerCount) {
            val ign = playerIgns[i].trim()
            val uid = playerUids[i].trim()
            if (ign.isEmpty()) {
                validationError = "Player ${i + 1} Free Fire IGN cannot be empty."
                return false
            }
            if (uid.length < 6 || !uid.all { it.isDigit() }) {
                validationError = "Player ${i + 1} Free Fire UID must be numeric (min 6 digits)."
                return false
            }
            if (!uidSet.add(uid)) {
                validationError = "Duplicate Free Fire UID ($uid) found inside the same team."
                return false
            }
        }
        validationError = null
        return true
    }

    Scaffold(
        containerColor = NexoraBlack,
        topBar = {
            if (currentStep < 5) {
                NexoraTopBar(
                    title = "Tournament Registration",
                    onBack = {
                        if (currentStep > 1) currentStep -= 1 else onBack()
                    }
                )
            }
        },
        bottomBar = {
            if (currentStep < 5) {
                Surface(color = NexoraCharcoal, border = BorderStroke(1.dp, DividerDark)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (currentStep > 1) {
                            NexoraOutlinedButton(
                                text = "Back",
                                onClick = {
                                    validationError = null
                                    currentStep -= 1
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        NexoraPurpleButton(
                            text = when {
                                isSubmitting -> "Submitting..."
                                currentStep == 4 -> "Confirm Registration"
                                else -> "Continue"
                            },
                            enabled = !isSubmitting,
                            onClick = {
                                when (currentStep) {
                                    1 -> currentStep = 2
                                    2 -> if (validateStep2()) {
                                        currentStep = if (requiresTeam) 3 else 4
                                    }
                                    3 -> if (validateStep3()) {
                                        currentStep = 4
                                    }
                                    4 -> {
                                        isSubmitting = true
                                        validationError = null
                                        val players = (0 until playerCount).map {
                                            FreeFirePlayerIdentity(
                                                ign = playerIgns[it].trim(),
                                                uid = playerUids[it].trim()
                                            )
                                        }
                                        onSubmitRegistration(
                                            if (requiresTeam) teamName.trim() else null,
                                            players,
                                            { reg ->
                                                isSubmitting = false
                                                confirmedReg = reg
                                                currentStep = 5
                                            },
                                            { err ->
                                                isSubmitting = false
                                                validationError = err
                                            }
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.6f),
                            testTag = "registration_next_button"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (currentStep < 5) {
                StepIndicatorBar(currentStep = currentStep, totalSteps = 5)
            }

            if (validationError != null) {
                Surface(
                    color = ErrorContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErrorRed,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            when (currentStep) {
                1 -> {
                    NexoraGlassCard(borderColor = VioletBorder) {
                        Text("Step 1: NexoraHub Account Identity", style = MaterialTheme.typography.titleLarge, color = SoftWhite)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Your NexoraHub User ID is automatically linked. Keep this separate from your Free Fire UID.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoolGrey
                        )
                        Spacer(Modifier.height(14.dp))
                        SummaryRow("Tournament", tournament.title)
                        SummaryRow("Mode", "${tournament.mode.label} (${tournament.mode.requiredPlayers} Player${if (playerCount > 1) "s" else ""})")
                        SummaryRow("NexoraHub Username", userProfile.displayName)
                        SummaryRow("NexoraHub User ID", userProfile.nexoraUserId, valueColor = ElectricViolet)
                        SummaryRow("Entry Fee", "Free Entry (0 Pts)", valueColor = SuccessGreen)
                    }
                }
                2 -> {
                    NexoraGlassCard(borderColor = VioletBorder) {
                        Text("Step 2: Free Fire Player 1 Identity", style = MaterialTheme.typography.titleLarge, color = SoftWhite)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Enter your exact Free Fire In-Game Name (IGN) and numeric Free Fire UID.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoolGrey
                        )
                        if (userProfile.savedIgn.isNotEmpty() && userProfile.savedUid.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            NexoraOutlinedButton(
                                text = "Use Saved (${userProfile.savedIgn} • ${userProfile.savedUid})",
                                onClick = {
                                    playerIgns[0] = userProfile.savedIgn
                                    playerUids[0] = userProfile.savedUid
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        NexoraTextField(
                            value = playerIgns[0],
                            onValueChange = { playerIgns[0] = it },
                            label = "Player 1 Free Fire IGN",
                            placeholder = "e.g. NXH・GamerPro",
                            leadingIcon = Icons.Default.SportsEsports,
                            testTag = "player1_ign_input"
                        )
                        Spacer(Modifier.height(12.dp))
                        NexoraTextField(
                            value = playerUids[0],
                            onValueChange = { playerUids[0] = it.filter { ch -> ch.isDigit() } },
                            label = "Player 1 Free Fire UID",
                            placeholder = "e.g. 2489104572",
                            leadingIcon = Icons.Default.Badge,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            testTag = "player1_uid_input"
                        )
                    }
                }
                3 -> {
                    NexoraGlassCard(borderColor = GoldBorder) {
                        Text("Step 3: ${tournament.mode.label} Team Details", style = MaterialTheme.typography.titleLarge, color = SoftWhite)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Enter your Team Name and all ${playerCount - 1} teammate Free Fire IGN & UID values.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoolGrey
                        )
                        Spacer(Modifier.height(14.dp))
                        NexoraTextField(
                            value = teamName,
                            onValueChange = { teamName = it },
                            label = "Team Name",
                            placeholder = "e.g. Violet Phantoms",
                            leadingIcon = Icons.Default.Groups,
                            testTag = "team_name_input"
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Player 1 (Leader): ${playerIgns[0]} (${playerUids[0]})",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricViolet
                        )
                        for (i in 1 until playerCount) {
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = DividerDark)
                            Spacer(Modifier.height(12.dp))
                            Text("Player ${i + 1}", style = MaterialTheme.typography.titleSmall, color = MetallicGold)
                            Spacer(Modifier.height(8.dp))
                            NexoraTextField(
                                value = playerIgns[i],
                                onValueChange = { playerIgns[i] = it },
                                label = "Player ${i + 1} Free Fire IGN",
                                placeholder = "Enter teammate IGN",
                                leadingIcon = Icons.Default.Person
                            )
                            Spacer(Modifier.height(8.dp))
                            NexoraTextField(
                                value = playerUids[i],
                                onValueChange = { playerUids[i] = it.filter { ch -> ch.isDigit() } },
                                label = "Player ${i + 1} Free Fire UID",
                                placeholder = "Numeric Free Fire UID",
                                leadingIcon = Icons.Default.Badge,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }
                4 -> {
                    NexoraGlassCard(borderColor = GoldBorder) {
                        Text("Step 4: Registration Summary", style = MaterialTheme.typography.titleLarge, color = SoftWhite)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Double-check every Free Fire IGN and UID before confirming with the server.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoolGrey
                        )
                        Spacer(Modifier.height(14.dp))
                        SummaryRow("Tournament", tournament.title)
                        SummaryRow("Schedule", "${tournament.dateLabel} • ${tournament.timeLabel}")
                        SummaryRow("NexoraHub Account", "${userProfile.displayName} (${userProfile.nexoraUserId})")
                        if (requiresTeam) {
                            SummaryRow("Team Name", teamName, valueColor = MetallicGold)
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = DividerDark)
                        Spacer(Modifier.height(8.dp))
                        for (i in 0 until playerCount) {
                            SummaryRow("Player ${i + 1} IGN", playerIgns[i])
                            SummaryRow("Player ${i + 1} UID", playerUids[i], valueColor = ElectricViolet)
                        }
                    }
                    if (isSubmitting) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = ElectricViolet, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Confirming slot with NexoraHub server...", color = CoolGrey)
                        }
                    }
                }
                5 -> {
                    val reg = confirmedReg
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(SuccessContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(52.dp))
                        }
                        Text(
                            text = "CONGRATULATIONS!",
                            style = MaterialTheme.typography.displaySmall,
                            color = BrightGold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "YOUR NEXORAHUB TOURNAMENT REGISTRATION IS SUCCESSFUL.",
                            style = MaterialTheme.typography.titleMedium,
                            color = SoftWhite,
                            textAlign = TextAlign.Center
                        )
                        if (reg != null) {
                            NexoraGlassCard(borderColor = GoldBorder, modifier = Modifier.fillMaxWidth()) {
                                SummaryRow("Registration Ref", reg.registrationId, valueColor = BrightGold)
                                SummaryRow("Tournament", reg.tournamentTitle)
                                SummaryRow("NexoraHub User", "${reg.nexoraUsername} (${reg.nexoraUserId})")
                                if (!reg.teamName.isNullOrBlank()) {
                                    SummaryRow("Team Name", reg.teamName)
                                }
                                reg.players.forEachIndexed { idx, p ->
                                    SummaryRow("Player ${idx + 1}", "${p.ign} • UID: ${p.uid}")
                                }
                                SummaryRow("Status", reg.registrationStatus.label, valueColor = SuccessGreen)
                            }
                        }
                        NexoraPurpleButton(
                            text = "View My Tournaments",
                            onClick = onViewMyTournaments,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "go_to_my_tournaments_button"
                        )
                        NexoraOutlinedButton(
                            text = "Back to Home",
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicatorBar(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (step in 1..totalSteps) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            step < currentStep -> SuccessGreen
                            step == currentStep -> ElectricViolet
                            else -> NexoraSurface
                        }
                    )
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = SoftWhite
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = CoolGrey)
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
}
