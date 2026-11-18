package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.NexoraDatabase
import com.example.data.local.SaleEntity
import com.example.data.local.SaleItemEntity
import com.example.data.remote.FirestoreSyncService
import com.example.data.remote.authStateFlow
import com.example.data.repository.AppPreferencesRepository
import com.example.data.repository.NetworkMonitor
import com.example.data.repository.NexoraRepository
import com.example.ui.components.OutletSwitchDialog
import com.example.ui.components.ReceiptModal
import com.example.ui.components.StaffSwitchDialog
import com.example.ui.components.StatusBadge
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.NexoraViewModel
import com.example.viewmodel.UiEvent
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_CASHIER = "cashier"
const val ROUTE_PRODUCTS = "products"
const val ROUTE_INVENTORY = "inventory"
const val ROUTE_MORE = "more"
const val ROUTE_SCANNER = "scanner"
const val ROUTE_RESTAURANT = "restaurant"
const val ROUTE_DIGITAL = "digital"
const val ROUTE_CUSTOMERS = "customers"
const val ROUTE_TRANSACTIONS = "transactions"
const val ROUTE_REPORTS = "reports"
const val ROUTE_OUTLETS = "outlets"
const val ROUTE_STAFF = "staff"
const val ROUTE_SETTINGS = "settings"

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

private val primaryMobileDestinations = listOf(
    NavDestinationItem(ROUTE_DASHBOARD, "Beranda", Icons.Default.Dashboard, "nav_dashboard"),
    NavDestinationItem(ROUTE_CASHIER, "Kasir", Icons.Default.PointOfSale, "nav_cashier"),
    NavDestinationItem(ROUTE_PRODUCTS, "Produk", Icons.Default.Inventory2, "nav_products"),
    NavDestinationItem(ROUTE_INVENTORY, "Stok", Icons.Default.Warehouse, "nav_inventory"),
    NavDestinationItem(ROUTE_MORE, "Lainnya", Icons.Default.Apps, "nav_more")
)

private val expandedRailDestinations = listOf(
    NavDestinationItem(ROUTE_DASHBOARD, "Beranda", Icons.Default.Dashboard, "rail_dashboard"),
    NavDestinationItem(ROUTE_CASHIER, "Kasir", Icons.Default.PointOfSale, "rail_cashier"),
    NavDestinationItem(ROUTE_SCANNER, "Scanner", Icons.Default.QrCodeScanner, "rail_scanner"),
    NavDestinationItem(ROUTE_PRODUCTS, "Produk", Icons.Default.Inventory2, "rail_products"),
    NavDestinationItem(ROUTE_INVENTORY, "Stok", Icons.Default.Warehouse, "rail_inventory"),
    NavDestinationItem(ROUTE_RESTAURANT, "Restoran", Icons.Default.Restaurant, "rail_restaurant"),
    NavDestinationItem(ROUTE_DIGITAL, "Digital", Icons.Default.PhonelinkSetup, "rail_digital"),
    NavDestinationItem(ROUTE_CUSTOMERS, "Pelanggan", Icons.Default.Groups, "rail_customers"),
    NavDestinationItem(ROUTE_TRANSACTIONS, "Transaksi", Icons.Default.ReceiptLong, "rail_transactions"),
    NavDestinationItem(ROUTE_REPORTS, "Laporan", Icons.Default.Assessment, "rail_reports"),
    NavDestinationItem(ROUTE_OUTLETS, "Cabang", Icons.Default.Store, "rail_outlets"),
    NavDestinationItem(ROUTE_STAFF, "Staf", Icons.Default.AdminPanelSettings, "rail_staff"),
    NavDestinationItem(ROUTE_SETTINGS, "Pengaturan", Icons.Default.Settings, "rail_settings")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NexoraAppRoot()
        }
    }
}

