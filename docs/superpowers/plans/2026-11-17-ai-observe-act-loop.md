# Loop Observasi–Aksi untuk Asisten AI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (atau subagent-driven-development) untuk menjalankan rencana ini task demi task. Langkah memakai checkbox (`- [ ]`).

**Goal:** Asisten membaca layar di antara setiap langkah dan memutuskan langkah berikutnya sampai tugas selesai, dengan batas dan penjaga keamanan.

**Architecture:** Logika keputusan (kapan lanjut, kapan berhenti, penjaga tombol, pemangkasan layar, penjaga kolom kata sandi) ditulis sebagai objek murni tanpa Android agar bisa diuji unit. Loop di `AiSession` memakai objek-objek itu dan menggantikan `runActionRounds` untuk permintaan yang memicu aksi perangkat.

**Tech Stack:** Kotlin, JUnit 4, Android accessibility service, org.json.

**Spec:** docs/superpowers/specs/2026-11-17-ai-observe-act-loop-design.md

## Global Constraints
- minSdk 35, targetSdk 35 (build.gradle.kts, tidak berubah).
- Maksimum 8 langkah per permintaan; batas waktu total 90 detik.
- Maksimum 3 aksi per putaran; ringkasan layar maksimum 1500 karakter.
- Dua kali kegagalan berturut-turut untuk aksi yang sama menghentikan loop.
- Panggilan dan SMS tidak pernah dieksekusi tanpa tombol pengguna.

## Review Focus
1. Model mengembalikan `done: false` tanpa aksi: harus berhenti, bukan berputar.
2. JSON model rusak di tengah loop: berhenti dan tampilkan teks mentah.
3. Layar kosong atau tidak bisa dibaca: berhenti dengan pesan jelas, bukan mengulang.
4. Pengguna menekan Stop saat loop sedang menunggu: loop berhenti pada langkah berikutnya.
5. Kolom kata sandi fokus saat aksi `type`: aksi ditolak.

---

### Task 1: AgentLoopPolicy (murni)

**Files:**
- Create: `app/src/main/java/com/android/island/ai/AgentLoopPolicy.kt`
- Test: `app/src/test/java/com/android/island/ai/AgentLoopPolicyTest.kt`

**Interfaces:**
- Produces: `object AgentLoopPolicy { const val MAX_STEPS = 8; const val MAX_MS = 90_000L; const val MAX_REPEAT_FAILURES = 2; fun decide(stepsDone: Int, elapsedMs: Long, modelSaysDone: Boolean, hasActions: Boolean, repeatedFailures: Int): Decision }` dan `sealed class Decision { object Continue; data class Stop(val reason: String) }`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun stopsWhenModelSaysDone() {
    val d = AgentLoopPolicy.decide(1, 1_000, modelSaysDone = true, hasActions = true, repeatedFailures = 0)
    assertEquals(AgentLoopPolicy.Decision.Stop("selesai"), d)
}
@Test fun stopsWhenNoActionsAndNotDone() {
    val d = AgentLoopPolicy.decide(1, 1_000, modelSaysDone = false, hasActions = false, repeatedFailures = 0)
    assertTrue(d is AgentLoopPolicy.Decision.Stop)
}
@Test fun stopsAtEightSteps() {
    val d = AgentLoopPolicy.decide(8, 1_000, false, true, 0)
    assertTrue(d is AgentLoopPolicy.Decision.Stop)
}
@Test fun stopsAfterNinetySeconds() {
    val d = AgentLoopPolicy.decide(2, 90_000, false, true, 0)
    assertTrue(d is AgentLoopPolicy.Decision.Stop)
}
@Test fun stopsOnRepeatedFailures() {
    val d = AgentLoopPolicy.decide(2, 1_000, false, true, 2)
    assertTrue(d is AgentLoopPolicy.Decision.Stop)
}
@Test fun continuesWhenWorkRemains() {
    assertEquals(AgentLoopPolicy.Decision.Continue, AgentLoopPolicy.decide(2, 1_000, false, true, 0))
}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests "*AgentLoopPolicyTest*"`
Expected: FAIL, `AgentLoopPolicy` tidak terdefinisi.

- [ ] **Step 3: Implement `decide` in `AgentLoopPolicy.kt`**
Urutan pengecekan: selesai, tanpa aksi, batas langkah, batas waktu, kegagalan berulang, lalu Continue.

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew testDebugUnitTest --tests "*AgentLoopPolicyTest*"`
Expected: PASS.

### Task 2: ClickGuard (murni)

**Files:**
- Create: `app/src/main/java/com/android/island/ai/ClickGuard.kt`
- Test: `app/src/test/java/com/android/island/ai/ClickGuardTest.kt`

