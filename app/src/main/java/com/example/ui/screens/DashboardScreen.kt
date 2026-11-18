package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.SaleEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.*
import com.example.viewmodel.*
import java.util.Calendar

@Composable
fun DashboardScreen(
    state: NexoraUiState,
    onLoadDemoData: () -> Unit,
    onClearDemoData: () -> Unit,
    onNavigateToCashier: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToReports: () -> Unit,
    onOpenSaleDetail: (SaleEntity) -> Unit
) {
    var selectedRange by remember { mutableStateOf(DashboardRange.WEEK) }
    var chartGranularity by remember { mutableStateOf(ChartGranularity.DAILY) }

    val activeOutletId = state.activeOutlet?.id
    val outletProducts = remember(state.products, activeOutletId) {
        if (activeOutletId == null) state.products
        else state.products.filter { it.outletId == activeOutletId }
    }
    val lowStockProducts = remember(outletProducts) {
        outletProducts.filter { it.productType != "DIGITAL" && it.stock <= it.minStock && it.active }
    }

    val rangeCutoffMs = remember(selectedRange) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (selectedRange.days > 1) {
            cal.add(Calendar.DAY_OF_YEAR, -(selectedRange.days - 1))
        }
        cal.timeInMillis
    }

    val completedSales = remember(state.sales, activeOutletId, rangeCutoffMs) {
        state.sales.filter {
            it.status == "COMPLETED" &&
                (activeOutletId == null || it.outletId == activeOutletId) &&
                it.createdAtEpochMs >= rangeCutoffMs
        }
    }

    val todayCutoffMs = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todaySales = remember(state.sales, activeOutletId, todayCutoffMs) {
        state.sales.filter {
            it.status == "COMPLETED" &&
                (activeOutletId == null || it.outletId == activeOutletId) &&
                it.createdAtEpochMs >= todayCutoffMs
        }
    }

    val omzetPeriod = completedSales.sumOf { it.total }
    val omzetToday = todaySales.sumOf { it.total }
    val grossProfitPeriod = completedSales.sumOf { it.grossProfit }
    val avgTicket = if (completedSales.isNotEmpty()) omzetPeriod / completedSales.size else 0.0

    val chartPoints = remember(state.sales, activeOutletId, chartGranularity) {
        val validSales = state.sales.filter {
            it.status == "COMPLETED" && (activeOutletId == null || it.outletId == activeOutletId)
        }
        buildChartPoints(validSales, chartGranularity)
    }

    val topProducts = remember(completedSales, state.saleItems) {
        val saleIds = completedSales.map { it.id }.toSet()
        state.saleItems
            .filter { it.saleId in saleIds }
            .groupBy { it.productName }
            .map { (name, items) ->
                Triple(name, items.sumOf { it.quantity }, items.sumOf { it.lineTotal })
            }
            .sortedByDescending { it.second }
            .take(5)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Welcome & Quick Actions
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(NexoraBlue, NexoraViolet)
                        )
                    )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.settings.businessName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${state.activeOutlet?.name ?: "Cabang Utama"} • ${state.activeRole.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        StatusBadge(
                            label = if (state.isOnline) "Online" else "Offline Lokal",
                            color = if (state.isOnline) Color(0xFF4ADE80) else Color(0xFFFBBF24),
                            icon = if (state.isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff
                        )
                    }

                    Text(
                        text = "Omzet Hari Ini: ${formatCurrency(omzetToday, state.settings.currencyCode)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onNavigateToCashier,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = NexoraBlue
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("dashboard_open_cashier_button")
                        ) {
                            Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Buka Kasir", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onNavigateToScanner,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("dashboard_scan_button")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Scan Cepat")
                        }
                    }
                }
            }
        }

        // Demo Data Banner (clearly separated & removable)
        if (state.products.isEmpty() && !state.hasDemoData) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, NexoraViolet.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Mulai Cepat dengan Data Contoh?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Katalog produk masih kosong. Anda bisa menambahkan produk sendiri atau memuat data demo multi-usaha (minimarket, kafe, pakaian, elektronik, produk digital) yang dapat dihapus kapan saja.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = onLoadDemoData,
                            modifier = Modifier.testTag("load_demo_data_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Muat Data Demo (Dapat Dihapus)")
                        }
                    }
                }
            }
        } else if (state.hasDemoData) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NexoraWarningContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Science, contentDescription = null, tint = NexoraWarning)
                            Text(
                                "Mode Data Demo Aktif (terpisah dari data produksi)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        TextButton(
                            onClick = onClearDemoData,
                            modifier = Modifier.testTag("clear_demo_data_banner_button")
                        ) {
                            Text("Hapus Demo", color = NexoraDanger, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Date Range Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DashboardRange.entries) { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { selectedRange = range },
                        label = { Text(range.label) }
                    )
                }
            }
        }

        // 2x2 KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiMetricCard(
                        title = "Omzet (${selectedRange.label})",
                        value = formatShortCurrency(omzetPeriod),
                        subtitle = formatCurrency(omzetPeriod, state.settings.currencyCode),
                        icon = Icons.Default.Payments,
                        accentColor = NexoraBlue,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "Total Transaksi",
                        value = "${completedSales.size}",
                        subtitle = "Rata-rata ${formatShortCurrency(avgTicket)}",
                        icon = Icons.Default.ReceiptLong,
                        accentColor = NexoraViolet,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiMetricCard(
                        title = "Keuntungan Kotor",
                        value = if (state.permissions.canViewProfitReports) formatShortCurrency(grossProfitPeriod) else "Terbatas",
                        subtitle = if (state.permissions.canViewProfitReports) "Margin ${if (omzetPeriod > 0) ((grossProfitPeriod / omzetPeriod) * 100).toInt() else 0}%" else "Khusus Manajer/Owner",
                        icon = Icons.Default.TrendingUp,
                        accentColor = NexoraSuccess,
                        onClick = onNavigateToReports,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "Jumlah Produk",
                        value = "${outletProducts.size}",
                        subtitle = "${lowStockProducts.size} stok menipis",
                        icon = Icons.Default.Inventory2,
                        accentColor = if (lowStockProducts.isNotEmpty()) NexoraWarning else NexoraInfo,
                        onClick = onNavigateToInventory,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Sales Chart (Daily / Weekly / Monthly)
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeader(
                        title = "Grafik Penjualan",
                        subtitle = "Ketuk batang grafik untuk melihat detail omzet & laba"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChartGranularity.entries.forEach { gran ->
                            FilterChip(
                                selected = chartGranularity == gran,
                                onClick = { chartGranularity = gran },
                                label = { Text(gran.label) }
                            )
                        }
                    }
                    NexoraSalesChart(points = chartPoints)
                }
            }
        }

        // Low Stock Warning
        if (lowStockProducts.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, NexoraDanger.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SectionHeader(
                            title = "Peringatan Stok Menipis",
                            subtitle = "${lowStockProducts.size} produk butuh restock",
                            actionLabel = "Kelola Stok",
                            onAction = onNavigateToInventory
                        )
                        lowStockProducts.take(4).forEach { prod ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NexoraDangerContainer.copy(alpha = 0.45f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prod.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text("SKU: ${prod.sku} • Min: ${prod.minStock} ${prod.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                StatusBadge(
                                    label = if (prod.stock == 0) "Habis" else "${prod.stock} ${prod.unit}",
                                    color = NexoraDanger
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Products
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeader(
                        title = "Produk Terlaris",
                        subtitle = "Berdasarkan kuantitas terjual (${selectedRange.label})",
                        actionLabel = "Laporan",
                        onAction = onNavigateToReports
                    )
                    if (topProducts.isEmpty()) {
                        Text(
                            "Belum ada penjualan pada rentang waktu ini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        topProducts.forEachIndexed { idx, (name, qty, rev) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = NexoraBlue.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            "#${idx + 1}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = NexoraBlue,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Column {
                                        Text(name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                        Text("$qty terjual", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text(
                                    formatCurrency(rev, state.settings.currencyCode),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NexoraSuccess
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Transactions
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeader(
                        title = "Transaksi Terbaru",
                        subtitle = "Riwayat penjualan cabang aktif"
                    )
                    val recent = state.sales
                        .filter { activeOutletId == null || it.outletId == activeOutletId }
                        .take(5)
                    if (recent.isEmpty()) {
                        Text(
                            "Belum ada transaksi. Buka menu Kasir untuk transaksi pertama.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        recent.forEach { sale ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onOpenSaleDetail(sale) }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sale.transactionNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${formatDateTime(sale.createdAtEpochMs)} • ${sale.paymentMethod}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        formatCurrency(sale.total, state.settings.currencyCode),
                                        fontWeight = FontWeight.Bold,
                                        color = if (sale.status == "VOIDED") NexoraDanger else MaterialTheme.colorScheme.onSurface
                                    )
                                    StatusBadge(
                                        label = sale.status,
                                        color = when (sale.status) {
                                            "COMPLETED" -> NexoraSuccess
                                            "VOIDED" -> NexoraDanger
                                            else -> NexoraWarning
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildChartPoints(sales: List<SaleEntity>, granularity: ChartGranularity): List<ChartPoint> {
    val now = Calendar.getInstance()
    return when (granularity) {
        ChartGranularity.DAILY -> {
            (6 downTo 0).map { offset ->
                val dayStart = (now.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, -offset)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val dayEnd = (dayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
                val daySales = sales.filter { it.createdAtEpochMs in dayStart.timeInMillis until dayEnd.timeInMillis }
                ChartPoint(
                    label = formatDayShort(dayStart.timeInMillis),
                    revenue = daySales.sumOf { it.total },
                    profit = daySales.sumOf { it.grossProfit }
                )
            }
        }
        ChartGranularity.WEEKLY -> {
            (3 downTo 0).map { weekIdx ->
                val weekStart = (now.clone() as Calendar).apply {
                    add(Calendar.WEEK_OF_YEAR, -weekIdx)
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val weekEnd = (weekStart.clone() as Calendar).apply { add(Calendar.WEEK_OF_YEAR, 1) }
                val wSales = sales.filter { it.createdAtEpochMs in weekStart.timeInMillis until weekEnd.timeInMillis }
                ChartPoint(
                    label = "Mgg ${4 - weekIdx}",
                    revenue = wSales.sumOf { it.total },
                    profit = wSales.sumOf { it.grossProfit }
                )
            }
        }
        ChartGranularity.MONTHLY -> {
            (5 downTo 0).map { monthIdx ->
                val mStart = (now.clone() as Calendar).apply {
                    add(Calendar.MONTH, -monthIdx)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val mEnd = (mStart.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                val mSales = sales.filter { it.createdAtEpochMs in mStart.timeInMillis until mEnd.timeInMillis }
                ChartPoint(
                    label = formatMonthShort(mStart.timeInMillis),
                    revenue = mSales.sumOf { it.total },
                    profit = mSales.sumOf { it.grossProfit }
                )
            }
        }
    }
}
