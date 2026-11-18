const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_owner_01";
const BOB_UID = "bob_other_02";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any business collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("products").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .doc("prod_1")
      .set({
        userId: ALICE_UID,
        outletId: "outlet_1",
        categoryId: "cat_1",
        name: "Illegal Item",
        sku: "SKU1",
        barcode: "123",
        buyPrice: 1000,
        sellPrice: 2000,
        stock: 5,
        minStock: 1,
        unit: "pcs",
        productType: "PHYSICAL",
        active: true,
        demoData: false,
        createdAt: pastDate(),
        updatedAt: pastDate(),
      })
  );
});

test("Authenticated owner can create and update user profile and business settings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Budi Santoso",
      email: "budi@nexorapos.id",
      businessName: "Toko Maju Jaya",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      businessName: "Nexora Mart",
      updatedAt: pastDate(1000),
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("business_settings")
      .doc("default")
      .set({
        userId: ALICE_UID,
        businessName: "Nexora Mart",
        businessType: "Minimarket & Kelontong",
        address: "Jl. Sudirman No. 12",
        phone: "08123456789",
        taxPercent: 11,
        currencyCode: "IDR",
        receiptFooter: "Terima kasih sudah berbelanja",
        receiptWidthMm: 58,
        taxEnabled: true,
        loyaltyEnabled: true,
        scannerSoundEnabled: true,
        createdAt: ts,
        updatedAt: ts,
      })
  );
});

test("Cross-user isolation blocks Bob from reading or writing Alice's subcollections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("outlets")
      .doc("outlet_main")
      .set({
        userId: ALICE_UID,
        name: "Cabang Utama",
        address: "Jl. Merdeka 1",
        phone: "081111111",
        active: true,
        mainOutlet: true,
        demoData: false,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("outlets")
      .doc("outlet_main")
      .get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("outlets").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("outlets")
      .doc("outlet_main")
      .update({ name: "Hacked", updatedAt: pastDate(500) })
  );
});

test("Product, customer, supplier, and stock movement Kotlin payloads pass and reject ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .doc("prod_1")
      .set({
        userId: ALICE_UID,
        outletId: "outlet_main",
        categoryId: "cat_drink",
        name: "Kopi Arabika 250g",
        sku: "KP-250",
        barcode: "8991234567890",
        buyPrice: 45000,
        sellPrice: 75000,
        stock: 20,
        minStock: 5,
        unit: "pcs",
        description: "Biji kopi pilihan",
        productType: "PHYSICAL",
        variantsSummary: "250g, 500g",
        active: true,
        demoData: false,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Reject ghost `id` field
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .doc("prod_ghost")
      .set({
        id: "prod_ghost",
        userId: ALICE_UID,
        outletId: "outlet_main",
        categoryId: "cat_drink",
        name: "Ghost",
        sku: "G-1",
        barcode: "111",
        buyPrice: 1000,
        sellPrice: 2000,
        stock: 1,
        minStock: 0,
        unit: "pcs",
        productType: "PHYSICAL",
        active: true,
        demoData: false,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Partial update on product
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .doc("prod_1")
      .update({
        stock: 18,
        updatedAt: pastDate(1000),
      })
  );

  // Immutable stock movement creation & forbid update
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("stock_movements")
      .doc("mov_1")
      .set({
        userId: ALICE_UID,
        outletId: "outlet_main",
        productId: "prod_1",
        productName: "Kopi Arabika 250g",
        type: "SALE",
        quantityDelta: -2,
        previousStock: 20,
        newStock: 18,
        note: "Penjualan TRX-001",
        actorName: "Kasir 1",
        demoData: false,
        createdAt: ts,
        updatedAt: ts,
      })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("stock_movements")
      .doc("mov_1")
      .update({ quantityDelta: -1, updatedAt: pastDate(500) })
  );
});

test("Sale lifecycle enforces terminal VOIDED state lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate(10000);
  const saleRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("sales")
    .doc("sale_1");

  await assertSucceeds(
    saleRef.set({
      userId: ALICE_UID,
      outletId: "outlet_main",
      transactionNumber: "NX-20261118-0001",
      cashierName: "Andi",
      cashierRole: "KASIR",
      subtotal: 150000,
      itemDiscountTotal: 0,
      orderDiscount: 0,
      taxAmount: 16500,
      extraFee: 0,
      total: 166500,
      paymentMethod: "TUNAI",
      paidAmount: 200000,
      changeAmount: 33500,
      status: "COMPLETED",
      itemsSummary: "Kopi Arabika 250g x2",
      itemCount: 2,
      grossProfit: 60000,
      createdAtEpochMs: Date.now() - 10000,
      demoData: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Void transition succeeds once
  await assertSucceeds(
    saleRef.update({
      status: "VOIDED",
      notes: "Dibatalkan oleh Owner",
      updatedAt: pastDate(1000),
    })
  );

  // Further updates after VOIDED must fail
  await assertFails(
    saleRef.update({
      status: "COMPLETED",
      updatedAt: pastDate(500),
    })
  );
});

test("Restaurant orders, digital orders, staff, and activity logs validate cleanly", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("restaurant_orders")
      .doc("ro_1")
      .set({
        userId: ALICE_UID,
        outletId: "outlet_main",
        queueNumber: "A-001",
        orderType: "DINE_IN",
        tableId: "table_1",
        tableName: "Meja 1",
        itemsSummary: "Nasi Goreng x2",
        notes: "Pedas sedang",
        status: "QUEUED",
        totalAmount: 70000,
        demoData: false,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("digital_orders")
      .doc("do_1")
      .set({
        userId: ALICE_UID,
        outletId: "outlet_main",
        productId: "prod_pulsa",
        productName: "Pulsa Reguler 50K",
        categoryName: "Produk Digital",
        variantLabel: "50.000",
        recipientInfo: "081234567890",
        price: 51500,
        deliveryStatus: "PENDING",
        demoData: false,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("staff_members")
      .doc("staff_1")
      .set({
        userId: ALICE_UID,
        name: "Siti Rahma",
        role: "KASIR",
        outletId: "outlet_main",
        pinCode: "1234",
        active: true,
        demoData: false,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("activity_logs")
      .doc("log_1")
      .set({
        userId: ALICE_UID,
        actorName: "Siti Rahma",
        actorRole: "KASIR",
        action: "CREATE_SALE",
        details: "Transaksi NX-001 selesai",
        createdAtEpochMs: Date.now() - 5000,
        createdAt: ts,
        updatedAt: ts,
      })
  );
});
