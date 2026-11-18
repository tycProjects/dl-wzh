package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class FirestoreSyncService(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")
    }

    suspend fun syncUserProfile(
        displayName: String,
        email: String?,
        businessName: String
    ): Result<Unit> {
        var docPath = "users/unknown"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
            docPath = ref.path
            val snap = ref.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName.take(120).ifBlank { "Pemilik Usaha" },
                    "email" to email?.take(200),
                    "businessName" to businessName.take(120).ifBlank { "NEXORA POS" },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                ref.set(payload).await()
            } else {
                val updatePayload = mapOf(
                    "displayName" to displayName.take(120).ifBlank { "Pemilik Usaha" },
                    "email" to email?.take(200),
                    "businessName" to businessName.take(120).ifBlank { "NEXORA POS" },
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                ref.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun pushQueueItem(item: SyncQueueEntity): Result<Unit> {
        val collectionName = when (item.entityType) {
            "outlet" -> "outlets"
            "category" -> "categories"
            "product" -> "products"
            "customer" -> "customers"
            "supplier" -> "suppliers"
            "sale" -> "sales"
            "stock_movement" -> "stock_movements"
            "purchase" -> "purchases"
            "return" -> "returns"
            "table" -> "tables"
            "restaurant_order" -> "restaurant_orders"
            "digital_order" -> "digital_orders"
            "staff_member" -> "staff_members"
            "activity_log" -> "activity_logs"
            "business_settings" -> "business_settings"
            else -> return Result.failure(IllegalArgumentException("Unknown entityType: ${item.entityType}"))
        }

        var docPath = "users/unknown/$collectionName/${item.entityId}"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
                .collection(collectionName).document(item.entityId)
            docPath = ref.path

            if (item.operation == "DELETE") {
                ref.delete().await()
                return Result.success(Unit)
            }

            val json = JSONObject(item.payloadJson)
            val snap = ref.get().await()
            if (!snap.exists()) {
                val createMap = buildCreatePayload(uid, item.entityType, json)
                ref.set(createMap).await()
            } else {
                val updateMap = buildUpdatePayload(item.entityType, json)
                if (updateMap != null) {
                    ref.update(updateMap).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private fun buildCreatePayload(uid: String, entityType: String, j: JSONObject): Map<String, Any> {
        val base = mutableMapOf<String, Any?>(
            "userId" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        when (entityType) {
            "outlet" -> {
                base["name"] = j.optString("name", "Cabang")
                base["address"] = j.optString("address", "")
                base["phone"] = j.optString("phone", "")
                base["active"] = j.optBoolean("active", true)
                base["mainOutlet"] = j.optBoolean("mainOutlet", false)
                base["demoData"] = j.optBoolean("demoData", false)
            }
            "category" -> {
                base["name"] = j.optString("name", "Umum")
                base["colorHex"] = j.optString("colorHex", "#2563EB")
                base["iconName"] = j.optString("iconName", "Category")
                base["businessType"] = j.optString("businessType", "Retail")
                base["demoData"] = j.optBoolean("demoData", false)
            }
            "product" -> {
                base["outletId"] = j.optString("outletId", "outlet_main")
                base["categoryId"] = j.optString("categoryId", "cat_general")
                base["name"] = j.optString("name", "Produk")
                base["sku"] = j.optString("sku", "")
                base["barcode"] = j.optString("barcode", "")
                base["buyPrice"] = j.optDouble("buyPrice", 0.0)
                base["sellPrice"] = j.optDouble("sellPrice", 0.0)
                base["stock"] = j.optInt("stock", 0)
                base["minStock"] = j.optInt("minStock", 5)
                base["unit"] = j.optString("unit", "pcs")
                val desc = j.optString("description", "")
                if (desc.isNotBlank()) base["description"] = desc
                base["productType"] = j.optString("productType", "PHYSICAL")
                val vars = j.optString("variantsSummary", "")
                if (vars.isNotBlank()) base["variantsSummary"] = vars
                base["active"] = j.optBoolean("active", true)
                base["demoData"] = j.optBoolean("demoData", false)
            }
            "customer" -> {
                base["name"] = j.optString("name", "Pelanggan")
                base["phone"] = j.optString("phone", "")
                base["address"] = j.optString("address", "")
                base["points"] = j.optInt("points", 0)
                base["totalSpent"] = j.optDouble("totalSpent", 0.0)
                base["transactionCount"] = j.optInt("transactionCount", 0)
                base["demoData"] = j.optBoolean("demoData", false)
            }
            "supplier" -> {
                base["name"] = j.optString("name", "Supplier")
                base["phone"] = j.optString("phone", "")
                base["address"] = j.optString("address", "")
                val notes = j.optString("notes", "")
                if (notes.isNotBlank()) base["notes"] = notes
                base["demoData"] = j.optBoolean("demoData", false)
            }
            "sale" -> {
                base["outletId"] = j.optString("outletId", "outlet_main")
                base["transactionNumber"] = j.optString("transactionNumber", "NX-000")
                base["cashierName"] = j.optString("cashierName", "Kasir")
                base["cashierRole"] = j.optString("cashierRole", "KASIR")
                val custId = j.optString("customerId", "")
                if (custId.isNotBlank()) base["customerId"] = custId
                val custName = j.optString("customerName", "")
                if (custName.isNotBlank()) base["customerName"] = custName
                base["subtotal"] = j.optDouble("subtotal", 0.0)
                base["itemDiscountTotal"] = j.optDouble("itemDiscountTotal", 0.0)
                base["orderDiscount"] = j.optDouble("orderDiscount", 0.0)
                base["taxAmount"] = j.optDouble("taxAmount", 0.0)
                base["extraFee"] = j.optDouble("extraFee", 0.0)
                base["total"] = j.optDouble("total", 0.0)
                base["paymentMethod"] = j.optString("paymentMethod", "TUNAI")
                base["paidAmount"] = j.optDouble("paidAmount", 0.0)
                base["changeAmount"] = j.optDouble("changeAmount", 0.0)
                base["status"] = j.optString("status", "COMPLETED")
                val notes = j.optString("notes", "")
                if (notes.isNotBlank()) base["notes"] = notes
                val summary = j.optString("itemsSummary", "")
                if (summary.isNotBlank()) base["itemsSummary"] = summary.take(1950)
                base["itemCount"] = j.optInt("itemCount", 1).coerceAtLeast(1)
                base["grossProfit"] = j.optDouble("grossProfit", 0.0)
                base["createdAtEpochMs"] = j.optLong("createdAtEpochMs", System.currentTimeMillis())
                base["demoData"] = j.optBoolean("demoData", false)
            }
            "stock_movement" -> {
                base["outletId"] = j.optString("outletId", "outlet_main")
                base["productId"] = j.optString("productId", "")
                base["productName"] = j.optString("productName", "Produk")
                base["type"] = j.optString("type", "ADJUST")
                base["quantityDelta"] = j.optInt("quantityDelta", 0)
                base["previousStock"] = j.optInt("previousStock", 0)
                base["newStock"] = j.optInt("newStock", 0)
                val note = j.optString("note", "")
                if (note.isNotBlank()) base["note"] = note
                base["actorName"] = j.optString("actorName", "Staf")
                base["demoData"] = j.optBoolean("demoData", false)
            }
            "purchase" -> {
                base["outletId"] = j.optString("outletId", "outlet_main")
                base["supplierId"] = j.optString("supplierId", "")
                base["supplierName"] = j.optString("supplierName", "Supplier")
                base["invoiceNumber"] = j.optString("invoiceNumber", "PO-001")
                base["totalAmount"] = j.optDouble("totalAmount", 0.0)
                base["status"] = j.optString("status", "RECEIVED")
                val notes = j.optString("notes", "")
                if (notes.isNotBlank()) base["notes"] = notes
                base["actorName"] = j.optString("actorName", "Staf")
                base["demoData"] = j.optBoolean("demoData", false)
            }
            "return" -> {
                base["outletId"] = j.optString("outletId", "outlet_main")
                base["saleId"] = j.optString("saleId", "")
                base["transactionNumber"] = j.optString("transactionNumber", "")
                base["reason"] = j.optString("reason", "Retur")
                base["refundAmount"] = j.optDouble("refundAmount", 0.0)
                base["actorName"] = j.optString("actorName", "Staf")
                base["demoData"] = j.optBoolean("demoData", false)
            }
            "table" -> {
                base["outletId"] = j.optString("outletId", "outlet_main")
                base["tableName"] = j.optString("tableName", "Meja")
                base["capacity"] = j.optInt("capacity", 4).coerceIn(1, 100)
                base["status"] = j.optString("status", "AVAILABLE")
                val activeOrder = j.optString("activeOrderNumber", "")
                if (activeOrder.isNotBlank()) base["activeOrderNumber"] = activeOrder
                base["demoData"] = j.optBoolean("demoData", false)
            }
            "restaurant_order" -> {
                base["outletId"] = j.optString("outletId", "outlet_main")
                base["queueNumber"] = j.optString("queueNumber", "A-001")
                base["orderType"] = j.optString("orderType", "DINE_IN")
                val tableId = j.optString("tableId", "")
                if (tableId.isNotBlank()) base["tableId"] = tableId
                val tableName = j.optString("tableName", "")
                if (tableName.isNotBlank()) base["tableName"] = tableName
                base["itemsSummary"] = j.optString("itemsSummary", "Pesanan")
                val notes = j.optString("notes", "")
                if (notes.isNotBlank()) base["notes"] = notes
                base["status"] = j.optString("status", "QUEUED")
                base["totalAmount"] = j.optDouble("totalAmount", 0.0)
                base["demoData"] = j.optBoolean("demoData", false)
            }
            "digital_order" -> {
                base["outletId"] = j.optString("outletId", "outlet_main")
                base["productId"] = j.optString("productId", "")
                base["productName"] = j.optString("productName", "Produk Digital").ifBlank { "Produk Digital" }
                base["categoryName"] = j.optString("categoryName", "Produk Digital").ifBlank { "Produk Digital" }
                base["variantLabel"] = j.optString("variantLabel", "Reguler").ifBlank { "Reguler" }
                base["recipientInfo"] = j.optString("recipientInfo", "-").ifBlank { "-" }
                val notes = j.optString("notes", "")
                if (notes.isNotBlank()) base["notes"] = notes
                base["price"] = j.optDouble("price", 0.0)
                base["deliveryStatus"] = j.optString("deliveryStatus", "PENDING")
                if (j.has("deliveredAtEpochMs") && !j.isNull("deliveredAtEpochMs")) {
                    base["deliveredAtEpochMs"] = j.optLong("deliveredAtEpochMs", 0L)
                }
                base["demoData"] = j.optBoolean("demoData", false)
            }
            "staff_member" -> {
                base["name"] = j.optString("name", "Staf")
                base["role"] = j.optString("role", "KASIR")
                base["outletId"] = j.optString("outletId", "outlet_main")
                base["pinCode"] = j.optString("pinCode", "1234")
                base["active"] = j.optBoolean("active", true)
                base["demoData"] = j.optBoolean("demoData", false)
            }
            "activity_log" -> {
                base["actorName"] = j.optString("actorName", "Sistem")
                base["actorRole"] = j.optString("actorRole", "OWNER")
                base["action"] = j.optString("action", "LOG")
                base["details"] = j.optString("details", "")
                base["createdAtEpochMs"] = j.optLong("createdAtEpochMs", System.currentTimeMillis())
            }
            "business_settings" -> {
                base["businessName"] = j.optString("businessName", "NEXORA POS")
                base["businessType"] = j.optString("businessType", "Minimarket & Retail")
                base["address"] = j.optString("address", "")
                base["phone"] = j.optString("phone", "")
                base["taxPercent"] = j.optDouble("taxPercent", 11.0)
                base["currencyCode"] = j.optString("currencyCode", "IDR")
                base["receiptFooter"] = j.optString("receiptFooter", "Terima kasih")
                base["receiptWidthMm"] = if (j.optInt("receiptWidthMm", 58) == 80) 80 else 58
                base["taxEnabled"] = j.optBoolean("taxEnabled", true)
                base["loyaltyEnabled"] = j.optBoolean("loyaltyEnabled", true)
                base["scannerSoundEnabled"] = j.optBoolean("scannerSoundEnabled", true)
            }
        }
        @Suppress("UNCHECKED_CAST")
        return base.filterValues { it != null } as Map<String, Any>
    }

    private fun buildUpdatePayload(entityType: String, j: JSONObject): Map<String, Any>? {
        val map = mutableMapOf<String, Any?>("updatedAt" to FieldValue.serverTimestamp())
        when (entityType) {
            "outlet" -> {
                map["name"] = j.optString("name", "Cabang")
                map["address"] = j.optString("address", "")
                map["phone"] = j.optString("phone", "")
                map["active"] = j.optBoolean("active", true)
                map["mainOutlet"] = j.optBoolean("mainOutlet", false)
            }
            "category" -> {
                map["name"] = j.optString("name", "Umum")
                map["colorHex"] = j.optString("colorHex", "#2563EB")
                map["iconName"] = j.optString("iconName", "Category")
                map["businessType"] = j.optString("businessType", "Retail")
            }
            "product" -> {
                map["outletId"] = j.optString("outletId", "outlet_main")
                map["categoryId"] = j.optString("categoryId", "cat_general")
                map["name"] = j.optString("name", "Produk")
                map["sku"] = j.optString("sku", "")
                map["barcode"] = j.optString("barcode", "")
                map["buyPrice"] = j.optDouble("buyPrice", 0.0)
                map["sellPrice"] = j.optDouble("sellPrice", 0.0)
                map["stock"] = j.optInt("stock", 0)
                map["minStock"] = j.optInt("minStock", 5)
                map["unit"] = j.optString("unit", "pcs")
                val desc = j.optString("description", "")
                if (desc.isNotBlank()) map["description"] = desc
                map["productType"] = j.optString("productType", "PHYSICAL")
                val vars = j.optString("variantsSummary", "")
                if (vars.isNotBlank()) map["variantsSummary"] = vars
                map["active"] = j.optBoolean("active", true)
            }
            "customer" -> {
                map["name"] = j.optString("name", "Pelanggan")
                map["phone"] = j.optString("phone", "")
                map["address"] = j.optString("address", "")
                map["points"] = j.optInt("points", 0)
                map["totalSpent"] = j.optDouble("totalSpent", 0.0)
                map["transactionCount"] = j.optInt("transactionCount", 0)
            }
            "supplier" -> {
                map["name"] = j.optString("name", "Supplier")
                map["phone"] = j.optString("phone", "")
                map["address"] = j.optString("address", "")
                val notes = j.optString("notes", "")
                if (notes.isNotBlank()) map["notes"] = notes
            }
            "sale" -> {
                map["status"] = j.optString("status", "COMPLETED")
                val notes = j.optString("notes", "")
                if (notes.isNotBlank()) map["notes"] = notes
            }
            "purchase" -> {
                map["status"] = j.optString("status", "RECEIVED")
                val notes = j.optString("notes", "")
                if (notes.isNotBlank()) map["notes"] = notes
            }
            "table" -> {
                map["tableName"] = j.optString("tableName", "Meja")
                map["capacity"] = j.optInt("capacity", 4).coerceIn(1, 100)
                map["status"] = j.optString("status", "AVAILABLE")
                val activeOrder = j.optString("activeOrderNumber", "")
                if (activeOrder.isNotBlank()) map["activeOrderNumber"] = activeOrder
            }
            "restaurant_order" -> {
                map["status"] = j.optString("status", "QUEUED")
                val notes = j.optString("notes", "")
                if (notes.isNotBlank()) map["notes"] = notes
            }
            "digital_order" -> {
                map["deliveryStatus"] = j.optString("deliveryStatus", "PENDING")
                val notes = j.optString("notes", "")
                if (notes.isNotBlank()) map["notes"] = notes
                if (j.has("deliveredAtEpochMs") && !j.isNull("deliveredAtEpochMs")) {
                    map["deliveredAtEpochMs"] = j.optLong("deliveredAtEpochMs", 0L)
                }
            }
            "staff_member" -> {
                map["name"] = j.optString("name", "Staf")
                map["role"] = j.optString("role", "KASIR")
                map["outletId"] = j.optString("outletId", "outlet_main")
                map["pinCode"] = j.optString("pinCode", "1234")
                map["active"] = j.optBoolean("active", true)
            }
            "business_settings" -> {
                map["businessName"] = j.optString("businessName", "NEXORA POS")
                map["businessType"] = j.optString("businessType", "Minimarket & Retail")
                map["address"] = j.optString("address", "")
                map["phone"] = j.optString("phone", "")
                map["taxPercent"] = j.optDouble("taxPercent", 11.0)
                map["currencyCode"] = j.optString("currencyCode", "IDR")
                map["receiptFooter"] = j.optString("receiptFooter", "Terima kasih")
                map["receiptWidthMm"] = if (j.optInt("receiptWidthMm", 58) == 80) 80 else 58
                map["taxEnabled"] = j.optBoolean("taxEnabled", true)
                map["loyaltyEnabled"] = j.optBoolean("loyaltyEnabled", true)
                map["scannerSoundEnabled"] = j.optBoolean("scannerSoundEnabled", true)
            }
            "stock_movement", "return", "activity_log" -> return null // Immutable audit records
            else -> return null
        }
        @Suppress("UNCHECKED_CAST")
        return map.filterValues { it != null } as Map<String, Any>
    }

    suspend fun fetchCloudSummaryCount(): Result<Int> {
        var path = "users/unknown/products"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("products")
            path = ref.path
            val snap = ref.get().await()
            Result.success(snap.size())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun hydrateCloudDataToRoom(dao: NexoraDao): Result<Int> {
        var currentPath = "users/unknown"
        return try {
            val uid = requireUserId()
            val userDoc = db.collection("users").document(uid)
            var restoredCount = 0

            suspend fun fetchDocs(sub: String): List<DocumentSnapshot> {
                currentPath = "users/$uid/$sub"
                return userDoc.collection(sub).get().await().documents
            }

            val outlets = fetchDocs("outlets").map { d ->
                OutletEntity(
                    id = d.id,
                    name = d.getString("name") ?: "Cabang",
                    address = d.getString("address") ?: "",
                    phone = d.getString("phone") ?: "",
                    active = d.getBoolean("active") ?: true,
                    mainOutlet = d.getBoolean("mainOutlet") ?: false,
                    demoData = d.getBoolean("demoData") ?: false
                )
            }
            if (outlets.isNotEmpty()) { dao.upsertOutlets(outlets); restoredCount += outlets.size }

            val categories = fetchDocs("categories").map { d ->
                CategoryEntity(
                    id = d.id,
                    name = d.getString("name") ?: "Umum",
                    colorHex = d.getString("colorHex") ?: "#2563EB",
                    iconName = d.getString("iconName") ?: "Category",
                    businessType = d.getString("businessType") ?: "Retail",
                    demoData = d.getBoolean("demoData") ?: false
                )
            }
            if (categories.isNotEmpty()) { dao.upsertCategories(categories); restoredCount += categories.size }

            val products = fetchDocs("products").map { d ->
                ProductEntity(
                    id = d.id,
                    outletId = d.getString("outletId") ?: "outlet_main",
                    categoryId = d.getString("categoryId") ?: "cat_general",
                    name = d.getString("name") ?: "Produk",
                    sku = d.getString("sku") ?: "",
                    barcode = d.getString("barcode") ?: "",
                    buyPrice = d.getDouble("buyPrice") ?: 0.0,
                    sellPrice = d.getDouble("sellPrice") ?: 0.0,
                    stock = (d.getLong("stock") ?: 0L).toInt(),
                    minStock = (d.getLong("minStock") ?: 5L).toInt(),
                    unit = d.getString("unit") ?: "pcs",
                    description = d.getString("description") ?: "",
                    productType = d.getString("productType") ?: "PHYSICAL",
                    variantsSummary = d.getString("variantsSummary") ?: "",
                    active = d.getBoolean("active") ?: true,
                    demoData = d.getBoolean("demoData") ?: false
                )
            }
            if (products.isNotEmpty()) { dao.upsertProducts(products); restoredCount += products.size }

            val customers = fetchDocs("customers").map { d ->
                CustomerEntity(
                    id = d.id,
                    name = d.getString("name") ?: "Pelanggan",
                    phone = d.getString("phone") ?: "",
                    address = d.getString("address") ?: "",
                    points = (d.getLong("points") ?: 0L).toInt(),
                    totalSpent = d.getDouble("totalSpent") ?: 0.0,
                    transactionCount = (d.getLong("transactionCount") ?: 0L).toInt(),
                    demoData = d.getBoolean("demoData") ?: false
                )
            }
            if (customers.isNotEmpty()) { dao.upsertCustomers(customers); restoredCount += customers.size }

            val sales = fetchDocs("sales").map { d ->
                SaleEntity(
                    id = d.id,
                    outletId = d.getString("outletId") ?: "outlet_main",
                    transactionNumber = d.getString("transactionNumber") ?: d.id,
                    cashierName = d.getString("cashierName") ?: "Kasir",
                    cashierRole = d.getString("cashierRole") ?: "KASIR",
                    customerId = d.getString("customerId"),
                    customerName = d.getString("customerName"),
                    subtotal = d.getDouble("subtotal") ?: 0.0,
                    itemDiscountTotal = d.getDouble("itemDiscountTotal") ?: 0.0,
                    orderDiscount = d.getDouble("orderDiscount") ?: 0.0,
                    taxAmount = d.getDouble("taxAmount") ?: 0.0,
                    extraFee = d.getDouble("extraFee") ?: 0.0,
                    total = d.getDouble("total") ?: 0.0,
                    paymentMethod = d.getString("paymentMethod") ?: "TUNAI",
                    paidAmount = d.getDouble("paidAmount") ?: 0.0,
                    changeAmount = d.getDouble("changeAmount") ?: 0.0,
                    status = d.getString("status") ?: "COMPLETED",
                    notes = d.getString("notes") ?: "",
                    itemsSummary = d.getString("itemsSummary") ?: "",
                    itemCount = (d.getLong("itemCount") ?: 1L).toInt(),
                    grossProfit = d.getDouble("grossProfit") ?: 0.0,
                    createdAtEpochMs = d.getLong("createdAtEpochMs") ?: System.currentTimeMillis(),
                    demoData = d.getBoolean("demoData") ?: false
                )
            }
            if (sales.isNotEmpty()) { dao.upsertSales(sales); restoredCount += sales.size }

            val suppliers = fetchDocs("suppliers").map { d ->
                SupplierEntity(
                    id = d.id,
                    name = d.getString("name") ?: "Supplier",
                    phone = d.getString("phone") ?: "",
                    address = d.getString("address") ?: "",
                    notes = d.getString("notes") ?: "",
                    demoData = d.getBoolean("demoData") ?: false
                )
            }
            if (suppliers.isNotEmpty()) { dao.upsertSuppliers(suppliers); restoredCount += suppliers.size }

            val movements = fetchDocs("stock_movements").map { d ->
                StockMovementEntity(
                    id = d.id,
                    outletId = d.getString("outletId") ?: "outlet_main",
                    productId = d.getString("productId") ?: "",
                    productName = d.getString("productName") ?: "Produk",
                    type = d.getString("type") ?: "ADJUST",
                    quantityDelta = (d.getLong("quantityDelta") ?: 0L).toInt(),
                    previousStock = (d.getLong("previousStock") ?: 0L).toInt(),
                    newStock = (d.getLong("newStock") ?: 0L).toInt(),
                    note = d.getString("note") ?: "",
                    actorName = d.getString("actorName") ?: "Staf",
                    createdAtEpochMs = d.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                    demoData = d.getBoolean("demoData") ?: false
                )
            }
            if (movements.isNotEmpty()) { dao.upsertStockMovements(movements); restoredCount += movements.size }

            val purchases = fetchDocs("purchases").map { d ->
                PurchaseEntity(
                    id = d.id,
                    outletId = d.getString("outletId") ?: "outlet_main",
                    supplierId = d.getString("supplierId") ?: "",
                    supplierName = d.getString("supplierName") ?: "Supplier",
                    invoiceNumber = d.getString("invoiceNumber") ?: d.id,
                    totalAmount = d.getDouble("totalAmount") ?: 0.0,
                    status = d.getString("status") ?: "RECEIVED",
                    notes = d.getString("notes") ?: "",
                    actorName = d.getString("actorName") ?: "Staf",
                    createdAtEpochMs = d.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                    demoData = d.getBoolean("demoData") ?: false
                )
            }
            if (purchases.isNotEmpty()) { dao.upsertPurchases(purchases); restoredCount += purchases.size }

            val returns = fetchDocs("returns").map { d ->
                ReturnEntity(
                    id = d.id,
                    outletId = d.getString("outletId") ?: "outlet_main",
                    saleId = d.getString("saleId") ?: "",
                    transactionNumber = d.getString("transactionNumber") ?: "",
                    reason = d.getString("reason") ?: "",
                    refundAmount = d.getDouble("refundAmount") ?: 0.0,
                    actorName = d.getString("actorName") ?: "Staf",
                    createdAtEpochMs = d.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                    demoData = d.getBoolean("demoData") ?: false
                )
            }
            if (returns.isNotEmpty()) { dao.upsertReturns(returns); restoredCount += returns.size }

            val tables = fetchDocs("tables").map { d ->
                RestaurantTableEntity(
                    id = d.id,
                    outletId = d.getString("outletId") ?: "outlet_main",
                    tableName = d.getString("tableName") ?: "Meja",
                    capacity = (d.getLong("capacity") ?: 4L).toInt(),
                    status = d.getString("status") ?: "AVAILABLE",
                    activeOrderNumber = d.getString("activeOrderNumber"),
                    demoData = d.getBoolean("demoData") ?: false
                )
            }
            if (tables.isNotEmpty()) { dao.upsertRestaurantTables(tables); restoredCount += tables.size }

            val restOrders = fetchDocs("restaurant_orders").map { d ->
                RestaurantOrderEntity(
                    id = d.id,
                    outletId = d.getString("outletId") ?: "outlet_main",
                    queueNumber = d.getString("queueNumber") ?: "Q-001",
                    orderType = d.getString("orderType") ?: "DINE_IN",
                    tableId = d.getString("tableId"),
                    tableName = d.getString("tableName"),
                    itemsSummary = d.getString("itemsSummary") ?: "",
                    notes = d.getString("notes") ?: "",
                    status = d.getString("status") ?: "QUEUED",
                    totalAmount = d.getDouble("totalAmount") ?: 0.0,
                    createdAtEpochMs = d.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                    demoData = d.getBoolean("demoData") ?: false
                )
            }
            if (restOrders.isNotEmpty()) { dao.upsertRestaurantOrders(restOrders); restoredCount += restOrders.size }

            val digitalOrders = fetchDocs("digital_orders").map { d ->
                DigitalOrderEntity(
                    id = d.id,
                    outletId = d.getString("outletId") ?: "outlet_main",
                    productId = d.getString("productId") ?: "",
                    productName = d.getString("productName") ?: "Produk Digital",
                    categoryName = d.getString("categoryName") ?: "Produk Digital",
                    variantLabel = d.getString("variantLabel") ?: "Reguler",
                    recipientInfo = d.getString("recipientInfo") ?: "",
                    notes = d.getString("notes") ?: "",
                    price = d.getDouble("price") ?: 0.0,
                    deliveryStatus = d.getString("deliveryStatus") ?: "PENDING",
                    deliveredAtEpochMs = d.getLong("deliveredAtEpochMs"),
                    createdAtEpochMs = d.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                    demoData = d.getBoolean("demoData") ?: false
                )
            }
            if (digitalOrders.isNotEmpty()) { dao.upsertDigitalOrders(digitalOrders); restoredCount += digitalOrders.size }

            val staff = fetchDocs("staff_members").map { d ->
                StaffMemberEntity(
                    id = d.id,
                    name = d.getString("name") ?: "Staf",
                    role = d.getString("role") ?: "KASIR",
                    outletId = d.getString("outletId") ?: "outlet_main",
                    pinCode = d.getString("pinCode") ?: "0000",
                    active = d.getBoolean("active") ?: true,
                    demoData = d.getBoolean("demoData") ?: false
                )
            }
            if (staff.isNotEmpty()) { dao.upsertStaffMembers(staff); restoredCount += staff.size }

            val settingsDocs = fetchDocs("business_settings")
            settingsDocs.firstOrNull()?.let { d ->
                dao.upsertSettings(
                    BusinessSettingsEntity(
                        id = "default",
                        businessName = d.getString("businessName") ?: "NEXORA POS",
                        businessType = d.getString("businessType") ?: "Minimarket & Retail",
                        address = d.getString("address") ?: "",
                        phone = d.getString("phone") ?: "",
                        taxPercent = d.getDouble("taxPercent") ?: 11.0,
                        currencyCode = d.getString("currencyCode") ?: "IDR",
                        receiptFooter = d.getString("receiptFooter") ?: "Terima kasih!",
                        receiptWidthMm = (d.getLong("receiptWidthMm") ?: 58L).toInt(),
                        taxEnabled = d.getBoolean("taxEnabled") ?: true,
                        loyaltyEnabled = d.getBoolean("loyaltyEnabled") ?: true,
                        scannerSoundEnabled = d.getBoolean("scannerSoundEnabled") ?: true
                    )
                )
                restoredCount++
            }

            Result.success(restoredCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, currentPath)
            Result.failure(e)
        }
    }
}
