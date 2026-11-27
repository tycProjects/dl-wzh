package com.example.ui.screens.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeScreen(
    availablePoints: Int,
    banners: List<PromoBanner>,
    tournaments: List<Tournament>,
    ongoingMatches: List<MatchItem>,
    selectedCategory: TournamentCategory,
    unreadNotifications: Int,
    isRefreshing: Boolean,
    onOpenDrawer: () -> Unit,
    onOpenFreeCredits: () -> Unit,
    onOpenWallet: () -> Unit,
    onRefresh: () -> Unit,
    onOpenNotifications: () -> Unit,
    onSelectCategory: (TournamentCategory) -> Unit,
    onBannerClick: (PromoBanner) -> Unit,
    onSeeAllTournaments: () -> Unit,
    onOpenTournament: (String) -> Unit,
    onOpenMatch: (String) -> Unit,
    onSeeAllMatches: () -> Unit
) {
    val animatedBalance by animateIntAsState(
        targetValue = availablePoints,
        animationSpec = tween(600),
        label = "balance_counter"
    )
    val formattedBalance = remember(animatedBalance) {
        NumberFormat.getNumberInstance(Locale.US).format(animatedBalance)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraBlack),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HomeTopBar(
                formattedBalance = formattedBalance,
                unreadNotifications = unreadNotifications,
                isRefreshing = isRefreshing,
                onOpenDrawer = onOpenDrawer,
                onOpenFreeCredits = onOpenFreeCredits,
                onOpenWallet = onOpenWallet,
                onRefresh = onRefresh,
                onOpenNotifications = onOpenNotifications
            )
        }

        if (banners.isNotEmpty()) {
            item {
                PromoBannerCarousel(
                    banners = banners,
                    onBannerClick = onBannerClick
                )
            }
        }

        item {
            CategoryFilterStrip(
                selectedCategory = selectedCategory,
                onSelectCategory = onSelectCategory
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Featured Tournaments",
                    style = MaterialTheme.typography.titleLarge,
                    color = SoftWhite
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSeeAllTournaments() }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "See All",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricViolet
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = ElectricViolet,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (tournaments.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    NexoraEmptyState(
                        icon = Icons.Default.EmojiEvents,
                        title = "No Tournaments in Category",
                        description = "Try switching to All or check back shortly when admin publishes new events.",
                        actionLabel = "View All",
                        onAction = { onSelectCategory(TournamentCategory.ALL) }
                    )
                }
            }
        } else {
            items(tournaments, key = { it.id }) { tournament ->
                TournamentCard(
                    tournament = tournament,
                    onClick = { onOpenTournament(tournament.id) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        if (ongoingMatches.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ongoing Matches",
                        style = MaterialTheme.typography.titleLarge,
                        color = SoftWhite
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSeeAllMatches() }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("See All", style = MaterialTheme.typography.labelLarge, color = ElectricViolet)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(18.dp))
                    }
                }
            }

            items(ongoingMatches.take(2), key = { it.matchId }) { match ->
                NexoraGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    borderColor = GoldBorder,
                    onClick = { onOpenMatch(match.matchId) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NexoraStatusBadge(
                                    text = "LIVE",
                                    containerColor = SuccessContainer,
                                    contentColor = SuccessGreen
                                )
                                Text(
                                    text = "Match ${match.matchId}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MetallicGold
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = match.tournamentTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = SoftWhite
                            )
                            Text(
                                text = "${match.mode.label} • ${match.mapName} • ${match.startTime}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CoolGrey
                            )
                        }
                        NexoraOutlinedButton(
                            text = "Room",
                            onClick = { onOpenMatch(match.matchId) },
                            borderColor = ElectricViolet
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    formattedBalance: String,
    unreadNotifications: Int,
    isRefreshing: Boolean,
    onOpenDrawer: () -> Unit,
    onOpenFreeCredits: () -> Unit,
    onOpenWallet: () -> Unit,
    onRefresh: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart),
        label = "spin"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NexoraCharcoal, NexoraBlack)
                )
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 5-element top bar: Hamburger -> Free Credits -> Top-up Balance -> Refresh -> Bell
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(NexoraSurface)
                    .border(1.dp, DividerDark, CircleShape)
                    .testTag("drawer_menu_button")
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Open menu", tint = SoftWhite)
            }

            Surface(
                onClick = onOpenFreeCredits,
                shape = RoundedCornerShape(50),
                color = NeonPurple,
                modifier = Modifier
                    .height(40.dp)
                    .testTag("free_credits_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = SoftWhite, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Free Credits",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SoftWhite,
                        maxLines = 1
                    )
                }
            }

            Surface(
                onClick = onOpenWallet,
                shape = RoundedCornerShape(12.dp),
                color = NexoraClay,
                border = BorderStroke(1.dp, GoldBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("top_up_balance_chip")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "TOP-UP BALANCE",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = CoolGrey,
                        maxLines = 1
                    )
                    Text(
                        text = "$formattedBalance PTS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrightGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(NexoraSurface)
                    .border(1.dp, DividerDark, CircleShape)
                    .testTag("refresh_home_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Home",
                    tint = MetallicGold,
                    modifier = Modifier.rotate(if (isRefreshing) spinAngle else 0f)
                )
            }

            Box {
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(NexoraSurface)
                        .border(1.dp, DividerDark, CircleShape)
                        .testTag("notifications_button")
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = SoftWhite)
                }
                if (unreadNotifications > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(ErrorRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unreadNotifications.coerceAtMost(9).toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = SoftWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PromoBannerCarousel(
    banners: List<PromoBanner>,
    onBannerClick: (PromoBanner) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { banners.size })

    LaunchedEffect(pagerState.pageCount) {
        while (true) {
            delay(4000)
            if (pagerState.pageCount > 1) {
                val next = (pagerState.currentPage + 1) % pagerState.pageCount
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp
        ) { page ->
            val banner = banners[page]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.5.dp, VioletBorder, RoundedCornerShape(22.dp))
                    .clickable { onBannerClick(banner) }
                    .testTag("promo_banner_${banner.id}")
            ) {
                Image(
                    painter = painterResource(id = banner.imageRes),
                    contentDescription = banner.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    NexoraBlack.copy(alpha = 0.92f),
                                    NexoraBlack.copy(alpha = 0.55f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.fillMaxWidth(0.76f)) {
                        NexoraStatusBadge(
                            text = "FEATURED",
                            containerColor = GoldContainer,
                            contentColor = BrightGold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = banner.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = SoftWhite,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = banner.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = CoolGrey,
                            maxLines = 2
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MetallicGold
                    ) {
                        Text(
                            text = banner.ctaLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NexoraBlack,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(banners.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(if (isSelected) 20.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) ElectricViolet else DividerDark)
                )
            }
        }
    }
}

@Composable
fun CategoryFilterStrip(
    selectedCategory: TournamentCategory,
    onSelectCategory: (TournamentCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TournamentCategory.entries.forEach { category ->
            val selected = category == selectedCategory
            FilterChip(
                selected = selected,
                onClick = { onSelectCategory(category) },
                label = {
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonPurple,
                    selectedLabelColor = SoftWhite,
                    containerColor = NexoraSurface,
                    labelColor = CoolGrey
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = DividerDark,
                    selectedBorderColor = ElectricViolet
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("category_chip_${category.name.lowercase()}")
            )
        }
    }
}

@Composable
fun TournamentCard(
    tournament: Tournament,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (tournament.registeredSlots.toFloat() / tournament.totalSlots.coerceAtLeast(1)).coerceIn(0f, 1f)
    val ptsFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    NexoraGlassCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = if (tournament.isPinned) GoldBorder else VioletBorder,
        onClick = onClick,
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
        ) {
            Image(
                painter = painterResource(id = tournament.imageRes),
                contentDescription = tournament.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, NexoraBlack.copy(alpha = 0.9f))
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NexoraStatusBadge(
                        text = tournament.mode.label.uppercase(),
                        containerColor = NeonPurple.copy(alpha = 0.85f),
                        contentColor = SoftWhite
                    )
                    NexoraStatusBadge(
                        text = "FREE ENTRY",
                        containerColor = SuccessContainer,
                        contentColor = SuccessGreen
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (tournament.isPinned) {
                        NexoraStatusBadge(
                            text = "PINNED",
                            containerColor = GoldContainer,
                            contentColor = BrightGold,
                            icon = Icons.Default.PushPin
                        )
                    }
                    val (statusBg, statusFg) = when (tournament.status) {
                        TournamentStatus.LIVE -> SuccessContainer to SuccessGreen
                        TournamentStatus.UPCOMING -> VioletSubtle to ElectricViolet
                        TournamentStatus.CLOSED, TournamentStatus.CANCELLED -> ErrorContainer to ErrorRed
                        TournamentStatus.COMPLETED -> GoldContainer to BrightGold
                    }
                    NexoraStatusBadge(
                        text = tournament.status.label.uppercase(),
                        containerColor = statusBg,
                        contentColor = statusFg
                    )
                }
            }
            Text(
                text = tournament.title,
                style = MaterialTheme.typography.titleLarge,
                color = SoftWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("REWARD POOL", style = MaterialTheme.typography.labelSmall, color = CoolGrey)
                    Text(
                        "${ptsFormat.format(tournament.rewardPoolPoints)} Pts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrightGold
                    )
                }
                Column {
                    Text("PER KILL", style = MaterialTheme.typography.labelSmall, color = CoolGrey)
                    Text(
                        "${tournament.perKillPoints} Pts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ElectricViolet
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TOP 1 / 2 / 3", style = MaterialTheme.typography.labelSmall, color = CoolGrey)
                    Text(
                        "${tournament.top1Points / 1000}k / ${tournament.top2Points / 1000}k / ${tournament.top3Points / 1000}k",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SoftWhite
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = CoolGrey, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("${tournament.dateLabel} • ${tournament.timeLabel}", style = MaterialTheme.typography.bodySmall, color = CoolGrey)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = CoolGrey, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(tournament.mapName, style = MaterialTheme.typography.bodySmall, color = CoolGrey)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Slots: ${tournament.registeredSlots}/${tournament.totalSlots}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftWhite
                    )
                    Text(
                        text = "${tournament.remainingSlots} left",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (tournament.remainingSlots > 0) SuccessGreen else ErrorRed
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = ElectricViolet,
                    trackColor = NexoraSurface
                )
            }

            NexoraPurpleButton(
                text = if (tournament.isJoinable) "View Details & Join" else "View Details",
                onClick = onClick,
                testTag = "view_tournament_${tournament.id}"
            )
        }
    }
}

@Composable
fun AllTournamentsScreen(
    tournaments: List<Tournament>,
    selectedCategory: TournamentCategory,
    onSelectCategory: (TournamentCategory) -> Unit,
    onBack: () -> Unit,
    onOpenTournament: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(tournaments, query) {
        if (query.isBlank()) tournaments
        else tournaments.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.mapName.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraBlack)
    ) {
        NexoraTopBar(title = "All Tournaments", onBack = onBack)
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            NexoraSearchBar(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Search tournaments or maps..."
            )
        }
        Spacer(Modifier.height(10.dp))
        CategoryFilterStrip(selectedCategory = selectedCategory, onSelectCategory = onSelectCategory)
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(filtered, key = { it.id }) { tournament ->
                TournamentCard(
                    tournament = tournament,
                    onClick = { onOpenTournament(tournament.id) }
                )
            }
        }
    }
}

