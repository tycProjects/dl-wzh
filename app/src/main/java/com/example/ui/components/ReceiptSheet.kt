package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.BusinessSettingsEntity
import com.example.data.local.SaleEntity
import com.example.data.local.SaleItemEntity
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.util.formatCurrency
import com.example.util.formatDateTime

@Composable
fun ReceiptModal(
    sale: SaleEntity,
    items: List<SaleItemEntity>,
    settings: BusinessSettingsEntity,
    outletName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var formatMode by remember(settings.receiptWidthMm) {
        mutableStateOf(if (settings.receiptWidthMm == 80) "80MM" else "58MM")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 500.dp)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Struk Transaksi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup Struk")
                    }
                }

                // Format Switcher: 58mm, 80mm, Digital
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("58MM" to "Thermal 58mm", "80MM" to "Thermal 80mm", "DIGITAL" to "Struk Digital").forEach { (key, label) ->
                        FilterChip(
                            selected = formatMode == key,
                            onClick = { formatMode = key },
                            label = { Text(label, maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Receipt Paper Surface
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    shape = RoundedCornerShape(14.dp),
                    color = if (formatMode == "DIGITAL") MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0xFFFFFDF7),
                    tonalElevation = 1.dp
                ) {
                    val textColor = if (formatMode == "DIGITAL") MaterialTheme.colorScheme.onSurface else Color(0xFF111827)
                    val mutedColor = if (formatMode == "DIGITAL") MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF4B5563)
                    val mono = JetBrainsMonoFamily
                    val horizontalPad = when (formatMode) {
                        "58MM" -> 22.dp
                        "80MM" -> 14.dp
                        else -> 16.dp
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPad, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_nexora_launcher),
                            contentDescription = "Logo Usaha",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = settings.businessName,
                            fontFamily = mono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "$outletName\n${settings.address}\nTelp: ${settings.phone}",
                            fontFamily = mono,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = mutedColor,
                            textAlign = TextAlign.Center
                        )
                        DashedDivider(mutedColor)
                        ReceiptKeyVal("No. Struk", sale.transactionNumber, mono, textColor)
                        ReceiptKeyVal("Tanggal", formatDateTime(sale.createdAtEpochMs), mono, textColor)
                        ReceiptKeyVal("Kasir", "${sale.cashierName} (${sale.cashierRole})", mono, textColor)
                        if (!sale.customerName.isNullOrBlank()) {
                            ReceiptKeyVal("Pelanggan", sale.customerName, mono, textColor)
                        }
                        ReceiptKeyVal("Status", sale.status, mono, textColor)
                        DashedDivider(mutedColor)

                        if (items.isEmpty()) {
                            Text(
                                text = sale.itemsSummary,
                                fontFamily = mono,
                                fontSize = 11.sp,
                                color = textColor,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            items.forEach { item ->
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = item.productName + if (item.variantLabel.isNotBlank()) " (${item.variantLabel})" else "",
                                        fontFamily = mono,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = textColor
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${item.quantity} x ${formatCurrency(item.unitPrice, settings.currencyCode)}",
                                            fontFamily = mono,
                                            fontSize = 11.sp,
                                            color = mutedColor
                                        )
                                        Text(
                                            text = formatCurrency(item.lineTotal, settings.currencyCode),
                                            fontFamily = mono,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textColor
                                        )
                                    }
                                    if (item.discountAmount > 0) {
                                        Text(
                                            text = "  Diskon item: -${formatCurrency(item.discountAmount, settings.currencyCode)}",
                                            fontFamily = mono,
                                            fontSize = 10.sp,
                                            color = mutedColor
                                        )
                                    }
                                }
                            }
                        }

                        DashedDivider(mutedColor)
                        ReceiptKeyVal("Subtotal", formatCurrency(sale.subtotal, settings.currencyCode), mono, textColor)
                        if (sale.itemDiscountTotal + sale.orderDiscount > 0) {
                            ReceiptKeyVal(
                                "Total Diskon",
                                "-${formatCurrency(sale.itemDiscountTotal + sale.orderDiscount, settings.currencyCode)}",
                                mono,
                                textColor
                            )
                        }
                        if (sale.taxAmount > 0) {
                            ReceiptKeyVal("Pajak", formatCurrency(sale.taxAmount, settings.currencyCode), mono, textColor)
                        }
                        if (sale.extraFee > 0) {
                            ReceiptKeyVal("Biaya Tambahan", formatCurrency(sale.extraFee, settings.currencyCode), mono, textColor)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = mutedColor.copy(alpha = 0.4f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("TOTAL", fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                            Text(
                                formatCurrency(sale.total, settings.currencyCode),
                                fontFamily = mono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = textColor
                            )
                        }
                        ReceiptKeyVal("Bayar (${sale.paymentMethod})", formatCurrency(sale.paidAmount, settings.currencyCode), mono, textColor)
                        ReceiptKeyVal("Kembali", formatCurrency(sale.changeAmount, settings.currencyCode), mono, textColor)
                        if (sale.notes.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Catatan: ${sale.notes}",
                                fontFamily = mono,
                                fontSize = 11.sp,
                                color = mutedColor,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        DashedDivider(mutedColor)
                        Text(
                            text = settings.receiptFooter,
                            fontFamily = mono,
                            fontSize = 11.sp,
                            color = mutedColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val text = buildReceiptPlainText(sale, items, settings, outletName)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, text)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Bagikan Struk"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("share_receipt_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Bagikan")
                    }
                    Button(
                        onClick = {
                            printReceiptHtml(context, sale, items, settings, outletName, formatMode == "80MM")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("print_receipt_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cetak")
                    }
                }
            }
        }
    }
}

