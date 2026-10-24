package com.personal.mediacleaner

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentUris
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.content.pm.PackageManager
import android.Manifest
import java.security.MessageDigest

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("private_cleaner", MODE_PRIVATE) }
    private val requestRead = 501
    private val requestDelete = 502
    private var pendingCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (prefs.getString("pin_hash", null) == null) showPinSetup() else showUnlock()
    }

    private fun baseLayout(title: String, subtitle: String): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 48, 36, 36)
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF101114.toInt())
        }
        root.addView(TextView(this).apply {
            text = title; textSize = 27f; setTextColor(0xFFF4F5F7.toInt())
        })
        root.addView(TextView(this).apply {
            text = subtitle; textSize = 15f; setTextColor(0xFFB3B6BF.toInt())
            setPadding(0, 14, 0, 22)
        })
        setContentView(root)
        return root
    }

    private fun addButton(root: LinearLayout, label: String, action: () -> Unit) {
        root.addView(Button(this).apply {
            text = label
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 12 }
        })
    }

    private fun pinHash(pin: String): String =
        MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun showPinSetup() {
        val root = baseLayout("Private Media Cleaner", "Create an app PIN. This lock is separate from your phone lock.")
        val pin = EditText(this).apply {
            hint = "Create PIN (6+ characters)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val confirm = EditText(this).apply {
            hint = "Confirm PIN"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        root.addView(pin); root.addView(confirm)
        addButton(root, "Save PIN") {
            val a = pin.text.toString()
            if (a.length < 6) { toast("Use at least 6 characters."); return@addButton }
            if (a != confirm.text.toString()) { toast("PINs do not match."); return@addButton }
            prefs.edit().putString("pin_hash", pinHash(a)).apply()
            showHome()
        }
    }

    private fun showUnlock() {
        val root = baseLayout("Unlock app", "Enter your app PIN to continue.")
        val pin = EditText(this).apply {
            hint = "PIN"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        root.addView(pin)
        addButton(root, "Unlock") {
            if (pinHash(pin.text.toString()) == prefs.getString("pin_hash", "")) showHome()
            else toast("Incorrect PIN.")
        }
    }

    private fun showHome() {
        val root = baseLayout("Media cleaner", "Targets photos and videos only. No factory reset.")
        root.addView(TextView(this).apply {
            text = "WARNING: This can permanently delete photos and videos from shared media storage. Make a backup first. Android will show its own deletion confirmation. This app does not erase app-private files or guarantee removal from every folder or SD card."
            textSize = 16f
            setTextColor(0xFFFFB5B5.toInt())
        })
        addButton(root, "Scan photos and videos") { checkPermissionAndScan() }
        addButton(root, "Lock app") { showUnlock() }
    }

    private fun checkPermissionAndScan() {
        val permissions = if (Build.VERSION.SDK_INT >= 33) arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO
        ) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        val missing = permissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), requestRead)
        else confirmWipe()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestRead) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) confirmWipe()
            else toast("Media permission was not granted.")
        }
    }

    private fun confirmWipe() {
        AlertDialog.Builder(this)
            .setTitle("Review before deletion")
            .setMessage("The app will request deletion of all photos and videos visible in Android shared media storage. You will still see Android's system confirmation. This may be irreversible. Continue?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue") { _, _ -> requestSystemDelete() }
            .show()
    }

    private fun requestSystemDelete() {
        try {
            val uris = mutableListOf<android.net.Uri>()
            val collections = listOf(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
            for (collection in collections) {
                contentResolver.query(
                    collection,
                    arrayOf(MediaStore.MediaColumns._ID),
                    null, null, null
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    while (cursor.moveToNext()) {
                        uris.add(ContentUris.withAppendedId(collection, cursor.getLong(idCol)))
                    }
                }
            }
            if (uris.isEmpty()) {
                toast("No accessible photos or videos found.")
                return
            }
            pendingCount = uris.size
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, uris)
                startIntentSenderForResult(pendingIntent.intentSender, requestDelete, null, 0, 0, 0)
            } else {
                toast("Bulk system deletion requires Android 11 or newer.")
            }
        } catch (e: SecurityException) {
            toast("Android blocked this request. Check media permissions and try again.")
        } catch (e: Exception) {
            toast("Could not prepare deletion request.")
        }
    }

    @Deprecated("Deprecated by Android, retained for broad compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestDelete) {
            if (resultCode == RESULT_OK) toast("Android accepted the deletion request.")
            else toast("Deletion cancelled. No files were deleted by this app.")
        }
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
