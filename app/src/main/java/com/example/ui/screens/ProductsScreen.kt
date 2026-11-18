package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import com.example.data.local.CategoryEntity
import com.example.data.local.ProductEntity
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.NexoraBlue
import com.example.ui.theme.NexoraDanger
import com.example.ui.theme.NexoraSuccess
import com.example.ui.theme.NexoraWarning
import com.example.util.formatCurrency
import com.example.viewmodel.NexoraUiState
import com.example.viewmodel.NexoraViewModel

@Composable
fun ProductsScreen(
    state: NexoraUiState,
    viewModel: NexoraViewModel,
    initialAddWithBarcode: String? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Produk, 1 = Kategori
    var searchQuery by remember { mutableStateOf("") }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showProductForm by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var deleteConfirmProduct by remember { mutableStateOf<ProductEntity?>(null) }

    LaunchedEffect(initialAddWithBarcode) {
        if (!initialAddWithBarcode.isNullOrBlank()) {
            editingProduct = ProductEntity(
                id = "",
                outletId = state.activeOutlet?.id ?: "outlet_main",
                categoryId = state.categories.firstOrNull()?.id ?: "cat_general",
                name = "",
                sku = "",
                barcode = initialAddWithBarcode,
                buyPrice = 0.0,
                sellPrice = 0.0,
                stock = 0,
                minStock = 5,
                unit = "pcs"
            )
            showProductForm = true
            viewModel.clearPrefillBarcode()
        }
    }

    val activeOutletId = state.activeOutlet?.id
    val products = remember(state.products, activeOutletId, searchQuery) {
        state.products.filter { prod ->
            (activeOutletId == null || prod.outletId == activeOutletId) &&
                (searchQuery.isBlank() ||
                    prod.name.contains(searchQuery, ignoreCase = true) ||
                    prod.sku.contains(searchQuery, ignoreCase = true) ||
                    prod.barcode.contains(searchQuery, ignoreCase = true))
        }
    }

    Scaffold(
        floatingActionButton = {
            if (state.permissions.canManageProducts) {
                FloatingActionButton(
                    onClick = {
                        if (selectedTab == 0) {
                            editingProduct = null
                            showProductForm = true
                        } else {
                            editingCategory = null
                            showCategoryDialog = true
                        }
                    },
                    modifier = Modifier.testTag("products_fab_add")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Produk (${products.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Kategori (${state.categories.size})") })
            }

            if (selectedTab == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari nama, SKU, atau barcode...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (products.isEmpty()) {
                        EmptyStatePanel(
                            icon = Icons.Default.Inventory2,
                            title = "Belum Ada Produk",
                            description = "Tambahkan produk pertama untuk cabang ${state.activeOutlet?.name ?: "ini"}.",
                            actionLabel = if (state.permissions.canManageProducts) "Tambah Produk" else null,
                            onAction = if (state.permissions.canManageProducts) {
                                { editingProduct = null; showProductForm = true }
                            } else null
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 84.dp)
                        ) {
                            items(products, key = { it.id }) { prod ->
                                val catName = state.categories.firstOrNull { it.id == prod.categoryId }?.name ?: "Umum"
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = state.permissions.canManageProducts) {
                                            editingProduct = prod
                                            showProductForm = true
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!prod.photoUri.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = prod.photoUri,
                                                    contentDescription = prod.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(Icons.Default.Inventory, contentDescription = null, tint = NexoraBlue)
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(prod.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
                                                if (!prod.active) {
                                                    StatusBadge("Nonaktif", NexoraDanger)
                                                }
                                            }
                                            Text(
                                                "$catName • SKU: ${prod.sku}" + if (prod.barcode.isNotBlank()) " • ${prod.barcode}" else "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (prod.variantsSummary.isNotBlank()) {
                                                Text(
                                                    "Varian: ${prod.variantsSummary}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = NexoraBlue
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    formatCurrency(prod.sellPrice, state.settings.currencyCode),
                                                    fontWeight = FontWeight.Bold,
                                                    color = NexoraBlue
                                                )
                                                StatusBadge(
                                                    label = if (prod.productType == "DIGITAL") "Digital" else "Stok: ${prod.stock} ${prod.unit}",
                                                    color = when {
                                                        prod.productType == "DIGITAL" -> NexoraBlue
                                                        prod.stock == 0 -> NexoraDanger
                                                        prod.stock <= prod.minStock -> NexoraWarning
                                                        else -> NexoraSuccess
                                                    }
                                                )
                                            }
                                        }

                                        if (state.permissions.canManageProducts) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                IconButton(
                                                    onClick = { editingProduct = prod; showProductForm = true },
                                                    modifier = Modifier.testTag("edit_product_${prod.id}")
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Produk")
                                                }
                                                IconButton(
                                                    onClick = { deleteConfirmProduct = prod },
                                                    modifier = Modifier.testTag("delete_product_${prod.id}")
                                                ) {
                                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Produk", tint = NexoraDanger)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Categories Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp, 14.dp, 14.dp, 84.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.categories.isEmpty()) {
                        item {
                            EmptyStatePanel(
                                icon = Icons.Default.Category,
                                title = "Belum Ada Kategori",
                                description = "Buat kategori produk seperti Makanan, Minuman, Pakaian, Elektronik, atau Produk Digital."
                            )
                        }
                    } else {
                        items(state.categories, key = { it.id }) { cat ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cat.name, fontWeight = FontWeight.Bold)
                                        Text("Sektor: ${cat.businessType}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (state.permissions.canManageProducts) {
                                        Row {
                                            IconButton(onClick = { editingCategory = cat; showCategoryDialog = true }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit Kategori")
                                            }
                                            IconButton(onClick = { viewModel.deleteCategory(cat) }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Kategori", tint = NexoraDanger)
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

    if (showProductForm) {
        ProductFormDialog(
            initial = editingProduct,
            categories = state.categories,
            defaultOutletId = state.activeOutlet?.id ?: "outlet_main",
            onDismiss = { showProductForm = false },
            onSave = { prod ->
                viewModel.saveProduct(prod)
                showProductForm = false
            },
            onDelete = { prod ->
                viewModel.deleteProduct(prod)
                showProductForm = false
            }
        )
    }

    if (showCategoryDialog) {
        CategoryFormDialog(
            initial = editingCategory,
            onDismiss = { showCategoryDialog = false },
            onSave = { cat ->
                viewModel.saveCategory(cat)
                showCategoryDialog = false
            }
        )
    }
    deleteConfirmProduct?.let { prod ->
        AlertDialog(
            onDismissRequest = { deleteConfirmProduct = null },
            title = { Text("Hapus Produk?", fontWeight = FontWeight.Bold) },
            text = { Text("Produk \"${prod.name}\" akan dihapus dari katalog.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(prod)
                        deleteConfirmProduct = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexoraDanger)
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmProduct = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun ProductFormDialog(
    initial: ProductEntity?,
    categories: List<CategoryEntity>,
    defaultOutletId: String,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit,
    onDelete: (ProductEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember(initial?.id, initial?.barcode) { mutableStateOf(initial?.name ?: "") }
    var sku by remember(initial?.id, initial?.barcode) { mutableStateOf(initial?.sku ?: "") }
    var barcode by remember(initial?.id, initial?.barcode) { mutableStateOf(initial?.barcode ?: "") }
    var selectedCategoryId by remember(initial?.id) {
        mutableStateOf(initial?.categoryId ?: categories.firstOrNull()?.id ?: "cat_general")
    }
    var buyPrice by remember(initial?.id) { mutableStateOf(if ((initial?.buyPrice ?: 0.0) > 0) initial!!.buyPrice.toLong().toString() else "") }
    var sellPrice by remember(initial?.id) { mutableStateOf(if ((initial?.sellPrice ?: 0.0) > 0) initial!!.sellPrice.toLong().toString() else "") }
    var stock by remember(initial?.id) { mutableStateOf((initial?.stock ?: 10).toString()) }
    var minStock by remember(initial?.id) { mutableStateOf((initial?.minStock ?: 5).toString()) }
    var unit by remember(initial?.id) { mutableStateOf(initial?.unit ?: "pcs") }
    var description by remember(initial?.id) { mutableStateOf(initial?.description ?: "") }
    var variants by remember(initial?.id) { mutableStateOf(initial?.variantsSummary ?: "") }
    var productType by remember(initial?.id) { mutableStateOf(initial?.productType ?: "PHYSICAL") }
    var active by remember(initial?.id) { mutableStateOf(initial?.active ?: true) }
    var photoUri by remember(initial?.id) { mutableStateOf(initial?.photoUri) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val savedPath = withContext(Dispatchers.IO) {
                    try {
                        val dir = File(context.filesDir, "product_photos").apply { mkdirs() }
                        val dest = File(dir, "prod_${UUID.randomUUID()}.jpg")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            dest.outputStream().use { output -> input.copyTo(output) }
                        }
                        dest.absolutePath
                    } catch (_: Exception) {
                        uri.toString()
                    }
                }
                photoUri = savedPath
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null || initial.id.isBlank()) "Tambah Produk" else "Edit Produk", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Photo Picker (Zero-permission Android Photo Picker)
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (photoUri != null) "Ganti Foto Produk" else "Pilih Foto Produk")
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Product Type Selector
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("PHYSICAL" to "Fisik", "FOOD" to "Resto", "DIGITAL" to "Digital").forEach { (type, lbl) ->
                        FilterChip(
                            selected = productType == type,
                            onClick = { productType = type },
                            label = { Text(lbl) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (categories.isNotEmpty()) {
                    var catExpanded by remember { mutableStateOf(false) }
                    val catName = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Pilih Kategori"
                    Box {
                        OutlinedButton(
                            onClick = { catExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Kategori: $catName")
                        }
                        DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = { selectedCategoryId = cat.id; catExpanded = false }
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = buyPrice,
                        onValueChange = { buyPrice = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Harga Beli") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellPrice,
                        onValueChange = { sellPrice = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Harga Jual*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_sell_price_input")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Stok") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minStock,
                        onValueChange = { minStock = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Min. Stok") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Satuan") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = variants,
                    onValueChange = { variants = it },
                    label = { Text("Varian (pisahkan koma, cth: M, L, XL)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Status Produk Aktif")
                    Switch(checked = active, onCheckedChange = { active = it })
                }

                if (initial != null && initial.id.isNotBlank()) {
                    TextButton(
                        onClick = { onDelete(initial) },
                        colors = ButtonDefaults.textButtonColors(contentColor = NexoraDanger)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Hapus Produk Ini")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entity = ProductEntity(
                        id = initial?.id ?: "",
                        outletId = initial?.outletId ?: defaultOutletId,
                        categoryId = selectedCategoryId,
                        name = name.trim(),
                        sku = sku.trim(),
                        barcode = barcode.trim(),
                        photoUri = photoUri,
                        buyPrice = buyPrice.toDoubleOrNull() ?: 0.0,
                        sellPrice = sellPrice.toDoubleOrNull() ?: 0.0,
                        stock = stock.toIntOrNull() ?: 0,
                        minStock = minStock.toIntOrNull() ?: 5,
                        unit = unit.ifBlank { "pcs" },
                        description = description.trim(),
                        productType = productType,
                        variantsSummary = variants.trim(),
                        active = active,
                        demoData = initial?.demoData ?: false
                    )
                    onSave(entity)
                },
                enabled = name.isNotBlank() && (sellPrice.toDoubleOrNull() != null),
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun CategoryFormDialog(
    initial: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (CategoryEntity) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var businessType by remember { mutableStateOf(initial?.businessType ?: "Minimarket") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Kategori" else "Edit Kategori") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Kategori*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = businessType,
                    onValueChange = { businessType = it },
                    label = { Text("Jenis Usaha (cth: Minimarket, Restoran)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        CategoryEntity(
                            id = initial?.id ?: "",
                            name = name.trim(),
                            colorHex = initial?.colorHex ?: "#2563EB",
                            iconName = initial?.iconName ?: "Category",
                            businessType = businessType.trim().ifBlank { "Retail" },
                            demoData = initial?.demoData ?: false
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
