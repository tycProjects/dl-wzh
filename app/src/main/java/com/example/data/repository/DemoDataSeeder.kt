package com.example.data.repository

import com.example.data.local.*

object DemoDataSeeder {
    fun buildDemoBundle(ownerName: String): DemoBundle {
        val now = System.currentTimeMillis()
        val hour = 3_600_000L
        val day = 86_400_000L

        val outlets = listOf(
            OutletEntity(
                id = "outlet_main",
                name = "Cabang Pusat Jakarta",
                address = "Jl. Jend. Sudirman Kav. 28, Jakarta",
                phone = "021-5550199",
                active = true,
                mainOutlet = true,
                demoData = true
            ),
            OutletEntity(
                id = "outlet_bandung",
                name = "Cabang Dago Bandung",
                address = "Jl. Ir. H. Juanda No. 88, Bandung",
                phone = "022-4209871",
                active = true,
                mainOutlet = false,
                demoData = true
            )
        )

        val categories = listOf(
            CategoryEntity("cat_minimarket", "Minimarket", "#2563EB", "ShoppingBag", "Minimarket", demoData = true),
            CategoryEntity("cat_cafe", "Kafe & Resto", "#D97706", "Restaurant", "Restoran", demoData = true),
            CategoryEntity("cat_fashion", "Pakaian", "#7C3AED", "Checkroom", "Pakaian", demoData = true),
            CategoryEntity("cat_elektronik", "Elektronik", "#0284C7", "Devices", "Elektronik", demoData = true),
            CategoryEntity("cat_apotek", "Farmasi OTC", "#16A34A", "MedicalServices", "Apotek", demoData = true),
            CategoryEntity("cat_digital", "Produk Digital", "#0EA5E9", "Bolt", "Produk Digital", demoData = true)
        )

        val products = listOf(
            ProductEntity(
                id = "prod_kopi_susu",
                outletId = "outlet_main",
                categoryId = "cat_cafe",
                name = "Es Kopi Susu Gula Aren",
                sku = "KFE-001",
                barcode = "8991001000011",
                buyPrice = 11000.0,
                sellPrice = 24000.0,
                stock = 48,
                minStock = 10,
                unit = "cup",
                description = "Espresso arabika dengan susu segar & gula aren organik.",
                productType = "FOOD",
                variantsSummary = "Regular, Large, Less Sugar",
                demoData = true
            ),
            ProductEntity(
                id = "prod_nasi_goreng",
                outletId = "outlet_main",
                categoryId = "cat_cafe",
                name = "Nasi Goreng Spesial Nexora",
                sku = "RST-002",
                barcode = "8991001000028",
                buyPrice = 16000.0,
                sellPrice = 35000.0,
                stock = 30,
                minStock = 8,
                unit = "porsi",
                description = "Nasi goreng rempah dengan ayam panggang dan telur mata sapi.",
                productType = "FOOD",
                variantsSummary = "Original, Pedas, Ekstra Pedas",
                demoData = true
            ),
            ProductEntity(
                id = "prod_air_mineral",
                outletId = "outlet_main",
                categoryId = "cat_minimarket",
                name = "Air Mineral 600ml",
                sku = "MIN-001",
                barcode = "8992775001014",
                buyPrice = 2200.0,
                sellPrice = 4000.0,
                stock = 120,
                minStock = 24,
                unit = "botol",
                description = "Air mineral pegunungan kemasan praktis.",
                productType = "PHYSICAL",
                demoData = true
            ),
            ProductEntity(
                id = "prod_beras",
                outletId = "outlet_main",
                categoryId = "cat_minimarket",
                name = "Beras Premium Setra 5kg",
                sku = "GRO-005",
                barcode = "8998866200321",
                buyPrice = 62000.0,
                sellPrice = 74500.0,
                stock = 6,
                minStock = 10,
                unit = "sak",
                description = "Beras pulen berkualitas tinggi untuk keluarga.",
                productType = "PHYSICAL",
                demoData = true
            ),
            ProductEntity(
                id = "prod_kaos",
                outletId = "outlet_main",
                categoryId = "cat_fashion",
                name = "Kaos Katun Combed 30s",
                sku = "FSH-010",
                barcode = "8993001005123",
                buyPrice = 48000.0,
                sellPrice = 89000.0,
                stock = 24,
                minStock = 6,
                unit = "pcs",
                description = "Kaos kasual bahan katun adem, jahitan rantai rapi.",
                productType = "PHYSICAL",
                variantsSummary = "Hitam - M, Hitam - L, Putih - XL",
                demoData = true
            ),
            ProductEntity(
                id = "prod_earbuds",
                outletId = "outlet_main",
                categoryId = "cat_elektronik",
                name = "Earphone TWS Pro ANC",
                sku = "ELK-104",
                barcode = "6941059648215",
                buyPrice = 165000.0,
                sellPrice = 249000.0,
                stock = 4,
                minStock = 5,
                unit = "unit",
                description = "Wireless earbuds Bluetooth 5.3 dengan peredam bising aktif.",
                productType = "PHYSICAL",
                variantsSummary = "Hitam Matte, Putih Mutiara",
                demoData = true
            ),
            ProductEntity(
                id = "prod_vitamin",
                outletId = "outlet_main",
                categoryId = "cat_apotek",
                name = "Vitamin C 500mg (Strip 10)",
                sku = "APT-021",
                barcode = "8992811002310",
                buyPrice = 12000.0,
                sellPrice = 18500.0,
                stock = 35,
                minStock = 10,
                unit = "strip",
                description = "Suplemen harian untuk menjaga daya tahan tubuh (OTC).",
                productType = "PHYSICAL",
                demoData = true
            ),
            ProductEntity(
                id = "prod_pulsa_50",
                outletId = "outlet_main",
                categoryId = "cat_digital",
                name = "Pulsa & Paket Data 50K",
                sku = "DIG-050",
                barcode = "DIGI-PULSA-50K",
                buyPrice = 49200.0,
                sellPrice = 52000.0,
                stock = 999,
                minStock = 10,
                unit = "paket",
                description = "Pengiriman manual kode/saldo ke nomor tujuan pelanggan.",
                productType = "DIGITAL",
                variantsSummary = "Telkomsel 50K, Indosat 50K, XL 50K",
                demoData = true
            ),
            ProductEntity(
                id = "prod_token_pln",
                outletId = "outlet_main",
                categoryId = "cat_digital",
                name = "Token Listrik PLN 100K",
                sku = "DIG-100",
                barcode = "DIGI-PLN-100K",
                buyPrice = 99500.0,
                sellPrice = 102500.0,
                stock = 999,
                minStock = 10,
                unit = "token",
                description = "Token prabayar PLN proses manual dengan bukti struk.",
                productType = "DIGITAL",
                variantsSummary = "20K, 50K, 100K, 200K",
                demoData = true
            ),
            ProductEntity(
                id = "prod_bandung_kopi",
                outletId = "outlet_bandung",
                categoryId = "cat_cafe",
                name = "Matcha Latte Dingin",
                sku = "KFE-BDG-01",
                barcode = "8991002000099",
                buyPrice = 13000.0,
                sellPrice = 28000.0,
                stock = 25,
                minStock = 5,
                unit = "cup",
                description = "Pure Uji Matcha dengan susu oat/fresh milk.",
                productType = "FOOD",
                variantsSummary = "Regular, Oat Milk",
                demoData = true
            )
        )

        val customers = listOf(
            CustomerEntity("cust_1", "Andi Pratama", "0812-3456-7890", "Jl. Melati No. 5", points = 145, totalSpent = 850000.0, transactionCount = 6, demoData = true),
            CustomerEntity("cust_2", "Rina Kusuma", "0857-9911-2233", "Jl. Kemang Timur No. 18", points = 320, totalSpent = 1640000.0, transactionCount = 11, demoData = true),
            CustomerEntity("cust_3", "PT Karya Mandiri (Grosir)", "021-8891200", "Kawasan Industri Pulogadung", points = 890, totalSpent = 4250000.0, transactionCount = 4, demoData = true)
        )

        val suppliers = listOf(
            SupplierEntity("sup_1", "PT Sumber Makmur Distribusi", "021-5432100", "Jakarta Barat", "Pemasok sembako & minuman", demoData = true),
            SupplierEntity("sup_2", "CV Roastery Nusantara", "022-7788990", "Bandung", "Pemasok biji kopi & bahan kafe", demoData = true),
            SupplierEntity("sup_3", "NexTech Aksesoris Indonesia", "0811-9008-7766", "Mangga Dua Jakarta", "Distributor aksesoris elektronik", demoData = true)
        )

        val tables = listOf(
            RestaurantTableEntity("tbl_1", "outlet_main", "Meja 01", 4, "AVAILABLE", null, demoData = true),
            RestaurantTableEntity("tbl_2", "outlet_main", "Meja 02", 4, "OCCUPIED", "Q-014", demoData = true),
            RestaurantTableEntity("tbl_3", "outlet_main", "Meja 03", 6, "AVAILABLE", null, demoData = true),
            RestaurantTableEntity("tbl_4", "outlet_main", "Meja VIP", 8, "RESERVED", null, demoData = true)
        )

        val restaurantOrders = listOf(
            RestaurantOrderEntity(
                id = "ro_demo_1",
                outletId = "outlet_main",
                queueNumber = "Q-014",
                orderType = "DINE_IN",
                tableId = "tbl_2",
                tableName = "Meja 02",
                itemsSummary = "Nasi Goreng Spesial Nexora x2, Es Kopi Susu Gula Aren x2",
                notes = "1 nasi goreng tidak pakai acar",
                status = "PREPARING",
                totalAmount = 118000.0,
                createdAtEpochMs = now - 15 * 60_000L,
                demoData = true
            ),
            RestaurantOrderEntity(
                id = "ro_demo_2",
                outletId = "outlet_main",
                queueNumber = "Q-015",
                orderType = "TAKEAWAY",
                tableId = null,
                tableName = "Bawa Pulang",
                itemsSummary = "Es Kopi Susu Gula Aren (Large) x1",
                notes = "Es terpisah",
                status = "QUEUED",
                totalAmount = 24000.0,
                createdAtEpochMs = now - 5 * 60_000L,
                demoData = true
            )
        )

        val digitalOrders = listOf(
            DigitalOrderEntity(
                id = "do_demo_1",
                outletId = "outlet_main",
                productId = "prod_pulsa_50",
                productName = "Pulsa & Paket Data 50K",
                categoryName = "Produk Digital",
                variantLabel = "Telkomsel 50K",
                recipientInfo = "0812-3456-7890",
                notes = "Diproses manual via aplikasi mitra",
                price = 52000.0,
                deliveryStatus = "DELIVERED",
                deliveredAtEpochMs = now - 2 * hour,
                createdAtEpochMs = now - 2 * hour,
                demoData = true
            ),
            DigitalOrderEntity(
                id = "do_demo_2",
                outletId = "outlet_main",
                productId = "prod_token_pln",
                productName = "Token Listrik PLN 100K",
                categoryName = "Produk Digital",
                variantLabel = "100K",
                recipientInfo = "ID Pel: 542100998812",
                notes = "Menunggu kirim nomor token ke pelanggan",
                price = 102500.0,
                deliveryStatus = "PENDING",
                deliveredAtEpochMs = null,
                createdAtEpochMs = now - 25 * 60_000L,
                demoData = true
            )
        )

        val staff = listOf(
            StaffMemberEntity("staff_owner", ownerName.ifBlank { "Pemilik Usaha" }, "OWNER", "outlet_main", "0000", active = true, demoData = true),
            StaffMemberEntity("staff_admin", "Dewi Kartika", "ADMIN", "outlet_main", "1111", active = true, demoData = true),
            StaffMemberEntity("staff_manager", "Rizky Wijaya", "MANAJER", "outlet_main", "2222", active = true, demoData = true),
            StaffMemberEntity("staff_cashier", "Siti Rahma", "KASIR", "outlet_main", "1234", active = true, demoData = true)
        )

        val sales = listOf(
            SaleEntity(
                id = "sale_demo_1",
                outletId = "outlet_main",
                transactionNumber = "NX-DEMO-001",
                cashierName = "Siti Rahma",
                cashierRole = "KASIR",
                customerId = "cust_1",
                customerName = "Andi Pratama",
                subtotal = 118000.0,
                taxAmount = 12980.0,
                total = 130980.0,
                paymentMethod = "TUNAI",
                paidAmount = 150000.0,
                changeAmount = 19020.0,
                status = "COMPLETED",
                itemsSummary = "Es Kopi Susu Gula Aren x2, Nasi Goreng Spesial Nexora x2",
                itemCount = 4,
                grossProfit = 64000.0,
                createdAtEpochMs = now - 1 * hour,
                demoData = true
            ),
            SaleEntity(
                id = "sale_demo_2",
                outletId = "outlet_main",
                transactionNumber = "NX-DEMO-002",
                cashierName = "Siti Rahma",
                cashierRole = "KASIR",
                customerId = "cust_2",
                customerName = "Rina Kusuma",
                subtotal = 249000.0,
                taxAmount = 27390.0,
                total = 276390.0,
                paymentMethod = "QRIS_MANUAL",
                paidAmount = 276390.0,
                changeAmount = 0.0,
                status = "COMPLETED",
                notes = "QRIS statis toko terkonfirmasi kasir",
                itemsSummary = "Earphone TWS Pro ANC x1",
                itemCount = 1,
                grossProfit = 84000.0,
                createdAtEpochMs = now - 3 * hour,
                demoData = true
            ),
            SaleEntity(
                id = "sale_demo_3",
                outletId = "outlet_main",
                transactionNumber = "NX-DEMO-003",
                cashierName = "Dewi Kartika",
                cashierRole = "ADMIN",
                subtotal = 178000.0,
                taxAmount = 19580.0,
                total = 197580.0,
                paymentMethod = "TRANSFER",
                paidAmount = 197580.0,
                changeAmount = 0.0,
                status = "COMPLETED",
                itemsSummary = "Kaos Katun Combed 30s x2",
                itemCount = 2,
                grossProfit = 82000.0,
                createdAtEpochMs = now - 1 * day,
                demoData = true
            ),
            SaleEntity(
                id = "sale_demo_4",
                outletId = "outlet_main",
                transactionNumber = "NX-DEMO-004",
                cashierName = "Siti Rahma",
                cashierRole = "KASIR",
                subtotal = 149000.0,
                taxAmount = 16390.0,
                total = 165390.0,
                paymentMethod = "TUNAI",
                paidAmount = 170000.0,
                changeAmount = 4610.0,
                status = "COMPLETED",
                itemsSummary = "Beras Premium Setra 5kg x2",
                itemCount = 2,
                grossProfit = 25000.0,
                createdAtEpochMs = now - 2 * day,
                demoData = true
            ),
            SaleEntity(
                id = "sale_demo_5",
                outletId = "outlet_main",
                transactionNumber = "NX-DEMO-005",
                cashierName = "Siti Rahma",
                cashierRole = "KASIR",
                subtotal = 96000.0,
                taxAmount = 10560.0,
                total = 106560.0,
                paymentMethod = "TUNAI",
                paidAmount = 110000.0,
                changeAmount = 3440.0,
                status = "COMPLETED",
                itemsSummary = "Es Kopi Susu Gula Aren x4",
                itemCount = 4,
                grossProfit = 52000.0,
                createdAtEpochMs = now - 4 * day,
                demoData = true
            )
        )

        val saleItems = listOf(
            SaleItemEntity("si_1", "sale_demo_1", "prod_kopi_susu", "Es Kopi Susu Gula Aren", "Regular", 2, 11000.0, 24000.0, 0.0, 48000.0, demoData = true),
            SaleItemEntity("si_2", "sale_demo_1", "prod_nasi_goreng", "Nasi Goreng Spesial Nexora", "Original", 2, 16000.0, 35000.0, 0.0, 70000.0, demoData = true),
            SaleItemEntity("si_3", "sale_demo_2", "prod_earbuds", "Earphone TWS Pro ANC", "Hitam Matte", 1, 165000.0, 249000.0, 0.0, 249000.0, demoData = true),
            SaleItemEntity("si_4", "sale_demo_3", "prod_kaos", "Kaos Katun Combed 30s", "Hitam - L", 2, 48000.0, 89000.0, 0.0, 178000.0, demoData = true),
            SaleItemEntity("si_5", "sale_demo_4", "prod_beras", "Beras Premium Setra 5kg", "", 2, 62000.0, 74500.0, 0.0, 149000.0, demoData = true),
            SaleItemEntity("si_6", "sale_demo_5", "prod_kopi_susu", "Es Kopi Susu Gula Aren", "Regular", 4, 11000.0, 24000.0, 0.0, 96000.0, demoData = true)
        )

        val movements = listOf(
            StockMovementEntity("mov_demo_1", "outlet_main", "prod_beras", "Beras Premium Setra 5kg", "SALE", -2, 8, 6, "Penjualan NX-DEMO-004", "Siti Rahma", now - 2 * day, demoData = true),
            StockMovementEntity("mov_demo_2", "outlet_main", "prod_earbuds", "Earphone TWS Pro ANC", "SALE", -1, 5, 4, "Penjualan NX-DEMO-002", "Siti Rahma", now - 3 * hour, demoData = true),
            StockMovementEntity("mov_demo_3", "outlet_main", "prod_air_mineral", "Air Mineral 600ml", "IN", 48, 72, 120, "Restock mingguan PT Sumber Makmur", "Dewi Kartika", now - 1 * day, demoData = true)
        )

        val purchases = listOf(
            PurchaseEntity("po_demo_1", "outlet_main", "sup_1", "PT Sumber Makmur Distribusi", "INV-SM-202611", 105600.0, "RECEIVED", "48 botol Air Mineral 600ml", "Dewi Kartika", now - 1 * day, demoData = true)
        )

        return DemoBundle(
            outlets = outlets,
            categories = categories,
            products = products,
            customers = customers,
            suppliers = suppliers,
            tables = tables,
            restaurantOrders = restaurantOrders,
            digitalOrders = digitalOrders,
            staffMembers = staff,
            sales = sales,
            saleItems = saleItems,
            stockMovements = movements,
            purchases = purchases
        )
    }
}

data class DemoBundle(
    val outlets: List<OutletEntity>,
    val categories: List<CategoryEntity>,
    val products: List<ProductEntity>,
    val customers: List<CustomerEntity>,
    val suppliers: List<SupplierEntity>,
    val tables: List<RestaurantTableEntity>,
    val restaurantOrders: List<RestaurantOrderEntity>,
    val digitalOrders: List<DigitalOrderEntity>,
    val staffMembers: List<StaffMemberEntity>,
    val sales: List<SaleEntity>,
    val saleItems: List<SaleItemEntity>,
    val stockMovements: List<StockMovementEntity>,
    val purchases: List<PurchaseEntity>
)
