package com.example.connect

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.device.DeviceControlManager
import com.example.service.ScreenShareStateHolder
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import kotlin.random.Random

data class SharedFileItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long
)

data class PcConnectionState(
    val isServerRunning: Boolean = false,
    val serverUrl: String = "",
    val pairingPin: String = "",
    val qrBitmap: Bitmap? = null,
    val connectedClientInfo: String? = null,
    val lastActivityMs: Long = 0L,
    val errorMessage: String? = null,
    val sharedFiles: List<SharedFileItem> = emptyList()
)

class PcCompanionServer(
    private val context: Context,
    private val deviceControl: DeviceControlManager,
    private val onRemoteCommand: (String) -> Unit
) {
    companion object {
        private const val TAG = "PcCompanionServer"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    private val _state = MutableStateFlow(PcConnectionState())
    val state: StateFlow<PcConnectionState> = _state.asStateFlow()

    fun startServer() {
        if (_state.value.isServerRunning) return

        scope.launch {
            try {
                val socket = findAndBindPort()
                serverSocket = socket
                val port = socket.localPort
                val ip = getLocalIpAddress() ?: "127.0.0.1"
                val pin = (100000 + Random.nextInt(900000)).toString()
                val url = "http://$ip:$port/?pin=$pin"
                val qr = generateQrBitmap(url)

                _state.value = _state.value.copy(
                    isServerRunning = true,
                    serverUrl = url,
                    pairingPin = pin,
                    qrBitmap = qr,
                    errorMessage = null
                )

                acceptJob = scope.launch {
                    while (isActive && !socket.isClosed) {
                        val client = runCatching { socket.accept() }.getOrNull() ?: break
                        launch { handleClient(client) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start PC companion server", e)
                _state.value = _state.value.copy(
                    isServerRunning = false,
                    errorMessage = "PC সার্ভার চালু করতে সমস্যা হয়েছে: ${e.localizedMessage}"
                )
            }
        }
    }

    fun stopServer() {
        acceptJob?.cancel()
        runCatching { serverSocket?.close() }
        serverSocket = null
        _state.value = _state.value.copy(
            isServerRunning = false,
            serverUrl = "",
            pairingPin = "",
            qrBitmap = null,
            connectedClientInfo = null
        )
    }

    fun disconnectClient() {
        _state.value = _state.value.copy(connectedClientInfo = null)
        // Rotate PIN so previous session cannot reconnect without re-pairing
        stopServer()
    }

    fun addApprovedSharedFile(uri: Uri) {
        val resolver = context.contentResolver
        var name = "shared_file"
        var size = 0L
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }
        val item = SharedFileItem(uri = uri, displayName = name, sizeBytes = size)
        _state.value = _state.value.copy(
            sharedFiles = _state.value.sharedFiles + item
        )
    }

    fun removeSharedFile(id: String) {
        _state.value = _state.value.copy(
            sharedFiles = _state.value.sharedFiles.filterNot { it.id == id }
        )
    }

    private fun handleClient(socket: Socket) {
        socket.use { s ->
            val reader = BufferedReader(InputStreamReader(s.getInputStream()))
            val out = s.getOutputStream()
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0]
            val fullPath = parts[1]

            var contentLength = 0
            var userAgent = "PC Browser"
            var headerLine: String?
            while (reader.readLine().also { headerLine = it } != null) {
                val line = headerLine ?: break
                if (line.isEmpty()) break
                if (line.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                } else if (line.startsWith("User-Agent:", ignoreCase = true)) {
                    userAgent = line.substringAfter(":").trim()
                }
            }

            val pathOnly = fullPath.substringBefore("?")
            val queryParams = fullPath.substringAfter("?", "")
                .split("&")
                .mapNotNull { kv ->
                    val idx = kv.indexOf('=')
                    if (idx > 0) kv.substring(0, idx) to kv.substring(idx + 1) else null
                }
                .toMap()

            val expectedPin = _state.value.pairingPin
            val providedPin = queryParams["pin"]

            if (pathOnly == "/" && providedPin == expectedPin) {
                val clientIp = s.inetAddress.hostAddress ?: "PC"
                _state.value = _state.value.copy(
                    connectedClientInfo = "$clientIp (${userAgent.take(28)})",
                    lastActivityMs = System.currentTimeMillis()
                )
            }

            when {
                pathOnly == "/" -> {
                    if (providedPin != expectedPin) {
                        sendHtml(out, 403, "<html><body style='background:#070913;color:#fff;font-family:sans-serif;padding:40px'><h2>MAYA Pairing Required</h2><p>অনুগ্রহ করে ফোনের MAYA অ্যাপের QR কোড স্ক্যান করো অথবা সঠিক PIN দাও।</p></body></html>")
                    } else {
                        sendHtml(out, 200, buildCompanionHtml(expectedPin))
                    }
                }
                providedPin != expectedPin -> {
                    sendJson(out, 403, """{"error":"Invalid PIN"}""")
                }
                pathOnly == "/api/status" -> {
                    val t = deviceControl.collectTelemetry()
                    val filesJson = JSONArray().apply {
                        _state.value.sharedFiles.forEachIndexed { index, item ->
                            put(JSONObject().apply {
                                put("index", index)
                                put("name", item.displayName)
                                put("sizeBytes", item.sizeBytes)
                            })
                        }
                    }
                    val json = JSONObject().apply {
                        put("device", "${t.brand} ${t.model}")
                        put("android", t.androidVersion)
                        put("battery", t.battery.levelPercent)
                        put("charging", t.battery.isCharging)
                        put("storageFreeGb", String.format("%.1f", t.storage.freeGb))
                        put("network", if (t.network.isConnected) t.network.transportType else "Offline")
                        put("screenSharing", ScreenShareStateHolder.isSharing.value)
                        put("files", filesJson)
                    }
                    sendJson(out, 200, json.toString())
                }
                pathOnly == "/api/screen.jpg" -> {
                    val bytes = ScreenShareStateHolder.latestFrameBytes.value
                    if (bytes != null) {
                        sendBinary(out, 200, "image/jpeg", bytes)
                    } else {
                        sendJson(out, 404, """{"error":"Screen share inactive"}""")
                    }
                }
                pathOnly == "/api/command" && method == "POST" -> {
                    val bodyChars = CharArray(contentLength.coerceIn(0, 4096))
                    var read = 0
                    while (read < bodyChars.size) {
                        val r = reader.read(bodyChars, read, bodyChars.size - read)
                        if (r <= 0) break
                        read += r
                    }
                    val cmdJson = runCatching { JSONObject(String(bodyChars, 0, read)) }.getOrNull()
                    val cmd = cmdJson?.optString("command")?.trim().orEmpty()
                    if (cmd.isNotEmpty()) {
                        onRemoteCommand(cmd)
                        sendJson(out, 200, """{"ok":true}""")
                    } else {
                        sendJson(out, 400, """{"error":"Empty command"}""")
                    }
                }
                pathOnly == "/api/download" -> {
                    val idx = queryParams["index"]?.toIntOrNull()
                    val item = idx?.let { _state.value.sharedFiles.getOrNull(it) }
                    if (item == null) {
                        sendJson(out, 404, """{"error":"File not found"}""")
                    } else {
                        val bytes = context.contentResolver.openInputStream(item.uri)?.use { it.readBytes() }
                        if (bytes != null) {
                            val header = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Disposition: attachment; filename=\"${item.displayName}\"\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                            out.write(header.toByteArray())
                            out.write(bytes)
                            out.flush()
                        } else {
                            sendJson(out, 500, """{"error":"Unable to read file"}""")
                        }
                    }
                }
                else -> sendJson(out, 404, """{"error":"Not found"}""")
            }
        }
    }

    private fun buildCompanionHtml(pin: String): String = """
        <!DOCTYPE html>
        <html lang="bn">
        <head>
        <meta charset="utf-8"/>
        <meta name="viewport" content="width=device-width,initial-scale=1"/>
        <title>MAYA PC Companion</title>
        <style>
          body{background:#070913;color:#f8fafc;font-family:system-ui,sans-serif;margin:0;padding:24px;}
          .grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:18px;max-width:1100px;margin:auto;}
          .card{background:#12172e;border:1px solid rgba(255,255,255,.12);border-radius:18px;padding:18px;}
          h1{color:#00e5ff;margin:0 0 8px} h3{margin-top:0;color:#c4b5fd}
          input,button{padding:10px 14px;border-radius:10px;border:1px solid rgba(255,255,255,.18);background:#0b0f20;color:#fff;}
          button{background:linear-gradient(90deg,#ec4899,#8b5cf6);border:none;cursor:pointer;font-weight:600}
          img{max-width:100%;border-radius:12px;border:1px solid rgba(255,255,255,.15)}
          a{color:#38bdf8}
        </style>
        </head>
        <body>
          <h1>MAYA Phone ↔ PC Companion</h1>
          <p id="status">সংযুক্ত হচ্ছে...</p>
          <div class="grid">
            <div class="card">
              <h3>Remote Command</h3>
              <input id="cmd" placeholder="যেমন: ফ্ল্যাশলাইট চালু করো / ইউটিউব খোলো" style="width:72%"/>
              <button onclick="sendCmd()">পাঠাও</button>
              <p id="cmdRes"></p>
            </div>
            <div class="card">
              <h3>Approved File Transfer</h3>
              <ul id="files"></ul>
            </div>
            <div class="card">
              <h3>Live Screen Share</h3>
              <img id="screen" alt="Phone Screen Share inactive"/>
            </div>
          </div>
          <script>
            const pin="$pin";
            async function refresh(){
              try{
                const r=await fetch('/api/status?pin='+pin);
                const d=await r.json();
                document.getElementById('status').innerText=d.device+' • Battery '+d.battery+'% • Storage Free '+d.storageFreeGb+' GB • '+d.network;
                const ul=document.getElementById('files');
                ul.innerHTML=d.files.length?d.files.map(f=>'<li><a href="/api/download?pin='+pin+'&index='+f.index+'">'+f.name+'</a></li>').join(''):'<li>ফোন থেকে কোনো ফাইল শেয়ার করা হয়নি</li>';
                const img=document.getElementById('screen');
                if(d.screenSharing){ img.src='/api/screen.jpg?pin='+pin+'&t='+Date.now(); img.style.display='block'; }
                else{ img.removeAttribute('src'); }
              }catch(e){}
            }
            async function sendCmd(){
              const c=document.getElementById('cmd').value;
              if(!c)return;
              await fetch('/api/command?pin='+pin,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({command:c})});
              document.getElementById('cmdRes').innerText='কমান্ড পাঠানো হয়েছে: '+c;
              document.getElementById('cmd').value='';
            }
            refresh(); setInterval(refresh, 1500);
          </script>
        </body>
        </html>
    """.trimIndent()

    private fun sendHtml(out: OutputStream, code: Int, html: String) {
        val bytes = html.toByteArray(Charsets.UTF_8)
        sendBinary(out, code, "text/html; charset=utf-8", bytes)
    }

    private fun sendJson(out: OutputStream, code: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        sendBinary(out, code, "application/json; charset=utf-8", bytes)
    }

    private fun sendBinary(out: OutputStream, code: Int, mime: String, bytes: ByteArray) {
        val statusText = when (code) {
            200 -> "OK"
            403 -> "Forbidden"
            404 -> "Not Found"
            else -> "Error"
        }
        val headers = "HTTP/1.1 $code $statusText\r\nContent-Type: $mime\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
        out.write(headers.toByteArray())
        out.write(bytes)
        out.flush()
    }

    private fun findAndBindPort(): ServerSocket {
        for (port in 8765..8775) {
            runCatching { return ServerSocket(port) }
        }
        return ServerSocket(0)
    }

    private fun getLocalIpAddress(): String? {
        return runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { addr ->
                    !addr.isLoopbackAddress && addr is Inet4Address
                }?.hostAddress
        }.getOrNull()
    }

    private fun generateQrBitmap(content: String): Bitmap? {
        return runCatching {
            val size = 512
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        }.getOrNull()
    }
}
