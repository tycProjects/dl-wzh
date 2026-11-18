package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.SaleEntity
import com.example.data.local.SaleItemEntity
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.ReceiptModal
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.formatCurrency
import com.example.util.formatDateTime
import com.example.viewmodel.NexoraUiState
import com.example.viewmodel.NexoraViewModel
import java.util.Calendar

@Composable
fun TransactionsScreen(
    state: NexoraUiState,
    viewModel: NexoraViewModel,
    initialSelectedSaleId: String? = null,
    onConsumeInitialSale: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Transaksi, 1 = Riwayat Retur
    var searchQuery by remember { mutableStateOf("") }
    var dateFilter by remember { mutableStateOf("ALL") } // TODAY, WEEK, ALL
    var statusFilter by remember { mutableStateOf("ALL") } // ALL, COMPLETED, VOIDED, RETURNED
    var selectedSale by remember { mutableStateOf<SaleEntity?>(null) }
    var receiptTarget by remember { mutableStateOf<Pair<SaleEntity, List<SaleItemEntity>>?>(null) }

    LaunchedEffect(initialSelectedSaleId, state.sales) {
        if (!initialSelectedSaleId.isNullOrBlank()) {
            val found = state.sales.firstOrNull { it.id == initialSelectedSaleId }
            if (found != null) {
                selectedSale = found
                onConsumeInitialSale()
            }
        }
    }

    val activeOutletId = state.activeOutlet?.id
    val cutoffMs = remember(dateFilter) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        when (dateFilter) {
            "TODAY" -> cal.timeInMillis
            "WEEK" -> {
                cal.add(Calendar.DAY_OF_YEAR, -6)
                cal.timeInMillis
            }
            else -> 0L
        }
    }

    val filteredSales = remember(state.sales, activeOutletId, searchQuery, dateFilter, statusFilter, cutoffMs) {
        state.sales.filter { sale ->
            val outletMatch = activeOutletId == null || sale.outletId == activeOutletId
            val dateMatch = dateFilter == "ALL" || sale.createdAtEpochMs >= cutoffMs
            val statusMatch = statusFilter == "ALL" || sale.status == statusFilter
            val searchMatch = searchQuery.isBlank() ||
                sale.transactionNumber.contains(searchQuery, ignoreCase = true) ||
                sale.cashierName.contains(searchQuery, ignoreCase = true) ||
                (sale.customerName?.contains(searchQuery, ignoreCase = true) == true) ||
                sale.itemsSummary.contains(searchQuery, ignoreCase = true)
            outletMatch && dateMatch && statusMatch && searchMatch
        }
    }

    val filteredReturns = remember(state.returns, activeOutletId, searchQuery) {
        state.returns.filter { ret ->
            (activeOutletId == null || ret.outletId == activeOutletId) &&
                (searchQuery.isBlank() ||
                    ret.transactionNumber.contains(searchQuery, ignoreCase = true) ||
                    ret.reason.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Riwayat Transaksi & Retur", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Penjualan (${filteredSales.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Retur (${filteredReturns.size})") }
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari no. struk, pelanggan, atau kasir...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transactions_search_input")
        )

        if (selectedTab == 0) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = dateFilter == "TODAY",
                        onClick = { dateFilter = "TODAY" },
                        label = { Text("Hari Ini") }
                    )
                }
                item {
                    FilterChip(
                        selected = dateFilter == "WEEK",
                        onClick = { dateFilter = "WEEK" },
                        label = { Text("7 Hari") }
                    )
                }
                item {
                    FilterChip(
                        selected = dateFilter == "ALL",
                        onClick = { dateFilter = "ALL" },
                        label = { Text("Semua Tanggal") }
                    )
                }
                item { VerticalDivider(modifier = Modifier.height(32.dp)) }
                listOf("ALL" to "Semua Status", "COMPLETED" to "Selesai", "VOIDED" to "Void", "RETURNED" to "Retur").forEach { (key, label) ->
                    item {
                        FilterChip(
                            selected = statusFilter == key,
                            onClick = { statusFilter = key },
                            label = { Text(label) }
                        )
                    }
                }
            }

            val completedSum = filteredSales.filter { it.status == "COMPLETED" }.sumOf { it.total }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${filteredSales.size} transaksi ditampilkan", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Total Selesai: ${formatCurrency(completedSum, state.settings.currencyCode)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NexoraBlue
                    )
                }
            }

            if (filteredSales.isEmpty()) {
                EmptyStatePanel(
                    icon = Icons.Default.ReceiptLong,
                    title = "Belum Ada Transaksi",
                    description = "Transaksi yang selesai di kasir akan muncul di sini beserta struk dan audit trail."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSales, key = { it.id }) { sale ->
                        val statusColor = when (sale.status) {
                            "COMPLETED" -> NexoraSuccess
                            "VOIDED" -> NexoraDanger
                            else -> NexoraWarning
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSale = sale }
                                .testTag("sale_card_${sale.id}"),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(sale.transactionNumber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    StatusBadge(
                                        label = when (sale.status) {
                                            "COMPLETED" -> "Selesai"
                                            "VOIDED" -> "Dibatalkan"
                                            else -> "Diretur"
                                        },
                                        color = statusColor
                                    )
                                }
                                Text(
                                    text = sale.itemsSummary.ifBlank { "${sale.itemCount} item" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${formatDateTime(sale.createdAtEpochMs)} • ${sale.cashierName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Bayar: ${sale.paymentMethod}" + if (!sale.customerName.isNullOrBlank()) " • ${sale.customerName}" else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NexoraViolet
                                        )
                                    }
                                    Text(
                                        text = formatCurrency(sale.total, state.settings.currencyCode),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sale.status == "VOIDED") NexoraDanger else NexoraBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (filteredReturns.isEmpty()) {
                EmptyStatePanel(
                    icon = Icons.Default.AssignmentReturn,
                    title = "Belum Ada Riwayat Retur",
                    description = "Retur penjualan yang diproses akan tercatat di sini beserta alasan dan nominal pengembalian."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredReturns, key = { it.id }) { ret ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(ret.transactionNumber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Alasan: ${ret.reason}", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "${formatDateTime(ret.createdAtEpochMs)} • Oleh ${ret.actorName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    formatCurrency(ret.refundAmount, state.settings.currencyCode),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NexoraWarning
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedSale?.let { sale ->
        SaleDetailDialog(
            sale = sale,
            state = state,
            viewModel = viewModel,
            onDismiss = { selectedSale = null },
            onOpenReceipt = { items ->
                receiptTarget = sale to items
            },
            onVoid = { reason ->
                viewModel.voidSale(sale, reason)
                selectedSale = null
            },
            onReturn = { reason, refundAmount ->
                viewModel.recordSaleReturn(sale, reason, refundAmount)
                selectedSale = null
            }
        )
    }

    receiptTarget?.let { (sale, items) ->
        val outletName = state.outlets.firstOrNull { it.id == sale.outletId }?.name ?: (state.activeOutlet?.name ?: "NEXORA Store")
        ReceiptModal(
            sale = sale,
            items = items,
            settings = state.settings,
            outletName = outletName,
            onDismiss = { receiptTarget = null }
        )
    }
}

@Composable
private fun SaleDetailDialog(
    sale: SaleEntity,
    state: NexoraUiState,
    viewModel: NexoraViewModel,
    onDismiss: () -> Unit,
    onOpenReceipt: (List<SaleItemEntity>) -> Unit,
    onVoid: (String) -> Unit,
    onReturn: (String, Double) -> Unit
) {
    var items by remember(sale.id) { mutableStateOf<List<SaleItemEntity>>(emptyList()) }
    var loadingItems by remember(sale.id) { mutableStateOf(true) }
    var showVoidPrompt by remember { mutableStateOf(false) }
    var showReturnPrompt by remember { mutableStateOf(false) }
    var reasonInput by remember { mutableStateOf("") }
    var refundInput by remember(sale.id) { mutableStateOf(sale.total.toLong().toString()) }

    LaunchedEffect(sale.id) {
        loadingItems = true
        items = viewModel.fetchSaleItems(sale.id)
        loadingItems = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 540.dp)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(sale.transactionNumber, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(formatDateTime(sale.createdAtEpochMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup Detail")
                    }
                }

                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DetailRow("Kasir", "${sale.cashierName} (${sale.cashierRole})")
                            DetailRow("Pelanggan", sale.customerName ?: "Pelanggan Umum")
                            DetailRow("Metode Bayar", sale.paymentMethod)
                            DetailRow("Status", sale.status)
                            if (sale.notes.isNotBlank()) DetailRow("Catatan", sale.notes)
                        }
                    }

                    Text("Daftar Barang", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (loadingItems) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else if (items.isEmpty()) {
                        Text(sale.itemsSummary, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.productName + if (item.variantLabel.isNotBlank()) " (${item.variantLabel})" else "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${item.quantity} x ${formatCurrency(item.unitPrice, state.settings.currencyCode)}" +
                                            if (item.discountAmount > 0) " • Diskon -${formatCurrency(item.discountAmount, state.settings.currencyCode)}" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    formatCurrency(item.lineTotal, state.settings.currencyCode),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider()
                    DetailRow("Subtotal", formatCurrency(sale.subtotal, state.settings.currencyCode))
                    if (sale.itemDiscountTotal + sale.orderDiscount > 0) {
                        DetailRow("Total Diskon", "-${formatCurrency(sale.itemDiscountTotal + sale.orderDiscount, state.settings.currencyCode)}")
                    }
                    if (sale.taxAmount > 0) {
                        DetailRow("Pajak", formatCurrency(sale.taxAmount, state.settings.currencyCode))
                    }
                    if (sale.extraFee > 0) {
                        DetailRow("Biaya Tambahan", formatCurrency(sale.extraFee, state.settings.currencyCode))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Bayar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(formatCurrency(sale.total, state.settings.currencyCode), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NexoraBlue)
                    }
                    DetailRow("Diterima", formatCurrency(sale.paidAmount, state.settings.currencyCode))
                    DetailRow("Kembalian", formatCurrency(sale.changeAmount, state.settings.currencyCode))
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { onOpenReceipt(items) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("reopen_receipt_button")
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Lihat / Cetak / Bagikan Struk")
                }

                if (sale.status == "COMPLETED" && state.permissions.canVoidOrReturn) {
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { reasonInput = ""; showReturnPrompt = true },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("return_sale_button")
                        ) {
                            Icon(Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retur")
                        }
                        OutlinedButton(
                            onClick = { reasonInput = ""; showVoidPrompt = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoraDanger),
                            border = BorderStroke(1.dp, NexoraDanger),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("void_sale_button")
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Void")
                        }
                    }
                }
            }
        }
    }

    if (showVoidPrompt) {
        AlertDialog(
            onDismissRequest = { showVoidPrompt = false },
            title = { Text("Void Transaksi ${sale.transactionNumber}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Stok barang akan dikembalikan otomatis dan tindakan ini dicatat pada Audit Trail.")
                    OutlinedTextField(
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        label = { Text("Alasan Pembatalan *") },
                        placeholder = { Text("Misal: Salah input item / pelanggan batal") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reasonInput.isNotBlank()) {
                            onVoid(reasonInput.trim())
                            showVoidPrompt = false
                        }
                    },
                    enabled = reasonInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NexoraDanger)
                ) { Text("Konfirmasi Void") }
            },
            dismissButton = { TextButton(onClick = { showVoidPrompt = false }) { Text("Batal") } }
        )
    }

    if (showReturnPrompt) {
        AlertDialog(
            onDismissRequest = { showReturnPrompt = false },
            title = { Text("Retur Penjualan", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        label = { Text("Alasan Retur *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = refundInput,
                        onValueChange = { refundInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Nominal Pengembalian") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = refundInput.toDoubleOrNull() ?: sale.total
                        if (reasonInput.isNotBlank()) {
                            onReturn(reasonInput.trim(), amt)
                            showReturnPrompt = false
                        }
                    },
                    enabled = reasonInput.isNotBlank()
                ) { Text("Simpan Retur") }
            },
            dismissButton = { TextButton(onClick = { showReturnPrompt = false }) { Text("Batal") } }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
