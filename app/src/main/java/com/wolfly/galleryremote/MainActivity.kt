package com.wolfly.galleryremote

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.net.NetworkInterface
import java.util.Collections

class MainActivity : Activity() {
    private val port = 8765
    private val pin = (100000..999999).random().toString()
    private var server: GalleryServer? = null
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply { textSize = 16f }
        val start = Button(this).apply {
            text = "Start authorized gallery sharing"
            setOnClickListener { requestGalleryPermission() }
        }
        val stop = Button(this).apply {
            text = "Stop sharing"
            setOnClickListener { stopSharing() }
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 48, 28, 28)
            addView(TextView(this@MainActivity).apply { text = "Wolfly Gallery Remote"; textSize = 25f })
            addView(TextView(this@MainActivity).apply { text = "Sharing stays OFF until you explicitly start it. Keep this app open while sharing." })
            addView(start); addView(stop); addView(status)
        }
        setContentView(layout)
        status.text = "Sharing is OFF."
    }

    private fun requestGalleryPermission() {
        val permissions = when {
            Build.VERSION.SDK_INT >= 34 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        ActivityCompat.requestPermissions(this, permissions, 41)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == 41 && hasImagePermission()) startSharing()
        else status.text = "Permission not granted. Sharing remains OFF."
    }

    private fun hasImagePermission(): Boolean {
        val p = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED ||
            (Build.VERSION.SDK_INT >= 34 && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED)
    }

    private fun startSharing() {
        if (!hasImagePermission()) return
        stopSharing()
        server = GalleryServer(port, pin)
        try {
            server?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            status.text = "SHARING ON\nOn the viewer phone, open:\nhttp://${wifiIp()}:$port\n\nPIN: $pin\n\nSame Wi-Fi only. Keep this app open."
        } catch (e: Exception) {
            status.text = "Could not start server: ${e.message}"
            server = null
        }
    }

    private fun stopSharing() {
        server?.stop(); server = null
        if (::status.isInitialized) status.text = "Sharing is OFF."
    }

    private fun wifiIp(): String {
        for (intf in Collections.list(NetworkInterface.getNetworkInterfaces())) {
            for (addr in Collections.list(intf.inetAddresses)) {
                val host = addr.hostAddress ?: continue
                if (!addr.isLoopbackAddress && !host.contains(":")) return host
            }
        }
        return "PHONE_IP"
    }

    private fun listImages(): JSONArray {
        val result = JSONArray()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)
        contentResolver.query(collection, projection, null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC")?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            while (c.moveToNext()) result.put(JSONObject().put("id", c.getLong(idCol)).put("name", c.getString(nameCol) ?: "Photo"))
        }
        return result
    }

    private fun imageBytes(id: Long): Pair<ByteArray, String>? {
        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        return try {
            contentResolver.openInputStream(uri)?.use { input -> Pair(input.readBytes(), contentResolver.getType(uri) ?: "image/jpeg") }
        } catch (_: Exception) { null }
    }

    inner class GalleryServer(port: Int, private val secret: String) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            val path = session.uri
            if (path == "/" || path == "/index.html") return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", dashboard)
            if (session.headers["x-gallery-pin"] != secret) return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "application/json", "{\"error\":\"PIN required\"}")
            if (path == "/api/photos") return newFixedLengthResponse(Response.Status.OK, "application/json", listImages().toString())
            if (path.startsWith("/api/image/")) {
                val id = path.substringAfterLast("/").toLongOrNull() ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Bad ID")
                val item = imageBytes(id) ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Unavailable")
                return newFixedLengthResponse(Response.Status.OK, item.second, ByteArrayInputStream(item.first), item.first.size.toLong())
            }
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
        }
    }

    private val dashboard = """
<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><title>Wolfly Gallery</title>
<style>body{margin:0;background:#111;color:#fff;font:16px Arial;padding:20px}h1{color:#ff78a8}input,button{padding:12px;border:0;border-radius:8px;margin:5px}button{background:#ff568e;color:white}#grid{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-top:20px}img{width:100%;aspect-ratio:1;object-fit:cover;border-radius:8px}</style></head>
<body><h1>Wolfly Gallery ❤️</h1><p>Enter the PIN displayed on the sharing phone.</p><input id="pin" inputmode="numeric" placeholder="6-digit PIN"><button onclick="loadGallery()">Open gallery</button><p id="status"></p><div id="grid"></div>
<script>async function loadGallery(){const pin=document.getElementById('pin').value,s=document.getElementById('status'),g=document.getElementById('grid');g.innerHTML='';s.textContent='Connecting...';try{const r=await fetch('/api/photos',{headers:{'x-gallery-pin':pin}});if(!r.ok)throw Error('Incorrect PIN or sharing unavailable');const items=await r.json();s.textContent=items.length+' photos available';for(const item of items){const img=document.createElement('img');img.alt=item.name;img.loading='lazy';img.src='/api/image/'+item.id;g.appendChild(img)}}catch(e){s.textContent=e.message}}</script></body></html>
""".trimIndent()

    override fun onDestroy() { stopSharing(); super.onDestroy() }
}
