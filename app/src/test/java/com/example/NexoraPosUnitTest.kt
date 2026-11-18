package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.remote.FirestoreSyncService
import com.example.data.repository.*
import com.example.viewmodel.RolePermissions
import com.example.viewmodel.StaffRole
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NexoraPosUnitTest {

    private lateinit var db: NexoraDatabase
    private lateinit var dao: NexoraDao
    private lateinit var repository: NexoraRepository
    private lateinit var networkMonitor: NetworkMonitor

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        db = Room.inMemoryDatabaseBuilder(context, NexoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.nexoraDao()
        networkMonitor = NetworkMonitor(context)
        networkMonitor.setManualOfflineMode(true)
        val firestore = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        repository = NexoraRepository(dao, FirestoreSyncService(firestore), networkMonitor)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `checkout calculates tax discounts change deducts stock and queues sync`() = runTest {
        val outlet = OutletEntity("outlet_1", "Cabang Utama", "Jl. Mawar", "0812", active = true, mainOutlet = true)
        dao.upsertOutlet(outlet)
        val product = ProductEntity(
            id = "prod_1",
            outletId = "outlet_1",
            categoryId = "cat_1",
            name = "Kopi Susu",
            sku = "KOP-1",
            barcode = "8991002003001",
            buyPrice = 10000.0,
            sellPrice = 20000.0,
            stock = 10,
            minStock = 2,
            unit = "cup"
        )
        dao.upsertProduct(product)
        val customer = CustomerEntity("cust_1", "Budi", "08123", "Jakarta", points = 0)
        dao.upsertCustomer(customer)

        val result = repository.completeCheckout(
            CheckoutRequest(
                outletId = "outlet_1",
                cashierName = "Siti",
                cashierRole = "KASIR",
                customer = customer,
                lines = listOf(CartLineInput(product, "Large", 2, 5000.0)),
                orderDiscount = 5000.0,
                taxRatePercent = 10.0,
                taxEnabled = true,
                extraFee = 2000.0,
                paymentMethod = "TUNAI",
                paidAmount = 50000.0,
                notes = "Tanpa gula",
                loyaltyEnabled = true
            )
        )

        assertTrue(result.isSuccess)
        val (sale, items) = result.getOrThrow()
        // Subtotal = 2 * 20000 = 40000; itemDiscount = 5000; orderDiscount = 5000 => taxable = 30000
        // Tax 10% = 3000; extraFee = 2000 => Total = 35000; change = 15000
        assertEquals(40000.0, sale.subtotal, 0.01)
        assertEquals(3000.0, sale.taxAmount, 0.01)
        assertEquals(35000.0, sale.total, 0.01)
        assertEquals(15000.0, sale.changeAmount, 0.01)
        assertEquals(1, items.size)

        val updatedProduct = dao.getProductById("prod_1")!!
        assertEquals(8, updatedProduct.stock)

        val updatedCustomer = dao.getCustomerById("cust_1")!!
        assertEquals(1, updatedCustomer.transactionCount)
        assertEquals(35000.0, updatedCustomer.totalSpent, 0.01)
        assertEquals(3, updatedCustomer.points) // 35000 / 10000 = 3

        val pending = dao.getPendingSyncItems()
        assertTrue(pending.any { it.entityType == "sale" && it.entityId == sale.id })
    }

    @Test
    fun `void sale restores product stock and records audit trail`() = runTest {
        val product = ProductEntity(
            id = "prod_v",
            outletId = "outlet_1",
            categoryId = "cat_1",
            name = "Roti Bakar",
            sku = "ROT-1",
            barcode = "8990001112223",
            buyPrice = 8000.0,
            sellPrice = 15000.0,
            stock = 5,
            minStock = 1,
            unit = "pcs"
        )
        dao.upsertProduct(product)
        val (sale, _) = repository.completeCheckout(
            CheckoutRequest(
                outletId = "outlet_1",
                cashierName = "Owner",
                cashierRole = "OWNER",
                customer = null,
                lines = listOf(CartLineInput(product, "", 3, 0.0)),
                orderDiscount = 0.0,
                taxRatePercent = 0.0,
                taxEnabled = false,
                extraFee = 0.0,
                paymentMethod = "TUNAI",
                paidAmount = 45000.0,
                notes = "",
                loyaltyEnabled = false
            )
        ).getOrThrow()

        assertEquals(2, dao.getProductById("prod_v")!!.stock)

        val voidRes = repository.voidSale(sale, "Salah input", "Owner", "OWNER")
        assertTrue(voidRes.isSuccess)
        assertEquals("VOIDED", dao.getSaleById(sale.id)!!.status)
        assertEquals(5, dao.getProductById("prod_v")!!.stock)
        val logs = dao.observeActivityLogs().first()
        assertTrue(logs.any { it.action == "VOID_SALE" })
    }

    @Test
    fun `demo data loads and clears cleanly without removing real products`() = runTest {
        val realProduct = ProductEntity(
            id = "real_p1",
            outletId = "outlet_main",
            categoryId = "cat_1",
            name = "Produk Asli Toko",
            sku = "REAL-1",
            barcode = "1234567890123",
            buyPrice = 5000.0,
            sellPrice = 10000.0,
            stock = 20,
            minStock = 3,
            unit = "pcs",
            demoData = false
        )
        dao.upsertProduct(realProduct)

        repository.loadDemoDataset("Pemilik")
        val allAfterLoad = dao.getAllProducts()
        assertTrue(allAfterLoad.any { it.demoData })
        assertTrue(allAfterLoad.any { it.id == "real_p1" })

        repository.clearDemoDataset("Pemilik", "OWNER")
        val allAfterClear = dao.getAllProducts()
        assertFalse(allAfterClear.any { it.demoData })
        assertTrue(allAfterClear.any { it.id == "real_p1" })
    }

    @Test
    fun `rbac permissions enforce strict boundaries across roles`() {
        val owner = RolePermissions.forRole(StaffRole.OWNER)
        val admin = RolePermissions.forRole(StaffRole.ADMIN)
        val manager = RolePermissions.forRole(StaffRole.MANAJER)
        val cashier = RolePermissions.forRole(StaffRole.KASIR)

        assertTrue(owner.canManageStaff && owner.canManageOutlets && owner.canVoidOrReturn)
        assertTrue(admin.canManageProducts && admin.canManageSettings && !admin.canManageStaff)
        assertTrue(manager.canViewProfitReports && manager.canManageOutlets && !manager.canManageProducts)
        assertFalse(cashier.canManageProducts || cashier.canVoidOrReturn || cashier.canViewProfitReports || cashier.canManageStaff)
    }

    @Test
    fun `product add edit barcode lookup and delete persist properly`() = runTest {
        val created = repository.saveProduct(
            ProductEntity(
                id = "",
                outletId = "outlet_main",
                categoryId = "cat_general",
                name = "Teh Botol",
                sku = "TEH-01",
                barcode = "8996001300112",
                buyPrice = 3000.0,
                sellPrice = 5000.0,
                stock = 24,
                minStock = 5,
                unit = "btl"
            ),
            actorName = "Admin",
            actorRole = "ADMIN"
        ).getOrThrow()
        assertNotNull(dao.getProductById(created.id))
        assertEquals(created.id, repository.findProductByBarcode("8996001300112")?.id)

        repository.saveProduct(
            created.copy(name = "Teh Botol Sosro 450ml", sellPrice = 5500.0),
            actorName = "Admin",
            actorRole = "ADMIN"
        ).getOrThrow()
        val updated = dao.getProductById(created.id)!!
        assertEquals("Teh Botol Sosro 450ml", updated.name)
        assertEquals(5500.0, updated.sellPrice, 0.01)

        repository.deleteProduct(updated, "Admin", "ADMIN")
        assertNull(dao.getProductById(created.id))
    }

    @Test
    fun `stock adjustments and sale returns update stock and record stock movements`() = runTest {
        val prod = repository.saveProduct(
            ProductEntity(
                id = "prod_stk",
                outletId = "outlet_main",
                categoryId = "cat_general",
                name = "Kabel USB-C",
                sku = "KBL-01",
                barcode = "8881112223334",
                buyPrice = 15000.0,
                sellPrice = 30000.0,
                stock = 10,
                minStock = 2,
                unit = "pcs"
            ),
            actorName = "Owner",
            actorRole = "OWNER"
        ).getOrThrow()

        val inRes = repository.adjustStock(
            productId = prod.id,
            movementType = "IN",
            quantityInput = 5,
            note = "Restok gudang",
            actorName = "Admin",
            actorRole = "ADMIN"
        )
        assertTrue(inRes.isSuccess)
        assertEquals(15, dao.getProductById(prod.id)!!.stock)

        val opnameRes = repository.adjustStock(
            productId = prod.id,
            movementType = "OPNAME",
            quantityInput = 12,
            note = "Opname fisik",
            actorName = "Admin",
            actorRole = "ADMIN"
        )
        assertTrue(opnameRes.isSuccess)
        assertEquals(12, dao.getProductById(prod.id)!!.stock)

        val (sale, _) = repository.completeCheckout(
            CheckoutRequest(
                outletId = "outlet_main",
                cashierName = "Kasir A",
                cashierRole = "KASIR",
                customer = null,
                lines = listOf(CartLineInput(dao.getProductById(prod.id)!!, "", 2, 0.0)),
                orderDiscount = 0.0,
                taxRatePercent = 0.0,
                taxEnabled = false,
                extraFee = 0.0,
                paymentMethod = "TUNAI",
                paidAmount = 60000.0,
                notes = "",
                loyaltyEnabled = false
            )
        ).getOrThrow()
        assertEquals(10, dao.getProductById(prod.id)!!.stock)

        val retRes = repository.recordSaleReturn(
            sale = sale,
            reason = "Cacat pabrik",
            refundAmount = 60000.0,
            actorName = "Owner",
            actorRole = "OWNER"
        )
        assertTrue(retRes.isSuccess)
        assertEquals(12, dao.getProductById(prod.id)!!.stock)

        val movements = dao.observeStockMovements().first().filter { it.productId == prod.id }
        assertTrue(movements.any { it.type == "IN" && it.quantityDelta == 5 })
        assertTrue(movements.any { it.type == "OPNAME" && it.newStock == 12 })
        assertTrue(movements.any { it.type == "SALE" && it.quantityDelta == -2 })
        assertTrue(movements.any { it.type == "RETURN" && it.quantityDelta == 2 })
    }

    @Test
    fun `initial bootstrap seeds default outlet category and owner staff`() = runTest {
        repository.ensureInitialBootstrap("Pemilik Toko", "owner@nexora.id")
        assertTrue(dao.getAllOutlets().isNotEmpty())
        assertTrue(dao.getAllCategories().isNotEmpty())
        assertTrue(dao.observeStaffMembers().first().isNotEmpty())
    }
}
