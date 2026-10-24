# Template side (add to the wrapper that builds template.apk, then rebuild the template once)

The builder now writes this into assets/wzh_quick.json:
  "protect": { "enc":1, "k":"<base64, XOR sha256('wzh:'+packageName)>", "certSha":"<hex>", "secure":1, "noRoot":1 }
and, when enc=1, stores every site file as assets/<path>.wzp = nonce(12) + AES-GCM(ciphertext+tag), AAD = "<path>".

```kotlin
// 1) Read config + key
val p = JSONObject(assets.open("wzh_quick.json").bufferedReader().readText()).optJSONObject("protect")
fun key(): ByteArray {
    val m = MessageDigest.getInstance("SHA-256").digest("wzh:$packageName".toByteArray())
    val k = Base64.decode(p!!.getString("k"), Base64.DEFAULT)
    return ByteArray(32) { (k[it].toInt() xor m[it].toInt()).toByte() }
}

// 2) Decrypt for the WebView (use in shouldInterceptRequest; path = request path without leading "/")
fun readAsset(path: String): ByteArray {
    if (p?.optInt("enc") != 1) return assets.open(path).readBytes()
    val raw = assets.open("$path.wzp").readBytes()
    val c = Cipher.getInstance("AES/GCM/NoPadding")
    c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key(), "AES"), GCMParameterSpec(128, raw, 0, 12))
    c.updateAAD(path.toByteArray())
    return c.doFinal(raw, 12, raw.size - 12)
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
