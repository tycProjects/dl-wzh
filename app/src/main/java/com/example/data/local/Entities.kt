package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "outlets")
data class OutletEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val phone: String,
    val active: Boolean = true,
    val mainOutlet: Boolean = false,
    val demoData: Boolean = false,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val businessType: String,
    val demoData: Boolean = false,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["barcode"]),
        Index(value = ["sku"]),
        Index(value = ["outletId"]),
        Index(value = ["categoryId"])
    ]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val outletId: String,
    val categoryId: String,
    val name: String,
    val sku: String,
    val barcode: String,
    val photoUri: String? = null,
    val buyPrice: Double,
    val sellPrice: Double,
    val stock: Int,
    val minStock: Int,
    val unit: String,
    val description: String = "",
    val productType: String = "PHYSICAL", // PHYSICAL, FOOD, DIGITAL
    val variantsSummary: String = "", // Comma-separated variants e.g. "Merah - M, Biru - L"
    val active: Boolean = true,
    val demoData: Boolean = false,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val address: String,
    val points: Int = 0,
    val totalSpent: Double = 0.0,
    val transactionCount: Int = 0,
    val demoData: Boolean = false,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val address: String,
    val notes: String = "",
    val demoData: Boolean = false,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["transactionNumber"], unique = true),
        Index(value = ["outletId"]),
        Index(value = ["createdAtEpochMs"])
    ]
)
data class SaleEntity(
    @PrimaryKey val id: String,
    val outletId: String,
    val transactionNumber: String,
    val cashierName: String,
    val cashierRole: String,
    val customerId: String? = null,
    val customerName: String? = null,
    val subtotal: Double,
    val itemDiscountTotal: Double = 0.0,
    val orderDiscount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val extraFee: Double = 0.0,
    val total: Double,
    val paymentMethod: String, // TUNAI, TRANSFER, QRIS_MANUAL, LAINNYA
    val paidAmount: Double,
    val changeAmount: Double,
    val status: String = "COMPLETED", // COMPLETED, VOIDED, RETURNED
    val notes: String = "",
    val itemsSummary: String = "",
    val itemCount: Int = 1,
    val grossProfit: Double = 0.0,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val demoData: Boolean = false
)

@Entity(
    tableName = "sale_items",
    indices = [Index(value = ["saleId"])]
)
data class SaleItemEntity(
    @PrimaryKey val id: String,
    val saleId: String,
    val productId: String,
    val productName: String,
    val variantLabel: String = "",
    val quantity: Int,
    val buyPrice: Double,
    val unitPrice: Double,
    val discountAmount: Double = 0.0,
    val lineTotal: Double,
    val demoData: Boolean = false
)

@Entity(
    tableName = "stock_movements",
    indices = [Index(value = ["productId"]), Index(value = ["outletId"])]
)
data class StockMovementEntity(
    @PrimaryKey val id: String,
    val outletId: String,
    val productId: String,
    val productName: String,
    val type: String, // IN, OUT, ADJUST, OPNAME, SALE, RETURN, TRANSFER
    val quantityDelta: Int,
    val previousStock: Int,
    val newStock: Int,
    val note: String = "",
    val actorName: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val demoData: Boolean = false
)

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey val id: String,
    val outletId: String,
    val supplierId: String,
    val supplierName: String,
    val invoiceNumber: String,
    val totalAmount: Double,
    val status: String = "RECEIVED", // RECEIVED, RETURNED
    val notes: String = "",
    val actorName: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val demoData: Boolean = false
)

@Entity(tableName = "returns")
data class ReturnEntity(
    @PrimaryKey val id: String,
    val outletId: String,
    val saleId: String,
    val transactionNumber: String,
    val reason: String,
    val refundAmount: Double,
    val actorName: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val demoData: Boolean = false
)

@Entity(tableName = "restaurant_tables")
data class RestaurantTableEntity(
    @PrimaryKey val id: String,
    val outletId: String,
    val tableName: String,
    val capacity: Int,
    val status: String = "AVAILABLE", // AVAILABLE, OCCUPIED, RESERVED
    val activeOrderNumber: String? = null,
    val demoData: Boolean = false
)

@Entity(tableName = "restaurant_orders")
data class RestaurantOrderEntity(
    @PrimaryKey val id: String,
    val outletId: String,
    val queueNumber: String,
    val orderType: String, // DINE_IN, TAKEAWAY
    val tableId: String? = null,
    val tableName: String? = null,
    val itemsSummary: String,
    val notes: String = "",
    val status: String = "QUEUED", // QUEUED, PREPARING, READY, SERVED
    val totalAmount: Double,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val demoData: Boolean = false
)

@Entity(tableName = "digital_orders")
data class DigitalOrderEntity(
    @PrimaryKey val id: String,
    val outletId: String,
    val productId: String,
    val productName: String,
    val categoryName: String,
    val variantLabel: String,
    val recipientInfo: String,
    val notes: String = "",
    val price: Double,
    val deliveryStatus: String = "PENDING", // PENDING, DELIVERED, FAILED
    val deliveredAtEpochMs: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val demoData: Boolean = false
)

@Entity(tableName = "staff_members")
data class StaffMemberEntity(
    @PrimaryKey val id: String,
    val name: String,
    val role: String, // OWNER, ADMIN, MANAJER, KASIR
    val outletId: String,
    val pinCode: String,
    val active: Boolean = true,
    val demoData: Boolean = false
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey val id: String,
    val actorName: String,
    val actorRole: String,
    val action: String,
    val details: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "business_settings")
data class BusinessSettingsEntity(
    @PrimaryKey val id: String = "default",
    val businessName: String = "NEXORA POS Store",
    val businessType: String = "Minimarket & Retail",
    val address: String = "Jl. Bisnis Modern No. 1, Jakarta",
    val phone: String = "0812-0000-0000",
    val taxPercent: Double = 11.0,
    val currencyCode: String = "IDR",
    val receiptFooter: String = "Terima kasih telah berbelanja!",
    val receiptWidthMm: Int = 58,
    val taxEnabled: Boolean = true,
    val loyaltyEnabled: Boolean = true,
    val scannerSoundEnabled: Boolean = true,
    val enabledPaymentMethods: String = "TUNAI,TRANSFER,QRIS_MANUAL,LAINNYA",
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val entityType: String, // outlet, category, product, customer, supplier, sale, stock_movement, purchase, return, table, restaurant_order, digital_order, staff_member, activity_log, business_settings
    val entityId: String,
    val operation: String, // UPSERT, DELETE
    val payloadJson: String,
    val status: String = "PENDING", // PENDING, SYNCED, FAILED
    val retryCount: Int = 0,
    val lastError: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
