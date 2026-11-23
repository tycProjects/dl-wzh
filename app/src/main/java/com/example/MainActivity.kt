package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.ClubSelectionScreen
import com.example.ui.CompetitionsScreen
import com.example.ui.DashboardScreen
import com.example.ui.MatchScreen
import com.example.ui.PressConferenceScreen
import com.example.ui.SquadScreen
import com.example.ui.TransferMarketScreen
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TouchlineTheme
import com.example.ui.theme.TurfEmerald
import com.example.viewmodel.CareerViewModel

const val ROUTE_HUB = "hub"
const val ROUTE_SQUAD = "squad"
const val ROUTE_COMPETITIONS = "competitions"
const val ROUTE_TRANSFERS = "transfers"

private data class NavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val TopDestinations = listOf(
    NavDestination(ROUTE_HUB, "Club", Icons.Filled.Home, Icons.Outlined.Home),
    NavDestination(ROUTE_SQUAD, "Squad", Icons.Filled.Groups, Icons.Outlined.Groups),
    NavDestination(ROUTE_COMPETITIONS, "League", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
    NavDestination(ROUTE_TRANSFERS, "Market", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            TouchlineTheme {
                TouchlineApp()
            }
        }
    }
}

@Composable
fun TouchlineApp(
    viewModel: CareerViewModel = viewModel(),
) {
    val careerState by viewModel.careerState.collectAsStateWithLifecycle()
    val liveMatch by viewModel.liveMatch.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val activeMatch = liveMatch
    if (activeMatch != null) {
        val activeUserClub = viewModel.userClub(careerState)
        val activeDivision = careerState.divisions.find { it.id == activeUserClub?.divisionId }
        Scaffold(
            containerColor = PitchBlack,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                MatchScreen(
                    session = activeMatch,
                    divisionName = activeDivision?.name ?: "League",
                    domesticCupName = activeUserClub?.country?.domesticCupName ?: "Domestic Cup",
                    continentalName = activeMatch.fixture.continentalId?.displayName
                        ?: careerState.continentalCompetitionName,
                    onTogglePause = viewModel::togglePauseMatch,
                    onSetSpeed = viewModel::setMatchSpeed,
                    onSimulateRest = viewModel::simulateRemainingMatch,
                    onMakeSub = viewModel::makeInMatchSubstitution,
                    onSetTactics = viewModel::setLiveMatchTactics,
                    onFinishMatch = viewModel::finishAndCommitMatch,
                )
            }
        }
        return
    }

    if (!careerState.careerStarted) {
        ClubSelectionScreen(
            divisions = careerState.divisions,
            clubs = careerState.clubs,
            onStartCareer = viewModel::startCareer,
        )
        return
    }

    val userClub = viewModel.userClub(careerState) ?: return
    val userDivision = remember(careerState.divisions, userClub.divisionId) {
        careerState.divisions.find { it.id == userClub.divisionId }
    }

    val pendingPress = careerState.pendingPressConference
    if (pendingPress != null) {
        Scaffold(
            containerColor = PitchBlack,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                PressConferenceScreen(
                    pressSession = pendingPress,
                    userClub = userClub,
                    onSelectOption = viewModel::answerPressQuestion,
                    onComplete = viewModel::completePressConference,
                    onSkip = viewModel::skipPressConference,
                )
            }
        }
        return
    }

    val sortedStandings = remember(careerState.standings, careerState.clubs, userClub.divisionId) {
        viewModel.sortedStandingsForDivision(userClub.divisionId, careerState)
    }
    val nextFixture = remember(careerState) {
        viewModel.nextUserMatch(careerState)
    }
    val pendingNonUserStage = remember(careerState) {
        viewModel.pendingNonUserStageLabel(careerState)
    }
    val unreadInboxCount = remember(careerState.inboxMessages) {
        careerState.inboxMessages.count { !it.isRead }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_HUB

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = PitchBlack,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        contentColor = TextPrimary,
                    ) {
                        TopDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    if (dest.route == ROUTE_HUB && unreadInboxCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = CrimsonRed, contentColor = TextPrimary) {
                                                    Text("$unreadInboxCount")
                                                }
                                            },
                                        ) {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.label,
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label,
                                        )
                                    }
                                },
                                label = { Text(dest.label, maxLines = 1, softWrap = false) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PitchBlack,
                                    selectedTextColor = TurfEmerald,
                                    indicatorColor = TurfEmerald,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                ),
                                modifier = Modifier.testTag("nav_${dest.route}"),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        contentColor = TextPrimary,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        TopDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    if (dest.route == ROUTE_HUB && unreadInboxCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = CrimsonRed, contentColor = TextPrimary) {
                                                    Text("$unreadInboxCount")
                                                }
                                            },
                                        ) {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.label,
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label,
                                        )
                                    }
                                },
                                label = { Text(dest.label, maxLines = 1, softWrap = false) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = PitchBlack,
                                    selectedTextColor = TurfEmerald,
                                    indicatorColor = TurfEmerald,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                ),
                                modifier = Modifier.testTag("rail_${dest.route}"),
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_HUB,
                    modifier = Modifier.weight(1f),
                ) {
                    composable(ROUTE_HUB) {
                        DashboardScreen(
                            state = careerState,
                            userClub = userClub,
                            userDivision = userDivision,
                            sortedStandings = sortedStandings,
                            nextFixture = nextFixture,
                            pendingNonUserStageLabel = pendingNonUserStage,
                            onPlayMatch = { viewModel.startNextMatchday() },
                            onAdvanceNonUserStage = viewModel::advanceNonUserStage,
                            onStartNextSeason = viewModel::startNextSeason,
                            onRunTraining = viewModel::runTraining,
                            onMarkInboxRead = viewModel::markInboxRead,
                            onRespondToOffer = viewModel::respondToTransferOffer,
                            onNavigateToSquad = { navigateToTab(ROUTE_SQUAD) },
                            onNavigateToTransfers = { navigateToTab(ROUTE_TRANSFERS) },
                            onResetCareer = viewModel::resetCareer,
                        )
                    }
                    composable(ROUTE_SQUAD) {
                        SquadScreen(
                            club = userClub,
                            youthAcademy = careerState.youthAcademy,
                            onSelectFormation = viewModel::setFormation,
                            onSwapPlayers = viewModel::swapPlayers,
                            onScoutYouth = viewModel::scoutYouthProspects,
                            onPromoteYouth = viewModel::promoteYouthProspect,
                            onReleaseYouth = viewModel::releaseYouthProspect,
                            onRenewContract = viewModel::renewPlayerContract,
                        )
                    }
                    composable(ROUTE_COMPETITIONS) {
                        CompetitionsScreen(
                            state = careerState,
                            userClub = userClub,
                        )
                    }
                    composable(ROUTE_TRANSFERS) {
                        TransferMarketScreen(
                            userClub = userClub,
                            marketPlayers = careerState.transferMarket,
                            onBuyPlayer = viewModel::buyPlayer,
                            onSellPlayer = viewModel::sellPlayer,
                        )
                    }
                }
            }
        }
    }
}
