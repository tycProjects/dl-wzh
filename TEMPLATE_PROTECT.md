# Template side (add to the wrapper that builds template.apk, then rebuild the template once)

The builder now writes this into assets/dexopt/.wc (moved out of assets/wzh_quick.json so it
doesn't read as an obvious config file -- assets/dexopt/ is reserved for the template, and a
name like .wc blends in with real bytecode-cache clutter):
  "protect": { "enc":1, "k":"<base64, XOR sha256('wzh:'+packageName)>", "certSha":"<hex>", "secure":1, "noRoot":1 }
and, when enc=1, stores every site file at an opaque hashed name (no extension, no original path):
  assets/<assetName(path)> = nonce(12) + AES-GCM(ciphertext+tag), AAD = "<path>"
assetName(path) = first 16 bytes of sha256("wzh:map:$packageName:$path"), as lowercase hex.
No path->name table is stored anywhere -- the template recomputes the same name from the
request path and its own package name, same as it already does for the content key.

For website-link builds there are no site files to encrypt, so when "Encrypt app content" is on the
destination URL is masked instead: assets/dexopt/.wc carries "u" (masked, base64) instead of a plain
"remoteUrl". "u" is present only when masking was on for that build; fall back to "remoteUrl" when
it isn't. This uses the same per-app mask as the key above, repeated to cover the whole string --
same caveat: it only stops casual unzipping, since the template derives the mask itself.

```kotlin
// 1) Read config + key
val p = JSONObject(assets.open("dexopt/.wc").bufferedReader().readText()).optJSONObject("protect")
fun key(): ByteArray {
    val m = MessageDigest.getInstance("SHA-256").digest("wzh:$packageName".toByteArray())
    val k = Base64.decode(p!!.getString("k"), Base64.DEFAULT)
    return ByteArray(32) { (k[it].toInt() xor m[it].toInt()).toByte() }
}

// 2) Decrypt for the WebView (use in shouldInterceptRequest; path = request path without leading "/")
fun assetName(path: String): String {
    val h = MessageDigest.getInstance("SHA-256").digest("wzh:map:$packageName:$path".toByteArray())
    return h.take(16).joinToString("") { "%02x".format(it) }
}
fun readAsset(path: String): ByteArray {
    if (p?.optInt("enc") != 1) return assets.open(path).readBytes()
    val raw = assets.open(assetName(path)).readBytes()
    val c = Cipher.getInstance("AES/GCM/NoPadding")
    c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key(), "AES"), GCMParameterSpec(128, raw, 0, 12))
    c.updateAAD(path.toByteArray())
    return c.doFinal(raw, 12, raw.size - 12)
}

// 2b) Website-link builds: read the (possibly masked) destination URL
val root = JSONObject(assets.open("dexopt/.wc").bufferedReader().readText())
fun remoteUrl(): String? {
    root.optString("u", "").let {
        if (it.isNotEmpty()) {
            val m = MessageDigest.getInstance("SHA-256").digest("wzh:$packageName".toByteArray())
            val b = Base64.decode(it, Base64.DEFAULT)
            return String(ByteArray(b.size) { i -> (b[i].toInt() xor m[i % m.size].toInt()).toByte() })
        }
    }
    return root.optString("remoteUrl", "").ifEmpty { null }
}

// 3) In onCreate, before setContentView
if (p?.optInt("secure") == 1) window.setFlags(FLAG_SECURE, FLAG_SECURE)
if (p != null && !signatureOk(p.getString("certSha"))) finishAndRemoveTask()
if (p?.optInt("noRoot") == 1 && (isRooted() || isDebugged() || isEmulator())) finishAndRemoveTask()

fun signatureOk(expected: String): Boolean {
    val sig = if (Build.VERSION.SDK_INT >= 28)
        packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo!!.apkContentsSigners[0]
    else @Suppress("DEPRECATION") packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures[0]
    val h = MessageDigest.getInstance("SHA-256").digest(sig.toByteArray())
    return h.joinToString("") { "%02x".format(it) } == expected
}
fun isDebugged() = Debug.isDebuggerConnected() || (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
fun isRooted() = listOf("/system/bin/su","/system/xbin/su","/sbin/su","/system/app/Superuser.apk").any { File(it).exists() } ||
    Build.TAGS?.contains("test-keys") == true
fun isEmulator() = Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator") || Build.HARDWARE.contains("goldfish")
```
