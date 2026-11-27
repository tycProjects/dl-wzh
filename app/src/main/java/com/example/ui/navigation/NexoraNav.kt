package com.example.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.domain.model.BottomNavTab
import com.example.ui.screens.auth.NexoraSplashScreen
import com.example.ui.screens.auth.WelcomeAuthScreen
import com.example.ui.screens.home.AllTournamentsScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.NexoraDrawerContent
import com.example.ui.screens.leaderboard.LeaderboardScreen
import com.example.ui.screens.matches.MatchDetailsScreen
import com.example.ui.screens.matches.MatchesScreen
import com.example.ui.screens.profile.EditProfileScreen
import com.example.ui.screens.profile.PlayerIdentityScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.RedeemCodeScreen
import com.example.ui.screens.profile.ReferralTeamScreen
import com.example.ui.screens.rewards.FreeCreditsScreen
import com.example.ui.screens.rewards.RewardsHistoryScreen
import com.example.ui.screens.rewards.RewardsScreen
import com.example.ui.screens.rewards.WalletScreen
import com.example.ui.screens.settings.DeleteAccountScreen
import com.example.ui.screens.settings.LegalDocumentScreen
import com.example.ui.screens.settings.MaintenanceScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.store.ListingDetailsScreen
import com.example.ui.screens.store.PrivateChatScreen
import com.example.ui.screens.store.PublishListingScreen
import com.example.ui.screens.store.StoreScreen
import com.example.ui.screens.support.FeedbackScreen
import com.example.ui.screens.support.NotificationsScreen
import com.example.ui.screens.support.SupportCenterScreen
import com.example.ui.screens.tournaments.MyTournamentsScreen
import com.example.ui.screens.tournaments.TournamentDetailsScreen
import com.example.ui.screens.tournaments.TournamentRegistrationScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.NexoraViewModel
import com.example.ui.viewmodel.UiEvent
import kotlinx.coroutines.launch

