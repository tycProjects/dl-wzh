package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.*
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface UiEvent {
    data class ShowMessage(val message: String, val isError: Boolean = false) : UiEvent
    data class OpenReceipt(val sale: SaleEntity, val items: List<SaleItemEntity>) : UiEvent
    data class NavigateToAddProductWithBarcode(val barcode: String) : UiEvent
}

private data class CartState(
    val lines: List<CartLine> = emptyList(),
    val selectedCustomer: CustomerEntity? = null,
    val orderDiscount: Double = 0.0,
    val extraFee: Double = 0.0,
    val notes: String = ""
)

class NexoraViewModel(
    private val repository: NexoraRepository,
    private val prefs: AppPreferencesRepository,
    private val networkMonitor: NetworkMonitor,
    private val currentUserId: String,
    private val ownerDisplayName: String,
    private val ownerEmail: String
) : ViewModel() {

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val _cart = MutableStateFlow(CartState())
    private val _isSyncing = MutableStateFlow(false)
    private val _lastReceipt = MutableStateFlow<Pair<SaleEntity, List<SaleItemEntity>>?>(null)
    private val _notFoundBarcode = MutableStateFlow<String?>(null)
    private val _prefillBarcode = MutableStateFlow<String?>(null)
    private val _bootstrapped = MutableStateFlow(false)

    private val coreCatalogFlow = combine(
        repository.outlets,
        repository.categories,
        repository.products,
        repository.customers,
        repository.suppliers
    ) { outlets, cats, prods, custs, sups ->
        CatalogBundle(outlets, cats, prods, custs, sups)
    }

    private val salesFlow = combine(
        repository.sales,
        repository.saleItems,
        repository.stockMovements,
        repository.purchases,
        repository.returns
    ) { sales, saleItems, movements, purchases, returns ->
        SalesBundle(sales, saleItems, movements, purchases, returns)
    }

    private val operationsFlow = combine(
        repository.tables,
        repository.restaurantOrders,
        repository.digitalOrders,
        repository.staffMembers,
        repository.activityLogs
    ) { tables, restOrders, digOrders, staff, logs ->
        OperationsBundle(tables, restOrders, digOrders, staff, logs)
    }

    private val infraFlow = combine(
        repository.settings,
        repository.syncQueue,
        networkMonitor.isOnlineFlow,
        networkMonitor.manualOfflineOverride,
        prefs.themeMode
    ) { settings, queue, online, manualOff, theme ->
        InfraBundle(settings, queue, online, manualOff, theme)
    }

    private val sessionFlow = combine(
        prefs.activeOutletId,
        prefs.activeStaffId,
        _cart,
        _isSyncing,
        _lastReceipt
    ) { outletId, staffId, cart, syncing, receipt ->
        SessionBundle(outletId, staffId, cart, syncing, receipt)
    }

    val uiState: StateFlow<NexoraUiState> = combine(
        coreCatalogFlow,
        salesFlow,
        operationsFlow,
        infraFlow,
        sessionFlow
    ) { catalog, salesB, ops, infra, sess ->
        val activeOutlet = catalog.outlets.firstOrNull { it.id == sess.activeOutletId }
            ?: catalog.outlets.firstOrNull { it.mainOutlet }
            ?: catalog.outlets.firstOrNull()

        val activeStaff = ops.staff.firstOrNull { it.id == sess.activeStaffId && it.active }
            ?: ops.staff.firstOrNull { it.role == "OWNER" && it.active }
            ?: ops.staff.firstOrNull { it.active }

        val role = if (activeStaff != null) StaffRole.fromString(activeStaff.role) else StaffRole.OWNER
        val pendingCount = infra.syncQueue.count { it.status != "SYNCED" }
        val hasDemo = catalog.products.any { it.demoData } || salesB.sales.any { it.demoData }

        NexoraUiState(
            isLoading = !_bootstrapped.value,
            currentUserId = currentUserId,
            ownerDisplayName = ownerDisplayName,
            ownerEmail = ownerEmail,
            isOnline = infra.isOnline,
            manualOfflineMode = infra.manualOffline,
            themeMode = infra.theme,
            outlets = catalog.outlets,
            activeOutlet = activeOutlet,
            categories = catalog.categories,
            products = catalog.products,
            customers = catalog.customers,
            suppliers = catalog.suppliers,
            sales = salesB.sales,
            saleItems = salesB.saleItems,
            stockMovements = salesB.movements,
            purchases = salesB.purchases,
            returns = salesB.returns,
            tables = ops.tables,
            restaurantOrders = ops.restOrders,
            digitalOrders = ops.digOrders,
            staffMembers = ops.staff,
            activeStaff = activeStaff,
            activeRole = role,
            permissions = RolePermissions.forRole(role),
            activityLogs = ops.logs,
            settings = infra.settings,
            syncQueue = infra.syncQueue,
            pendingSyncCount = pendingCount,
            isSyncing = sess.isSyncing,
            hasDemoData = hasDemo,
            cartLines = sess.cart.lines,
            selectedCustomer = sess.cart.selectedCustomer,
            orderDiscount = sess.cart.orderDiscount,
            extraFee = sess.cart.extraFee,
            cartNotes = sess.cart.notes,
            lastReceiptSale = sess.lastReceipt?.first,
            lastReceiptItems = sess.lastReceipt?.second ?: emptyList(),
            lastScannedNotFoundBarcode = _notFoundBarcode.value,
            prefillProductBarcode = _prefillBarcode.value
        )
    }.combine(_notFoundBarcode) { state, notFound ->
        state.copy(lastScannedNotFoundBarcode = notFound)
    }.combine(_prefillBarcode) { state, prefill ->
        state.copy(prefillProductBarcode = prefill)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = NexoraUiState(
            currentUserId = currentUserId,
            ownerDisplayName = ownerDisplayName,
            ownerEmail = ownerEmail
        )
    )

    init {
        viewModelScope.launch {
            try {
                repository.ensureInitialBootstrap(ownerDisplayName, ownerEmail)
                // If local DB has no products yet and device is online, try hydrating from Firestore
                val productsSnapshot = repository.products.first()
                if (productsSnapshot.isEmpty() && networkMonitor.isCurrentlyOnline) {
                    repository.pullFromCloud()
                }
            } catch (_: Exception) {
                // Safe offline fallback
            } finally {
                _bootstrapped.value = true
            }
        }
    }

    private fun emitMsg(msg: String, isError: Boolean = false) {
        viewModelScope.launch { _events.emit(UiEvent.ShowMessage(msg, isError)) }
    }

    private fun currentActorName(): String =
        uiState.value.activeStaff?.name ?: ownerDisplayName.ifBlank { "Owner" }

    private fun currentActorRole(): String = uiState.value.activeRole.name

    private fun activeOutletId(): String = uiState.value.activeOutlet?.id ?: "outlet_main"

    // Theme & Session Controls
    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun setManualOfflineMode(enabled: Boolean) {
        networkMonitor.setManualOfflineMode(enabled)
        emitMsg(
            if (enabled) "Mode Offline diaktifkan. Transaksi tetap tersimpan aman secara lokal."
            else "Mode Online aktif."
        )
    }

    fun switchOutlet(outlet: OutletEntity) {
        viewModelScope.launch {
            prefs.setActiveOutletId(outlet.id)
            emitMsg("Cabang aktif: ${outlet.name}")
        }
    }

    fun switchStaffWithPin(staff: StaffMemberEntity, pin: String): Boolean {
        return if (staff.pinCode == pin) {
            viewModelScope.launch {
                prefs.setActiveStaffId(staff.id)
                repository.logActivity(staff.name, staff.role, "SWITCH_ROLE", "Aktif sebagai ${staff.name} (${staff.role})")
                emitMsg("Masuk sebagai ${staff.name} (${StaffRole.fromString(staff.role).displayName})")
            }
            true
        } else {
            emitMsg("PIN staf tidak sesuai", isError = true)
            false
        }
    }

    // Demo Data Controls
    fun loadDemoData() {
        viewModelScope.launch {
            repository.loadDemoDataset(ownerDisplayName)
            emitMsg("Data demo multi-bisnis berhasil dimuat")
        }
    }

    fun clearDemoData() {
        viewModelScope.launch {
            repository.clearDemoDataset(currentActorName(), currentActorRole())
            emitMsg("Seluruh data demo telah dihapus")
        }
    }

    // Sync Actions
    fun syncNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            val res = repository.flushSyncQueue()
            _isSyncing.value = false
            res.onSuccess { count ->
                emitMsg(if (count > 0) "$count perubahan berhasil disinkronkan ke cloud" else "Semua data sudah sinkron")
            }.onFailure { err ->
                emitMsg(err.localizedMessage ?: "Sinkronisasi gagal", isError = true)
            }
        }
    }

    fun pullCloudData() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            val res = repository.pullFromCloud()
            _isSyncing.value = false
            res.onSuccess { count ->
                emitMsg("Berhasil memulihkan $count data dari cloud")
            }.onFailure { err ->
                emitMsg(err.localizedMessage ?: "Gagal memuat data cloud", isError = true)
            }
        }
    }

    // Cart & Checkout
    fun addToCart(product: ProductEntity, variantLabel: String = "") {
        if (!product.active) {
            emitMsg("Produk ${product.name} sedang nonaktif", isError = true)
            return
        }
        if (product.productType != "DIGITAL" && product.stock <= 0) {
            emitMsg("Stok ${product.name} habis", isError = true)
            return
        }
        val key = "${product.id}__$variantLabel"
        _cart.update { current ->
            val existing = current.lines.firstOrNull { it.key == key }
            val nextLines = if (existing != null) {
                if (product.productType != "DIGITAL" && existing.quantity + 1 > product.stock) {
                    emitMsg("Stok ${product.name} tersisa ${product.stock} ${product.unit}", isError = true)
                    return@update current
                }
                current.lines.map {
                    if (it.key == key) it.copy(quantity = it.quantity + 1) else it
                }
            } else {
                current.lines + CartLine(key, product, variantLabel, 1, 0.0)
            }
            current.copy(lines = nextLines)
        }
    }

    fun updateCartQuantity(key: String, delta: Int) {
        _cart.update { current ->
            val target = current.lines.firstOrNull { it.key == key } ?: return@update current
            val nextQty = target.quantity + delta
            if (nextQty <= 0) {
                current.copy(lines = current.lines.filterNot { it.key == key })
            } else if (target.product.productType != "DIGITAL" && nextQty > target.product.stock) {
                emitMsg("Maksimal stok tersedia: ${target.product.stock}", isError = true)
                current
            } else {
                current.copy(lines = current.lines.map {
                    if (it.key == key) it.copy(quantity = nextQty) else it
                })
            }
        }
    }

    fun setCartItemDiscount(key: String, discountAmount: Double) {
        _cart.update { current ->
            current.copy(lines = current.lines.map {
                if (it.key == key) it.copy(itemDiscountAmount = discountAmount.coerceIn(0.0, it.lineSubtotal))
                else it
            })
        }
    }

    fun removeFromCart(key: String) {
        _cart.update { it.copy(lines = it.lines.filterNot { line -> line.key == key }) }
    }

    fun clearCart() {
        _cart.value = CartState()
    }

    fun setOrderDiscount(amount: Double) {
        _cart.update { it.copy(orderDiscount = amount.coerceAtLeast(0.0)) }
    }

    fun setExtraFee(amount: Double) {
        _cart.update { it.copy(extraFee = amount.coerceAtLeast(0.0)) }
    }

    fun setCartNotes(notes: String) {
        _cart.update { it.copy(notes = notes) }
    }

    fun selectCustomerForCart(customer: CustomerEntity?) {
        _cart.update { it.copy(selectedCustomer = customer) }
    }

    fun processCheckout(paymentMethod: String, paidAmount: Double) {
        val st = uiState.value
        if (st.cartLines.isEmpty()) {
            emitMsg("Keranjang masih kosong", isError = true)
            return
        }
        viewModelScope.launch {
            val req = CheckoutRequest(
                outletId = activeOutletId(),
                cashierName = currentActorName(),
                cashierRole = currentActorRole(),
                customer = st.selectedCustomer,
                lines = st.cartLines.map {
                    CartLineInput(it.product, it.variantLabel, it.quantity, it.itemDiscountAmount)
                },
                orderDiscount = st.orderDiscount,
                taxRatePercent = st.settings.taxPercent,
                taxEnabled = st.settings.taxEnabled,
                extraFee = st.extraFee,
                paymentMethod = paymentMethod,
                paidAmount = paidAmount,
                notes = st.cartNotes,
                loyaltyEnabled = st.settings.loyaltyEnabled
            )
            repository.completeCheckout(req).onSuccess { (sale, items) ->
                clearCart()
                _lastReceipt.value = sale to items
                _events.emit(UiEvent.OpenReceipt(sale, items))
                emitMsg("Transaksi ${sale.transactionNumber} berhasil disimpan")
            }.onFailure { err ->
                emitMsg(err.localizedMessage ?: "Gagal memproses transaksi", isError = true)
            }
        }
    }

    // Barcode Scanner
    fun handleScannedBarcode(code: String, addToCartImmediately: Boolean = true) {
        val cleaned = code.trim()
        if (cleaned.isBlank()) return
        viewModelScope.launch {
            val found = repository.findProductByBarcode(cleaned)
                ?: uiState.value.products.firstOrNull {
                    it.active && (it.barcode.equals(cleaned, ignoreCase = true) || it.sku.equals(cleaned, ignoreCase = true))
                }
            if (found != null) {
                _notFoundBarcode.value = null
                if (addToCartImmediately) {
                    addToCart(found)
                    emitMsg("Ditambahkan: ${found.name}")
                }
            } else {
                _notFoundBarcode.value = cleaned
                emitMsg("Produk dengan barcode $cleaned tidak ditemukan", isError = true)
            }
        }
    }

    fun dismissNotFoundBarcode() {
        _notFoundBarcode.value = null
    }

    fun startRegisterProductWithBarcode(barcode: String) {
        _prefillBarcode.value = barcode
        _notFoundBarcode.value = null
        viewModelScope.launch { _events.emit(UiEvent.NavigateToAddProductWithBarcode(barcode)) }
    }

    fun clearPrefillBarcode() {
        _prefillBarcode.value = null
    }

    // CRUD Operations
    fun saveProduct(product: ProductEntity) {
        if (!uiState.value.permissions.canManageProducts) {
            emitMsg("Role ${uiState.value.activeRole.displayName} tidak memiliki izin kelola produk", isError = true)
            return
        }
        viewModelScope.launch {
            repository.saveProduct(product.copy(outletId = product.outletId.ifBlank { activeOutletId() }), currentActorName(), currentActorRole())
                .onSuccess { emitMsg("Produk ${it.name} berhasil disimpan") }
                .onFailure { emitMsg(it.localizedMessage ?: "Gagal menyimpan produk", isError = true) }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        if (!uiState.value.permissions.canManageProducts) {
            emitMsg("Tidak memiliki izin menghapus produk", isError = true)
            return
        }
        viewModelScope.launch {
            repository.deleteProduct(product, currentActorName(), currentActorRole())
            emitMsg("Produk ${product.name} dihapus")
        }
    }

    fun saveCategory(category: CategoryEntity) {
        if (!uiState.value.permissions.canManageProducts) {
            emitMsg("Tidak memiliki izin kelola kategori", isError = true)
            return
        }
        viewModelScope.launch {
            repository.saveCategory(category, currentActorName(), currentActorRole())
                .onSuccess { emitMsg("Kategori ${it.name} disimpan") }
                .onFailure { emitMsg(it.localizedMessage ?: "Gagal menyimpan kategori", isError = true) }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        if (!uiState.value.permissions.canManageProducts) return
        viewModelScope.launch {
            repository.deleteCategory(category.id, category.name, currentActorName(), currentActorRole())
            emitMsg("Kategori ${category.name} dihapus")
        }
    }

    fun adjustStock(productId: String, type: String, qty: Int, note: String) {
        if (!uiState.value.permissions.canManageInventory) {
            emitMsg("Tidak memiliki izin ubah inventaris", isError = true)
            return
        }
        viewModelScope.launch {
            repository.adjustStock(productId, type, qty, note, currentActorName(), currentActorRole())
                .onSuccess { emitMsg("Stok ${it.name} diperbarui: ${it.stock} ${it.unit}") }
                .onFailure { emitMsg(it.localizedMessage ?: "Gagal memperbarui stok", isError = true) }
        }
    }

    fun saveSupplier(supplier: SupplierEntity) {
        if (!uiState.value.permissions.canManageInventory) {
            emitMsg("Tidak memiliki izin kelola supplier", isError = true)
            return
        }
        viewModelScope.launch {
            repository.saveSupplier(supplier, currentActorName(), currentActorRole())
                .onSuccess { emitMsg("Supplier ${it.name} disimpan") }
                .onFailure { emitMsg(it.localizedMessage ?: "Gagal menyimpan supplier", isError = true) }
        }
    }

    fun recordPurchase(supplier: SupplierEntity, product: ProductEntity, qty: Int, cost: Double, invoice: String, notes: String) {
        if (!uiState.value.permissions.canManageInventory) return
        viewModelScope.launch {
            repository.recordPurchase(activeOutletId(), supplier, product, qty, cost, invoice, notes, currentActorName(), currentActorRole())
                .onSuccess { emitMsg("Pembelian ${it.invoiceNumber} dicatat & stok bertambah") }
                .onFailure { emitMsg(it.localizedMessage ?: "Gagal mencatat pembelian", isError = true) }
        }
    }

    fun returnPurchase(purchase: PurchaseEntity) {
        if (!uiState.value.permissions.canManageInventory) return
        viewModelScope.launch {
            repository.returnPurchase(purchase, currentActorName(), currentActorRole())
                .onSuccess { emitMsg("Retur pembelian ${purchase.invoiceNumber} dicatat") }
        }
    }

    fun saveCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.saveCustomer(customer, currentActorName(), currentActorRole())
                .onSuccess { emitMsg("Pelanggan ${it.name} disimpan") }
                .onFailure { emitMsg(it.localizedMessage ?: "Gagal menyimpan pelanggan", isError = true) }
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer, currentActorName(), currentActorRole())
            emitMsg("Pelanggan ${customer.name} dihapus")
        }
    }

    fun voidSale(sale: SaleEntity, reason: String) {
        viewModelScope.launch {
            repository.voidSale(sale, reason, currentActorName(), currentActorRole())
                .onSuccess { emitMsg("Transaksi ${sale.transactionNumber} dibatalkan (audit trail tersimpan)") }
                .onFailure { emitMsg(it.localizedMessage ?: "Gagal membatalkan transaksi", isError = true) }
        }
    }

    fun recordSaleReturn(sale: SaleEntity, reason: String, refundAmount: Double) {
        if (!uiState.value.permissions.canVoidOrReturn) {
            emitMsg("Role ${uiState.value.activeRole.displayName} tidak memiliki izin retur", isError = true)
            return
        }
        viewModelScope.launch {
            repository.recordSaleReturn(sale, reason, refundAmount, currentActorName(), currentActorRole())
                .onSuccess { emitMsg("Retur transaksi ${sale.transactionNumber} dicatat") }
                .onFailure { emitMsg(it.localizedMessage ?: "Gagal mencatat retur", isError = true) }
        }
    }

    suspend fun fetchSaleItems(saleId: String): List<SaleItemEntity> = repository.getSaleItems(saleId)

    // Restaurant
    fun saveTable(table: RestaurantTableEntity) {
        viewModelScope.launch {
            repository.saveTable(table.copy(outletId = table.outletId.ifBlank { activeOutletId() }), currentActorName(), currentActorRole())
            emitMsg("Meja ${table.tableName} diperbarui")
        }
    }

    fun createRestaurantOrder(orderType: String, table: RestaurantTableEntity?, itemsSummary: String, notes: String, totalAmount: Double) {
        viewModelScope.launch {
            val order = repository.createRestaurantOrder(activeOutletId(), orderType, table, itemsSummary, notes, totalAmount, currentActorName(), currentActorRole())
            emitMsg("Pesanan dapur ${order.queueNumber} dikirim ke Dapur")
        }
    }

    fun updateRestaurantOrderStatus(order: RestaurantOrderEntity, newStatus: String) {
        viewModelScope.launch {
            repository.updateRestaurantOrderStatus(order, newStatus, uiState.value.tables, currentActorName(), currentActorRole())
            emitMsg("Pesanan ${order.queueNumber}: $newStatus")
        }
    }

    // Digital Store
    fun createDigitalOrder(product: ProductEntity, variantLabel: String, recipientInfo: String, notes: String) {
        viewModelScope.launch {
            repository.createDigitalOrder(activeOutletId(), product, variantLabel, recipientInfo, notes, currentActorName(), currentActorRole())
            emitMsg("Pesanan digital dicatat (antrean pengiriman manual)")
        }
    }

    fun updateDigitalDeliveryStatus(order: DigitalOrderEntity, status: String, notes: String) {
        viewModelScope.launch {
            repository.updateDigitalDeliveryStatus(order, status, notes, currentActorName(), currentActorRole())
            emitMsg("Status pengiriman digital diperbarui: $status")
        }
    }

    // Outlets
    fun saveOutlet(outlet: OutletEntity) {
        if (!uiState.value.permissions.canManageOutlets) {
            emitMsg("Tidak memiliki izin kelola cabang", isError = true)
            return
        }
        viewModelScope.launch {
            repository.saveOutlet(outlet, currentActorName(), currentActorRole())
                .onSuccess { emitMsg("Cabang ${it.name} disimpan") }
                .onFailure { emitMsg(it.localizedMessage ?: "Gagal menyimpan cabang", isError = true) }
        }
    }

    fun transferStock(productId: String, targetOutletId: String, quantity: Int) {
        if (!uiState.value.permissions.canManageOutlets && !uiState.value.permissions.canManageInventory) {
            emitMsg("Tidak memiliki izin transfer stok", isError = true)
            return
        }
        viewModelScope.launch {
            repository.transferStockBetweenOutlets(productId, targetOutletId, quantity, currentActorName(), currentActorRole())
                .onSuccess { emitMsg("Transfer $quantity unit ke cabang tujuan berhasil") }
                .onFailure { emitMsg(it.localizedMessage ?: "Gagal transfer stok", isError = true) }
        }
    }

    // Staff & Settings
    fun saveStaff(member: StaffMemberEntity) {
        if (!uiState.value.permissions.canManageStaff) {
            emitMsg("Hanya Owner yang dapat mengelola akun staf", isError = true)
            return
        }
        viewModelScope.launch {
            repository.saveStaffMember(member.copy(outletId = member.outletId.ifBlank { activeOutletId() }), currentActorName(), currentActorRole())
                .onSuccess { emitMsg("Akun staf ${it.name} disimpan") }
                .onFailure { emitMsg(it.localizedMessage ?: "Gagal menyimpan staf", isError = true) }
        }
    }

    fun deleteStaff(member: StaffMemberEntity) {
        if (!uiState.value.permissions.canManageStaff) return
        viewModelScope.launch {
            repository.deleteStaffMember(member, currentActorName(), currentActorRole())
            emitMsg("Staf ${member.name} dihapus")
        }
    }

    fun saveSettings(settings: BusinessSettingsEntity) {
        if (!uiState.value.permissions.canManageSettings) {
            emitMsg("Tidak memiliki izin mengubah pengaturan bisnis", isError = true)
            return
        }
        viewModelScope.launch {
            repository.saveSettings(settings, currentActorName(), currentActorRole())
            emitMsg("Pengaturan bisnis disimpan")
        }
    }

    suspend fun exportSalesCsv(): String = repository.exportSalesCsv()
    suspend fun createBackupJson(): String = repository.createBackupJson()

    fun restoreFromBackupJson(json: String) {
        viewModelScope.launch {
            repository.restoreFromBackupJson(json, currentActorName(), currentActorRole())
                .onSuccess { count -> emitMsg("Berhasil memulihkan $count entri data") }
                .onFailure { err -> emitMsg("Format cadangan tidak valid: ${err.localizedMessage}", isError = true) }
        }
    }

    private data class CatalogBundle(
        val outlets: List<OutletEntity>,
        val categories: List<CategoryEntity>,
        val products: List<ProductEntity>,
        val customers: List<CustomerEntity>,
        val suppliers: List<SupplierEntity>
    )

    private data class SalesBundle(
        val sales: List<SaleEntity>,
        val saleItems: List<SaleItemEntity>,
        val movements: List<StockMovementEntity>,
        val purchases: List<PurchaseEntity>,
        val returns: List<ReturnEntity>
    )

    private data class OperationsBundle(
        val tables: List<RestaurantTableEntity>,
        val restOrders: List<RestaurantOrderEntity>,
        val digOrders: List<DigitalOrderEntity>,
        val staff: List<StaffMemberEntity>,
        val logs: List<ActivityLogEntity>
    )

    private data class InfraBundle(
        val settings: BusinessSettingsEntity,
        val syncQueue: List<SyncQueueEntity>,
        val isOnline: Boolean,
        val manualOffline: Boolean,
        val theme: AppThemeMode
    )

    private data class SessionBundle(
        val activeOutletId: String?,
        val activeStaffId: String?,
        val cart: CartState,
        val isSyncing: Boolean,
        val lastReceipt: Pair<SaleEntity, List<SaleItemEntity>>?
    )
}
