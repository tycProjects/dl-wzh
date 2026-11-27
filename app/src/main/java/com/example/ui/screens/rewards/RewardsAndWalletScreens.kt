package com.example.ui.screens.rewards

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.FreeCreditItem
import com.example.domain.model.PointTransaction
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RewardsScreen(
    availablePoints: Int,
    pendingPoints: Int,
    totalEarnedPoints: Int,
    freeCreditsBalance: Int,
    transactions: List<PointTransaction>,
    onOpenWallet: () -> Unit,
    onOpenFreeCredits: () -> Unit,
    onOpenRewardsHistory: () -> Unit,
    onOpenRedeemCode: () -> Unit
) {
    val fmt = remember { NumberFormat.getNumberInstance(Locale.US) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraBlack)
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rewards & Points", style = MaterialTheme.typography.headlineMedium, color = SoftWhite)
                Surface(
                    onClick = onOpenRewardsHistory,
                    shape = RoundedCornerShape(50),
                    color = NexoraSurface,
                    border = BorderStroke(1.dp, DividerDark)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("History", style = MaterialTheme.typography.labelMedium, color = SoftWhite)
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(listOf(NeonPurple, NexoraClay, NexoraSurface))
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AVAILABLE PLATFORM POINTS", style = MaterialTheme.typography.labelMedium, color = CoolGrey)
                    Text("${fmt.format(availablePoints)} Points", style = MaterialTheme.typography.displaySmall, color = BrightGold)
                    Text(
                        "Free non-cash community reward points • Non-transferable • Not redeemable for money",
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftWhite.copy(alpha = 0.85f)
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Pending Points", fmt.format(pendingPoints), WarningAmber, Modifier.weight(1f))
                StatTile("Promo Credits", fmt.format(freeCreditsBalance), ElectricViolet, Modifier.weight(1f))
                StatTile("Total Earned", fmt.format(totalEarnedPoints), SuccessGreen, Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NexoraPurpleButton(
                    text = "Redeem Promo Code",
                    icon = Icons.Default.CardGiftcard,
                    onClick = onOpenRedeemCode,
                    modifier = Modifier.weight(1f)
                )
                NexoraOutlinedButton(
                    text = "Free Credits",
                    icon = Icons.Default.Stars,
                    onClick = onOpenFreeCredits,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            NexoraClayCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenWallet
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MetallicGold)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Open Full Wallet Overview", style = MaterialTheme.typography.titleSmall, color = SoftWhite)
                        Text("View tournament rewards & credit rules", style = MaterialTheme.typography.bodySmall, color = CoolGrey)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = CoolGrey)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Rewards Activity", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                Text(
                    "See All",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricViolet,
                    modifier = Modifier.clickable { onOpenRewardsHistory() }
                )
            }
        }

        items(transactions.take(4), key = { it.referenceId }) { txn ->
            TransactionRowCard(txn)
        }
    }
}

@Composable
fun WalletScreen(
    availablePoints: Int,
    pendingPoints: Int,
    totalEarnedPoints: Int,
    freeCreditsBalance: Int,
    transactions: List<PointTransaction>,
    onBack: () -> Unit,
    onOpenFreeCredits: () -> Unit,
    onSeeAllHistory: () -> Unit
) {
    val fmt = remember { NumberFormat.getNumberInstance(Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraBlack)
    ) {
        NexoraTopBar(title = "My Wallet", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                NexoraGlassCard(borderColor = GoldBorder) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(GoldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = BrightGold)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Available Points", style = MaterialTheme.typography.labelMedium, color = CoolGrey)
                            Text("${fmt.format(availablePoints)} Points", style = MaterialTheme.typography.displaySmall, color = SoftWhite)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Platform points are earned from free-entry tournaments, referrals, and admin promos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CoolGrey
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Pending", fmt.format(pendingPoints), WarningAmber, Modifier.weight(1f))
                    StatTile("Credits", fmt.format(freeCreditsBalance), ElectricViolet, Modifier.weight(1f))
                    StatTile("Earned", fmt.format(totalEarnedPoints), SuccessGreen, Modifier.weight(1f))
                }
            }

            item {
                NexoraOutlinedButton(
                    text = "View Promotional Free Credits",
                    onClick = onOpenFreeCredits,
                    icon = Icons.Default.Stars,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Points History", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                    Text(
                        "See All",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricViolet,
                        modifier = Modifier.clickable { onSeeAllHistory() }
                    )
                }
            }

            items(transactions, key = { it.referenceId }) { txn ->
                TransactionRowCard(txn)
            }
        }
    }
}

@Composable
fun FreeCreditsScreen(
    freeCreditsBalance: Int,
    creditItems: List<FreeCreditItem>,
    onBack: () -> Unit,
    onOpenRedeemCode: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraBlack)
    ) {
        NexoraTopBar(title = "Free Credits", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                NexoraGlassCard(borderColor = VioletBorder) {
                    Text("AVAILABLE PROMOTIONAL CREDITS", style = MaterialTheme.typography.labelSmall, color = CoolGrey)
                    Spacer(Modifier.height(4.dp))
                    Text("$freeCreditsBalance Credits", style = MaterialTheme.typography.displaySmall, color = ElectricViolet)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Free Credits are issued by NexoraHub admins. They are 100% free, non-cash, non-transferable, and not purchasable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CoolGrey
                    )
                    Spacer(Modifier.height(14.dp))
                    NexoraPurpleButton(
                        text = "Redeem Promotional Code",
                        onClick = onOpenRedeemCode,
                        icon = Icons.Default.CardGiftcard,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Text("Credit Allocation History", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
            }

            items(creditItems, key = { it.id }) { credit ->
                NexoraClayCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(credit.title, style = MaterialTheme.typography.titleSmall, color = SoftWhite)
                            Text("+${credit.amountPts} Pts", style = MaterialTheme.typography.titleSmall, color = SuccessGreen, fontWeight = FontWeight.Bold)
                        }
                        Text("Issued by: ${credit.issuedBy} • ${credit.dateLabel}", style = MaterialTheme.typography.bodySmall, color = CoolGrey)
                        Text(credit.usageRules, style = MaterialTheme.typography.bodySmall, color = MutedGrey)
                        NexoraStatusBadge(credit.expiryLabel, GoldContainer, BrightGold)
                    }
                }
            }
        }
    }
}

@Composable
fun RewardsHistoryScreen(
    transactions: List<PointTransaction>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraBlack)
    ) {
        NexoraTopBar(title = "Rewards History", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(transactions, key = { it.referenceId }) { txn ->
                TransactionRowCard(txn)
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    NexoraClayCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = CoolGrey)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TransactionRowCard(txn: PointTransaction) {
    val isPositive = txn.pointsDelta >= 0
    NexoraGlassCard(
        borderColor = DividerDark,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isPositive) SuccessContainer else ErrorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = if (isPositive) SuccessGreen else ErrorRed,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(txn.title, style = MaterialTheme.typography.titleSmall, color = SoftWhite)
                Text(txn.description, style = MaterialTheme.typography.bodySmall, color = CoolGrey)
                Text("${txn.referenceId} • ${txn.dateLabel} ${txn.timeLabel}", style = MaterialTheme.typography.labelSmall, color = MutedGrey)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isPositive) "+${txn.pointsDelta} Pts" else "${txn.pointsDelta} Pts",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isPositive) SuccessGreen else ErrorRed,
                    fontWeight = FontWeight.Bold
                )
                Text(txn.status, style = MaterialTheme.typography.labelSmall, color = CoolGrey)
            }
        }
    }
}