**Interfaces:**
- Produces: `object ClickGuard { fun allowed(label: String, userText: String): Boolean }`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun destructiveLabelBlockedUnlessUserAskedForIt() {
    assertFalse(ClickGuard.allowed("Hapus", "buka galeri"))
    assertTrue(ClickGuard.allowed("Hapus", "hapus foto ini"))
}
@Test fun ordinaryLabelAlwaysAllowed() {
    assertTrue(ClickGuard.allowed("Putar", "putar lagu"))
}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests "*ClickGuardTest*"`
Expected: FAIL, `ClickGuard` tidak terdefinisi.

- [ ] **Step 3: Implement** daftar kata berisiko: hapus, bayar, beli, uninstall, reset, kirim. Label dicek tanpa huruf besar-kecil.

- [ ] **Step 4: Run test to verify it passes**
Expected: PASS.

### Task 3: ScreenSummary (murni)

**Files:**
- Create: `app/src/main/java/com/android/island/ai/ScreenSummary.kt`
- Test: `app/src/test/java/com/android/island/ai/ScreenSummaryTest.kt`

**Interfaces:**
- Produces: `object ScreenSummary { const val LIMIT = 1500; fun trim(text: String): String }`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun longScreenIsCutToLimit() {
    assertEquals(ScreenSummary.LIMIT, ScreenSummary.trim("a".repeat(5000)).length)
}
@Test fun blankScreenStaysBlank() {
    assertEquals("", ScreenSummary.trim("   "))
}
```

- [ ] **Step 2: Run test to verify it fails**
Expected: FAIL.

- [ ] **Step 3: Implement** `trim`: buang baris kosong, potong ke LIMIT karakter.

- [ ] **Step 4: Run test to verify it passes**
Expected: PASS.

### Task 4: Protokol `done` dan kolom kata sandi

**Files:**
- Modify: `app/src/main/java/com/android/island/ai/AiActionProtocol.kt`
- Modify: `app/src/main/java/com/android/island/ai/AiCommandRouter.kt` (tambah helper `typeAllowed`)
- Test: `app/src/test/java/com/android/island/ai/AiActionProtocolTest.kt`

**Interfaces:**
- Produces: `data class Envelope(val reply: String, val actions: List<AiAction>, val done: Boolean = false, val broken: Boolean = false)`; `AiActionProtocol.parse` mengisi `done` dari field `"done"`; JSON rusak menghasilkan `broken = true`.
- Produces: `fun typeAllowed(focusedIsPassword: Boolean): Boolean = !focusedIsPassword`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun doneFlagIsParsed() {
    val env = AiActionProtocol.parse("""{"reply":"ok","actions":[],"done":true}""")
    assertTrue(env.done)
}
@Test fun brokenJsonIsMarked() {
    assertTrue(AiActionProtocol.parse("bukan json").broken)
}
@Test fun typingIntoPasswordIsRefused() {
    assertFalse(AiCommandRouter.typeAllowed(focusedIsPassword = true))
}
```

- [ ] **Step 2: Run test to verify it fails**
Expected: FAIL, `done` dan `broken` belum ada.

- [ ] **Step 3: Implement** field baru dan helper.

- [ ] **Step 4: Run test to verify it passes**
Expected: PASS.

### Task 5: Loop observasi–aksi di AiSession

**Files:**
- Modify: `app/src/main/java/com/android/island/ai/AiSession.kt` (ganti `runActionRounds` untuk permintaan perangkat)
- Modify: `app/src/main/java/com/android/island/ai/DeviceActions.kt` (`type` memanggil `typeAllowed`; `click` memanggil `ClickGuard`)
- Modify: `app/src/main/java/com/android/island/service/IslandAccessibilityService.kt` (tambah `isFocusedPassword()`)

**Interfaces:**
- Consumes: `AgentLoopPolicy.decide`, `ClickGuard.allowed`, `ScreenSummary.trim`, `AiActionProtocol.parse`, `AiActionProtocol.limit`
- Produces: `suspend fun runObserveActLoop(ctx: Context, endpoint: AiEndpoint, userText: String, requestGeneration: Long): String`

- [ ] **Step 1: Write the failing test**
Loop Android-dependen tidak bisa diuji unit di sini. Logika keputusannya sudah diuji di Task 1–4. Test integrasi ditulis sebagai instrumented test `AgentLoopInstrumentedTest`: model palsu mengembalikan `open_app`, lalu `scroll` dua kali, lalu `done`. Expected: tiga aksi dijalankan, loop berhenti.

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew connectedDebugAndroidTest --tests "*AgentLoopInstrumentedTest*"` (membutuhkan perangkat)
Expected: FAIL, fungsi belum ada.

- [ ] **Step 3: Implement** loop: baca layar (`ScreenSummary.trim(readScreenText())`), panggil model, jalankan maksimum 3 aksi lewat `limit`, cek `Stop` dari `AgentLoopPolicy`, catat ke `IslandDiagnostics`, tampilkan "Membaca layar..." sebelum setiap pengiriman, dan batalkan bila `requestGeneration` berubah.

- [ ] **Step 4: Run test to verify it passes**
Expected: PASS di perangkat.

### Task 6: Verifikasi keseluruhan

- [ ] **Step 1:** Jalankan `./gradlew testDebugUnitTest`. Expected: semua test lulus, termasuk test lama.
- [ ] **Step 2:** Jalankan `./gradlew assembleDebug`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 3:** Uji manual di perangkat: "buka TikTok lalu scroll 2 kali" dan "buka Auralys, cari lagu X, lalu putar".
