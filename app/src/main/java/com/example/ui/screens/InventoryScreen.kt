package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.example.data.local.ProductEntity
import com.example.data.local.SupplierEntity
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.formatCurrency
import com.example.util.formatDateTime
import com.example.viewmodel.NexoraUiState
import com.example.viewmodel.NexoraViewModel

@Composable
fun InventoryScreen(
    state: NexoraUiState,
    viewModel: NexoraViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Stok & Opname, 1=Riwayat Stok, 2=Supplier, 3=Pembelian
    var adjustTarget by remember { mutableStateOf<Pair<ProductEntity, String>?>(null) } // product + type (IN/OUT/OPNAME)
    var showSupplierDialog by remember { mutableStateOf(false) }
    var showPurchaseDialog by remember { mutableStateOf(false) }

    val activeOutletId = state.activeOutlet?.id
    val outletProducts = remember(state.products, activeOutletId) {
        state.products.filter { it.productType != "DIGITAL" && (activeOutletId == null || it.outletId == activeOutletId) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Stok & Opname") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Riwayat Stok") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Supplier (${state.suppliers.size})") })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Pembelian (${state.purchases.size})") })
        }

        when (selectedTab) {
            0 -> {
                val lowCount = outletProducts.count { it.stock in 1..it.minStock }
                val outCount = outletProducts.count { it.stock == 0 }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusBadge("Total Item: ${outletProducts.size}", NexoraBlue)
                            StatusBadge("Menipis: $lowCount", NexoraWarning)
                            StatusBadge("Habis: $outCount", NexoraDanger)
                        }
                    }
                    if (outletProducts.isEmpty()) {
                        item {
                            EmptyStatePanel(
                                icon = Icons.Default.Warehouse,
                                title = "Belum Ada Stok Produk Fisik",
                                description = "Tambahkan produk di menu Produk untuk memantau stok masuk, keluar, dan opname."
                            )
                        }
                    } else {
                        items(outletProducts, key = { it.id }) { prod ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
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
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(prod.name, fontWeight = FontWeight.Bold)
                                            Text("SKU: ${prod.sku} • Min: ${prod.minStock} ${prod.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        StatusBadge(
                                            label = if (prod.stock == 0) "Habis" else "${prod.stock} ${prod.unit}",
                                            color = when {
                                                prod.stock == 0 -> NexoraDanger
                                                prod.stock <= prod.minStock -> NexoraWarning
                                                else -> NexoraSuccess
                                            }
                                        )
                                    }
                                    if (state.permissions.canManageInventory) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(
                                                onClick = { adjustTarget = prod to "IN" },
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                            ) { Text("+ Masuk", style = MaterialTheme.typography.labelSmall) }
                                            OutlinedButton(
                                                onClick = { adjustTarget = prod to "OUT" },
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                            ) { Text("- Keluar", style = MaterialTheme.typography.labelSmall) }
                                            FilledTonalButton(
                                                onClick = { adjustTarget = prod to "OPNAME" },
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                            ) { Text("Opname", style = MaterialTheme.typography.labelSmall) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.stockMovements.isEmpty()) {
                        item {
                            EmptyStatePanel(
                                icon = Icons.Default.History,
                                title = "Belum Ada Mutasi Stok",
                                description = "Setiap perubahan stok masuk, keluar, penjualan, retur, dan opname tercatat di sini."
                            )
                        }
                    } else {
                        items(state.stockMovements, key = { it.id }) { mov ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(mov.productName, fontWeight = FontWeight.Bold)
                                        Text("${mov.type} • Oleh: ${mov.actorName}", style = MaterialTheme.typography.bodySmall)
                                        if (mov.note.isNotBlank()) {
                                            Text(mov.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(formatDateTime(mov.createdAtEpochMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        val sign = if (mov.quantityDelta > 0) "+${mov.quantityDelta}" else "${mov.quantityDelta}"
                                        Text(
                                            sign,
                                            fontWeight = FontWeight.Bold,
                                            color = if (mov.quantityDelta >= 0) NexoraSuccess else NexoraDanger
                                        )
                                        Text("${mov.previousStock} -> ${mov.newStock}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp, 14.dp, 14.dp, 84.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (state.suppliers.isEmpty()) {
                            item {
                                EmptyStatePanel(
                                    icon = Icons.Default.LocalShipping,
                                    title = "Belum Ada Supplier",
                                    description = "Simpan data pemasok barang untuk mencatat pembelian dan stok masuk.",
                                    actionLabel = if (state.permissions.canManageInventory) "Tambah Supplier" else null,
                                    onAction = if (state.permissions.canManageInventory) { { showSupplierDialog = true } } else null
                                )
                            }
                        } else {
                            items(state.suppliers, key = { it.id }) { sup ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(sup.name, fontWeight = FontWeight.Bold)
                                        Text("${sup.phone} • ${sup.address}", style = MaterialTheme.typography.bodySmall)
                                        if (sup.notes.isNotBlank()) {
                                            Text(sup.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (state.permissions.canManageInventory) {
                        FloatingActionButton(
                            onClick = { showSupplierDialog = true },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah Supplier")
                        }
                    }
                }
            }

            3 -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp, 14.dp, 14.dp, 84.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (state.purchases.isEmpty()) {
                            item {
                                EmptyStatePanel(
                                    icon = Icons.Default.ShoppingCartCheckout,
                                    title = "Belum Ada Pembelian",
                                    description = "Catat pembelian dari supplier untuk otomatis menambah stok produk.",
                                    actionLabel = if (state.permissions.canManageInventory && state.suppliers.isNotEmpty() && outletProducts.isNotEmpty()) "Catat Pembelian" else null,
                                    onAction = { showPurchaseDialog = true }
                                )
                            }
                        } else {
                            items(state.purchases, key = { it.id }) { po ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(po.invoiceNumber, fontWeight = FontWeight.Bold)
                                            StatusBadge(
                                                label = po.status,
                                                color = if (po.status == "RECEIVED") NexoraSuccess else NexoraDanger
                                            )
                                        }
                                        Text("Supplier: ${po.supplierName}", style = MaterialTheme.typography.bodyMedium)
                                        if (po.notes.isNotBlank()) {
                                            Text(po.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(formatCurrency(po.totalAmount, state.settings.currencyCode), fontWeight = FontWeight.Bold, color = NexoraBlue)
                                            if (po.status == "RECEIVED" && state.permissions.canManageInventory) {
                                                TextButton(onClick = { viewModel.returnPurchase(po) }) {
                                                    Text("Retur Pembelian", color = NexoraDanger)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (state.permissions.canManageInventory && state.suppliers.isNotEmpty() && outletProducts.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = { showPurchaseDialog = true },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Catat Pembelian")
                        }
                    }
                }
            }
        }
    }

    adjustTarget?.let { (prod, type) ->
        var qtyText by remember { mutableStateOf(if (type == "OPNAME") prod.stock.toString() else "10") }
        var note by remember { mutableStateOf("") }
        val title = when (type) {
            "IN" -> "Stok Masuk: ${prod.name}"
            "OUT" -> "Stok Keluar: ${prod.name}"
            else -> "Stock Opname: ${prod.name}"
        }
        AlertDialog(
            onDismissRequest = { adjustTarget = null },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Stok saat ini: ${prod.stock} ${prod.unit}", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (type == "OPNAME") "Stok Fisik Aktual" else "Jumlah (${prod.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("stock_adjust_qty_input")
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = qtyText.toIntOrNull() ?: 0
                        viewModel.adjustStock(prod.id, type, qty, note)
                        adjustTarget = null
                    },
                    enabled = qtyText.isNotBlank()
                ) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { adjustTarget = null }) { Text("Batal") } }
        )
    }

    if (showSupplierDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSupplierDialog = false },
            title = { Text("Tambah Supplier", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Supplier*") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("No. Telepon") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Catatan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveSupplier(SupplierEntity("", name, phone, address, notes))
                        showSupplierDialog = false
                    },
                    enabled = name.isNotBlank()
                ) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showSupplierDialog = false }) { Text("Batal") } }
        )
    }

    if (showPurchaseDialog && state.suppliers.isNotEmpty() && outletProducts.isNotEmpty()) {
        var selectedSup by remember { mutableStateOf(state.suppliers.first()) }
        var selectedProd by remember { mutableStateOf(outletProducts.first()) }
        var qtyText by remember { mutableStateOf("10") }
        var costText by remember { mutableStateOf(selectedProd.buyPrice.toLong().toString()) }
        var invoice by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPurchaseDialog = false },
            title = { Text("Catat Pembelian Barang", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var supExp by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { supExp = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Supplier: ${selectedSup.name}")
                        }
                        DropdownMenu(expanded = supExp, onDismissRequest = { supExp = false }) {
                            state.suppliers.forEach { sup ->
                                DropdownMenuItem(text = { Text(sup.name) }, onClick = { selectedSup = sup; supExp = false })
                            }
                        }
                    }

                    var prodExp by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { prodExp = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Produk: ${selectedProd.name}")
                        }
                        DropdownMenu(expanded = prodExp, onDismissRequest = { prodExp = false }) {
                            outletProducts.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.name) },
                                    onClick = {
                                        selectedProd = p
                                        costText = p.buyPrice.toLong().toString()
                                        prodExp = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(value = invoice, onValueChange = { invoice = it }, label = { Text("No. Invoice / PO") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = qtyText, onValueChange = { qtyText = it.filter { ch -> ch.isDigit() } }, label = { Text("Jumlah") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = costText, onValueChange = { costText = it.filter { ch -> ch.isDigit() } }, label = { Text("Harga Beli Satuan (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Catatan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.recordPurchase(
                            selectedSup,
                            selectedProd,
                            qtyText.toIntOrNull() ?: 1,
                            costText.toDoubleOrNull() ?: selectedProd.buyPrice,
                            invoice,
                            notes
                        )
                        showPurchaseDialog = false
                    },
                    enabled = (qtyText.toIntOrNull() ?: 0) > 0
                ) { Text("Simpan Pembelian") }
            },
            dismissButton = { TextButton(onClick = { showPurchaseDialog = false }) { Text("Batal") } }
        )
    }
}
