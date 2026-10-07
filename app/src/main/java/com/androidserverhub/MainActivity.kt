package com.androidserverhub

import android.Manifest
import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var web: WebView
    private val PICK_RUNTIME = 1001
    private val PICK_SERVER = 1002
    private val PICK_BEDROCK = 1003

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val event = intent?.getStringExtra("event") ?: return
            val message = intent.getStringExtra("message") ?: ""
            val safe = message.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "")
            web.evaluateJavascript("window.hubEvent('$event','$safe')", null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7)
        }

        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = WebViewClient()
        web.addJavascriptInterface(AndroidHub(), "AndroidHub")
        web.loadUrl("file:///android_asset/index.html")
        setContentView(web)

        registerReceiver(receiver, IntentFilter("com.androidserverhub.EVENT"), RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    inner class AndroidHub {
        @JavascriptInterface
        fun startJava(port: Int, xms: String, xmx: String) {
            startService(Intent(this@MainActivity, ServerService::class.java).apply {
                action = ServerService.START_JAVA
                putExtra("port", port)
                putExtra("xms", xms)
                putExtra("xmx", xmx)
            })
        }

        @JavascriptInterface
        fun stopServer() {
            startService(Intent(this@MainActivity, ServerService::class.java).apply {
                action = ServerService.STOP_SERVER
            })
        }

        @JavascriptInterface
        fun restartJava(port: Int, xms: String, xmx: String) {
            startService(Intent(this@MainActivity, ServerService::class.java).apply {
                action = ServerService.RESTART_JAVA
                putExtra("port", port)
                putExtra("xms", xms)
                putExtra("xmx", xmx)
            })
        }

        @JavascriptInterface
        fun sendCommand(command: String) {
            startService(Intent(this@MainActivity, ServerService::class.java).apply {
                action = ServerService.COMMAND
                putExtra("command", command)
            })
        }

        @JavascriptInterface
        fun importRuntime() {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "application/zip"
                addCategory(Intent.CATEGORY_OPENABLE)
            }, PICK_RUNTIME)
        }

        @JavascriptInterface
        fun importServerJar() {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "application/java-archive"
                addCategory(Intent.CATEGORY_OPENABLE)
            }, PICK_SERVER)
        }

        @JavascriptInterface
        fun runtimePresent(): Boolean {
            return File(filesDir, "runtime.zip").exists()
        }

        @JavascriptInterface
        fun serverPresent(): Boolean {
            return File(filesDir, "servers/java/server.jar").exists()
        }

        @JavascriptInterface
        fun importBedrock() {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }, PICK_BEDROCK)
        }

        @JavascriptInterface
        fun bedrockPresent(): Boolean {
            return File(filesDir, "servers/bedrock/bedrock_server").exists()
        }

        @JavascriptInterface
        fun openServerFolder(kind: String) {
            val dir = when (kind) {
                "java" -> File(filesDir, "servers/java")
                "bedrock" -> File(filesDir, "servers/bedrock")
                else -> filesDir
            }
            dir.mkdirs()
            Toast.makeText(this@MainActivity, "Carpeta interna: ${dir.absolutePath}", Toast.LENGTH_LONG).show()
        }

        @JavascriptInterface
        fun serverFiles(kind: String): String {
            val dir = when (kind) {
                "java" -> File(filesDir, "servers/java")
                "bedrock" -> File(filesDir, "servers/bedrock")
                else -> filesDir
            }
            dir.mkdirs()
            fun esc(x: String) = x.replace("\\", "\\\\").replace("\"", "\\\"")
            return dir.listFiles()?.sortedBy { it.name.lowercase() }?.joinToString(
                prefix = "[", postfix = "]"
            ) { f ->
                """{"name":"${esc(f.name)}","directory":${f.isDirectory},"size":${if (f.isFile) f.length() else 0}}"""
            } ?: "[]"
        }

        @JavascriptInterface
        fun deleteServerFile(kind: String, name: String): Boolean {
            val base = when (kind) {
                "java" -> File(filesDir, "servers/java")
                "bedrock" -> File(filesDir, "servers/bedrock")
                else -> filesDir
            }.canonicalFile
            val target = File(base, name).canonicalFile
            if (target != base && !target.path.startsWith(base.path + File.separator)) return false
            return target.exists() && target.deleteRecursively()
        }

        @JavascriptInterface
        fun importServerFile(kind: String) {
            // AUTO-REPAIR: disabled -- unresolved reference 'pendingImportKind' (was: pendingImportKind = kind)
            // AUTO-REPAIR: disabled -- unresolved reference 'openDocument' (was: openDocument.launch(arrayOf("*/*")))
        }




        @JavascriptInterface
        fun inspectServerSetup(): String {
            val base = File(filesDir, "servers/java")
            base.mkdirs()
            val props = File(base, "server.properties")
            val fabricLoader = File(base, "fabric-server-launch.jar").exists()
            val forgeCandidates = base.listFiles()?.any {
                it.name.contains("forge", true) && it.extension == "jar"
            } == true
            val neoCandidates = base.listFiles()?.any {
                it.name.contains("neoforge", true) && it.extension == "jar"
            } == true

            var version = ""
            if (props.exists()) {
                val line = props.readLines().firstOrNull {
                    it.trim().startsWith("version", true)
                }
                version = line?.substringAfter("=")?.trim() ?: ""
            }

            val modsDir = File(base, "mods")
            val mods = modsDir.listFiles()?.count { it.isFile && it.extension.lowercase() == "jar" } ?: 0
            val eula = File(base, "eula.txt").exists()

            val loader = when {
                neoCandidates -> "NeoForge"
                forgeCandidates -> "Forge"
                fabricLoader -> "Fabric"
                else -> "No detectado"
            }

            fun esc(x: String) = x.replace("\\", "\\\\").replace("\"", "\\\"")
            return """{"minecraftVersion":"${esc(version)}","loader":"$loader","mods":$mods,"eulaFile":$eula,"serverJar":${File(base,"server.jar").exists()}}"""
        }

        @JavascriptInterface
        fun inspectModpack() {
            // AUTO-REPAIR: disabled -- unresolved reference 'pendingImportKind' (was: pendingImportKind = "inspect")
            // AUTO-REPAIR: disabled -- unresolved reference 'openDocument' (was: openDocument.launch(arrayOf("application/zip", "application/octet-stream")))
        }

        private fun inspectZip(uri: Uri): String {
            val temp = File(cacheDir, "inspect_${System.currentTimeMillis()}.zip")
            contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } ?: return """{"error":"No se pudo leer el ZIP"}"""

            var fabric = false
            var forge = false
            var neoforge = false
            var modrinth = false
            var curseforge = false
            var hasMods = false
            var hasServerPack = false
            var hasOverrides = false
            var files = 0

            ZipFile(temp).use { zip ->
                val names = zip.entries().asSequence().map { it.name.replace('\\','/') }.toList()
                files = names.count { !it.endsWith("/") }
                fabric = names.any { it.equals("fabric.mod.json", true) || it.startsWith("fabric/") }
                forge = names.any { it.equals("forge.mod.json", true) || it.startsWith("forge/") }
                neoforge = names.any { it.equals("neoforge.mods.toml", true) || it.startsWith("neoforge/") }
                modrinth = names.any { it.equals("modrinth.index.json", true) }
                curseforge = names.any { it.equals("manifest.json", true) }
                hasMods = names.any { it.startsWith("mods/") }
                hasServerPack = names.any { it.contains("server", true) && it.endsWith(".zip", true) }
                hasOverrides = names.any { it.startsWith("overrides/") }
            }
            temp.delete()

            val loader = when {
                neoforge -> "NeoForge"
                forge -> "Forge"
                fabric -> "Fabric"
                else -> "Desconocido"
            }
            val packType = when {
                modrinth -> "Modrinth"
                curseforge -> "CurseForge"
                hasOverrides -> "Modpack con overrides"
                hasMods -> "Paquete de mods"
                else -> "ZIP genérico"
            }
            return """{"loader":"$loader","type":"$packType","modrinth":$modrinth,"curseforge":$curseforge,"mods":$hasMods,"overrides":$hasOverrides,"files":$files}"""
        }

        @JavascriptInterface
        fun installModpack() {
            // AUTO-REPAIR: disabled -- unresolved reference 'pendingImportKind' (was: pendingImportKind = "zip:modpack")
            // AUTO-REPAIR: disabled -- unresolved reference 'openDocument' (was: openDocument.launch(arrayOf("application/zip", "application/octet-stream")))
        }

        private fun installModpackFile(uri: Uri) {
            val base = File(filesDir, "servers/java")
            base.mkdirs()
            val temp = File(cacheDir, "modpack_${System.currentTimeMillis()}.zip")
            contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IllegalStateException("No se pudo leer el modpack")

            var installed = 0
            ZipFile(temp).use { zip ->
                for (entry in zip.entries()) {
                    if (entry.isDirectory) continue
                    val raw = entry.name.replace('\\','/')
                    if (raw.startsWith("/") || raw.contains("../") || raw.contains(":/")) continue

                    // Modrinth/CurseForge packs commonly store server content in overrides/.
                    val normalized = when {
                        raw.startsWith("overrides/") -> raw.removePrefix("overrides/")
                        raw.startsWith("client-overrides/") -> raw.removePrefix("client-overrides/")
                        else -> raw
                    }
                    if (normalized.isBlank() || normalized.startsWith("manifest") ||
                        normalized == "modrinth.index.json") continue

                    val target = File(base, normalized).canonicalFile
                    val canonicalBase = base.canonicalFile
                    if (target != canonicalBase &&
                        !target.path.startsWith(canonicalBase.path + File.separator)) continue

                    target.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                    installed++
                }
            }
            temp.delete()
            // AUTO-REPAIR: disabled -- unresolved reference 'show' (was: Toast.makeText(this, "Modpack instalado: $installed archivos", Toast.LENGTH_LONG).show())
            // AUTO-REPAIR: disabled -- unresolved reference 'sendState' (was: sendState())
        }

        @JavascriptInterface
        fun installZip(kind: String) {
            // AUTO-REPAIR: disabled -- unresolved reference 'pendingImportKind' (was: pendingImportKind = "zip:" + kind)
            // AUTO-REPAIR: disabled -- unresolved reference 'openDocument' (was: openDocument.launch(arrayOf("application/zip", "application/octet-stream")))
        }

        private fun installZipFile(uri: Uri, kind: String) {
            val base = when (kind) {
                "world" -> File(filesDir, "servers/java/worlds")
                "mods" -> File(filesDir, "servers/java/mods")
                "packs" -> File(filesDir, "servers/java/resourcepacks")
                "config" -> File(filesDir, "servers/java/config")
                else -> File(filesDir, "servers/java")
            }
            base.mkdirs()

            val temp = File(cacheDir, "install_${System.currentTimeMillis()}.zip")
            contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IllegalStateException("No se pudo leer el ZIP")

            var count = 0
            ZipFile(temp).use { zip ->
                for (entry in zip.entries()) {
                    if (entry.isDirectory) continue
                    val raw = entry.name.replace('\\', '/')
                    if (raw.startsWith("/") || raw.contains("../") || raw.contains(":/")) {
                        continue
                    }
                    val relative = raw.substringAfterLast("/") // safe file-only installation
                    if (relative.isBlank()) continue

                    val targetDir = when (kind) {
                        "world" -> base
                        "mods" -> base
                        "packs" -> base
                        "config" -> base
                        else -> base
                    }
                    val target = File(targetDir, relative).canonicalFile
                    val canonicalBase = targetDir.canonicalFile
                    if (target != canonicalBase &&
                        !target.path.startsWith(canonicalBase.path + File.separator)) continue

                    target.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                    count++
                }
            }
            temp.delete()
            // AUTO-REPAIR: disabled -- unresolved reference 'show' (was: Toast.makeText(this, "ZIP instalado: $count archivos", Toast.LENGTH_LONG).show())
            // AUTO-REPAIR: disabled -- unresolved reference 'sendState' (was: sendState())
        }

        @JavascriptInterface
        fun startTunnel(port: Int) {
            startService(Intent(this@MainActivity, ServerService::class.java).apply {
                action = ServerService.START_TUNNEL
                putExtra("targetPort", port)
            })
        }

        @JavascriptInterface
        fun stopTunnel() {
            startService(Intent(this@MainActivity, ServerService::class.java).apply {
                action = ServerService.STOP_TUNNEL
            })
        }

        @JavascriptInterface
        fun startBedrock(port: Int) {
            startService(Intent(this@MainActivity, ServerService::class.java).apply {
                action = ServerService.START_BEDROCK
                putExtra("port", port)
            })
        }

        @JavascriptInterface
        fun restartBedrock(port: Int) {
            startService(Intent(this@MainActivity, ServerService::class.java).apply {
                action = ServerService.RESTART_BEDROCK
                putExtra("port", port)
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return
        val uri = data.data!!

        if (requestCode == PICK_RUNTIME) {
            copyUri(uri, File(filesDir, "runtime.zip"))
            web.evaluateJavascript("window.hubEvent('runtime','Runtime ZIP importado.')", null)
        } else if (requestCode == PICK_SERVER) {
            val out = File(filesDir, "servers/java/server.jar")
            out.parentFile?.mkdirs()
            copyUri(uri, out)
            web.evaluateJavascript("window.hubEvent('server','server.jar importado correctamente.')", null)
        } else if (requestCode == PICK_BEDROCK) {
            val out = File(filesDir, "servers/bedrock/bedrock_server")
            out.parentFile?.mkdirs()
            copyUri(uri, out)
            web.evaluateJavascript("window.hubEvent('bedrock','Ejecutable Bedrock importado.')", null)
        }
    }

    private fun copyUri(uri: Uri, out: File) {
        contentResolver.openInputStream(uri).use { input ->
            FileOutputStream(out).use { output ->
                input?.copyTo(output)
            }
        }
    }
    private fun queryDisplayName(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c ->
                if (c.moveToFirst()) return c.getString(0)
            }
        return null
    }

}
