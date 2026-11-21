# Model Otomatis, Gambar, dan UI Panel AI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans untuk menjalankan rencana ini task demi task. Langkah memakai checkbox (`- [ ]`).

**Goal:** Panel AI memilih model chat dan gambar secara otomatis dari penyedia yang diatur pengguna, bisa membuat gambar, dan tampil dengan nuansa AI dan liquid glass.

**Architecture:** Logika pemilihan model dan parsing respons gambar ditulis sebagai fungsi murni agar bisa diuji unit. Pemanggilan jaringan dan UI memakai kode yang sudah ada (`AiClient`, `AiPanel`, `liquidGlass`, `RenderTier`).

**Tech Stack:** Kotlin, JUnit 4, Jetpack Compose, org.json.

**Spec:** docs/superpowers/specs/2026-11-17-ai-panel-ui-design.md (revisi 1 sampai 4)

## Global Constraints
- Nama model gambar hanya dari katalog terverifikasi: `gpt-image-2`, `gpt-image-1.5`, `gpt-image-1-mini`, `gemini-3.1-flash-image`, `gemini-3.1-flash-lite-image`.
- Nama default chat tidak ditulis sebagai fakta: `gemini-3.8-flash` dihapus dari kode.
- Tidak ada font baru; serif memakai `FontFamily.Serif` sistem.
- Maksimum satu gambar per permintaan; biaya dicatat di `IslandDiagnostics`.
- Gambar tidak disimpan ke galeri tanpa tombol "Simpan".

## Review Focus
1. Daftar `/models` kosong atau gagal: pakai preset sebagai cadangan dan beri tahu pengguna.
2. Penyedia tanpa model gambar (mis. Groq): tampilkan pesan, jangan mencoba model lain diam-diam.
3. Galat 403 verifikasi organisasi OpenAI: sebutkan penyebab dan langkah perbaikan.
4. Respons gambar tanpa data base64: anggap gagal dengan pesan jelas.
5. Perangkat LOW_END: panel memakai permukaan datar, tanpa efek liquid glass.

---

### Task 1: ModelPicker (murni)

**Files:**
- Create: `app/src/main/java/com/android/island/ai/ModelPicker.kt`
- Test: `app/src/test/java/com/android/island/ai/ModelPickerTest.kt`

**Interfaces:**
- Produces: `object ModelPicker { fun pickChat(ids: List<String>, familyRegex: Regex): String?; fun pickImage(ids: List<String>): String? }`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun picksNewestStableFlashAndIgnoresPreview() {
    val ids = listOf("gemini-3.1-flash-lite", "gemini-3.5-flash", "gemini-3.5-flash-preview-05-2026", "gemini-2.5-flash")
    val flash = Regex("""^gemini-(\d+(?:\.\d+)*)-flash$""")
    assertEquals("gemini-3.5-flash", ModelPicker.pickChat(ids, flash))
}
@Test fun returnsNullWhenFamilyMissing() {
    assertNull(ModelPicker.pickChat(listOf("llama-3.3-70b"), Regex("""^gemini-(\d+)-flash$""")))
}
@Test fun picksPrimaryImageModelFromCatalog() {
    assertEquals("gpt-image-2", ModelPicker.pickImage(listOf("gpt-image-1-mini", "gpt-image-2", "dall-e-3")))
}
@Test fun imageModelNotInCatalogIsNotPicked() {
    assertNull(ModelPicker.pickImage(listOf("dall-e-3")))
}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests "*ModelPickerTest*"`
Expected: FAIL, `ModelPicker` belum ada.

- [ ] **Step 3: Implement**
`pickChat`: ambil id yang cocok regex dan tidak mengandung `-preview`, `-live`, `-tts`, `-image`, `-embedding`, `-transcribe`; urutkan menurut tuple versi dari grup pertama (dipecah titik, dibandingkan angka); kembalikan yang tertinggi. `pickImage`: iterasi katalog terurut prioritas, kembalikan id pertama yang ada di daftar.

- [ ] **Step 4: Run test to verify it passes**
Expected: PASS.

### Task 2: Parser respons gambar (murni)

**Files:**
- Create: `app/src/main/java/com/android/island/ai/ImageResponseParser.kt`
- Test: `app/src/test/java/com/android/island/ai/ImageResponseParserTest.kt`

**Interfaces:**
- Produces: `object ImageResponseParser { fun openai(json: String): ByteArray?; fun gemini(json: String): ByteArray? }` (null bila tidak ada data)

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun openaiBase64IsDecoded() {
    val b = ImageResponseParser.openai("""{"data":[{"b64_json":"aGVsbG8="}]}""")
    assertEquals("hello", String(b!!))
}
@Test fun geminiInlineDataIsDecoded() {
    val json = """{"candidates":[{"content":{"parts":[{"inlineData":{"mimeType":"image/png","data":"aGVsbG8="}}]}}]}"""
    assertEquals("hello", String(ImageResponseParser.gemini(json)!!))
}
@Test fun missingDataIsNull() {
    assertNull(ImageResponseParser.openai("""{"data":[]}"""))
    assertNull(ImageResponseParser.gemini("""{"candidates":[]}"""))
}
```