@Composable
fun NexoraAppRoot() {
    val authUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    if (authUser == null) {
        NexoraPosTheme {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        val user = authUser!!
        key(user.uid) {
            NexoraAuthenticatedApp(
                uid = user.uid,
                displayName = user.displayName ?: "Pemilik Usaha",
                email = user.email ?: ""
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NexoraAuthenticatedApp(
    uid: String,
    displayName: String,
    email: String
) {
    val context = LocalContext.current.applicationContext
    val factory = remember(uid) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = NexoraDatabase.getInstance(context)
                val syncService = FirestoreSyncService(context)
                val networkMonitor = NetworkMonitor(context)
                val prefs = AppPreferencesRepository(context)
                val repo = NexoraRepository(db.nexoraDao(), syncService, networkMonitor)
                return NexoraViewModel(
                    repository = repo,
                    prefs = prefs,
                    networkMonitor = networkMonitor,
                    currentUserId = uid,
                    ownerDisplayName = displayName,
                    ownerEmail = email
                ) as T
            }
        }
    }

    val nexoraViewModel: NexoraViewModel = viewModel(key = uid, factory = factory)
    val state by nexoraViewModel.uiState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_DASHBOARD
    val snackbarHostState = remember { SnackbarHostState() }

    var showOutletDialog by remember { mutableStateOf(false) }
    var showStaffDialog by remember { mutableStateOf(false) }
    var activeReceipt by remember { mutableStateOf<Pair<SaleEntity, List<SaleItemEntity>>?>(null) }
    var pendingDetailSaleId by remember { mutableStateOf<String?>(null) }

    val navigateToRoute: (String) -> Unit = remember(navController) {
        { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    LaunchedEffect(nexoraViewModel) {
        nexoraViewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is UiEvent.OpenReceipt -> {
                    activeReceipt = event.sale to event.items
                }
                is UiEvent.NavigateToAddProductWithBarcode -> {
                    navigateToRoute(ROUTE_PRODUCTS)
                }
            }
        }
    }

    NexoraPosTheme(themeMode = state.themeMode) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedWidth = maxWidth >= 840.dp
            val primaryRoutes = remember { primaryMobileDestinations.map { it.route }.toSet() }
            val showBackArrow = !isExpandedWidth && currentRoute !in primaryRoutes && navController.previousBackStackEntry != null

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            if (showBackArrow) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                                }
                            } else {
                                Image(
                                    painter = painterResource(R.drawable.ic_nexora_launcher),
                                    contentDescription = "Logo NEXORA POS",
                                    modifier = Modifier
                                        .padding(start = 12.dp)
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(9.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        },
                        title = {
                            Column(
                                modifier = Modifier
                                    .clickable { showOutletDialog = true }
                                    .padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = state.settings.businessName.ifBlank { "NEXORA POS" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${state.activeOutlet?.name ?: "Cabang Utama"} ▾",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NexoraBlue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        actions = {
                            // Role Switch Chip
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clickable { showStaffDialog = true }
                                    .testTag("topbar_role_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Badge, contentDescription = "Ganti Operator", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Text(
                                        text = state.activeRole.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            // Network / Sync Indicator
                            Box(
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .clickable {
                                        if (state.pendingSyncCount > 0 && state.isOnline) nexoraViewModel.syncNow()
                                        else navigateToRoute(ROUTE_SETTINGS)
                                    }
                                    .testTag("topbar_sync_badge")
                            ) {
                                StatusBadge(
                                    label = when {
                                        state.isSyncing -> "Sync..."
                                        !state.isOnline -> "Offline"
                                        state.pendingSyncCount > 0 -> "Sync (${state.pendingSyncCount})"
                                        else -> "Online"
                                    },
                                    color = when {
                                        !state.isOnline -> NexoraWarning
                                        state.pendingSyncCount > 0 -> NexoraInfo
                                        else -> NexoraSuccess
                                    }
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!isExpandedWidth) {
                        NavigationBar {
                            primaryMobileDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route ||
                                    (dest.route == ROUTE_MORE && currentRoute !in primaryRoutes)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToRoute(dest.route) },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (dest.route == ROUTE_CASHIER && state.cartItemCount > 0) {
                                                    Badge { Text("${state.cartItemCount}") }
                                                }
                                            }
                                        ) {
                                            Icon(dest.icon, contentDescription = dest.label)
                                        }
                                    },
                                    label = { Text(dest.label, maxLines = 1) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedWidth) {
                        NavigationRail(
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            expandedRailDestinations.forEach { dest ->
                                NavigationRailItem(
                                    selected = currentRoute == dest.route,
                                    onClick = { navigateToRoute(dest.route) },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (dest.route == ROUTE_CASHIER && state.cartItemCount > 0) {
                                                    Badge { Text("${state.cartItemCount}") }
                                                }
                                            }
                                        ) {
                                            Icon(dest.icon, contentDescription = dest.label)
                                        }
                                    },
                                    label = { Text(dest.label, maxLines = 1) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                        VerticalDivider()
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_DASHBOARD
                        ) {
                            composable(ROUTE_DASHBOARD) {
                                DashboardScreen(
                                    state = state,
                                    onLoadDemoData = { nexoraViewModel.loadDemoData() },
                                    onClearDemoData = { nexoraViewModel.clearDemoData() },
                                    onNavigateToCashier = { navigateToRoute(ROUTE_CASHIER) },
                                    onNavigateToScanner = { navigateToRoute(ROUTE_SCANNER) },
                                    onNavigateToInventory = { navigateToRoute(ROUTE_INVENTORY) },
                                    onNavigateToReports = { navigateToRoute(ROUTE_REPORTS) },
                                    onOpenSaleDetail = { sale ->
                                        pendingDetailSaleId = sale.id
                                        navigateToRoute(ROUTE_TRANSACTIONS)
                                    }
                                )
                            }
                            composable(ROUTE_CASHIER) {
                                CashierScreen(
                                    state = state,
                                    viewModel = nexoraViewModel,
                                    isExpandedWidth = isExpandedWidth,
                                    onOpenScanner = { navigateToRoute(ROUTE_SCANNER) },
                                    onNavigateToAddProduct = { navigateToRoute(ROUTE_PRODUCTS) }
                                )
                            }
                            composable(ROUTE_PRODUCTS) {
                                ProductsScreen(
                                    state = state,
                                    viewModel = nexoraViewModel,
                                    initialAddWithBarcode = state.prefillProductBarcode
                                )
                            }
                            composable(ROUTE_INVENTORY) {
                                InventoryScreen(
                                    state = state,
                                    viewModel = nexoraViewModel
                                )
                            }
                            composable(ROUTE_MORE) {
                                MoreHubScreen(
                                    state = state,
                                    onNavigate = navigateToRoute
                                )
                            }
                            composable(ROUTE_SCANNER) {
                                ScannerScreen(
                                    state = state,
                                    viewModel = nexoraViewModel,
                                    onGoToCashier = { navigateToRoute(ROUTE_CASHIER) }
                                )
                            }
                            composable(ROUTE_RESTAURANT) {
                                RestaurantScreen(
                                    state = state,
                                    viewModel = nexoraViewModel
                                )
                            }
                            composable(ROUTE_DIGITAL) {
                                DigitalStoreScreen(
                                    state = state,
                                    viewModel = nexoraViewModel
                                )
                            }
                            composable(ROUTE_CUSTOMERS) {
                                CustomersScreen(
                                    state = state,
                                    viewModel = nexoraViewModel
                                )
                            }
                            composable(ROUTE_TRANSACTIONS) {
                                TransactionsScreen(
                                    state = state,
                                    viewModel = nexoraViewModel,
                                    initialSelectedSaleId = pendingDetailSaleId,
                                    onConsumeInitialSale = { pendingDetailSaleId = null }
                                )
                            }
                            composable(ROUTE_REPORTS) {
                                ReportsScreen(
                                    state = state,
                                    viewModel = nexoraViewModel
                                )
                            }
                            composable(ROUTE_OUTLETS) {
                                OutletsScreen(
                                    state = state,
                                    viewModel = nexoraViewModel
                                )
                            }
                            composable(ROUTE_STAFF) {
                                StaffScreen(
                                    state = state,
                                    viewModel = nexoraViewModel
                                )
                            }
                            composable(ROUTE_SETTINGS) {
                                SettingsAndProfileScreen(
                                    state = state,
                                    viewModel = nexoraViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOutletDialog) {
        OutletSwitchDialog(
            outlets = state.outlets,
            activeOutletId = state.activeOutlet?.id,
            onDismiss = { showOutletDialog = false },
            onSelect = { nexoraViewModel.switchOutlet(it) }
        )
    }

    if (showStaffDialog) {
        StaffSwitchDialog(
            staffMembers = state.staffMembers,
            activeStaffId = state.activeStaff?.id,
            onDismiss = { showStaffDialog = false },
            onSwitch = { member, pin -> nexoraViewModel.switchStaffWithPin(member, pin) }
        )
    }

    activeReceipt?.let { (sale, items) ->
        val outletName = state.outlets.firstOrNull { it.id == sale.outletId }?.name ?: (state.activeOutlet?.name ?: "NEXORA Store")
        ReceiptModal(
            sale = sale,
            items = items,
            settings = state.settings,
            outletName = outletName,
            onDismiss = { activeReceipt = null }
        )
    }
}
