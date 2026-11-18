package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.CustomerEntity
import com.example.data.local.ProductEntity
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.formatCurrency
import com.example.viewmodel.CartLine
import com.example.viewmodel.NexoraUiState
import com.example.viewmodel.NexoraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierScreen(
    state: NexoraUiState,
    viewModel: NexoraViewModel,
    isExpandedWidth: Boolean,
    onOpenScanner: () -> Unit,
    onNavigateToAddProduct: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var variantPickerProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showCartBottomSheet by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var editDiscountLine by remember { mutableStateOf<CartLine?>(null) }

    val activeOutletId = state.activeOutlet?.id
    val filteredProducts = remember(state.products, activeOutletId, searchQuery, selectedCategoryId) {
        state.products.filter { prod ->
            prod.active &&
                (activeOutletId == null || prod.outletId == activeOutletId) &&
                (selectedCategoryId == null || prod.categoryId == selectedCategoryId) &&
                (searchQuery.isBlank() ||
                    prod.name.contains(searchQuery, ignoreCase = true) ||
                    prod.sku.contains(searchQuery, ignoreCase = true) ||
                    prod.barcode.contains(searchQuery, ignoreCase = true))
        }
    }

    // Main Layout: Side-by-side on Tablet/Expanded, Product Grid + Bottom Checkout Bar on Phone
    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left / Full Pane: Product Catalog
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search + Hardware/Camera Barcode Trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari produk / scan barcode...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Bersihkan")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchQuery.isNotBlank()) {
                                    val q = searchQuery.trim()
                                    val exact = state.products.firstOrNull {
                                        it.active &&
                                            (activeOutletId == null || it.outletId == activeOutletId) &&
                                            (it.barcode.equals(q, ignoreCase = true) || it.sku.equals(q, ignoreCase = true))
                                    }
                                    if (exact != null) {
                                        viewModel.addToCart(exact)
                                        searchQuery = ""
                                    } else {
                                        viewModel.handleScannedBarcode(q, addToCartImmediately = true)
                                    }
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cashier_search_input")
                    )
                    FilledTonalIconButton(
                        onClick = onOpenScanner,
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("cashier_scanner_button")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Buka Scanner Barcode")
                    }
                }

                // Category Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                            label = { Text("Semua") }
                        )
                    }
                    items(state.categories) { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = {
                                selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id
                            },
                            label = { Text(cat.name) }
                        )
                    }
                }

                if (filteredProducts.isEmpty()) {
                    EmptyStatePanel(
                        icon = Icons.Default.Inventory2,
                        title = "Produk Tidak Ditemukan",
                        description = "Belum ada produk di cabang ini atau kata kunci pencarian tidak cocok.",
                        actionLabel = if (state.permissions.canManageProducts) "Tambah Produk" else null,
                        onAction = if (state.permissions.canManageProducts) onNavigateToAddProduct else null
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 152.dp),
                        contentPadding = PaddingValues(
                            bottom = if (!isExpandedWidth && state.cartLines.isNotEmpty()) 96.dp else 16.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredProducts, key = { it.id }) { prod ->
                            CashierProductCard(
                                product = prod,
                                currencyCode = state.settings.currencyCode,
                                qtyInCart = state.cartLines.filter { it.product.id == prod.id }.sumOf { it.quantity },
                                onClick = {
                                    val variants = prod.variantsSummary
                                        .split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotBlank() }
                                    if (variants.size > 1) {
                                        variantPickerProduct = prod
                                    } else {
                                        viewModel.addToCart(prod, variants.firstOrNull() ?: "")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Right Pane on Tablet/Expanded screens
            if (isExpandedWidth) {
                Surface(
                    modifier = Modifier
                        .width(360.dp)
                        .fillMaxHeight(),
                    tonalElevation = 3.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    CartPanelContent(
                        state = state,
                        viewModel = viewModel,
                        onPickCustomer = { showCustomerPicker = true },
                        onEditLineDiscount = { editDiscountLine = it },
                        onProceedCheckout = { showCheckoutDialog = true }
                    )
                }
            }
        }

        // Mobile Sticky Checkout Bar
        if (!isExpandedWidth && state.cartLines.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(18.dp),
                color = NexoraBlue,
                shadowElevation = 6.dp,
                onClick = { showCartBottomSheet = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("mobile_cart_summary_bar"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Badge(containerColor = Color.White, contentColor = NexoraBlue) {
                            Text("${state.cartItemCount}", fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Keranjang Belanja", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Text("Ketuk untuk rincian & bayar", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(
                        text = formatCurrency(state.cartGrandTotal, state.settings.currencyCode),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Mobile Cart BottomSheet
    if (showCartBottomSheet && !isExpandedWidth) {
        ModalBottomSheet(
            onDismissRequest = { showCartBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
            ) {
                CartPanelContent(
                    state = state,
                    viewModel = viewModel,
                    onPickCustomer = { showCustomerPicker = true },
                    onEditLineDiscount = { editDiscountLine = it },
                    onProceedCheckout = {
                        showCartBottomSheet = false
                        showCheckoutDialog = true
                    }
                )
            }
        }
    }

    // Variant Picker Dialog
    variantPickerProduct?.let { prod ->
        val variants = prod.variantsSummary.split(",").map { it.trim() }.filter { it.isNotBlank() }
        AlertDialog(
            onDismissRequest = { variantPickerProduct = null },
            title = { Text("Pilih Varian: ${prod.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    variants.forEach { variant ->
                        OutlinedButton(
                            onClick = {
                                viewModel.addToCart(prod, variant)
                                variantPickerProduct = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text(variant)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { variantPickerProduct = null }) { Text("Tutup") }
            }
        )
    }

    // Customer Picker Dialog
    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = state.customers,
            selected = state.selectedCustomer,
            onSelect = {
                viewModel.selectCustomerForCart(it)
                showCustomerPicker = false
            },
            onDismiss = { showCustomerPicker = false }
        )
    }

    // Line Discount Dialog
    editDiscountLine?.let { line ->
        var discText by remember(line) { mutableStateOf(if (line.itemDiscountAmount > 0) line.itemDiscountAmount.toLong().toString() else "") }
        AlertDialog(
            onDismissRequest = { editDiscountLine = null },
            title = { Text("Diskon Item: ${line.product.name}") },
            text = {
                OutlinedTextField(
                    value = discText,
                    onValueChange = { discText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Nominal Diskon (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val amt = discText.toDoubleOrNull() ?: 0.0
                    viewModel.setCartItemDiscount(line.key, amt)
                    editDiscountLine = null
                }) { Text("Terapkan") }
            },
            dismissButton = {
                TextButton(onClick = { editDiscountLine = null }) { Text("Batal") }
            }
        )
    }

    // Payment / Checkout Dialog
    if (showCheckoutDialog) {
        CheckoutPaymentDialog(
            state = state,
            onDismiss = { showCheckoutDialog = false },
            onConfirmPayment = { method, paid ->
                showCheckoutDialog = false
                viewModel.processCheckout(method, paid)
            }
        )
    }
}

@Composable
private fun CashierProductCard(
    product: ProductEntity,
    currencyCode: String,
    qtyInCart: Int,
    onClick: () -> Unit
) {
    val outOfStock = product.productType != "DIGITAL" && product.stock <= 0
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !outOfStock, onClick = onClick)
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(
            width = if (qtyInCart > 0) 1.5.dp else 1.dp,
            color = if (qtyInCart > 0) NexoraBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!product.photoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = product.photoUri,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = when (product.productType) {
                            "FOOD" -> Icons.Default.Restaurant
                            "DIGITAL" -> Icons.Default.Bolt
                            else -> Icons.Default.ShoppingBag
                        },
                        contentDescription = product.name,
                        tint = NexoraBlue.copy(alpha = 0.7f),
                        modifier = Modifier.size(34.dp)
                    )
                }

                if (qtyInCart > 0) {
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        containerColor = NexoraBlue,
                        contentColor = Color.White
                    ) {
                        Text("${qtyInCart}x", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )

            Text(
                text = formatCurrency(product.sellPrice, currencyCode),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = NexoraBlue
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (product.productType == "DIGITAL") "Digital"
                    else if (outOfStock) "Habis"
                    else "Stok: ${product.stock} ${product.unit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (outOfStock) NexoraDanger else if (product.stock <= product.minStock && product.productType != "DIGITAL") NexoraWarning else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CartPanelContent(
    state: NexoraUiState,
    viewModel: NexoraViewModel,
    onPickCustomer: () -> Unit,
    onEditLineDiscount: (CartLine) -> Unit,
    onProceedCheckout: () -> Unit
) {
    var orderDiscText by remember(state.orderDiscount) {
        mutableStateOf(if (state.orderDiscount > 0) state.orderDiscount.toLong().toString() else "")
    }
    var extraFeeText by remember(state.extraFee) {
        mutableStateOf(if (state.extraFee > 0) state.extraFee.toLong().toString() else "")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Keranjang (${state.cartItemCount})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (state.cartLines.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearCart() }) {
                    Text("Kosongkan", color = NexoraDanger)
                }
            }
        }

        // Customer Selector Pill
        OutlinedButton(
            onClick = onPickCustomer,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(state.selectedCustomer?.name ?: "Pilih Pelanggan (Opsional)")
        }

        if (state.cartLines.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Ketuk produk untuk menambahkan ke keranjang",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.cartLines, key = { it.key }) { line ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        line.product.name + if (line.variantLabel.isNotBlank()) " (${line.variantLabel})" else "",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        formatCurrency(line.product.sellPrice, state.settings.currencyCode) +
                                            if (line.itemDiscountAmount > 0) " • Disc -${formatCurrency(line.itemDiscountAmount)}" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.removeFromCart(line.key) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = NexoraDanger, modifier = Modifier.size(18.dp))
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    FilledTonalIconButton(
                                        onClick = { viewModel.updateCartQuantity(line.key, -1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Kurangi", modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        "${line.quantity}",
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                    FilledTonalIconButton(
                                        onClick = { viewModel.updateCartQuantity(line.key, 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                                    }
                                    TextButton(onClick = { onEditLineDiscount(line) }) {
                                        Text("Diskon", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Text(
                                    formatCurrency(line.lineTotal, state.settings.currencyCode),
                                    fontWeight = FontWeight.Bold,
                                    color = NexoraBlue
                                )
                            }
                        }
                    }
                }
            }

            // Order Discount, Extra Fee & Notes
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = orderDiscText,
                    onValueChange = {
                        val cleaned = it.filter { ch -> ch.isDigit() }
                        orderDiscText = cleaned
                        viewModel.setOrderDiscount(cleaned.toDoubleOrNull() ?: 0.0)
                    },
                    label = { Text("Diskon (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = extraFeeText,
                    onValueChange = {
                        val cleaned = it.filter { ch -> ch.isDigit() }
                        extraFeeText = cleaned
                        viewModel.setExtraFee(cleaned.toDoubleOrNull() ?: 0.0)
                    },
                    label = { Text("Biaya Lain") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = state.cartNotes,
                onValueChange = { viewModel.setCartNotes(it) },
                label = { Text("Catatan transaksi (opsional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Summary Totals
            HorizontalDivider()
            SummaryLine("Subtotal", formatCurrency(state.cartSubtotal, state.settings.currencyCode))
            if (state.cartItemDiscountTotal + state.orderDiscount > 0) {
                SummaryLine("Diskon", "-${formatCurrency(state.cartItemDiscountTotal + state.orderDiscount, state.settings.currencyCode)}")
            }
            if (state.settings.taxEnabled) {
                SummaryLine("Pajak (${state.settings.taxPercent}%)", formatCurrency(state.cartTaxAmount, state.settings.currencyCode))
            }
            if (state.extraFee > 0) {
                SummaryLine("Biaya Tambahan", formatCurrency(state.extraFee, state.settings.currencyCode))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Tagihan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    formatCurrency(state.cartGrandTotal, state.settings.currencyCode),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = NexoraBlue
                )
            }

            Button(
                onClick = onProceedCheckout,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("checkout_pay_button")
            ) {
                Icon(Icons.Default.Payments, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Bayar Sekarang", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CustomerPickerDialog(
    customers: List<CustomerEntity>,
    selected: CustomerEntity?,
    onSelect: (CustomerEntity?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Pelanggan") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onSelect(null) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Pelanggan Umum (Tanpa Akun)") }
                customers.forEach { cust ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(cust) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected?.id == cust.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(cust.name, fontWeight = FontWeight.SemiBold)
                            Text("${cust.phone} • ${cust.points} poin", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } }
    )
}

@Composable
private fun CheckoutPaymentDialog(
    state: NexoraUiState,
    onDismiss: () -> Unit,
    onConfirmPayment: (String, Double) -> Unit
) {
    val total = state.cartGrandTotal
    val isUsd = state.settings.currencyCode.equals("USD", ignoreCase = true)
    val defaultPaidStr = remember(total, isUsd) {
        if (isUsd) String.format(java.util.Locale.US, "%.2f", total)
        else kotlin.math.ceil(total).toLong().toString()
    }
    val methods = listOf(
        "TUNAI" to "Tunai",
        "TRANSFER" to "Transfer Bank",
        "QRIS_MANUAL" to "QRIS (Manual)",
        "LAINNYA" to "Lainnya"
    )
    var selectedMethod by remember { mutableStateOf("TUNAI") }
    var paidText by remember(defaultPaidStr) { mutableStateOf(defaultPaidStr) }
    val paidAmount = paidText.toDoubleOrNull() ?: 0.0
    val change = (paidAmount - total).coerceAtLeast(0.0)
    val isValid = paidAmount + 0.01 >= total

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pembayaran", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text("Total Tagihan", style = MaterialTheme.typography.labelMedium)
                        Text(
                            formatCurrency(total, state.settings.currencyCode),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = NexoraBlue
                        )
                    }
                }

                Text("Metode Pembayaran", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    methods.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (key, label) ->
                                FilterChip(
                                    selected = selectedMethod == key,
                                    onClick = {
                                        selectedMethod = key
                                        if (key != "TUNAI") paidText = defaultPaidStr
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                if (selectedMethod == "QRIS_MANUAL") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NexoraInfoContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            "Mode QRIS Manual: Pastikan Anda telah melihat bukti transfer/QRIS berhasil di aplikasi merchant sebelum menekan Selesaikan Transaksi.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = paidText,
                    onValueChange = { raw ->
                        paidText = if (isUsd) {
                            var seenDot = false
                            raw.filter { ch ->
                                if (ch == '.' && !seenDot) { seenDot = true; true } else ch.isDigit()
                            }
                        } else {
                            raw.filter { ch -> ch.isDigit() }
                        }
                    },
                    label = { Text("Uang Diterima (${state.settings.currencyCode})") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isUsd) KeyboardType.Decimal else KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_paid_amount_input")
                )

                if (selectedMethod == "TUNAI" && !isUsd) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val ceilTotal = kotlin.math.ceil(total).toLong()
                        val quickOptions = listOf(ceilTotal, 50_000L, 100_000L, 200_000L)
                            .distinct()
                            .filter { it >= ceilTotal }
                            .take(3)
                        quickOptions.forEach { amt ->
                            OutlinedButton(
                                onClick = { paidText = amt.toString() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                val lbl = if (amt == ceilTotal) "Uang Pas" else "Rp ${amt / 1000}rb"
                                Text(lbl, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Kembalian", style = MaterialTheme.typography.titleSmall)
                    Text(
                        formatCurrency(change, state.settings.currencyCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isValid) NexoraSuccess else NexoraDanger
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmPayment(selectedMethod, paidAmount) },
                enabled = isValid,
                modifier = Modifier.testTag("confirm_payment_button")
            ) {
                Text("Selesaikan Transaksi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
