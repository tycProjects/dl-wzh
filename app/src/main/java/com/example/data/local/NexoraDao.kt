package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface NexoraDao {
    // Outlets
    @Query("SELECT * FROM outlets ORDER BY mainOutlet DESC, name ASC")
    fun observeOutlets(): Flow<List<OutletEntity>>

    @Query("SELECT * FROM outlets")
    suspend fun getAllOutlets(): List<OutletEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOutlet(outlet: OutletEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOutlets(outlets: List<OutletEntity>)

    // Categories
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)

    // Products
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND active = 1 LIMIT 1")
    suspend fun findProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProducts(products: List<ProductEntity>)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)

    @Query("UPDATE products SET stock = :newStock, updatedAtEpochMs = :updatedAt WHERE id = :productId")
    suspend fun updateProductStock(productId: String, newStock: Int, updatedAt: Long)

    // Customers
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun observeCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomers(): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomers(customers: List<CustomerEntity>)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: String)

    // Suppliers
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun observeSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers")
    suspend fun getAllSuppliers(): List<SupplierEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSupplier(supplier: SupplierEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSuppliers(suppliers: List<SupplierEntity>)

    @Query("DELETE FROM suppliers WHERE id = :id")
    suspend fun deleteSupplierById(id: String)

    // Sales & Sale Items
    @Query("SELECT * FROM sales ORDER BY createdAtEpochMs DESC")
    fun observeSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :saleId LIMIT 1")
    suspend fun getSaleById(saleId: String): SaleEntity?

    @Query("SELECT * FROM sales")
    suspend fun getAllSales(): List<SaleEntity>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getSaleItems(saleId: String): List<SaleItemEntity>

    @Query("SELECT * FROM sale_items")
    fun observeAllSaleItems(): Flow<List<SaleItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSales(sales: List<SaleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSaleItems(items: List<SaleItemEntity>)

    // Stock Movements
    @Query("SELECT * FROM stock_movements ORDER BY createdAtEpochMs DESC")
    fun observeStockMovements(): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStockMovement(movement: StockMovementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStockMovements(movements: List<StockMovementEntity>)

    // Purchases
    @Query("SELECT * FROM purchases ORDER BY createdAtEpochMs DESC")
    fun observePurchases(): Flow<List<PurchaseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPurchase(purchase: PurchaseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPurchases(purchases: List<PurchaseEntity>)

    // Returns
    @Query("SELECT * FROM returns ORDER BY createdAtEpochMs DESC")
    fun observeReturns(): Flow<List<ReturnEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReturn(ret: ReturnEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReturns(rets: List<ReturnEntity>)

    // Restaurant Tables & Orders
    @Query("SELECT * FROM restaurant_tables ORDER BY tableName ASC")
    fun observeRestaurantTables(): Flow<List<RestaurantTableEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRestaurantTable(table: RestaurantTableEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRestaurantTables(tables: List<RestaurantTableEntity>)

    @Query("SELECT * FROM restaurant_orders ORDER BY createdAtEpochMs DESC")
    fun observeRestaurantOrders(): Flow<List<RestaurantOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRestaurantOrder(order: RestaurantOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRestaurantOrders(orders: List<RestaurantOrderEntity>)

    // Digital Orders
    @Query("SELECT * FROM digital_orders ORDER BY createdAtEpochMs DESC")
    fun observeDigitalOrders(): Flow<List<DigitalOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDigitalOrder(order: DigitalOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDigitalOrders(orders: List<DigitalOrderEntity>)

    // Staff Members
    @Query("SELECT * FROM staff_members ORDER BY name ASC")
    fun observeStaffMembers(): Flow<List<StaffMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStaffMember(member: StaffMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStaffMembers(members: List<StaffMemberEntity>)

    @Query("DELETE FROM staff_members WHERE id = :id")
    suspend fun deleteStaffMemberById(id: String)

    // Activity Logs
    @Query("SELECT * FROM activity_logs ORDER BY createdAtEpochMs DESC LIMIT 200")
    fun observeActivityLogs(): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLogEntity)

    // Business Settings
    @Query("SELECT * FROM business_settings WHERE id = 'default' LIMIT 1")
    fun observeSettings(): Flow<BusinessSettingsEntity?>

    @Query("SELECT * FROM business_settings WHERE id = 'default' LIMIT 1")
    suspend fun getSettings(): BusinessSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: BusinessSettingsEntity)

    // Sync Queue
    @Query("SELECT * FROM sync_queue ORDER BY createdAtEpochMs ASC")
    fun observeSyncQueue(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue WHERE status != 'SYNCED' ORDER BY createdAtEpochMs ASC")
    suspend fun getPendingSyncItems(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueSyncItem(item: SyncQueueEntity)

    @Query("UPDATE sync_queue SET status = :status, retryCount = :retryCount, lastError = :lastError WHERE id = :id")
    suspend fun updateSyncItemStatus(id: String, status: String, retryCount: Int, lastError: String?)

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun pruneSyncedItems()

    // Demo Data Cleanup
    @Transaction
    suspend fun clearDemoData() {
        deleteDemoOutlets()
        deleteDemoCategories()
        deleteDemoProducts()
        deleteDemoCustomers()
        deleteDemoSuppliers()
        deleteDemoSales()
        deleteDemoSaleItems()
        deleteDemoMovements()
        deleteDemoPurchases()
        deleteDemoReturns()
        deleteDemoTables()
        deleteDemoRestaurantOrders()
        deleteDemoDigitalOrders()
        deleteDemoStaff()
    }

    @Query("DELETE FROM outlets WHERE demoData = 1")
    suspend fun deleteDemoOutlets()

    @Query("DELETE FROM categories WHERE demoData = 1")
    suspend fun deleteDemoCategories()

    @Query("DELETE FROM products WHERE demoData = 1")
    suspend fun deleteDemoProducts()

    @Query("DELETE FROM customers WHERE demoData = 1")
    suspend fun deleteDemoCustomers()

    @Query("DELETE FROM suppliers WHERE demoData = 1")
    suspend fun deleteDemoSuppliers()

    @Query("DELETE FROM sales WHERE demoData = 1")
    suspend fun deleteDemoSales()

    @Query("DELETE FROM sale_items WHERE demoData = 1")
    suspend fun deleteDemoSaleItems()

    @Query("DELETE FROM stock_movements WHERE demoData = 1")
    suspend fun deleteDemoMovements()

    @Query("DELETE FROM purchases WHERE demoData = 1")
    suspend fun deleteDemoPurchases()

    @Query("DELETE FROM returns WHERE demoData = 1")
    suspend fun deleteDemoReturns()

    @Query("DELETE FROM restaurant_tables WHERE demoData = 1")
    suspend fun deleteDemoTables()

    @Query("DELETE FROM restaurant_orders WHERE demoData = 1")
    suspend fun deleteDemoRestaurantOrders()

    @Query("DELETE FROM digital_orders WHERE demoData = 1")
    suspend fun deleteDemoDigitalOrders()

    @Query("DELETE FROM staff_members WHERE demoData = 1")
    suspend fun deleteDemoStaff()

    @Transaction
    suspend fun clearAllLocalBusinessData() {
        deleteAllOutlets()
        deleteAllCategories()
        deleteAllProducts()
        deleteAllCustomers()
        deleteAllSuppliers()
        deleteAllSales()
        deleteAllSaleItems()
        deleteAllMovements()
        deleteAllPurchases()
        deleteAllReturns()
        deleteAllTables()
        deleteAllRestaurantOrders()
        deleteAllDigitalOrders()
        deleteAllStaff()
        deleteAllLogs()
        deleteAllSyncQueue()
    }

    @Query("DELETE FROM outlets") suspend fun deleteAllOutlets()
    @Query("DELETE FROM categories") suspend fun deleteAllCategories()
    @Query("DELETE FROM products") suspend fun deleteAllProducts()
    @Query("DELETE FROM customers") suspend fun deleteAllCustomers()
    @Query("DELETE FROM suppliers") suspend fun deleteAllSuppliers()
    @Query("DELETE FROM sales") suspend fun deleteAllSales()
    @Query("DELETE FROM sale_items") suspend fun deleteAllSaleItems()
    @Query("DELETE FROM stock_movements") suspend fun deleteAllMovements()
    @Query("DELETE FROM purchases") suspend fun deleteAllPurchases()
    @Query("DELETE FROM returns") suspend fun deleteAllReturns()
    @Query("DELETE FROM restaurant_tables") suspend fun deleteAllTables()
    @Query("DELETE FROM restaurant_orders") suspend fun deleteAllRestaurantOrders()
    @Query("DELETE FROM digital_orders") suspend fun deleteAllDigitalOrders()
    @Query("DELETE FROM staff_members") suspend fun deleteAllStaff()
    @Query("DELETE FROM activity_logs") suspend fun deleteAllLogs()
    @Query("DELETE FROM sync_queue") suspend fun deleteAllSyncQueue()
}
