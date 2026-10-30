package com.example.screencapturenative

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry

class ScreenCapturePlugin : FlutterPlugin, ActivityAware,
    MethodChannel.MethodCallHandler, PluginRegistry.ActivityResultListener {

    private val reqCapture = 4001
    private val reqPerms = 4002

    private var appCtx: Context? = null
    private var activity: Activity? = null
    private var binding: ActivityPluginBinding? = null
    private var method: MethodChannel? = null
    private var events: EventChannel? = null
    private var sink: EventChannel.EventSink? = null
    private var pendingResult: MethodChannel.Result? = null
    private var pendingQuality = "auto"

    private fun prefs() = appCtx!!.getSharedPreferences("recorder", Context.MODE_PRIVATE)

    // ---- FlutterPlugin ----
    override fun onAttachedToEngine(b: FlutterPlugin.FlutterPluginBinding) {
        appCtx = b.applicationContext
        method = MethodChannel(b.binaryMessenger, "screenrecorder/control").also { it.setMethodCallHandler(this) }
        events = EventChannel(b.binaryMessenger, "screenrecorder/status").also {
            it.setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, s: EventChannel.EventSink?) {
                    sink = s
                    RecordingService.listener = { m -> sink?.success(m) }
                }
                override fun onCancel(arguments: Any?) {
                    sink = null
                    RecordingService.listener = null
                }
            })
        }
    }

    override fun onDetachedFromEngine(b: FlutterPlugin.FlutterPluginBinding) {
        method?.setMethodCallHandler(null)
        events?.setStreamHandler(null)
        RecordingService.listener = null
        method = null; events = null; sink = null; appCtx = null
    }

    // ---- ActivityAware ----
    override fun onAttachedToActivity(b: ActivityPluginBinding) {
        binding = b
        activity = b.activity
        b.addActivityResultListener(this)
        askPermissions(b.activity)
    }

    override fun onDetachedFromActivityForConfigChanges() = detach()
    override fun onReattachedToActivityForConfigChanges(b: ActivityPluginBinding) = onAttachedToActivity(b)
    override fun onDetachedFromActivity() = detach()

    private fun detach() {
        binding?.removeActivityResultListener(this)
        binding = null
        activity = null
    }

    private fun askPermissions(a: Activity) {
        val need = ArrayList<String>()
        if (Build.VERSION.SDK_INT >= 33 &&
            a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) need.add(Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT in 23..28 &&
            a.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) need.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (need.isNotEmpty()) a.requestPermissions(need.toTypedArray(), reqPerms)
    }

    // ---- Dart calls ----
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        val ctx = appCtx ?: return result.error("detached", "No context", null)
        when (call.method) {
            "getQuality" -> result.success(prefs().getString("quality", "auto"))
            "setQuality" -> {
                prefs().edit().putString("quality", call.argument<String>("quality") ?: "auto").apply()
                result.success(null)
            }
            "plan" -> {
                val q = call.argument<String>("quality") ?: "auto"
                val plans = RecorderPlanner.candidates(ctx, q)
                result.success(mapOf(
                    "summary" to (plans.firstOrNull()?.summary ?: "No supported encoder found"),
                    "tier" to RecorderPlanner.tier(ctx).name,
                    "resolved" to RecorderPlanner.resolve(ctx, q).key
                ))
            }
            "status" -> result.success(RecordingService.state)
            "start" -> {
                val act = activity
                if (RecordingService.isRunning) { result.success(true); return }
                if (act == null || pendingResult != null) { result.success(false); return }
                pendingResult = result
                pendingQuality = call.argument<String>("quality") ?: "auto"
                val pm = act.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                @Suppress("DEPRECATION")
                act.startActivityForResult(pm.createScreenCaptureIntent(), reqCapture)
            }
            "stop" -> {
                ctx.startService(Intent(ctx, RecordingService::class.java).setAction(RecordingService.ACTION_STOP))
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != reqCapture) return false
        val result = pendingResult
        pendingResult = null
        val ctx = appCtx
        val act = activity
        if (resultCode == Activity.RESULT_OK && data != null && ctx != null) {
            val i = Intent(ctx, RecordingService::class.java).apply {
                action = RecordingService.ACTION_START
                putExtra(RecordingService.EXTRA_RESULT_CODE, resultCode)
                putExtra(RecordingService.EXTRA_RESULT_DATA, data)
                putExtra(RecordingService.EXTRA_QUALITY, pendingQuality)
            }
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
            result?.success(true)
            // Close the Flutter UI so its memory is freed while recording.
            Handler(Looper.getMainLooper()).postDelayed({ act?.finish() }, 250)
        } else {
            result?.success(false)
        }
        return true
    }
}