@Composable
fun NexoraApp(
    viewModel: NexoraViewModel = viewModel()
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val promoBanners by viewModel.promoBanners.collectAsStateWithLifecycle()
    val tournaments by viewModel.tournaments.collectAsStateWithLifecycle()
    val filteredTournaments by viewModel.filteredTournaments.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val registrations by viewModel.registrations.collectAsStateWithLifecycle()
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val listings by viewModel.listings.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val availablePoints by viewModel.availablePoints.collectAsStateWithLifecycle()
    val pendingPoints by viewModel.pendingPoints.collectAsStateWithLifecycle()
    val totalEarnedPoints by viewModel.totalEarnedPoints.collectAsStateWithLifecycle()
    val freeCreditsBalance by viewModel.freeCreditsBalance.collectAsStateWithLifecycle()
    val freeCreditItems by viewModel.freeCreditItems.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val tickets by viewModel.tickets.collectAsStateWithLifecycle()
    val referrals by viewModel.referrals.collectAsStateWithLifecycle()
    val maintenanceConfig by viewModel.maintenanceConfig.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val authLoading by viewModel.authLoading.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()

    var splashDone by remember { mutableStateOf(false) }
    var matchesInitialTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.Navigate -> navController.navigate(event.route)
                UiEvent.AccountDeleted -> {
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    LaunchedEffect(isAuthenticated, splashDone) {
        if (splashDone) {
            val currentRoute = navController.currentDestination?.route
            if (isAuthenticated && (currentRoute == "welcome" || currentRoute == "splash")) {
                navController.navigate(BottomNavTab.HOME.route) {
                    popUpTo(0) { inclusive = true }
                }
            } else if (!isAuthenticated && currentRoute != "welcome" && currentRoute?.startsWith("legal/") != true) {
                navController.navigate("welcome") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    if (maintenanceConfig.isMaintenanceActive) {
        MaintenanceScreen(
            config = maintenanceConfig,
            onExitPreview = { viewModel.toggleMaintenancePreview(false) }
        )
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = isAuthenticated && currentRoute in BottomNavTab.entries.map { it.route }

    fun navigateFromDrawer(route: String) {
        when (route) {
            "matches_ongoing" -> {
                matchesInitialTab = 0
                navController.navigate(BottomNavTab.MATCHES.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                }
            }
            "matches_completed" -> {
                matchesInitialTab = 1
                navController.navigate(BottomNavTab.MATCHES.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                }
            }
            BottomNavTab.HOME.route,
            BottomNavTab.STORE.route -> {
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            else -> navController.navigate(route)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isAuthenticated && showBottomBar,
        drawerContent = {
            NexoraDrawerContent(
                userProfile = userProfile,
                onCloseDrawer = { scope.launch { drawerState.close() } },
                onNavigate = ::navigateFromDrawer,
                onLogout = { viewModel.signOut() }
            )
        }
    ) {
        Scaffold(
            containerColor = NexoraBlack,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = NexoraClay,
                        contentColor = SoftWhite,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    Surface(
                        color = NexoraCharcoal,
                        border = BorderStroke(1.dp, DividerDark)
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            BottomNavTab.entries.forEach { tab ->
                                val selected = currentRoute == tab.route
                                val icon = when (tab) {
                                    BottomNavTab.HOME -> Icons.Default.Home
                                    BottomNavTab.MATCHES -> Icons.Default.SportsEsports
                                    BottomNavTab.STORE -> Icons.Default.Storefront
                                    BottomNavTab.REWARDS -> Icons.Default.EmojiEvents
                                    BottomNavTab.PROFILE -> Icons.Default.Person
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(icon, contentDescription = tab.label) },
                                    label = {
                                        Text(
                                            tab.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SoftWhite,
                                        selectedTextColor = ElectricViolet,
                                        indicatorColor = NeonPurple,
                                        unselectedIconColor = CoolGrey,
                                        unselectedTextColor = CoolGrey
                                    ),
                                    modifier = Modifier.testTag("bottom_nav_${tab.route}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NexoraBlack)
                    .padding(if (showBottomBar) PaddingValues(bottom = innerPadding.calculateBottomPadding()) else PaddingValues(0.dp))
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "splash"
                ) {
                    composable("splash") {
                        NexoraSplashScreen(
                            onFinished = {
                                splashDone = true
                                val dest = if (isAuthenticated) BottomNavTab.HOME.route else "welcome"
                                navController.navigate(dest) {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("welcome") {
                        WelcomeAuthScreen(
                            isLoading = authLoading,
                            errorMessage = authError,
                            onOpenTerms = { navController.navigate("legal/terms") },
                            onOpenPrivacy = { navController.navigate("legal/privacy") },
                            onSignInWithGoogle = { viewModel.signInWithGoogle() }
                        )
                    }

                    composable(BottomNavTab.HOME.route) {
                        HomeScreen(
                            availablePoints = availablePoints,
                            banners = promoBanners,
                            tournaments = filteredTournaments,
                            ongoingMatches = matches.filter { it.isOngoing },
                            selectedCategory = selectedCategory,
                            unreadNotifications = unreadCount,
                            isRefreshing = isRefreshing,
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onOpenFreeCredits = { navController.navigate("free_credits") },
                            onOpenWallet = { navController.navigate("wallet") },
                            onRefresh = { viewModel.refreshHome() },
                            onOpenNotifications = { navController.navigate("notifications") },
                            onSelectCategory = viewModel::selectCategory,
                            onBannerClick = { banner ->
                                when (banner.destinationRoute) {
                                    "free_credits" -> navController.navigate("free_credits")
                                    "store" -> navigateFromDrawer(BottomNavTab.STORE.route)
                                    else -> navController.navigate(banner.destinationRoute)
                                }
                            },
                            onSeeAllTournaments = { navController.navigate("all_tournaments") },
                            onOpenTournament = { id -> navController.navigate("tournament/$id") },
                            onOpenMatch = { matchId -> navController.navigate("match/$matchId") },
                            onSeeAllMatches = { navigateFromDrawer("matches_ongoing") }
                        )
                    }

                    composable(BottomNavTab.MATCHES.route) {
                        MatchesScreen(
                            matches = matches,
                            initialTab = matchesInitialTab,
                            onOpenMatchDetails = { matchId -> navController.navigate("match/$matchId") }
                        )
                    }

                    composable(BottomNavTab.STORE.route) {
                        StoreScreen(
                            listings = listings,
                            onOpenListing = { id -> navController.navigate("listing/$id") },
                            onOpenPublishListing = { navController.navigate("publish_listing") }
                        )
                    }

                    composable(BottomNavTab.REWARDS.route) {
                        RewardsScreen(
                            availablePoints = availablePoints,
                            pendingPoints = pendingPoints,
                            totalEarnedPoints = totalEarnedPoints,
                            freeCreditsBalance = freeCreditsBalance,
                            transactions = transactions,
                            onOpenWallet = { navController.navigate("wallet") },
                            onOpenFreeCredits = { navController.navigate("free_credits") },
                            onOpenRewardsHistory = { navController.navigate("rewards_history") },
                            onOpenRedeemCode = { navController.navigate("redeem_code") }
                        )
                    }

                    composable(BottomNavTab.PROFILE.route) {
                        ProfileScreen(
                            userProfile = userProfile,
                            onUpdateAvatar = { uri -> viewModel.updateProfile(userProfile.displayName, uri) },
                            onCopyReferral = { viewModel.showToast("Referral code copied!") },
                            onNavigate = { route -> navController.navigate(route) },
                            onLogout = { viewModel.signOut() }
                        )
                    }

                    composable("all_tournaments") {
                        AllTournamentsScreen(
                            tournaments = filteredTournaments,
                            selectedCategory = selectedCategory,
                            onSelectCategory = viewModel::selectCategory,
                            onBack = { navController.popBackStack() },
                            onOpenTournament = { id -> navController.navigate("tournament/$id") }
                        )
                    }

                    composable(
                        route = "tournament/{tournamentId}",
                        arguments = listOf(navArgument("tournamentId") { type = NavType.StringType })
                    ) { backStack ->
                        val id = backStack.arguments?.getString("tournamentId").orEmpty()
                        val tournament = tournaments.find { it.id == id } ?: tournaments.first()
                        val existingReg = registrations.find { it.tournamentId == tournament.id }
                        TournamentDetailsScreen(
                            tournament = tournament,
                            existingRegistration = existingReg,
                            leaderboardPreview = viewModel.getLeaderboard(com.example.domain.model.LeaderboardFilter.CURRENT_TOURNAMENT),
                            onBack = { navController.popBackStack() },
                            onShare = { viewModel.showToast("Tournament invite link copied") },
                            onJoinTournament = {
                                if (existingReg != null) {
                                    navController.navigate("my_tournaments")
                                } else {
                                    navController.navigate("register/${tournament.id}")
                                }
                            },
                            onOpenFullLeaderboard = { navController.navigate("leaderboard") }
                        )
                    }

                    composable(
                        route = "register/{tournamentId}",
                        arguments = listOf(navArgument("tournamentId") { type = NavType.StringType })
                    ) { backStack ->
                        val id = backStack.arguments?.getString("tournamentId").orEmpty()
                        val tournament = tournaments.find { it.id == id } ?: tournaments.first()
                        TournamentRegistrationScreen(
                            tournament = tournament,
                            userProfile = userProfile,
                            onBack = { navController.popBackStack() },
                            onSubmitRegistration = { teamName, players, onSuccess, onError ->
                                viewModel.submitTournamentRegistration(tournament, teamName, players, onSuccess, onError)
                            },
                            onViewMyTournaments = {
                                navController.navigate("my_tournaments") {
                                    popUpTo(BottomNavTab.HOME.route)
                                }
                            }
                        )
                    }

                    composable("my_tournaments") {
                        MyTournamentsScreen(
                            registrations = registrations,
                            onBack = { navController.popBackStack() },
                            onExploreTournaments = { navController.navigate("all_tournaments") }
                        )
                    }

                    composable(
                        route = "match/{matchId}",
                        arguments = listOf(navArgument("matchId") { type = NavType.StringType })
                    ) { backStack ->
                        val matchId = backStack.arguments?.getString("matchId").orEmpty()
                        val match = matches.find { it.matchId == matchId } ?: matches.first()
                        MatchDetailsScreen(
                            match = match,
                            onBack = { navController.popBackStack() },
                            onCopiedMessage = viewModel::showToast
                        )
                    }

                    composable("leaderboard") {
                        LeaderboardScreen(
                            getLeaderboard = viewModel::getLeaderboard,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "listing/{listingId}",
                        arguments = listOf(navArgument("listingId") { type = NavType.StringType })
                    ) { backStack ->
                        val listingId = backStack.arguments?.getString("listingId").orEmpty()
                        val listing = listings.find { it.id == listingId } ?: listings.first()
                        ListingDetailsScreen(
                            listing = listing,
                            onBack = { navController.popBackStack() },
                            onOpenChat = { navController.navigate("chat/${listing.id}") },
                            onReportListing = { reason -> viewModel.reportListing(listing.id, reason) }
                        )
                    }

                    composable("publish_listing") {
                        PublishListingScreen(
                            onBack = { navController.popBackStack() },
                            onSubmitListing = viewModel::submitListing
                        )
                    }

                    composable(
                        route = "chat/{listingId}",
                        arguments = listOf(navArgument("listingId") { type = NavType.StringType })
                    ) { backStack ->
                        val listingId = backStack.arguments?.getString("listingId").orEmpty()
                        val listing = listings.find { it.id == listingId } ?: listings.first()
                        PrivateChatScreen(
                            listing = listing,
                            conversation = conversations[listing.id],
                            onBack = { navController.popBackStack() },
                            onSendMessage = { text -> viewModel.sendChatMessage(listing.id, text) },
                            onToggleBlock = { viewModel.toggleBlockSeller(listing.id) },
                            onReportUser = { viewModel.reportListing(listing.id, "Reported user in private chat") }
                        )
                    }

                    composable("wallet") {
                        WalletScreen(
                            availablePoints = availablePoints,
                            pendingPoints = pendingPoints,
                            totalEarnedPoints = totalEarnedPoints,
                            freeCreditsBalance = freeCreditsBalance,
                            transactions = transactions,
                            onBack = { navController.popBackStack() },
                            onOpenFreeCredits = { navController.navigate("free_credits") },
                            onSeeAllHistory = { navController.navigate("rewards_history") }
                        )
                    }

                    composable("free_credits") {
                        FreeCreditsScreen(
                            freeCreditsBalance = freeCreditsBalance,
                            creditItems = freeCreditItems,
                            onBack = { navController.popBackStack() },
                            onOpenRedeemCode = { navController.navigate("redeem_code") }
                        )
                    }

                    composable("rewards_history") {
                        RewardsHistoryScreen(
                            transactions = transactions,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("notifications") {
                        NotificationsScreen(
                            notifications = notifications,
                            onBack = { navController.popBackStack() },
                            onMarkAllRead = { viewModel.markAllNotificationsRead() },
                            onMarkRead = { viewModel.markNotificationRead(it) }
                        )
                    }

                    composable("edit_profile") {
                        EditProfileScreen(
                            userProfile = userProfile,
                            onBack = { navController.popBackStack() },
                            onSaveProfile = viewModel::updateProfile
                        )
                    }

                    composable("player_identity") {
                        PlayerIdentityScreen(
                            userProfile = userProfile,
                            onBack = { navController.popBackStack() },
                            onSaveIdentity = viewModel::updateSavedPlayerIdentity
                        )
                    }

                    composable("referral_team") {
                        ReferralTeamScreen(
                            userProfile = userProfile,
                            referrals = referrals,
                            onBack = { navController.popBackStack() },
                            onCopyCode = { viewModel.showToast("Referral code copied!") }
                        )
                    }

                    composable("redeem_code") {
                        RedeemCodeScreen(
                            onBack = { navController.popBackStack() },
                            onRedeem = viewModel::redeemPromoCode
                        )
                    }

                    composable("support") {
                        SupportCenterScreen(
                            tickets = tickets,
                            onBack = { navController.popBackStack() },
                            onCreateTicket = viewModel::createSupportTicket,
                            onReplyToTicket = viewModel::replyToTicket
                        )
                    }

                    composable("feedback") {
                        FeedbackScreen(
                            onBack = { navController.popBackStack() },
                            onSubmitFeedback = viewModel::submitFeedback
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            settings = userSettings,
                            maintenanceConfig = maintenanceConfig,
                            onBack = { navController.popBackStack() },
                            onUpdateSettings = viewModel::updateSettings,
                            onToggleMaintenancePreview = viewModel::toggleMaintenancePreview,
                            onNavigate = { route -> navController.navigate(route) },
                            onLogout = { viewModel.signOut() }
                        )
                    }

                    composable("delete_account") {
                        DeleteAccountScreen(
                            nexoraUserId = userProfile.nexoraUserId,
                            onBack = { navController.popBackStack() },
                            onConfirmDeletion = viewModel::requestAccountDeletion
                        )
                    }

                    composable(
                        route = "legal/{docType}",
                        arguments = listOf(navArgument("docType") { type = NavType.StringType })
                    ) { backStack ->
                        val docType = backStack.arguments?.getString("docType") ?: "about"
                        LegalDocumentScreen(
                            docType = docType,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
