package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.RestaurantTableEntity
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.formatCurrency
import com.example.viewmodel.NexoraUiState
import com.example.viewmodel.NexoraViewModel

@Composable
fun RestaurantScreen(
    state: NexoraUiState,
    viewModel: NexoraViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Meja, 1 = Dapur (KDS)
    var showAddTableDialog by remember { mutableStateOf(false) }
    var showNewOrderDialog by remember { mutableStateOf(false) }

    val activeOutletId = state.activeOutlet?.id
    val tables = remember(state.tables, activeOutletId) {
        state.tables.filter { activeOutletId == null || it.outletId == activeOutletId }
    }
    val orders = remember(state.restaurantOrders, activeOutletId) {
        state.restaurantOrders.filter { activeOutletId == null || it.outletId == activeOutletId }
    }
    val foodMenu = remember(state.products, activeOutletId) {
        state.products.filter { it.productType == "FOOD" && it.active && (activeOutletId == null || it.outletId == activeOutletId) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showAddTableDialog = true else showNewOrderDialog = true
                },
                modifier = Modifier.testTag("restaurant_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Manajemen Meja (${tables.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Dapur / KDS (${orders.count { it.status != "SERVED" }})") })
            }

            if (selectedTab == 0) {
                if (tables.isEmpty()) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        EmptyStatePanel(
                            icon = Icons.Default.TableRestaurant,
                            title = "Belum Ada Meja",
                            description = "Tambahkan meja restoran untuk mendukung pesanan Dine-In dan status okupansi.",
                            actionLabel = "Tambah Meja",
                            onAction = { showAddTableDialog = true }
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(156.dp),
                        contentPadding = PaddingValues(14.dp, 14.dp, 14.dp, 84.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(tables, key = { it.id }) { tbl ->
                            val statusColor = when (tbl.status) {
                                "AVAILABLE" -> NexoraSuccess
                                "OCCUPIED" -> NexoraDanger
                                else -> NexoraWarning
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.5.dp, statusColor.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(tbl.tableName, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.TableRestaurant, contentDescription = null, tint = statusColor)
                                    }
                                    Text("Kapasitas ${tbl.capacity} orang", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    StatusBadge(
                                        label = when (tbl.status) {
                                            "AVAILABLE" -> "Kosong"
                                            "OCCUPIED" -> "Terisi ${tbl.activeOrderNumber ?: ""}".trim()
                                            else -> "Dipesan"
                                        },
                                        color = statusColor
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                val next = when (tbl.status) {
                                                    "AVAILABLE" -> "RESERVED"
                                                    "RESERVED" -> "OCCUPIED"
                                                    else -> "AVAILABLE"
                                                }
                                                viewModel.saveTable(tbl.copy(status = next, activeOrderNumber = if (next == "AVAILABLE") null else tbl.activeOrderNumber))
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Ubah Status", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Kitchen Display System (KDS) & Queue
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp, 14.dp, 14.dp, 84.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (orders.isEmpty()) {
                        item {
                            EmptyStatePanel(
                                icon = Icons.Default.SoupKitchen,
                                title = "Antrean Dapur Kosong",
                                description = "Buat pesanan Dine-In atau Takeaway baru untuk ditampilkan ke dapur.",
                                actionLabel = "Buat Pesanan Dapur",
                                onAction = { showNewOrderDialog = true }
                            )
                        }
                    } else {
                        items(orders, key = { it.id }) { order ->
                            val statusColor = when (order.status) {
                                "QUEUED" -> NexoraWarning
                                "PREPARING" -> NexoraBlue
                                "READY" -> NexoraSuccess
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(order.queueNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NexoraBlue)
                                            StatusBadge(
                                                if (order.orderType == "DINE_IN") "Dine-In • ${order.tableName ?: ""}" else "Takeaway",
                                                NexoraViolet
                                            )
                                        }
                                        StatusBadge(order.status, statusColor)
                                    }
                                    Text(order.itemsSummary, fontWeight = FontWeight.Medium)
                                    if (order.notes.isNotBlank()) {
                                        Text("Catatan: ${order.notes}", style = MaterialTheme.typography.bodySmall, color = NexoraWarning)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(formatCurrency(order.totalAmount, state.settings.currencyCode), fontWeight = FontWeight.Bold)
                                        if (order.status != "SERVED") {
                                            val nextStatus = when (order.status) {
                                                "QUEUED" -> "PREPARING"
                                                "PREPARING" -> "READY"
                                                else -> "SERVED"
                                            }
                                            val nextLabel = when (nextStatus) {
                                                "PREPARING" -> "Mulai Masak"
                                                "READY" -> "Siap Saji"
                                                else -> "Selesai Disajikan"
                                            }
                                            Button(onClick = { viewModel.updateRestaurantOrderStatus(order, nextStatus) }) {
                                                Text(nextLabel)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTableDialog) {
        var name by remember { mutableStateOf("Meja ${tables.size + 1}") }
        var cap by remember { mutableStateOf("4") }
        AlertDialog(
            onDismissRequest = { showAddTableDialog = false },
            title = { Text("Tambah Meja Restoran") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama / Nomor Meja*") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = cap, onValueChange = { cap = it.filter { ch -> ch.isDigit() } }, label = { Text("Kapasitas Kursi") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveTable(
                            RestaurantTableEntity(
                                id = "",
                                outletId = activeOutletId ?: "outlet_main",
                                tableName = name.trim(),
                                capacity = cap.toIntOrNull()?.coerceIn(1, 100) ?: 4,
                                status = "AVAILABLE"
                            )
                        )
                        showAddTableDialog = false
                    },
                    enabled = name.isNotBlank()
                ) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showAddTableDialog = false }) { Text("Batal") } }
        )
    }

    if (showNewOrderDialog) {
        var orderType by remember { mutableStateOf("DINE_IN") }
        var selectedTable by remember { mutableStateOf(tables.firstOrNull { it.status == "AVAILABLE" } ?: tables.firstOrNull()) }
        var itemsText by remember { mutableStateOf(foodMenu.firstOrNull()?.let { "${it.name} x1" } ?: "") }
        var notes by remember { mutableStateOf("") }
        var totalText by remember { mutableStateOf(foodMenu.firstOrNull()?.sellPrice?.toLong()?.toString() ?: "35000") }

        AlertDialog(
            onDismissRequest = { showNewOrderDialog = false },
            title = { Text("Buat Pesanan Dapur", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = orderType == "DINE_IN", onClick = { orderType = "DINE_IN" }, label = { Text("Dine-In") }, modifier = Modifier.weight(1f))
                        FilterChip(selected = orderType == "TAKEAWAY", onClick = { orderType = "TAKEAWAY" }, label = { Text("Takeaway") }, modifier = Modifier.weight(1f))
                    }
                    if (orderType == "DINE_IN" && tables.isNotEmpty()) {
                        var tExp by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { tExp = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Meja: ${selectedTable?.tableName ?: "Pilih Meja"}")
                            }
                            DropdownMenu(expanded = tExp, onDismissRequest = { tExp = false }) {
                                tables.forEach { t ->
                                    DropdownMenuItem(text = { Text("${t.tableName} (${t.status})") }, onClick = { selectedTable = t; tExp = false })
                                }
                            }
                        }
                    }
                    OutlinedTextField(value = itemsText, onValueChange = { itemsText = it }, label = { Text("Daftar Menu*") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Catatan Dapur (cth: tidak pedas)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = totalText, onValueChange = { totalText = it.filter { ch -> ch.isDigit() } }, label = { Text("Estimasi Total (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createRestaurantOrder(
                            orderType,
                            selectedTable,
                            itemsText.trim(),
                            notes.trim(),
                            totalText.toDoubleOrNull() ?: 0.0
                        )
                        showNewOrderDialog = false
                    },
                    enabled = itemsText.isNotBlank()
                ) { Text("Kirim ke Dapur") }
            },
            dismissButton = { TextButton(onClick = { showNewOrderDialog = false }) { Text("Batal") } }
        )
    }
}
