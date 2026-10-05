package com.example.smoothcamera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.Range
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.HighSpeedVideoSessionConfig
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.example.smoothcamera.camera.CameraCapabilities
import com.example.smoothcamera.media.MediaStoreRepository
import com.example.smoothcamera.processing.ColorProfileProcessor
import com.example.smoothcamera.settings.SettingsActivity
import com.example.smoothcamera.utils.Ui
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var cameraLayer: FrameLayout
    private lateinit var topBar: LinearLayout
    private lateinit var bottomBar: LinearLayout
    private lateinit var modeLabel: TextView
    private lateinit var statusLabel: TextView
    private lateinit var shutter: TextView
    private lateinit var zoomLabel: TextView
    private lateinit var flashButton: TextView
    private lateinit var profileButton: TextView

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var media: MediaStoreRepository
    private lateinit var capabilities: CameraCapabilities
    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var cameraFacing = CameraSelector.LENS_FACING_BACK
    private var flashMode = ImageCapture.FLASH_MODE_AUTO
    private var currentMode = Mode.PHOTO
    private var selectedFps = 30
    private var selectedQuality = Quality.FHD
    private var selectedProfile = ColorProfileProcessor.Profile.NATURAL
    private var highSpeed = false
    private var isCameraReady = false
    private var pinchBaseZoom = 1f

    private enum class Mode { PHOTO, VIDEO, SLOW_MO }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else showPermissionDialog()
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) showError("Microphone access is required for video with audio")
        else Toast.makeText(this, "Microphone enabled. Tap record again.", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        media = MediaStoreRepository(this)
        capabilities = CameraCapabilities(this)
        selectedProfile = runCatching { ColorProfileProcessor.Profile.valueOf(prefs.getString("profile", "NATURAL")!!) }.getOrDefault(ColorProfileProcessor.Profile.NATURAL)
        buildUi()
        if (hasCameraPermission()) startCamera() else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    override fun onDestroy() {
        recording?.stop()
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
        super.onDestroy()
    }

    private fun buildUi() {
        cameraLayer = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        previewView = PreviewView(this).apply {
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            setBackgroundColor(Color.BLACK)
        }
        cameraLayer.addView(previewView, FrameLayout.LayoutParams(-1, -1))

        topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10, 18, 10, 8)
        }
        flashButton = Ui.iconButton(this, "⚡", "Flash")
        profileButton = Ui.iconButton(this, "✦", "Color profile")
        val settings = Ui.iconButton(this, "⋯", "Settings")
        topBar.addView(flashButton, LinearLayout.LayoutParams(0, 56, 1f))
        topBar.addView(profileButton, LinearLayout.LayoutParams(0, 56, 1f))
        topBar.addView(settings, LinearLayout.LayoutParams(0, 56, 1f))
        cameraLayer.addView(topBar, FrameLayout.LayoutParams(-1, 90, Gravity.TOP))

        statusLabel = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 12f
            alpha = 0.86f
            gravity = Gravity.CENTER
            setPadding(14, 7, 14, 7)
            setBackgroundColor(0x66000000)
        }
        val statusLp = FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = 88 }
        cameraLayer.addView(statusLabel, statusLp)

        zoomLabel = Ui.pill(this, "1×")
        zoomLabel.setOnClickListener { setZoom(if ((camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f) < 1.5f) 2f else 1f) }
        cameraLayer.addView(zoomLabel, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL).apply { topMargin = 105 })

        bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(18, 12, 18, 22)
            setBackgroundColor(0x30000000)
        }
        val modes = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        listOf("PHOTO", "VIDEO", "SLOW-MO").forEach { label ->
            val v = Ui.pill(this, label)
            v.setOnClickListener { selectMode(label) }
            modes.addView(v, LinearLayout.LayoutParams(-2, -2).apply { setMargins(5, 0, 5, 0) })
        }
        modeLabel = TextView(this).apply { text = "PHOTO"; visibility = View.GONE }
        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val gallery = Ui.iconButton(this, "▣", "Latest media")
        gallery.setOnClickListener { openGallery() }
        shutter = TextView(this).apply {
            text = "●"
            setTextColor(Color.WHITE)
            textSize = 72f
            gravity = Gravity.CENTER
            includeFontPadding = false
            isClickable = true
            isFocusable = true
            setOnClickListener { onShutter() }
        }
        val switch = Ui.iconButton(this, "↻", "Switch camera")
        switch.setOnClickListener { switchCamera() }
        controls.addView(gallery, LinearLayout.LayoutParams(0, 78, 1f))
        controls.addView(shutter, LinearLayout.LayoutParams(110, 92))
        controls.addView(switch, LinearLayout.LayoutParams(0, 78, 1f))
        bottomBar.addView(modes, LinearLayout.LayoutParams(-1, 45))
        bottomBar.addView(controls, LinearLayout.LayoutParams(-1, 100))
        cameraLayer.addView(bottomBar, FrameLayout.LayoutParams(-1, 160, Gravity.BOTTOM))

        setContentView(cameraLayer)
        flashButton.setOnClickListener { cycleFlash() }
        profileButton.setOnClickListener { chooseProfile() }
        settings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        installGestures()
    }

    private fun installGestures() {
        val detector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                pinchBaseZoom = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
                return true
            }
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val state = camera?.cameraInfo?.zoomState?.value ?: return false
                val target = (pinchBaseZoom * detector.scaleFactor).coerceIn(state.minZoomRatio, state.maxZoomRatio)
                setZoom(target)
                return true
            }
        })
        var lastTap = 0L
        previewView.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                if (System.currentTimeMillis() - lastTap < 280) {
                    setZoom(if ((camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f) < 1.5f) 2f else 1f)
                    lastTap = 0L
                } else {
                    lastTap = System.currentTimeMillis()
                    focusAt(event.x, event.y)
                }
            }
            true
        }
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            runCatching {
                cameraProvider = future.get()
                bindCamera()
            }.onFailure { showError("Camera could not start") }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCamera() {
        val provider = cameraProvider ?: return
        val selector = CameraSelector.Builder().requireLensFacing(cameraFacing).build()
        if (!provider.hasCamera(selector)) {
            Toast.makeText(this, "This camera is unavailable on this device.", Toast.LENGTH_SHORT).show()
            cameraFacing = if (cameraFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
            return
        }

        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val provisionalCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(flashMode)
            .build()
        val baseSession = SessionConfig(preview, provisionalCapture)
        val info = provider.getCameraInfo(selector, baseSession)
        val imageCapabilities = ImageCapture.getImageCaptureCapabilities(info)
        val useUltraHdr = prefs.getBoolean("hdr", false) &&
            imageCapabilities.getSupportedOutputFormats().contains(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
        val captureBuilder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(flashMode)
            .setJpegQuality(95)
        if (useUltraHdr) captureBuilder.setOutputFormat(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
        val capture = captureBuilder.build()
        imageCapture = capture
        val preferredPreviewFps = CameraCapabilities.supportedFps(info, baseSession)
            .filter { it.upper <= 60 && it.lower == it.upper }
            .maxByOrNull { it.upper }
            ?: Range(30, 30)

        val session = SessionConfig.Builder(preview, capture)
            .setFrameRateRange(preferredPreviewFps)
            .build()
        runCatching {
            camera = provider.bindToLifecycle(this, selector, session)
            camera?.cameraControl?.setLinearZoom(0f)
            isCameraReady = true
            statusLabel.text = "${preferredPreviewFps.upper} FPS PREVIEW"
            updateFlashUi()
        }.onFailure {
            runCatching {
                provider.unbindAll()
                camera = provider.bindToLifecycle(this, selector, preview, capture)
                isCameraReady = true
                statusLabel.text = "AUTO PREVIEW"
                updateFlashUi()
            }.onFailure { showError("Camera configuration is not supported") }
        }
    }

    private fun selectMode(label: String) {
        if (recording != null) return
        currentMode = when (label) { "VIDEO" -> Mode.VIDEO; "SLOW-MO" -> Mode.SLOW_MO; else -> Mode.PHOTO }
        if (currentMode == Mode.PHOTO) {
            bindCamera()
            shutter.text = "●"
        } else {
            configureVideo()
            shutter.text = "■"
        }
    }

    private fun configureVideo() {
        val provider = cameraProvider ?: return
        val selector = CameraSelector.Builder().requireLensFacing(cameraFacing).build()
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val provisionalInfo = provider.getCameraInfo(selector)
        val caps = CameraCapabilities.highSpeedCapabilities(provisionalInfo)
        val fpsPreference = prefs.getString("fps", "Auto") ?: "Auto"
        val requestedFps = fpsPreference.toIntOrNull() ?: if (currentMode == Mode.SLOW_MO) 120 else 60
        highSpeed = caps != null && (currentMode == Mode.SLOW_MO || requestedFps >= 120)
        selectedFps = requestedFps
        selectedQuality = when (prefs.getString("resolution", "Auto")) {
            "4K" -> Quality.UHD
            "1080p" -> Quality.FHD
            "720p" -> Quality.HD
            else -> selectedQuality
        }

        val videoCapabilities = if (caps != null && highSpeed) caps else Recorder.getVideoCapabilities(provisionalInfo)
        val hdrRequested = prefs.getBoolean("hdr", false)
        val dynamicRange = if (hdrRequested && videoCapabilities.getSupportedDynamicRanges().contains(androidx.camera.core.DynamicRange.HLG_10_BIT)) {
            androidx.camera.core.DynamicRange.HLG_10_BIT
        } else androidx.camera.core.DynamicRange.SDR
        val qualityList = videoCapabilities.getSupportedQualities(dynamicRange)
        if (qualityList.isEmpty()) { showError("No compatible video quality was reported"); return }
        if (!qualityList.contains(selectedQuality)) selectedQuality = qualityList.firstOrNull { it == Quality.FHD } ?: qualityList.first()

        val qualitySelector = QualitySelector.fromOrderedList(
            listOf(selectedQuality) + qualityList.filterNot { it == selectedQuality } + listOf(Quality.HIGHEST),
            FallbackStrategy.higherQualityOrLowerThan(selectedQuality)
        )
        val recorder = Recorder.Builder().setQualitySelector(qualitySelector).build()
        val videoBuilder = VideoCapture.Builder(recorder)
            .setDynamicRange(dynamicRange)
            .setMirrorMode(if (cameraFacing == CameraSelector.LENS_FACING_FRONT) androidx.camera.core.MirrorMode.MIRROR_MODE_ON else androidx.camera.core.MirrorMode.MIRROR_MODE_OFF)
        val stabilizationEnabled = prefs.getBoolean("stabilization", true) && Recorder.getVideoCapabilities(provisionalInfo).isStabilizationSupported()
        if (stabilizationEnabled) videoBuilder.setVideoStabilizationEnabled(true)
        val vc = videoBuilder.build()
        videoCapture = vc

        if (highSpeed) {
            val builder = HighSpeedVideoSessionConfig.Builder(vc).setPreview(preview).setSlowMotionEnabled(currentMode == Mode.SLOW_MO)
            val ranges = runCatching { provisionalInfo.getSupportedFrameRateRanges(builder.build()) }.getOrDefault(emptySet())
            val chosen = ranges.filter { it.upper >= 120 }.maxByOrNull { it.upper } ?: ranges.maxByOrNull { it.upper }
            if (chosen == null) { highSpeed = false; configureNormalVideo(provider, selector, preview, vc, requestedFps.coerceAtMost(60)); return }
            selectedFps = chosen.upper
            builder.setFrameRateRange(chosen)
            bindVideoSession(provider, selector, builder.build(), preview)
        } else {
            configureNormalVideo(provider, selector, preview, vc, requestedFps.coerceAtMost(60))
        }
    }

    private fun configureNormalVideo(provider: ProcessCameraProvider, selector: CameraSelector, preview: Preview, vc: VideoCapture<Recorder>, requested: Int) {
        val base = SessionConfig(preview, vc)
        val info = provider.getCameraInfo(selector, base)
        val ranges = CameraCapabilities.supportedFps(info, base)
        val exact = ranges.firstOrNull { it.lower == requested && it.upper == requested }
        val fallback = ranges.filter { it.lower == it.upper && it.upper <= requested }.maxByOrNull { it.upper }
        val chosen = exact ?: fallback ?: ranges.maxByOrNull { it.upper }
        if (chosen == null) { bindVideoSession(provider, selector, base, preview); selectedFps = 30; return }
        selectedFps = chosen.upper
        bindVideoSession(provider, selector, SessionConfig.Builder(preview, vc).setFrameRateRange(chosen).build(), preview)
    }

    private fun bindVideoSession(provider: ProcessCameraProvider, selector: CameraSelector, session: SessionConfig, preview: Preview) {
        runCatching {
            camera = provider.bindToLifecycle(this, selector, session)
            statusLabel.text = if (highSpeed) "$selectedFps FPS • SLOW-MO" else "$selectedFps FPS • VIDEO"
        }.onFailure {
            showError("Requested video configuration is unavailable; using a safe fallback")
            runCatching { provider.unbindAll(); camera = provider.bindToLifecycle(this, selector, preview) }
        }
    }

    private fun onShutter() {
        if (!isCameraReady) return
        if (currentMode == Mode.PHOTO) capturePhoto() else if (recording == null) startRecording() else stopRecording()
    }

    private fun capturePhoto() {
        val capture = imageCapture ?: return
        val temp = File.createTempFile("smooth_capture_", ".jpg", cacheDir)
        val output = ImageCapture.OutputFileOptions.Builder(temp).build()
        shutter.animate().scaleX(0.88f).scaleY(0.88f).setDuration(55).withEndAction {
            shutter.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()
        capture.takePicture(output, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                runOnUiThread { showError("Photo capture failed") }
                temp.delete()
            }
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                lifecycleScope.launch(Dispatchers.Default) {
                    val result = runCatching {
                        if (selectedProfile == ColorProfileProcessor.Profile.NATURAL) {
                            media.copyFileToImage(temp, timestamp("IMG") + ".jpg")
                        } else {
                            val bitmap = ColorProfileProcessor.process(temp, selectedProfile)
                            try { media.saveBitmap(bitmap, timestamp("IMG") + ".jpg") } finally { bitmap.recycle() }
                        }
                    }.getOrElse { media.copyFileToImage(temp, timestamp("IMG") + ".jpg") }
                    temp.delete()
                    withContext(Dispatchers.Main) {
                        if (result != null) Toast.makeText(this@MainActivity, "Saved to Pictures/SmoothCamera", Toast.LENGTH_SHORT).show()
                        else showError("Unable to save photo")
                    }
                }
            }
        })
    }

    private fun startRecording() {
        val vc = videoCapture ?: return
        val uri = media.insertPendingVideo(timestamp("VID") + ".mp4")
        if (uri == null) { showError("Unable to prepare gallery storage"); return }
        var pending = vc.output.prepareRecording(this, MediaStoreOutputOptions.Builder(contentResolver, uri).build())
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            pending = pending.withAudioEnabled()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            media.deleteVideo(uri)
            return
        }
        recording = pending.start(ContextCompat.getMainExecutor(this)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    shutter.setTextColor(0xFFFF4D4D.toInt())
                    statusLabel.text = "● REC  ${selectedFps} FPS"
                }
                is VideoRecordEvent.Finalize -> {
                    recording = null
                    shutter.setTextColor(Color.WHITE)
                    if (event.hasError()) {
                        media.deleteVideo(uri)
                        showError("Recording failed")
                    } else {
                        media.finishVideo(uri)
                        Toast.makeText(this, "Video saved", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun stopRecording() { recording?.stop() }

    private fun switchCamera() {
        if (recording != null) return
        cameraFacing = if (cameraFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        bindCamera()
    }

    private fun cycleFlash() {
        flashMode = when (flashMode) {
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_AUTO
        }
        imageCapture?.flashMode = flashMode
        updateFlashUi()
    }

    private fun updateFlashUi() {
        flashButton.text = when (flashMode) {
            ImageCapture.FLASH_MODE_AUTO -> "⚡A"
            ImageCapture.FLASH_MODE_ON -> "⚡"
            else -> "⚡×"
        }
        flashButton.alpha = if (camera?.cameraInfo?.hasFlashUnit() == true) 1f else 0.45f
    }

    private fun focusAt(x: Float, y: Float) {
        val point = previewView.meteringPointFactory.createPoint(x, y)
        camera?.cameraControl?.startFocusAndMetering(
            androidx.camera.core.FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                .build()
        )
    }

    private fun setZoom(value: Float) {
        camera?.cameraControl?.setZoomRatio(value)
        zoomLabel.text = String.format(Locale.US, "%.1f×", value).replace(".0", "")
    }

    private fun chooseProfile() {
        val names = ColorProfileProcessor.Profile.values().map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Color profile").setSingleChoiceItems(names, selectedProfile.ordinal) { dialog, which ->
            selectedProfile = ColorProfileProcessor.Profile.values()[which]
            profileButton.text = "✦"
            dialog.dismiss()
        }.show()
    }

    private fun selectModeBySettings() { }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_VIEW).apply { type = "image/*"; data = "content://media/external/images/media".toUri() }
        runCatching { startActivity(intent) }.onFailure { Toast.makeText(this, "No gallery app found", Toast.LENGTH_SHORT).show() }
    }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Camera access needed")
            .setMessage("Smooth Camera needs camera access to show the preview and capture photos. Microphone access is requested only when recording audio.")
            .setPositiveButton("Open settings") { _, _ -> startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$packageName"))) }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showError(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }

    private fun timestamp(prefix: String): String = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
}
