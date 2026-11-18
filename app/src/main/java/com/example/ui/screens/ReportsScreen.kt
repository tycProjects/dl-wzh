package com.example.ui.screens

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.SaleEntity
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.NexoraSalesChart
import com.example.ui.theme.*
import com.example.util.formatCurrency
import com.example.util.formatDateOnly
import com.example.util.formatDayShort
import com.example.util.formatShortCurrency
import com.example.viewmodel.ChartPoint
import com.example.viewmodel.NexoraUiState
import com.example.viewmodel.NexoraViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun ReportsScreen(
    state: NexoraUiState,
    viewModel: NexoraViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Omzet & Laba, 1=Produk Terlaris, 2=Stok, 3=Kasir
    var daysRange by remember { mutableIntStateOf(30) } // 1, 7, 30, 0(all)
    var exportStatus by remember { mutableStateOf<String?>(null) }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val csv = viewModel.exportSalesCsv()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(csv.toByteArray(Charsets.UTF_8))
                    }
                    exportStatus = "Laporan CSV berhasil diekspor"
                } catch (e: Exception) {
                    exportStatus = "Gagal ekspor CSV: ${e.localizedMessage}"
                }
            }
        }
    }

    val activeOutletId = state.activeOutlet?.id
    val cutoff = remember(daysRange) {
        if (daysRange == 0) 0L else {
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (daysRange > 1) add(Calendar.DAY_OF_YEAR, -(daysRange - 1))
            }.timeInMillis
        }
    }

    val filteredSales = remember(state.sales, activeOutletId, cutoff) {
        state.sales.filter {
            it.status == "COMPLETED" &&
                (activeOutletId == null || it.outletId == activeOutletId) &&
                it.createdAtEpochMs >= cutoff
        }
    }

    val totalRevenue = filteredSales.sumOf { it.total }
    val totalGrossProfit = filteredSales.sumOf { it.grossProfit }
    val avgTicket = if (filteredSales.isNotEmpty()) totalRevenue / filteredSales.size else 0.0
    val marginPct = if (totalRevenue > 0) (totalGrossProfit / totalRevenue) * 100.0 else 0.0

    val outletProducts = remember(state.products, activeOutletId) {
        state.products.filter { activeOutletId == null || it.outletId == activeOutletId }
    }
    val stockAssetValue = outletProducts.filter { it.productType != "DIGITAL" }.sumOf { it.buyPrice * it.stock }

    val chartPoints = remember(filteredSales) {
        val now = Calendar.getInstance()
        (6 downTo 0).map { offset ->
            val dStart = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -offset)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val dEnd = (dStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
            val daySales = filteredSales.filter { it.createdAtEpochMs in dStart.timeInMillis until dEnd.timeInMillis }
            ChartPoint(
                label = formatDayShort(dStart.timeInMillis),
                revenue = daySales.sumOf { it.total },
                profit = daySales.sumOf { it.grossProfit }
            )
        }
    }

    val topProducts = remember(filteredSales, state.saleItems) {
        val saleIds = filteredSales.map { it.id }.toSet()
        state.saleItems
            .filter { it.saleId in saleIds }
            .groupBy { it.productName }
            .map { (name, items) ->
                val qty = items.sumOf { it.quantity }
                val rev = items.sumOf { it.lineTotal }
                val prof = items.sumOf { (it.unitPrice - it.buyPrice) * it.quantity - it.discountAmount }
                Triple(name, qty, rev to prof)
            }
            .sortedByDescending { it.second }
    }

    val cashierSummary = remember(filteredSales) {
        filteredSales.groupBy { it.cashierName }.map { (cashier, list) ->
            Triple(cashier, list.size, list.sumOf { it.total })
        }.sortedByDescending { it.third }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Laporan Bisnis", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        state.activeOutlet?.name ?: "Semua Cabang",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            csvLauncher.launch("laporan_nexora_${System.currentTimeMillis()}.csv")
                        },
                        modifier = Modifier.testTag("export_csv_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("CSV")
                    }
                    Button(
                        onClick = {
                            printBusinessReport(
                                context = context,
                                businessName = state.settings.businessName,
                                outletName = state.activeOutlet?.name ?: "Semua Cabang",
                                totalRevenue = totalRevenue,
                                totalProfit = totalGrossProfit,
                                txCount = filteredSales.size,
                                currencyCode = state.settings.currencyCode,
                                sales = filteredSales
                            )
                        },
                        modifier = Modifier.testTag("print_report_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cetak")
                    }
                }
            }
        }

        if (exportStatus != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NexoraSuccessContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        exportStatus!!,
                        color = NexoraSuccess,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "Hari Ini", 7 to "7 Hari", 30 to "30 Hari", 0 to "Semua").forEach { (d, label) ->
                    FilterChip(
                        selected = daysRange == d,
                        onClick = { daysRange = d },
                        label = { Text(label) }
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMetricCard(
                    title = "Total Omzet",
                    value = formatShortCurrency(totalRevenue),
                    subtitle = "${filteredSales.size} transaksi",
                    icon = Icons.Default.Payments,
                    accentColor = NexoraBlue,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = if (state.permissions.canViewProfitReports) "Laba Kotor" else "Rata-rata Order",
                    value = if (state.permissions.canViewProfitReports) formatShortCurrency(totalGrossProfit) else formatShortCurrency(avgTicket),
                    subtitle = if (state.permissions.canViewProfitReports) String.format("Margin %.1f%%", marginPct) else "Per struk",
                    icon = Icons.Default.TrendingUp,
                    accentColor = NexoraSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
                listOf("Omzet & Laba", "Produk Terlaris", "Persediaan", "Per Kasir").forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title) }
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Tren Penjualan 7 Hari Terakhir", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            NexoraSalesChart(points = chartPoints)
                        }
                    }
                }
                item {
                    val paymentBreakdown = filteredSales.groupBy { it.paymentMethod }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Ringkasan Metode Pembayaran", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (paymentBreakdown.isEmpty()) {
                                Text("Belum ada transaksi pada rentang ini.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                paymentBreakdown.forEach { (method, list) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("$method (${list.size}x)", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            formatCurrency(list.sumOf { it.total }, state.settings.currencyCode),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                if (topProducts.isEmpty()) {
                    item {
                        EmptyStatePanel(
                            icon = Icons.Default.Inventory2,
                            title = "Belum Ada Penjualan Produk",
                            description = "Produk terlaris berdasarkan kuantitas dan omzet akan muncul di sini."
                        )
                    }
                } else {
                    items(topProducts) { (name, qty, revProf) ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("$qty terjual", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        formatCurrency(revProf.first, state.settings.currencyCode),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NexoraBlue
                                    )
                                    if (state.permissions.canViewProfitReports) {
                                        Text(
                                            "Laba: ${formatShortCurrency(revProf.second)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NexoraSuccess
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Nilai Modal Stok", style = MaterialTheme.typography.titleSmall)
                            Text(
                                formatCurrency(stockAssetValue, state.settings.currencyCode),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NexoraBlue
                            )
                        }
                    }
                }
                items(outletProducts.filter { it.productType != "DIGITAL" }.sortedBy { it.stock }, key = { it.id }) { p ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("SKU ${p.sku} • Modal ${formatCurrency(p.buyPrice, state.settings.currencyCode)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                "${p.stock} ${p.unit}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (p.stock <= p.minStock) NexoraDanger else NexoraSuccess
                            )
                        }
                    }
                }
            }
            3 -> {
                if (cashierSummary.isEmpty()) {
                    item {
                        EmptyStatePanel(
                            icon = Icons.Default.Badge,
                            title = "Belum Ada Aktivitas Kasir",
                            description = "Performa transaksi per kasir akan ditampilkan setelah ada penjualan."
                        )
                    }
                } else {
                    items(cashierSummary) { (cashier, count, total) ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(cashier, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("$count transaksi berhasil", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    formatCurrency(total, state.settings.currencyCode),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NexoraBlue
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun printBusinessReport(
    context: Context,
    businessName: String,
    outletName: String,
    totalRevenue: Double,
    totalProfit: Double,
    txCount: Int,
    currencyCode: String,
    sales: List<SaleEntity>
) {
    fun esc(v: String): String = android.text.TextUtils.htmlEncode(v)
    val rows = sales.take(50).joinToString("") { s ->
        "<tr><td>${esc(s.transactionNumber)}</td><td>${esc(formatDateOnly(s.createdAtEpochMs))}</td><td>${esc(s.cashierName)}</td><td>${esc(s.paymentMethod)}</td><td style='text-align:right;'>${esc(formatCurrency(s.total, currencyCode))}</td></tr>"
    }
    val html = """
        <html><body style="font-family:sans-serif;padding:24px;">
        <h2>Laporan Penjualan - ${esc(businessName)}</h2>
        <p>Cabang: ${esc(outletName)}</p>
        <p><strong>Total Omzet:</strong> ${esc(formatCurrency(totalRevenue, currencyCode))} | <strong>Laba Kotor:</strong> ${esc(formatCurrency(totalProfit, currencyCode))} | <strong>Transaksi:</strong> $txCount</p>
        <table border="1" cellspacing="0" cellpadding="6" style="width:100%;border-collapse:collapse;font-size:12px;">
          <tr style="background:#f1f5f9;"><th>No. Struk</th><th>Tanggal</th><th>Kasir</th><th>Metode</th><th>Total</th></tr>
          $rows
        </table>
        </body></html>
    """.trimIndent()
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            val adapter = webView.createPrintDocumentAdapter("Laporan-$businessName")
            printManager?.print("Laporan $businessName", adapter, PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}
