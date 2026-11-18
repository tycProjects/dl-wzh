package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ProductEntity
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.formatCurrency
import com.example.util.formatDateTime
import com.example.viewmodel.NexoraUiState
import com.example.viewmodel.NexoraViewModel

@Composable
fun DigitalStoreScreen(
    state: NexoraUiState,
    viewModel: NexoraViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Katalog Layanan, 1=Riwayat Pengiriman Manual
    var orderProduct by remember { mutableStateOf<ProductEntity?>(null) }

    val activeOutletId = state.activeOutlet?.id
    val digitalProducts = remember(state.products, activeOutletId) {
        state.products.filter { it.productType == "DIGITAL" && (activeOutletId == null || it.outletId == activeOutletId) }
    }
    val digitalOrders = remember(state.digitalOrders, activeOutletId) {
        state.digitalOrders.filter { activeOutletId == null || it.outletId == activeOutletId }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Produk Digital (${digitalProducts.size})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Pengiriman Manual (${digitalOrders.size})") })
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = NexoraInfoContainer.copy(alpha = 0.55f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = NexoraInfo)
                Text(
                    "Pengiriman produk digital diproses manual oleh staf dan dicatat pada riwayat pengiriman untuk transparansi pelanggan.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (selectedTab == 0) {
            if (digitalProducts.isEmpty()) {
                Box(modifier = Modifier.padding(14.dp)) {
                    EmptyStatePanel(
                        icon = Icons.Default.Bolt,
                        title = "Belum Ada Produk Digital",
                        description = "Tambahkan produk bertipe Digital (Pulsa, Paket Data, Token PLN, Voucher) dari menu Produk."
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(digitalProducts, key = { it.id }) { prod ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(prod.name, fontWeight = FontWeight.Bold)
                                    if (prod.variantsSummary.isNotBlank()) {
                                        Text("Paket: ${prod.variantsSummary}", style = MaterialTheme.typography.bodySmall, color = NexoraViolet)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(formatCurrency(prod.sellPrice, state.settings.currencyCode), fontWeight = FontWeight.Bold, color = NexoraBlue)
                                        StatusBadge(if (prod.active) "Tersedia" else "Nonaktif", if (prod.active) NexoraSuccess else NexoraDanger)
                                    }
                                }
                                Button(
                                    onClick = { orderProduct = prod },
                                    enabled = prod.active,
                                    modifier = Modifier.testTag("digital_order_btn_${prod.id}")
                                ) {
                                    Text("Proses")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (digitalOrders.isEmpty()) {
                Box(modifier = Modifier.padding(14.dp)) {
                    EmptyStatePanel(
                        icon = Icons.Default.SendTimeExtension,
                        title = "Belum Ada Pesanan Digital",
                        description = "Pesanan produk digital akan muncul di sini untuk dikonfirmasi pengirimannya secara manual."
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(digitalOrders, key = { it.id }) { order ->
                        val color = when (order.deliveryStatus) {
                            "DELIVERED" -> NexoraSuccess
                            "FAILED" -> NexoraDanger
                            else -> NexoraWarning
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${order.productName} (${order.variantLabel})", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    StatusBadge(order.deliveryStatus, color)
                                }
                                Text("Tujuan: ${order.recipientInfo}", style = MaterialTheme.typography.bodyMedium)
                                if (order.notes.isNotBlank()) {
                                    Text("Catatan: ${order.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(formatDateTime(order.createdAtEpochMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (order.deliveryStatus == "PENDING") {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(onClick = { viewModel.updateDigitalDeliveryStatus(order, "FAILED", "Gagal diproses") }) {
                                                Text("Gagal", color = NexoraDanger)
                                            }
                                            Button(onClick = { viewModel.updateDigitalDeliveryStatus(order, "DELIVERED", "Terkirim manual oleh staf") }) {
                                                Text("Tandai Terkirim")
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

    orderProduct?.let { prod ->
        val variants = prod.variantsSummary.split(",").map { it.trim() }.filter { it.isNotBlank() }
        var selectedVariant by remember { mutableStateOf(variants.firstOrNull() ?: "Reguler") }
        var recipient by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { orderProduct = null },
            title = { Text("Pesanan Digital: ${prod.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (variants.isNotEmpty()) {
                        Text("Pilih Paket", style = MaterialTheme.typography.labelMedium)
                        variants.forEach { v ->
                            FilterChip(selected = selectedVariant == v, onClick = { selectedVariant = v }, label = { Text(v) })
                        }
                    }
                    OutlinedTextField(
                        value = recipient,
                        onValueChange = { recipient = it },
                        label = { Text("Nomor HP / ID Pelanggan / Email*") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan Pesanan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createDigitalOrder(prod, selectedVariant, recipient, notes)
                        orderProduct = null
                        selectedTab = 1
                    },
                    enabled = recipient.isNotBlank()
                ) { Text("Buat Pesanan Manual") }
            },
            dismissButton = { TextButton(onClick = { orderProduct = null }) { Text("Batal") } }
        )
    }
}