- [ ] **Step 2: Run test to verify it fails**
Expected: FAIL.

- [ ] **Step 3: Implement** dengan `org.json` dan `android.util.Base64`. Pada JVM unit test, gunakan `java.util.Base64` agar bisa berjalan tanpa Robolectric (pilih `java.util.Base64`).

- [ ] **Step 4: Run test to verify it passes**
Expected: PASS.

### Task 3: Pemanggilan gambar (jaringan)

**Files:**
- Create: `app/src/main/java/com/android/island/ai/ImageClient.kt`
- Modify: `app/src/main/java/com/android/island/ai/AiClient.kt` (ekspos `normalizeApiRoot` dan `canonicalBaseUrl` yang sudah ada jika belum publik)

**Interfaces:**
- Consumes: `ModelPicker.pickImage`, `ImageResponseParser`
- Produces: `suspend fun generate(provider: String, baseUrl: String, apiKey: String, prompt: String): Result<ByteArray>` yang memilih model otomatis dari `/models`.

- [ ] **Step 1: Write the failing test**
Pemanggilan jaringan tidak diuji unit. Test pemetaan galat murni: `ImageClient.errorMessage(code: Int, body: String): String` mengembalikan pesan Indonesia. Contoh: 403 dengan teks "organization" → "Organisasi OpenAI belum terverifikasi untuk model gambar. Verifikasi di konsol OpenAI, lalu coba lagi." Tulis test untuk ini.

- [ ] **Step 2: Run test to verify it fails**
Expected: FAIL.

- [ ] **Step 3: Implement** `generate`: panggil `discoverModels`, `ModelPicker.pickImage`, lalu POST ke `{root}/images/generations` (OpenAI) atau `{root}/models/{model}:generateContent` dengan `generationConfig.responseModalities = ["IMAGE"]` (Gemini). Jika model null, kembalikan `Result.failure` dengan pesan "Penyedia ini belum punya model gambar." Timeout 120 detik.

- [ ] **Step 4: Run test to verify it passes**
Expected: PASS untuk `errorMessage`.

### Task 4: Aksi `generate_image` dan tampilan gambar di chat

**Files:**
- Modify: `app/src/main/java/com/android/island/ai/AiCommandRouter.kt` (tambah `data class GenerateImage(val prompt: String)`)
- Modify: `app/src/main/java/com/android/island/ai/IntentResolver.kt` (parse `"generate_image"`)
- Modify: `app/src/main/java/com/android/island/ai/AiActionProtocol.kt` (tambah ke prompt sistem)
- Modify: `app/src/main/java/com/android/island/ai/AiSession.kt` (`runStep` memanggil `ImageClient.generate`, menyimpan file ke cache aplikasi, dan menambah `imagePath` ke pesan)
- Test: `app/src/test/java/com/android/island/ai/IntentResolverTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun generateImageIsParsedFromModelJson() {
    val raw = """{"actions":[{"type":"generate_image","prompt":"kucing oranye di pantai"}]}"""
    assertEquals(listOf(AiAction.GenerateImage("kucing oranye di pantai")), IntentResolver.parse(raw))
}
@Test fun emptyImagePromptIsIgnored() {
    assertNull(IntentResolver.parse("""{"actions":[{"type":"generate_image","prompt":" "}]}"""))
}
```

- [ ] **Step 2: Run test to verify it fails**
Expected: FAIL.

- [ ] **Step 3: Implement** parser, aksi, pemanggilan `ImageClient`, dan `ChatMessage.imagePath: String?`.

- [ ] **Step 4: Run test to verify it passes**
Expected: PASS.

### Task 5: Pemilihan model chat otomatis saat pengaturan

