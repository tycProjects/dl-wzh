package com.example.data.repository

import com.example.data.local.*
import com.example.data.remote.FirestoreSyncService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class CartLineInput(
    val product: ProductEntity,
    val variantLabel: String,
    val quantity: Int,
    val itemDiscountAmount: Double
)

data class CheckoutRequest(
    val outletId: String,
    val cashierName: String,
    val cashierRole: String,
    val customer: CustomerEntity?,
    val lines: List<CartLineInput>,
    val orderDiscount: Double,
    val taxRatePercent: Double,
    val taxEnabled: Boolean,
    val extraFee: Double,
    val paymentMethod: String,
    val paidAmount: Double,
    val notes: String,
    val loyaltyEnabled: Boolean
)

class NexoraRepository(
    private val dao: NexoraDao,
    private val syncService: FirestoreSyncService,
    private val networkMonitor: NetworkMonitor
) {
    val outlets: Flow<List<OutletEntity>> = dao.observeOutlets()
    val categories: Flow<List<CategoryEntity>> = dao.observeCategories()
    val products: Flow<List<ProductEntity>> = dao.observeProducts()
    val customers: Flow<List<CustomerEntity>> = dao.observeCustomers()
    val suppliers: Flow<List<SupplierEntity>> = dao.observeSuppliers()
    val sales: Flow<List<SaleEntity>> = dao.observeSales()
    val saleItems: Flow<List<SaleItemEntity>> = dao.observeAllSaleItems()
    val stockMovements: Flow<List<StockMovementEntity>> = dao.observeStockMovements()
    val purchases: Flow<List<PurchaseEntity>> = dao.observePurchases()
    val returns: Flow<List<ReturnEntity>> = dao.observeReturns()
    val tables: Flow<List<RestaurantTableEntity>> = dao.observeRestaurantTables()
    val restaurantOrders: Flow<List<RestaurantOrderEntity>> = dao.observeRestaurantOrders()
    val digitalOrders: Flow<List<DigitalOrderEntity>> = dao.observeDigitalOrders()
    val staffMembers: Flow<List<StaffMemberEntity>> = dao.observeStaffMembers()
    val activityLogs: Flow<List<ActivityLogEntity>> = dao.observeActivityLogs()
    val settings: Flow<BusinessSettingsEntity> = dao.observeSettings().map { it ?: BusinessSettingsEntity() }
    val syncQueue: Flow<List<SyncQueueEntity>> = dao.observeSyncQueue()

    private fun newId(prefix: String): String =
        "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(12)}"

    private suspend fun enqueue(entityType: String, entityId: String, operation: String, json: JSONObject) {
        dao.enqueueSyncItem(
            SyncQueueEntity(
                id = "${entityType}_${entityId}",
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                payloadJson = json.toString(),
                status = "PENDING",
                retryCount = 0,
                lastError = null,
                createdAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun logActivity(actorName: String, actorRole: String, action: String, details: String) {
        val id = newId("log")
        val now = System.currentTimeMillis()
        val entry = ActivityLogEntity(
            id = id,
            actorName = actorName.take(100).ifBlank { "Sistem" },
            actorRole = actorRole.take(30).ifBlank { "OWNER" },
            action = action.take(100),
            details = details.take(500),
            createdAtEpochMs = now
        )
        dao.insertActivityLog(entry)
        enqueue("activity_log", id, "UPSERT", JSONObject().apply {
            put("actorName", entry.actorName)
            put("actorRole", entry.actorRole)
            put("action", entry.action)
            put("details", entry.details)
            put("createdAtEpochMs", entry.createdAtEpochMs)
        })
    }

    suspend fun ensureInitialBootstrap(ownerName: String, ownerEmail: String?) {
        val currentSettings = dao.getSettings()
        if (currentSettings == null) {
            val defaultSettings = BusinessSettingsEntity(
                businessName = "NEXORA Store",
                businessType = "Minimarket & Retail"
            )
            dao.upsertSettings(defaultSettings)
            enqueueSettings(defaultSettings)
        }
        val existingOutlets = dao.getAllOutlets()
        if (existingOutlets.isEmpty()) {
            val mainOutlet = OutletEntity(
                id = "outlet_main",
                name = "Cabang Utama",
                address = "Alamat belum diatur",
                phone = "-",
                active = true,
                mainOutlet = true,
                demoData = false
            )
            dao.upsertOutlet(mainOutlet)
            enqueueOutlet(mainOutlet)
        }
        val existingStaff = dao.observeStaffMembers().first()
        if (existingStaff.isEmpty()) {
            val ownerStaff = StaffMemberEntity(
                id = "staff_owner",
                name = ownerName.ifBlank { "Pemilik Usaha" },
                role = "OWNER",
                outletId = "outlet_main",
                pinCode = "0000",
                active = true,
                demoData = false
            )
            dao.upsertStaffMember(ownerStaff)
            enqueueStaff(ownerStaff)
        }
        if (dao.getAllCategories().isEmpty()) {
            val defaultCategory = CategoryEntity(
                id = "cat_general",
                name = "Umum",
                colorHex = "#2563EB",
                iconName = "Category",
                businessType = "Retail",
                demoData = false
            )
            dao.upsertCategory(defaultCategory)
            enqueueCategory(defaultCategory)
        }
        if (networkMonitor.isCurrentlyOnline) {
            syncService.syncUserProfile(
                displayName = ownerName.ifBlank { "Pemilik Usaha" },
                email = ownerEmail,
                businessName = currentSettings?.businessName ?: "NEXORA Store"
            )
        }
    }

    suspend fun loadDemoDataset(ownerName: String) {
        val bundle = DemoDataSeeder.buildDemoBundle(ownerName)
        dao.upsertOutlets(bundle.outlets)
        dao.upsertCategories(bundle.categories)
        dao.upsertProducts(bundle.products)
        dao.upsertCustomers(bundle.customers)
        dao.upsertSuppliers(bundle.suppliers)
        dao.upsertRestaurantTables(bundle.tables)
        dao.upsertRestaurantOrders(bundle.restaurantOrders)
        dao.upsertDigitalOrders(bundle.digitalOrders)
        dao.upsertStaffMembers(bundle.staffMembers)
        dao.upsertSales(bundle.sales)
        dao.upsertSaleItems(bundle.saleItems)
        dao.upsertStockMovements(bundle.stockMovements)
        dao.upsertPurchases(bundle.purchases)
        bundle.outlets.forEach { enqueueOutlet(it) }
        bundle.categories.forEach { enqueueCategory(it) }
        bundle.products.forEach { enqueueProduct(it) }
        bundle.customers.forEach { enqueueCustomer(it) }
        bundle.suppliers.forEach { enqueueSupplier(it) }
        bundle.staffMembers.forEach { enqueueStaff(it) }
        bundle.sales.forEach { enqueueSale(it) }
        logActivity(ownerName, "OWNER", "LOAD_DEMO", "Data demo multi-bisnis dimuat")
    }

    suspend fun clearDemoDataset(actorName: String, actorRole: String) {
        dao.clearDemoData()
        logActivity(actorName, actorRole, "CLEAR_DEMO", "Seluruh data demo dihapus dari penyimpanan lokal")
    }

    suspend fun saveOutlet(outlet: OutletEntity, actorName: String, actorRole: String): Result<OutletEntity> {
        return try {
            require(outlet.name.trim().length in 1..100) { "Nama cabang wajib diisi (maks 100 karakter)" }
            val saved = outlet.copy(
                id = outlet.id.ifBlank { newId("outlet") },
                name = outlet.name.trim(),
                address = outlet.address.trim().take(300),
                phone = outlet.phone.trim().take(40),
                updatedAtEpochMs = System.currentTimeMillis()
            )
            dao.upsertOutlet(saved)
            enqueueOutlet(saved)
            logActivity(actorName, actorRole, "SAVE_OUTLET", "Cabang: ${saved.name}")
            tryAutoFlush()
            Result.success(saved)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun saveCategory(category: CategoryEntity, actorName: String, actorRole: String): Result<CategoryEntity> {
        return try {
            require(category.name.trim().length in 1..80) { "Nama kategori wajib diisi (maks 80 karakter)" }
            val saved = category.copy(
                id = category.id.ifBlank { newId("cat") },
                name = category.name.trim(),
                updatedAtEpochMs = System.currentTimeMillis()
            )
            dao.upsertCategory(saved)
            enqueueCategory(saved)
            logActivity(actorName, actorRole, "SAVE_CATEGORY", "Kategori: ${saved.name}")
            tryAutoFlush()
            Result.success(saved)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteCategory(id: String, name: String, actorName: String, actorRole: String) {
        dao.deleteCategoryById(id)
        enqueue("category", id, "DELETE", JSONObject())
        logActivity(actorName, actorRole, "DELETE_CATEGORY", "Hapus kategori: $name")
        tryAutoFlush()
    }

    suspend fun saveProduct(product: ProductEntity, actorName: String, actorRole: String): Result<ProductEntity> {
        return try {
            require(product.name.trim().length in 1..150) { "Nama produk wajib diisi" }
            require(product.sellPrice >= 0 && product.buyPrice >= 0) { "Harga tidak boleh negatif" }
            val saved = product.copy(
                id = product.id.ifBlank { newId("prod") },
                name = product.name.trim(),
                sku = product.sku.trim().ifBlank { "SKU-${System.currentTimeMillis() % 100000}" },
                barcode = product.barcode.trim(),
                unit = product.unit.trim().ifBlank { "pcs" },
                updatedAtEpochMs = System.currentTimeMillis()
            )
            dao.upsertProduct(saved)
            enqueueProduct(saved)
            logActivity(actorName, actorRole, "SAVE_PRODUCT", "${saved.name} (${saved.sku})")
            tryAutoFlush()
            Result.success(saved)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteProduct(product: ProductEntity, actorName: String, actorRole: String) {
        dao.deleteProductById(product.id)
        enqueue("product", product.id, "DELETE", JSONObject())
        logActivity(actorName, actorRole, "DELETE_PRODUCT", "Hapus produk: ${product.name}")
        tryAutoFlush()
    }

    suspend fun findProductByBarcode(barcode: String): ProductEntity? {
        val trimmed = barcode.trim()
        if (trimmed.isBlank()) return null
        return dao.findProductByBarcode(trimmed)
    }

    suspend fun saveCustomer(customer: CustomerEntity, actorName: String, actorRole: String): Result<CustomerEntity> {
        return try {
            require(customer.name.trim().length in 1..120) { "Nama pelanggan wajib diisi" }
            val saved = customer.copy(
                id = customer.id.ifBlank { newId("cust") },
                name = customer.name.trim(),
                phone = customer.phone.trim().take(40),
                address = customer.address.trim().take(300),
                updatedAtEpochMs = System.currentTimeMillis()
            )
            dao.upsertCustomer(saved)
            enqueueCustomer(saved)
            logActivity(actorName, actorRole, "SAVE_CUSTOMER", "Pelanggan: ${saved.name}")
            tryAutoFlush()
            Result.success(saved)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteCustomer(customer: CustomerEntity, actorName: String, actorRole: String) {
        dao.deleteCustomerById(customer.id)
        enqueue("customer", customer.id, "DELETE", JSONObject())
        logActivity(actorName, actorRole, "DELETE_CUSTOMER", "Hapus pelanggan: ${customer.name}")
        tryAutoFlush()
    }

    suspend fun saveSupplier(supplier: SupplierEntity, actorName: String, actorRole: String): Result<SupplierEntity> {
        return try {
            require(supplier.name.trim().length in 1..120) { "Nama supplier wajib diisi" }
            val saved = supplier.copy(
                id = supplier.id.ifBlank { newId("sup") },
                name = supplier.name.trim(),
                phone = supplier.phone.trim().take(40),
                address = supplier.address.trim().take(300),
                notes = supplier.notes.trim().take(300),
                updatedAtEpochMs = System.currentTimeMillis()
            )
            dao.upsertSupplier(saved)
            enqueueSupplier(saved)
            logActivity(actorName, actorRole, "SAVE_SUPPLIER", "Supplier: ${saved.name}")
            tryAutoFlush()
            Result.success(saved)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun adjustStock(
        productId: String,
        movementType: String, // IN, OUT, ADJUST, OPNAME
        quantityInput: Int,
        note: String,
        actorName: String,
        actorRole: String
    ): Result<ProductEntity> {
        return try {
            val product = dao.getProductById(productId) ?: return Result.failure(IllegalStateException("Produk tidak ditemukan"))
            val prevStock = product.stock
            val newStock = when (movementType) {
                "IN" -> prevStock + kotlin.math.abs(quantityInput)
                "OUT" -> (prevStock - kotlin.math.abs(quantityInput)).coerceAtLeast(0)
                "OPNAME" -> quantityInput.coerceAtLeast(0)
                else -> (prevStock + quantityInput).coerceAtLeast(0)
            }
            val delta = newStock - prevStock
            val now = System.currentTimeMillis()
            val updated = product.copy(stock = newStock, updatedAtEpochMs = now)
            dao.upsertProduct(updated)
            enqueueProduct(updated)

            val movement = StockMovementEntity(
                id = newId("mov"),
                outletId = product.outletId,
                productId = product.id,
                productName = product.name,
                type = movementType,
                quantityDelta = delta,
                previousStock = prevStock,
                newStock = newStock,
                note = note.take(300),
                actorName = actorName,
                createdAtEpochMs = now,
                demoData = product.demoData
            )
            dao.upsertStockMovement(movement)
            enqueueStockMovement(movement)
            logActivity(actorName, actorRole, "STOCK_$movementType", "${product.name}: $prevStock -> $newStock")
            tryAutoFlush()
            Result.success(updated)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun transferStockBetweenOutlets(
        productId: String,
        targetOutletId: String,
        quantity: Int,
        actorName: String,
        actorRole: String
    ): Result<Unit> {
        return try {
            require(quantity > 0) { "Jumlah transfer harus lebih dari 0" }
            val sourceProduct = dao.getProductById(productId)
                ?: return Result.failure(IllegalStateException("Produk sumber tidak ditemukan"))
            require(sourceProduct.outletId != targetOutletId) { "Cabang tujuan harus berbeda" }
            require(sourceProduct.stock >= quantity) { "Stok cabang asal tidak mencukupi" }

            val now = System.currentTimeMillis()
            val sourceNewStock = sourceProduct.stock - quantity
            val updatedSource = sourceProduct.copy(stock = sourceNewStock, updatedAtEpochMs = now)
            dao.upsertProduct(updatedSource)
            enqueueProduct(updatedSource)

            val outMov = StockMovementEntity(
                id = newId("mov"),
                outletId = sourceProduct.outletId,
                productId = sourceProduct.id,
                productName = sourceProduct.name,
                type = "TRANSFER",
                quantityDelta = -quantity,
                previousStock = sourceProduct.stock,
                newStock = sourceNewStock,
                note = "Transfer keluar ke cabang $targetOutletId",
                actorName = actorName,
                createdAtEpochMs = now,
                demoData = sourceProduct.demoData
            )
            dao.upsertStockMovement(outMov)
            enqueueStockMovement(outMov)

            val existingTarget = dao.getAllProducts().firstOrNull {
                it.outletId == targetOutletId && (
                    (sourceProduct.barcode.isNotBlank() && it.barcode == sourceProduct.barcode) ||
                    it.sku == sourceProduct.sku
                )
            }
            if (existingTarget != null) {
                val targetNew = existingTarget.stock + quantity
                val updatedTarget = existingTarget.copy(stock = targetNew, updatedAtEpochMs = now)
                dao.upsertProduct(updatedTarget)
                enqueueProduct(updatedTarget)
                val inMov = StockMovementEntity(
                    id = newId("mov"),
                    outletId = targetOutletId,
                    productId = updatedTarget.id,
                    productName = updatedTarget.name,
                    type = "TRANSFER",
                    quantityDelta = quantity,
                    previousStock = existingTarget.stock,
                    newStock = targetNew,
                    note = "Transfer masuk dari ${sourceProduct.outletId}",
                    actorName = actorName,
                    createdAtEpochMs = now
                )
                dao.upsertStockMovement(inMov)
                enqueueStockMovement(inMov)
            } else {
                val createdTarget = sourceProduct.copy(
                    id = newId("prod"),
                    outletId = targetOutletId,
                    stock = quantity,
                    updatedAtEpochMs = now
                )
                dao.upsertProduct(createdTarget)
                enqueueProduct(createdTarget)
                val inMov = StockMovementEntity(
                    id = newId("mov"),
                    outletId = targetOutletId,
                    productId = createdTarget.id,
                    productName = createdTarget.name,
                    type = "TRANSFER",
                    quantityDelta = quantity,
                    previousStock = 0,
                    newStock = quantity,
                    note = "Transfer masuk cabang baru",
                    actorName = actorName,
                    createdAtEpochMs = now
                )
                dao.upsertStockMovement(inMov)
                enqueueStockMovement(inMov)
            }

            logActivity(actorName, actorRole, "TRANSFER_STOCK", "Transfer $quantity ${sourceProduct.name} ke $targetOutletId")
            tryAutoFlush()
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun recordPurchase(
        outletId: String,
        supplier: SupplierEntity,
        product: ProductEntity,
        quantity: Int,
        unitCost: Double,
        invoiceNumber: String,
        notes: String,
        actorName: String,
        actorRole: String
    ): Result<PurchaseEntity> {
        return try {
            require(quantity > 0) { "Jumlah pembelian harus > 0" }
            val now = System.currentTimeMillis()
            val totalAmount = quantity * unitCost
            val purchase = PurchaseEntity(
                id = newId("po"),
                outletId = outletId,
                supplierId = supplier.id,
                supplierName = supplier.name,
                invoiceNumber = invoiceNumber.ifBlank { "PO-${now % 100000}" },
                totalAmount = totalAmount,
                status = "RECEIVED",
                notes = "${product.name} x$quantity @ $unitCost. $notes".trim().take(300),
                actorName = actorName,
                createdAtEpochMs = now
            )
            dao.upsertPurchase(purchase)
            enqueuePurchase(purchase)

            val prev = product.stock
            val next = prev + quantity
            val updatedProd = product.copy(stock = next, buyPrice = unitCost, updatedAtEpochMs = now)
            dao.upsertProduct(updatedProd)
            enqueueProduct(updatedProd)

            val mov = StockMovementEntity(
                id = newId("mov"),
                outletId = outletId,
                productId = product.id,
                productName = product.name,
                type = "IN",
                quantityDelta = quantity,
                previousStock = prev,
                newStock = next,
                note = "Pembelian ${purchase.invoiceNumber} (${supplier.name})",
                actorName = actorName,
                createdAtEpochMs = now
            )
            dao.upsertStockMovement(mov)
            enqueueStockMovement(mov)

            logActivity(actorName, actorRole, "PURCHASE", "${purchase.invoiceNumber} - ${product.name} +$quantity")
            tryAutoFlush()
            Result.success(purchase)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun returnPurchase(purchase: PurchaseEntity, actorName: String, actorRole: String): Result<Unit> {
        return try {
            val updated = purchase.copy(status = "RETURNED")
            dao.upsertPurchase(updated)
            enqueuePurchase(updated)
            logActivity(actorName, actorRole, "PURCHASE_RETURN", "Retur pembelian ${purchase.invoiceNumber}")
            tryAutoFlush()
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun completeCheckout(req: CheckoutRequest): Result<Pair<SaleEntity, List<SaleItemEntity>>> {
        return try {
            require(req.lines.isNotEmpty()) { "Keranjang belanja kosong" }
            val now = System.currentTimeMillis()
            val subtotal = req.lines.sumOf { it.product.sellPrice * it.quantity }
            val itemDiscountTotal = req.lines.sumOf { it.itemDiscountAmount.coerceAtLeast(0.0) }
            val afterItemDiscount = (subtotal - itemDiscountTotal).coerceAtLeast(0.0)
            val afterOrderDiscount = (afterItemDiscount - req.orderDiscount.coerceAtLeast(0.0)).coerceAtLeast(0.0)
            val taxAmount = if (req.taxEnabled) afterOrderDiscount * (req.taxRatePercent / 100.0) else 0.0
            val total = (afterOrderDiscount + taxAmount + req.extraFee.coerceAtLeast(0.0)).coerceAtLeast(0.0)

            require(req.paidAmount + 0.01 >= total) { "Nominal bayar kurang dari total tagihan" }
            val changeAmount = (req.paidAmount - total).coerceAtLeast(0.0)

            val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(now))
            val suffix = UUID.randomUUID().toString().substring(0, 4).uppercase(Locale.US)
            val txNumber = "NX-$dateStamp-$suffix"
            val saleId = newId("sale")

            var grossProfit = 0.0
            val saleItems = req.lines.map { line ->
                val lineGross = (line.product.sellPrice * line.quantity) - line.itemDiscountAmount
                val lineCost = line.product.buyPrice * line.quantity
                grossProfit += (lineGross - lineCost)
                SaleItemEntity(
                    id = newId("si"),
                    saleId = saleId,
                    productId = line.product.id,
                    productName = line.product.name,
                    variantLabel = line.variantLabel,
                    quantity = line.quantity,
                    buyPrice = line.product.buyPrice,
                    unitPrice = line.product.sellPrice,
                    discountAmount = line.itemDiscountAmount,
                    lineTotal = lineGross.coerceAtLeast(0.0),
                    demoData = false
                )
            }
            grossProfit -= req.orderDiscount.coerceAtLeast(0.0)

            val summary = req.lines.joinToString(", ") { "${it.product.name} x${it.quantity}" }.take(1950)
            val sale = SaleEntity(
                id = saleId,
                outletId = req.outletId,
                transactionNumber = txNumber,
                cashierName = req.cashierName,
                cashierRole = req.cashierRole,
                customerId = req.customer?.id,
                customerName = req.customer?.name,
                subtotal = subtotal,
                itemDiscountTotal = itemDiscountTotal,
                orderDiscount = req.orderDiscount,
                taxAmount = taxAmount,
                extraFee = req.extraFee,
                total = total,
                paymentMethod = req.paymentMethod,
                paidAmount = req.paidAmount,
                changeAmount = changeAmount,
                status = "COMPLETED",
                notes = req.notes.take(500),
                itemsSummary = summary,
                itemCount = req.lines.sumOf { it.quantity },
                grossProfit = grossProfit,
                createdAtEpochMs = now,
                demoData = false
            )

            dao.upsertSale(sale)
            dao.upsertSaleItems(saleItems)
            enqueueSale(sale)

            // Update stock & stock movements for non-digital products
            req.lines.forEach { line ->
                val latest = dao.getProductById(line.product.id) ?: line.product
                if (latest.productType != "DIGITAL") {
                    val prev = latest.stock
                    val next = (prev - line.quantity).coerceAtLeast(0)
                    val updatedProd = latest.copy(stock = next, updatedAtEpochMs = now)
                    dao.upsertProduct(updatedProd)
                    enqueueProduct(updatedProd)

                    val mov = StockMovementEntity(
                        id = newId("mov"),
                        outletId = req.outletId,
                        productId = latest.id,
                        productName = latest.name,
                        type = "SALE",
                        quantityDelta = -line.quantity,
                        previousStock = prev,
                        newStock = next,
                        note = "Transaksi $txNumber",
                        actorName = req.cashierName,
                        createdAtEpochMs = now
                    )
                    dao.upsertStockMovement(mov)
                    enqueueStockMovement(mov)
                }
            }

            // Update customer loyalty points & spend
            if (req.customer != null) {
                val latestCust = dao.getCustomerById(req.customer.id) ?: req.customer
                val earnedPoints = if (req.loyaltyEnabled) (total / 10_000.0).toInt() else 0
                val updatedCust = latestCust.copy(
                    points = latestCust.points + earnedPoints,
                    totalSpent = latestCust.totalSpent + total,
                    transactionCount = latestCust.transactionCount + 1,
                    updatedAtEpochMs = now
                )
                dao.upsertCustomer(updatedCust)
                enqueueCustomer(updatedCust)
            }

            logActivity(req.cashierName, req.cashierRole, "SALE_COMPLETED", "$txNumber • Total Rp ${total.toLong()}")
            tryAutoFlush()
            Result.success(sale to saleItems)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getSaleItems(saleId: String): List<SaleItemEntity> = dao.getSaleItems(saleId)

    suspend fun voidSale(sale: SaleEntity, reason: String, actorName: String, actorRole: String): Result<Unit> {
        return try {
            require(actorRole in listOf("OWNER", "ADMIN", "MANAJER")) {
                "Kasir tidak memiliki izin membatalkan transaksi"
            }
            require(sale.status != "VOIDED") { "Transaksi sudah dibatalkan" }
            val now = System.currentTimeMillis()
            val updated = sale.copy(
                status = "VOIDED",
                notes = if (sale.notes.isBlank()) "Dibatalkan ($actorName): $reason" else "${sale.notes} | Void ($actorName): $reason"
            )
            dao.upsertSale(updated)
            enqueueSale(updated)

            // Restore stock
            val items = dao.getSaleItems(sale.id)
            items.forEach { item ->
                val prod = dao.getProductById(item.productId)
                if (prod != null && prod.productType != "DIGITAL") {
                    val next = prod.stock + item.quantity
                    val upd = prod.copy(stock = next, updatedAtEpochMs = now)
                    dao.upsertProduct(upd)
                    enqueueProduct(upd)
                    val mov = StockMovementEntity(
                        id = newId("mov"),
                        outletId = sale.outletId,
                        productId = prod.id,
                        productName = prod.name,
                        type = "RETURN",
                        quantityDelta = item.quantity,
                        previousStock = prod.stock,
                        newStock = next,
                        note = "Pembatalan ${sale.transactionNumber}",
                        actorName = actorName,
                        createdAtEpochMs = now
                    )
                    dao.upsertStockMovement(mov)
                    enqueueStockMovement(mov)
                }
            }

            logActivity(actorName, actorRole, "VOID_SALE", "${sale.transactionNumber}: $reason")
            tryAutoFlush()
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun recordSaleReturn(
        sale: SaleEntity,
        reason: String,
        refundAmount: Double,
        actorName: String,
        actorRole: String
    ): Result<ReturnEntity> {
        return try {
            require(sale.status == "COMPLETED") { "Hanya transaksi selesai yang dapat diretur" }
            require(refundAmount >= 0 && refundAmount <= sale.total) { "Nominal retur tidak valid" }
            val now = System.currentTimeMillis()
            val ret = ReturnEntity(
                id = newId("ret"),
                outletId = sale.outletId,
                saleId = sale.id,
                transactionNumber = sale.transactionNumber,
                reason = reason.ifBlank { "Retur pelanggan" }.take(300),
                refundAmount = refundAmount,
                actorName = actorName,
                createdAtEpochMs = now
            )
            dao.upsertReturn(ret)
            enqueue("return", ret.id, "UPSERT", JSONObject().apply {
                put("outletId", ret.outletId)
                put("saleId", ret.saleId)
                put("transactionNumber", ret.transactionNumber)
                put("reason", ret.reason)
                put("refundAmount", ret.refundAmount)
                put("actorName", ret.actorName)
                put("demoData", ret.demoData)
            })

            val updatedSale = sale.copy(status = "RETURNED", notes = "${sale.notes} | Retur: $reason".trim())
            dao.upsertSale(updatedSale)
            enqueueSale(updatedSale)

            val items = dao.getSaleItems(sale.id)
            items.forEach { item ->
                val prod = dao.getProductById(item.productId)
                if (prod != null && prod.productType != "DIGITAL") {
                    val prev = prod.stock
                    val next = prev + item.quantity
                    val updatedProd = prod.copy(stock = next, updatedAtEpochMs = now)
                    dao.upsertProduct(updatedProd)
                    enqueueProduct(updatedProd)

                    val mov = StockMovementEntity(
                        id = newId("mov"),
                        outletId = sale.outletId,
                        productId = prod.id,
                        productName = prod.name,
                        type = "RETURN",
                        quantityDelta = item.quantity,
                        previousStock = prev,
                        newStock = next,
                        note = "Retur ${sale.transactionNumber}: ${ret.reason}",
                        actorName = actorName,
                        createdAtEpochMs = now
                    )
                    dao.upsertStockMovement(mov)
                    enqueueStockMovement(mov)
                }
            }

            logActivity(actorName, actorRole, "RETURN_SALE", "${sale.transactionNumber} - Rp ${refundAmount.toLong()}")
            tryAutoFlush()
            Result.success(ret)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(e) }
    }

    // Restaurant operations
    suspend fun saveTable(table: RestaurantTableEntity, actorName: String, actorRole: String) {
        val saved = table.copy(id = table.id.ifBlank { newId("tbl") })
        dao.upsertRestaurantTable(saved)
        enqueue("table", saved.id, "UPSERT", JSONObject().apply {
            put("outletId", saved.outletId)
            put("tableName", saved.tableName)
            put("capacity", saved.capacity)
            put("status", saved.status)
            if (!saved.activeOrderNumber.isNullOrBlank()) put("activeOrderNumber", saved.activeOrderNumber)
            put("demoData", saved.demoData)
        })
        logActivity(actorName, actorRole, "SAVE_TABLE", "${saved.tableName} (${saved.status})")
        tryAutoFlush()
    }

    suspend fun createRestaurantOrder(
        outletId: String,
        orderType: String,
        table: RestaurantTableEntity?,
        itemsSummary: String,
        notes: String,
        totalAmount: Double,
        actorName: String,
        actorRole: String
    ): RestaurantOrderEntity {
        val now = System.currentTimeMillis()
        val queueNo = "Q-${(now % 900 + 100)}"
        val order = RestaurantOrderEntity(
            id = newId("ro"),
            outletId = outletId,
            queueNumber = queueNo,
            orderType = orderType,
            tableId = table?.id,
            tableName = table?.tableName ?: if (orderType == "TAKEAWAY") "Bawa Pulang" else "Dine-In",
            itemsSummary = itemsSummary.take(1000),
            notes = notes.take(300),
            status = "QUEUED",
            totalAmount = totalAmount,
            createdAtEpochMs = now
        )
        dao.upsertRestaurantOrder(order)
        enqueueRestaurantOrder(order)
        if (table != null && orderType == "DINE_IN") {
            val occupied = table.copy(status = "OCCUPIED", activeOrderNumber = queueNo)
            saveTable(occupied, actorName, actorRole)
        }
        logActivity(actorName, actorRole, "RESTO_ORDER", "$queueNo • ${order.tableName}")
        tryAutoFlush()
        return order
    }

    suspend fun updateRestaurantOrderStatus(
        order: RestaurantOrderEntity,
        newStatus: String,
        tables: List<RestaurantTableEntity>,
        actorName: String,
        actorRole: String
    ) {
        val updated = order.copy(status = newStatus)
        dao.upsertRestaurantOrder(updated)
        enqueueRestaurantOrder(updated)
        if (newStatus == "SERVED" && order.tableId != null) {
            tables.firstOrNull { it.id == order.tableId }?.let { tbl ->
                saveTable(tbl.copy(status = "AVAILABLE", activeOrderNumber = null), actorName, actorRole)
            }
        }
        logActivity(actorName, actorRole, "KDS_STATUS", "${order.queueNumber} -> $newStatus")
        tryAutoFlush()
    }

    // Digital store operations
    suspend fun createDigitalOrder(
        outletId: String,
        product: ProductEntity,
        variantLabel: String,
        recipientInfo: String,
        notes: String,
        actorName: String,
        actorRole: String
    ): DigitalOrderEntity {
        val now = System.currentTimeMillis()
        val order = DigitalOrderEntity(
            id = newId("do"),
            outletId = outletId,
            productId = product.id,
            productName = product.name,
            categoryName = "Produk Digital",
            variantLabel = variantLabel.ifBlank { "Reguler" },
            recipientInfo = recipientInfo.trim(),
            notes = notes.trim(),
            price = product.sellPrice,
            deliveryStatus = "PENDING",
            deliveredAtEpochMs = null,
            createdAtEpochMs = now
        )
        dao.upsertDigitalOrder(order)
        enqueueDigitalOrder(order)
        logActivity(actorName, actorRole, "DIGITAL_ORDER", "${product.name} -> $recipientInfo")
        tryAutoFlush()
        return order
    }

    suspend fun updateDigitalDeliveryStatus(
        order: DigitalOrderEntity,
        status: String,
        notes: String,
        actorName: String,
        actorRole: String
    ) {
        val updated = order.copy(
            deliveryStatus = status,
            notes = notes.ifBlank { order.notes },
            deliveredAtEpochMs = if (status == "DELIVERED") System.currentTimeMillis() else order.deliveredAtEpochMs
        )
        dao.upsertDigitalOrder(updated)
        enqueueDigitalOrder(updated)
        logActivity(actorName, actorRole, "DIGITAL_DELIVERY", "${order.productName} ($status)")
        tryAutoFlush()
    }

    // Staff & Settings
    suspend fun saveStaffMember(member: StaffMemberEntity, actorName: String, actorRole: String): Result<StaffMemberEntity> {
        return try {
            require(member.name.trim().length in 1..100) { "Nama staf wajib diisi" }
            require(member.pinCode.length in 4..12) { "PIN minimal 4 digit" }
            val saved = member.copy(id = member.id.ifBlank { newId("staff") }, name = member.name.trim())
            dao.upsertStaffMember(saved)
            enqueueStaff(saved)
            logActivity(actorName, actorRole, "SAVE_STAFF", "${saved.name} (${saved.role})")
            tryAutoFlush()
            Result.success(saved)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteStaffMember(member: StaffMemberEntity, actorName: String, actorRole: String) {
        dao.deleteStaffMemberById(member.id)
        enqueue("staff_member", member.id, "DELETE", JSONObject())
        logActivity(actorName, actorRole, "DELETE_STAFF", "Hapus staf: ${member.name}")
        tryAutoFlush()
    }

    suspend fun saveSettings(settings: BusinessSettingsEntity, actorName: String, actorRole: String) {
        val updated = settings.copy(id = "default", updatedAtEpochMs = System.currentTimeMillis())
        dao.upsertSettings(updated)
        enqueueSettings(updated)
        logActivity(actorName, actorRole, "UPDATE_SETTINGS", "Pengaturan bisnis diperbarui")
        tryAutoFlush()
    }

    // Sync Engine (only marks SYNCED when Firestore confirms)
    private suspend fun tryAutoFlush() {
        if (networkMonitor.isCurrentlyOnline) {
            flushSyncQueue()
        }
    }

    suspend fun flushSyncQueue(): Result<Int> {
        if (!networkMonitor.isCurrentlyOnline) {
            return Result.failure(IllegalStateException("Perangkat sedang offline. Antrean tetap aman tersimpan secara lokal."))
        }
        val pending = dao.getPendingSyncItems()
        var syncedCount = 0
        var lastErr: Exception? = null
        for (item in pending) {
            val res = syncService.pushQueueItem(item)
            if (res.isSuccess) {
                dao.updateSyncItemStatus(item.id, "SYNCED", item.retryCount, null)
                syncedCount++
            } else {
                val err = res.exceptionOrNull() as? Exception ?: Exception("Sync error")
                lastErr = err
                dao.updateSyncItemStatus(
                    item.id,
                    "FAILED",
                    item.retryCount + 1,
                    err.localizedMessage?.take(200)
                )
            }
        }
        dao.pruneSyncedItems()
        return if (lastErr != null && syncedCount == 0) {
            Result.failure(lastErr)
        } else {
            Result.success(syncedCount)
        }
    }

    suspend fun pullFromCloud(): Result<Int> {
        if (!networkMonitor.isCurrentlyOnline) {
            return Result.failure(IllegalStateException("Tidak dapat menarik data cloud saat offline"))
        }
        return syncService.hydrateCloudDataToRoom(dao)
    }

    // Export CSV & JSON Backup
    suspend fun exportSalesCsv(): String {
        val allSales = dao.getAllSales().sortedByDescending { it.createdAtEpochMs }
        val sb = StringBuilder()
        sb.appendLine("No Transaksi,Tanggal,Cabang,Kasir,Pelanggan,Subtotal,Diskon,Pajak,Biaya Lain,Total,Laba Kotor,Metode Bayar,Status")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        allSales.forEach { s ->
            val date = sdf.format(Date(s.createdAtEpochMs))
            val totalDisc = s.itemDiscountTotal + s.orderDiscount
            val line = listOf(
                s.transactionNumber,
                date,
                s.outletId,
                s.cashierName,
                s.customerName ?: "Umum",
                s.subtotal.toLong().toString(),
                totalDisc.toLong().toString(),
                s.taxAmount.toLong().toString(),
                s.extraFee.toLong().toString(),
                s.total.toLong().toString(),
                s.grossProfit.toLong().toString(),
                s.paymentMethod,
                s.status
            ).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
            sb.appendLine(line)
        }
        return sb.toString()
    }

    suspend fun createBackupJson(): String {
        val root = JSONObject()
        root.put("app", "NEXORA POS")
        root.put("exportedAt", System.currentTimeMillis())

        val prodsArr = JSONArray()
        dao.getAllProducts().forEach { p ->
            prodsArr.put(JSONObject().apply {
                put("id", p.id); put("outletId", p.outletId); put("categoryId", p.categoryId)
                put("name", p.name); put("sku", p.sku); put("barcode", p.barcode)
                put("buyPrice", p.buyPrice); put("sellPrice", p.sellPrice); put("stock", p.stock)
                put("minStock", p.minStock); put("unit", p.unit); put("productType", p.productType)
                put("variantsSummary", p.variantsSummary); put("active", p.active)
            })
        }
        root.put("products", prodsArr)

        val catsArr = JSONArray()
        dao.getAllCategories().forEach { c ->
            catsArr.put(JSONObject().apply {
                put("id", c.id); put("name", c.name); put("colorHex", c.colorHex)
                put("iconName", c.iconName); put("businessType", c.businessType)
            })
        }
        root.put("categories", catsArr)

        val custArr = JSONArray()
        dao.getAllCustomers().forEach { c ->
            custArr.put(JSONObject().apply {
                put("id", c.id); put("name", c.name); put("phone", c.phone)
                put("address", c.address); put("points", c.points)
                put("totalSpent", c.totalSpent); put("transactionCount", c.transactionCount)
            })
        }
        root.put("customers", custArr)

        return root.toString(2)
    }

    suspend fun restoreFromBackupJson(jsonText: String, actorName: String, actorRole: String): Result<Int> {
        return try {
            val root = JSONObject(jsonText)
            var count = 0
            root.optJSONArray("categories")?.let { arr ->
                val list = mutableListOf<CategoryEntity>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val c = CategoryEntity(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        colorHex = o.optString("colorHex", "#2563EB"),
                        iconName = o.optString("iconName", "Category"),
                        businessType = o.optString("businessType", "Retail")
                    )
                    list.add(c); enqueueCategory(c); count++
                }
                dao.upsertCategories(list)
            }
            root.optJSONArray("products")?.let { arr ->
                val list = mutableListOf<ProductEntity>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val p = ProductEntity(
                        id = o.getString("id"),
                        outletId = o.optString("outletId", "outlet_main"),
                        categoryId = o.optString("categoryId", "cat_general"),
                        name = o.getString("name"),
                        sku = o.optString("sku", ""),
                        barcode = o.optString("barcode", ""),
                        buyPrice = o.optDouble("buyPrice", 0.0),
                        sellPrice = o.optDouble("sellPrice", 0.0),
                        stock = o.optInt("stock", 0),
                        minStock = o.optInt("minStock", 5),
                        unit = o.optString("unit", "pcs"),
                        productType = o.optString("productType", "PHYSICAL"),
                        variantsSummary = o.optString("variantsSummary", ""),
                        active = o.optBoolean("active", true)
                    )
                    list.add(p); enqueueProduct(p); count++
                }
                dao.upsertProducts(list)
            }
            root.optJSONArray("customers")?.let { arr ->
                val list = mutableListOf<CustomerEntity>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val c = CustomerEntity(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        phone = o.optString("phone", ""),
                        address = o.optString("address", ""),
                        points = o.optInt("points", 0),
                        totalSpent = o.optDouble("totalSpent", 0.0),
                        transactionCount = o.optInt("transactionCount", 0)
                    )
                    list.add(c); enqueueCustomer(c); count++
                }
                dao.upsertCustomers(list)
            }
            logActivity(actorName, actorRole, "RESTORE_BACKUP", "Pulihkan $count data dari cadangan JSON")
            tryAutoFlush()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Serialization helpers for SyncQueue
    private suspend fun enqueueOutlet(o: OutletEntity) = enqueue("outlet", o.id, "UPSERT", JSONObject().apply {
        put("name", o.name); put("address", o.address); put("phone", o.phone)
        put("active", o.active); put("mainOutlet", o.mainOutlet); put("demoData", o.demoData)
    })

    private suspend fun enqueueCategory(c: CategoryEntity) = enqueue("category", c.id, "UPSERT", JSONObject().apply {
        put("name", c.name); put("colorHex", c.colorHex); put("iconName", c.iconName)
        put("businessType", c.businessType); put("demoData", c.demoData)
    })

    private suspend fun enqueueProduct(p: ProductEntity) = enqueue("product", p.id, "UPSERT", JSONObject().apply {
        put("outletId", p.outletId); put("categoryId", p.categoryId); put("name", p.name)
        put("sku", p.sku); put("barcode", p.barcode); put("buyPrice", p.buyPrice)
        put("sellPrice", p.sellPrice); put("stock", p.stock); put("minStock", p.minStock)
        put("unit", p.unit); put("description", p.description); put("productType", p.productType)
        put("variantsSummary", p.variantsSummary); put("active", p.active); put("demoData", p.demoData)
    })

    private suspend fun enqueueCustomer(c: CustomerEntity) = enqueue("customer", c.id, "UPSERT", JSONObject().apply {
        put("name", c.name); put("phone", c.phone); put("address", c.address)
        put("points", c.points); put("totalSpent", c.totalSpent)
        put("transactionCount", c.transactionCount); put("demoData", c.demoData)
    })

    private suspend fun enqueueSupplier(s: SupplierEntity) = enqueue("supplier", s.id, "UPSERT", JSONObject().apply {
        put("name", s.name); put("phone", s.phone); put("address", s.address)
        put("notes", s.notes); put("demoData", s.demoData)
    })

    private suspend fun enqueueSale(s: SaleEntity) = enqueue("sale", s.id, "UPSERT", JSONObject().apply {
        put("outletId", s.outletId); put("transactionNumber", s.transactionNumber)
        put("cashierName", s.cashierName); put("cashierRole", s.cashierRole)
        s.customerId?.let { put("customerId", it) }
        s.customerName?.let { put("customerName", it) }
        put("subtotal", s.subtotal); put("itemDiscountTotal", s.itemDiscountTotal)
        put("orderDiscount", s.orderDiscount); put("taxAmount", s.taxAmount)
        put("extraFee", s.extraFee); put("total", s.total)
        put("paymentMethod", s.paymentMethod); put("paidAmount", s.paidAmount)
        put("changeAmount", s.changeAmount); put("status", s.status)
        put("notes", s.notes); put("itemsSummary", s.itemsSummary)
        put("itemCount", s.itemCount); put("grossProfit", s.grossProfit)
        put("createdAtEpochMs", s.createdAtEpochMs); put("demoData", s.demoData)
    })

    private suspend fun enqueueStockMovement(m: StockMovementEntity) = enqueue("stock_movement", m.id, "UPSERT", JSONObject().apply {
        put("outletId", m.outletId); put("productId", m.productId); put("productName", m.productName)
        put("type", m.type); put("quantityDelta", m.quantityDelta)
        put("previousStock", m.previousStock); put("newStock", m.newStock)
        put("note", m.note); put("actorName", m.actorName); put("demoData", m.demoData)
    })

    private suspend fun enqueuePurchase(p: PurchaseEntity) = enqueue("purchase", p.id, "UPSERT", JSONObject().apply {
        put("outletId", p.outletId); put("supplierId", p.supplierId); put("supplierName", p.supplierName)
        put("invoiceNumber", p.invoiceNumber); put("totalAmount", p.totalAmount)
        put("status", p.status); put("notes", p.notes); put("actorName", p.actorName)
        put("demoData", p.demoData)
    })

    private suspend fun enqueueRestaurantOrder(o: RestaurantOrderEntity) = enqueue("restaurant_order", o.id, "UPSERT", JSONObject().apply {
        put("outletId", o.outletId); put("queueNumber", o.queueNumber); put("orderType", o.orderType)
        o.tableId?.let { put("tableId", it) }
        o.tableName?.let { put("tableName", it) }
        put("itemsSummary", o.itemsSummary); put("notes", o.notes)
        put("status", o.status); put("totalAmount", o.totalAmount); put("demoData", o.demoData)
    })

    private suspend fun enqueueDigitalOrder(o: DigitalOrderEntity) = enqueue("digital_order", o.id, "UPSERT", JSONObject().apply {
        put("outletId", o.outletId); put("productId", o.productId); put("productName", o.productName)
        put("categoryName", o.categoryName); put("variantLabel", o.variantLabel)
        put("recipientInfo", o.recipientInfo); put("notes", o.notes); put("price", o.price)
        put("deliveryStatus", o.deliveryStatus)
        o.deliveredAtEpochMs?.let { put("deliveredAtEpochMs", it) }
        put("demoData", o.demoData)
    })

    private suspend fun enqueueStaff(s: StaffMemberEntity) = enqueue("staff_member", s.id, "UPSERT", JSONObject().apply {
        put("name", s.name); put("role", s.role); put("outletId", s.outletId)
        put("pinCode", s.pinCode); put("active", s.active); put("demoData", s.demoData)
    })

    private suspend fun enqueueSettings(s: BusinessSettingsEntity) = enqueue("business_settings", "default", "UPSERT", JSONObject().apply {
        put("businessName", s.businessName); put("businessType", s.businessType)
        put("address", s.address); put("phone", s.phone)
        put("taxPercent", s.taxPercent); put("currencyCode", s.currencyCode)
        put("receiptFooter", s.receiptFooter); put("receiptWidthMm", s.receiptWidthMm)
        put("taxEnabled", s.taxEnabled); put("loyaltyEnabled", s.loyaltyEnabled)
        put("scannerSoundEnabled", s.scannerSoundEnabled)
    })
}