private data class DrawerNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val isDestructive: Boolean = false
)

@Composable
fun NexoraDrawerContent(
    userProfile: UserProfile,
    onCloseDrawer: () -> Unit,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val items = remember {
        listOf(
            DrawerNavItem("Home", Icons.Default.Home, "home"),
            DrawerNavItem("All Tournaments", Icons.Default.EmojiEvents, "all_tournaments"),
            DrawerNavItem("My Tournaments", Icons.Default.ConfirmationNumber, "my_tournaments"),
            DrawerNavItem("Ongoing Matches", Icons.Default.SportsEsports, "matches_ongoing"),
            DrawerNavItem("Completed Matches", Icons.Default.History, "matches_completed"),
            DrawerNavItem("Leaderboard", Icons.Default.Leaderboard, "leaderboard"),
            DrawerNavItem("Store", Icons.Default.Storefront, "store"),
            DrawerNavItem("Wallet", Icons.Default.AccountBalanceWallet, "wallet"),
            DrawerNavItem("Rewards History", Icons.Default.History, "rewards_history"),
            DrawerNavItem("Referral Team", Icons.Default.Group, "referral_team"),
            DrawerNavItem("Feedback", Icons.Default.Feedback, "feedback"),
            DrawerNavItem("Support & Complaints", Icons.Default.HeadsetMic, "support"),
            DrawerNavItem("Settings", Icons.Default.Settings, "settings"),
            DrawerNavItem("About NexoraHub", Icons.Default.Info, "legal/about"),
            DrawerNavItem("Delete Account", Icons.Default.DeleteForever, "delete_account", isDestructive = true)
        )
    }

    ModalDrawerSheet(
        drawerContainerColor = NexoraCharcoal,
        drawerContentColor = SoftWhite,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NexoraAvatar(
                        avatarUri = userProfile.avatarUri,
                        contentDescription = userProfile.displayName,
                        size = 48.dp,
                        borderColor = MetallicGold
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = userProfile.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = SoftWhite
                        )
                        Text(
                            text = "ID: ${userProfile.nexoraUserId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ElectricViolet
                        )
                    }
                }
                IconButton(onClick = onCloseDrawer) {
                    Icon(Icons.Default.Close, contentDescription = "Close drawer", tint = CoolGrey)
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = DividerDark)
            Spacer(Modifier.height(8.dp))

            items.forEach { item ->
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (item.isDestructive) ErrorRed else SoftWhite
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = if (item.isDestructive) ErrorRed else ElectricViolet
                        )
                    },
                    selected = false,
                    onClick = {
                        onCloseDrawer()
                        onNavigate(item.route)
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = DividerDark)
            Spacer(Modifier.height(12.dp))

            Surface(
                onClick = {
                    onCloseDrawer()
                    onLogout()
                },
                shape = RoundedCornerShape(12.dp),
                color = ErrorContainer,
                border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drawer_logout_button")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = ErrorRed)
                    Spacer(Modifier.width(8.dp))
                    Text("Logout", style = MaterialTheme.typography.titleSmall, color = ErrorRed)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
