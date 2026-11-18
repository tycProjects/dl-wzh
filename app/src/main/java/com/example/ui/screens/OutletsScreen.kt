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
import com.example.data.local.OutletEntity
import com.example.data.local.ProductEntity
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.formatCurrency
import com.example.viewmodel.NexoraUiState
import com.example.viewmodel.NexoraViewModel

@Composable
fun OutletsScreen(
    state: NexoraUiState,
    viewModel: NexoraViewModel
) {
    var editingOutlet by remember { mutableStateOf<OutletEntity?>(null) }
    var showOutletDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Multi-Outlet & Cabang", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Kelola cabang toko, perpindahan cabang aktif, dan transfer stok antarcabang.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.permissions.canManageOutlets) {
                Button(
                    onClick = { editingOutlet = null; showOutletDialog = true },
                    modifier = Modifier.testTag("add_outlet_button")
                ) {
                    Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cabang")
                }
            }
        }

        if (state.outlets.size >= 2 && (state.permissions.canManageOutlets || state.permissions.canManageInventory)) {
            OutlinedButton(
                onClick = { showTransferDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("transfer_stock_button")
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Transfer Stok Antarcabang")
            }
        }

        if (state.outlets.isEmpty()) {
            EmptyStatePanel(
                icon = Icons.Default.Store,
                title = "Belum Ada Cabang",
                description = "Tambahkan cabang usaha Anda untuk memisahkan inventaris dan omzet per lokasi."
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.outlets, key = { it.id }) { outlet ->
                    val isActive = state.activeOutlet?.id == outlet.id
                    val branchProducts = state.products.filter { it.outletId == outlet.id }
                    val branchRevenue = state.sales.filter { it.outletId == outlet.id && it.status == "COMPLETED" }.sumOf { it.total }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            if (isActive) 1.5.dp else 1.dp,
                            if (isActive) NexoraBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(outlet.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(outlet.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Telp: ${outlet.phone}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (isActive) StatusBadge("Aktif Dipakai", NexoraBlue)
                                    if (outlet.mainOutlet) StatusBadge("Pusat", NexoraViolet)
                                }
                            }
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${branchProducts.size} produk • Omzet ${formatCurrency(branchRevenue, state.settings.currencyCode)}", style = MaterialTheme.typography.labelMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (!isActive) {
                                        FilledTonalButton(onClick = { viewModel.switchOutlet(outlet) }) {
                                            Text("Pilih Cabang")
                                        }
                                    }
                                    if (state.permissions.canManageOutlets) {
                                        IconButton(onClick = { editingOutlet = outlet; showOutletDialog = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Cabang")
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

    if (showOutletDialog) {
        var name by remember(editingOutlet) { mutableStateOf(editingOutlet?.name ?: "") }
        var address by remember(editingOutlet) { mutableStateOf(editingOutlet?.address ?: "") }
        var phone by remember(editingOutlet) { mutableStateOf(editingOutlet?.phone ?: "") }
        var active by remember(editingOutlet) { mutableStateOf(editingOutlet?.active ?: true) }

        AlertDialog(
            onDismissRequest = { showOutletDialog = false },
            title = { Text(if (editingOutlet == null) "Tambah Cabang Baru" else "Edit Cabang", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Cabang *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("No. Telepon") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Status Cabang Aktif")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveOutlet(
                            OutletEntity(
                                id = editingOutlet?.id ?: "",
                                name = name.trim(),
                                address = address.trim().ifBlank { "-" },
                                phone = phone.trim().ifBlank { "-" },
                                active = active,
                                mainOutlet = editingOutlet?.mainOutlet ?: false,
                                demoData = editingOutlet?.demoData ?: false
                            )
                        )
                        showOutletDialog = false
                    },
                    enabled = name.isNotBlank()
                ) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showOutletDialog = false }) { Text("Batal") } }
        )
    }

    if (showTransferDialog) {
        val activeOutletId = state.activeOutlet?.id
        val sourceProducts = state.products.filter {
            it.productType != "DIGITAL" && it.stock > 0 && (activeOutletId == null || it.outletId == activeOutletId)
        }
        val otherOutlets = state.outlets.filter { it.id != activeOutletId && it.active }
        var selectedProduct by remember { mutableStateOf<ProductEntity?>(sourceProducts.firstOrNull()) }
        var selectedTarget by remember { mutableStateOf<OutletEntity?>(otherOutlets.firstOrNull()) }
        var qtyInput by remember { mutableStateOf("5") }

        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = { Text("Transfer Stok Antarcabang", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (sourceProducts.isEmpty() || otherOutlets.isEmpty()) {
                        Text("Pastikan ada produk berstok di cabang aktif dan minimal 1 cabang tujuan aktif.")
                    } else {
                        Text("Dari: ${state.activeOutlet?.name ?: "-"}", style = MaterialTheme.typography.labelLarge)
                        Text("Pilih Produk:", style = MaterialTheme.typography.labelMedium)
                        sourceProducts.take(6).forEach { prod ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(selected = selectedProduct?.id == prod.id, onClick = { selectedProduct = prod })
                                Text("${prod.name} (Stok: ${prod.stock})", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text("Cabang Tujuan:", style = MaterialTheme.typography.labelMedium)
                        otherOutlets.forEach { target ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                RadioButton(selected = selectedTarget?.id == target.id, onClick = { selectedTarget = target })
                                Text(target.name, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        OutlinedTextField(
                            value = qtyInput,
                            onValueChange = { qtyInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Jumlah Transfer") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = selectedProduct ?: return@Button
                        val t = selectedTarget ?: return@Button
                        val q = qtyInput.toIntOrNull() ?: 0
                        if (q > 0) {
                            viewModel.transferStock(p.id, t.id, q)
                            showTransferDialog = false
                        }
                    },
                    enabled = selectedProduct != null && selectedTarget != null && (qtyInput.toIntOrNull() ?: 0) > 0
                ) { Text("Transfer") }
            },
            dismissButton = { TextButton(onClick = { showTransferDialog = false }) { Text("Batal") } }
        )
    }
}