**Files:**
- Modify: `app/src/main/java/com/android/island/ui/screens/HomeScreen.kt` (saat provider atau kunci berubah, panggil `discoverModels` lalu `ModelPicker.pickChat` dengan regex per preset)
- Modify: `app/src/main/java/com/android/island/ai/AiClient.kt` (hapus `GEMINI_DEFAULT_MODEL = "gemini-3.8-flash"`; ganti dengan nilai dari `ModelPicker`)
- Test: `ModelPickerTest` (regex Gemini Flash dan kasus kosong)

**Interfaces:**
- Consumes: `ModelPicker.pickChat`
- Produces: `AiProviderPreset.familyRegex: Regex?` (null untuk penyedia yang belum terverifikasi, sehingga pengguna memilih model manual)

- [ ] **Step 1: Write the failing test**
Gunakan test dari Task 1 untuk regex Gemini. Tambahkan test: `pickChat` dengan regex null tidak dipanggil (dijaga di pemanggil).

- [ ] **Step 2: Run test to verify it fails**
Expected: FAIL sampai `familyRegex` ada.

- [ ] **Step 3: Implement** field `familyRegex` di preset. Hanya preset Gemini yang diberi regex pada tahap ini; preset OpenAI dan penyedia lain `null` sampai diverifikasi.

- [ ] **Step 4: Run test to verify it passes**
Expected: PASS.

### Task 6: Panel UI sesuai desain

**Files:**
- Modify: `app/src/main/java/com/android/island/island/AiPanel.kt`
- Create: `app/src/main/java/com/android/island/island/StepTrail.kt` (jejak langkah sebagai composable)
- Create: `app/src/main/java/com/android/island/island/StepTrailModel.kt` (murni: ubah daftar log menjadi item langkah dengan status)
- Test: `app/src/test/java/com/android/island/island/StepTrailModelTest.kt`

**Interfaces:**
- Produces: `object StepTrailModel { fun from(lines: List<String>, running: Boolean): List<Step> }` dengan `data class Step(val text: String, val state: State)` dan `enum class State { DONE, ACTIVE, FAILED }`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun lastLineIsActiveWhileRunning() {
    val steps = StepTrailModel.from(listOf("Langkah 1: Membuka TikTok", "Langkah 2: Scroll"), running = true)
    assertEquals(StepTrailModel.State.DONE, steps[0].state)
    assertEquals(StepTrailModel.State.ACTIVE, steps[1].state)
}
@Test fun failedLineIsMarked() {
    val steps = StepTrailModel.from(listOf("Langkah 1: Tidak menemukan \"Putar\""), running = false)
    assertEquals(StepTrailModel.State.FAILED, steps[0].state)
}
```

- [ ] **Step 2: Run test to verify it fails**
Expected: FAIL.

- [ ] **Step 3: Implement** `StepTrailModel.from`: baris dengan kata "Tidak", "Gagal", "belum", atau "dilewati" ditandai FAILED; baris terakhir ACTIVE jika `running`, selain itu DONE. Composable `StepTrail` menggambar garis vertikal Lilac dan titik; titik ACTIVE berdenyut kecuali pengaturan animasi sistem dimatikan (`Settings.Global.ANIMATOR_DURATION_SCALE == 0f`).
  Di `AiPanel`: header dengan status langkah aktif, jawaban asisten tanpa gelembung memakai `FontFamily.Serif`, tombol Berhenti menggantikan Kirim saat `busy`, chip aksi jadi satu baris teks dengan tinggi sentuh 48 dp, permukaan memakai `Modifier.liquidGlass` dan permukaan datar saat `RenderTier.LOW_END`.

- [ ] **Step 4: Run test to verify it passes**
Expected: PASS untuk `StepTrailModelTest`. Tampilan diverifikasi manual (Task 7).

### Task 7: Verifikasi keseluruhan

- [ ] **Step 1:** `./gradlew testDebugUnitTest`. Expected: semua test lulus, termasuk test lama.
- [ ] **Step 2:** `./gradlew assembleDebug`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 3:** Uji manual: ganti penyedia Gemini dan lihat model chat otomatis; minta "buatkan gambar kucing oranye di pantai"; cek jejak langkah, Berhenti, dan mode LOW_END.
- [ ] **Step 4:** Tulis `UPDATE-1.14.0-MODELS-IMAGES-UI.md` dan naikkan versi ke 1.14.0 (versionCode 130).