@Composable
private fun DashedDivider(color: Color) {
    Text(
        text = "--------------------------------",
        fontFamily = JetBrainsMonoFamily,
        fontSize = 11.sp,
        color = color.copy(alpha = 0.6f),
        maxLines = 1
    )
}

@Composable
private fun ReceiptKeyVal(
    k: String,
    v: String,
    mono: androidx.compose.ui.text.font.FontFamily,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(k, fontFamily = mono, fontSize = 11.sp, color = color.copy(alpha = 0.78f))
        Text(v, fontFamily = mono, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

fun buildReceiptPlainText(
    sale: SaleEntity,
    items: List<SaleItemEntity>,
    settings: BusinessSettingsEntity,
    outletName: String
): String {
    val sb = StringBuilder()
    sb.appendLine(settings.businessName.uppercase())
    sb.appendLine(outletName)
    sb.appendLine(settings.address)
    sb.appendLine("Telp: ${settings.phone}")
    sb.appendLine("================================")
    sb.appendLine("No: ${sale.transactionNumber}")
    sb.appendLine("Tgl: ${formatDateTime(sale.createdAtEpochMs)}")
    sb.appendLine("Kasir: ${sale.cashierName}")
    if (!sale.customerName.isNullOrBlank()) sb.appendLine("Pelanggan: ${sale.customerName}")
    sb.appendLine("--------------------------------")
    if (items.isEmpty()) {
        sb.appendLine(sale.itemsSummary)
    } else {
        items.forEach {
            sb.appendLine("${it.productName} ${if (it.variantLabel.isNotBlank()) "(${it.variantLabel})" else ""}")
            sb.appendLine("  ${it.quantity} x ${formatCurrency(it.unitPrice, settings.currencyCode)} = ${formatCurrency(it.lineTotal, settings.currencyCode)}")
        }
    }
    sb.appendLine("--------------------------------")
    sb.appendLine("Subtotal : ${formatCurrency(sale.subtotal, settings.currencyCode)}")
    if (sale.itemDiscountTotal + sale.orderDiscount > 0) {
        sb.appendLine("Diskon   : -${formatCurrency(sale.itemDiscountTotal + sale.orderDiscount, settings.currencyCode)}")
    }
    if (sale.taxAmount > 0) sb.appendLine("Pajak    : ${formatCurrency(sale.taxAmount, settings.currencyCode)}")
    if (sale.extraFee > 0) sb.appendLine("Biaya    : ${formatCurrency(sale.extraFee, settings.currencyCode)}")
    sb.appendLine("TOTAL    : ${formatCurrency(sale.total, settings.currencyCode)}")
    sb.appendLine("Bayar (${sale.paymentMethod}): ${formatCurrency(sale.paidAmount, settings.currencyCode)}")
    sb.appendLine("Kembali  : ${formatCurrency(sale.changeAmount, settings.currencyCode)}")
    sb.appendLine("================================")
    sb.appendLine(settings.receiptFooter)
    return sb.toString()
}

fun printReceiptHtml(
    context: Context,
    sale: SaleEntity,
    items: List<SaleItemEntity>,
    settings: BusinessSettingsEntity,
    outletName: String,
    wide80mm: Boolean
) {
    fun esc(v: String): String = android.text.TextUtils.htmlEncode(v)
    val widthPx = if (wide80mm) "300px" else "210px"
    val rowsHtml = if (items.isEmpty()) {
        "<div>${esc(sale.itemsSummary)}</div>"
    } else {
        items.joinToString("") {
            "<div style='margin-bottom:4px;'><div><strong>${esc(it.productName)}</strong></div><div style='display:flex;justify-content:space-between;'><span>${it.quantity} x ${esc(formatCurrency(it.unitPrice, settings.currencyCode))}</span><span>${esc(formatCurrency(it.lineTotal, settings.currencyCode))}</span></div></div>"
        }
    }
    val html = """
        <html><body style="font-family:monospace;width:$widthPx;font-size:11px;margin:0 auto;padding:8px;">
        <div style="text-align:center;">
          <h3 style="margin:0;">${esc(settings.businessName)}</h3>
          <div>${esc(outletName)}</div>
          <div>${esc(settings.address)}</div>
          <div>${esc(settings.phone)}</div>
        </div>
        <hr/>
        <div>No: ${esc(sale.transactionNumber)}</div>
        <div>Tgl: ${esc(formatDateTime(sale.createdAtEpochMs))}</div>
        <div>Kasir: ${esc(sale.cashierName)}</div>
        <hr/>
        $rowsHtml
        <hr/>
        <div style="display:flex;justify-content:space-between;"><span>Subtotal</span><span>${esc(formatCurrency(sale.subtotal, settings.currencyCode))}</span></div>
        <div style="display:flex;justify-content:space-between;"><span>Pajak</span><span>${esc(formatCurrency(sale.taxAmount, settings.currencyCode))}</span></div>
        <div style="display:flex;justify-content:space-between;font-weight:bold;font-size:13px;"><span>TOTAL</span><span>${esc(formatCurrency(sale.total, settings.currencyCode))}</span></div>
        <div style="display:flex;justify-content:space-between;"><span>Bayar (${esc(sale.paymentMethod)})</span><span>${esc(formatCurrency(sale.paidAmount, settings.currencyCode))}</span></div>
        <div style="display:flex;justify-content:space-between;"><span>Kembali</span><span>${esc(formatCurrency(sale.changeAmount, settings.currencyCode))}</span></div>
        <hr/>
        <div style="text-align:center;">${esc(settings.receiptFooter)}</div>
        </body></html>
    """.trimIndent()

    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            val adapter = webView.createPrintDocumentAdapter("Struk-${sale.transactionNumber}")
            printManager?.print("NEXORA Struk ${sale.transactionNumber}", adapter, PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}
