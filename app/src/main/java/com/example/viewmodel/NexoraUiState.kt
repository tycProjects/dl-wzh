package com.example.viewmodel

import com.example.data.local.*
import com.example.ui.theme.AppThemeMode

enum class StaffRole(val displayName: String) {
    OWNER("Owner"),
    ADMIN("Admin"),
    MANAJER("Manajer"),
    KASIR("Kasir");

    companion object {
        fun fromString(raw: String): StaffRole =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: KASIR
    }
}

data class CartLine(
    val key: String,
    val product: ProductEntity,
    val variantLabel: String,
    val quantity: Int,
    val itemDiscountAmount: Double = 0.0
) {
    val lineSubtotal: Double get() = product.sellPrice * quantity
    val lineTotal: Double get() = (lineSubtotal - itemDiscountAmount).coerceAtLeast(0.0)
}

data class RolePermissions(
    val canManageProducts: Boolean,
    val canManageInventory: Boolean,
    val canVoidOrReturn: Boolean,
    val canViewProfitReports: Boolean,
    val canManageOutlets: Boolean,
    val canManageStaff: Boolean,
    val canManageSettings: Boolean
) {
    companion object {
        fun forRole(role: StaffRole): RolePermissions = when (role) {
            StaffRole.OWNER -> RolePermissions(
                canManageProducts = true,
                canManageInventory = true,
                canVoidOrReturn = true,
                canViewProfitReports = true,
                canManageOutlets = true,
                canManageStaff = true,
                canManageSettings = true
            )
            StaffRole.ADMIN -> RolePermissions(
                canManageProducts = true,
                canManageInventory = true,
                canVoidOrReturn = true,
                canViewProfitReports = true,
                canManageOutlets = false,
                canManageStaff = false,
                canManageSettings = true
            )
            StaffRole.MANAJER -> RolePermissions(
                canManageProducts = false,
                canManageInventory = true,
                canVoidOrReturn = true,
                canViewProfitReports = true,
                canManageOutlets = true,
                canManageStaff = false,
                canManageSettings = false
            )
            StaffRole.KASIR -> RolePermissions(
                canManageProducts = false,
                canManageInventory = false,
                canVoidOrReturn = false,
                canViewProfitReports = false,
                canManageOutlets = false,
                canManageStaff = false,
                canManageSettings = false
            )
        }
    }
}

data class ChartPoint(
    val label: String,
    val revenue: Double,
    val profit: Double
)

enum class DashboardRange(val label: String, val days: Int) {
    TODAY("Hari Ini", 1),
    WEEK("7 Hari", 7),
    MONTH("30 Hari", 30)
}

enum class ChartGranularity(val label: String) {
    DAILY("Harian"),
    WEEKLY("Mingguan"),
    MONTHLY("Bulanan")
}

data class NexoraUiState(
    val isLoading: Boolean = true,
    val currentUserId: String = "",
    val ownerDisplayName: String = "",
    val ownerEmail: String = "",
    val isOnline: Boolean = true,
    val manualOfflineMode: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val outlets: List<OutletEntity> = emptyList(),
    val activeOutlet: OutletEntity? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val suppliers: List<SupplierEntity> = emptyList(),
    val sales: List<SaleEntity> = emptyList(),
    val saleItems: List<SaleItemEntity> = emptyList(),
    val stockMovements: List<StockMovementEntity> = emptyList(),
    val purchases: List<PurchaseEntity> = emptyList(),
    val returns: List<ReturnEntity> = emptyList(),
    val tables: List<RestaurantTableEntity> = emptyList(),
    val restaurantOrders: List<RestaurantOrderEntity> = emptyList(),
    val digitalOrders: List<DigitalOrderEntity> = emptyList(),
    val staffMembers: List<StaffMemberEntity> = emptyList(),
    val activeStaff: StaffMemberEntity? = null,
    val activeRole: StaffRole = StaffRole.OWNER,
    val permissions: RolePermissions = RolePermissions.forRole(StaffRole.OWNER),
    val activityLogs: List<ActivityLogEntity> = emptyList(),
    val settings: BusinessSettingsEntity = BusinessSettingsEntity(),
    val syncQueue: List<SyncQueueEntity> = emptyList(),
    val pendingSyncCount: Int = 0,
    val isSyncing: Boolean = false,
    val hasDemoData: Boolean = false,
    // Cashier Cart State
    val cartLines: List<CartLine> = emptyList(),
    val selectedCustomer: CustomerEntity? = null,
    val orderDiscount: Double = 0.0,
    val extraFee: Double = 0.0,
    val cartNotes: String = "",
    val lastReceiptSale: SaleEntity? = null,
    val lastReceiptItems: List<SaleItemEntity> = emptyList(),
    val lastScannedNotFoundBarcode: String? = null,
    val prefillProductBarcode: String? = null
) {
    val cartSubtotal: Double get() = cartLines.sumOf { it.lineSubtotal }
    val cartItemDiscountTotal: Double get() = cartLines.sumOf { it.itemDiscountAmount }
    val cartTaxAmount: Double
        get() {
            if (!settings.taxEnabled) return 0.0
            val taxable = (cartSubtotal - cartItemDiscountTotal - orderDiscount).coerceAtLeast(0.0)
            return taxable * (settings.taxPercent / 100.0)
        }
    val cartGrandTotal: Double
        get() = ((cartSubtotal - cartItemDiscountTotal - orderDiscount).coerceAtLeast(0.0) + cartTaxAmount + extraFee).coerceAtLeast(0.0)
    val cartItemCount: Int get() = cartLines.sumOf { it.quantity }
}
