package com.example.screenrecorder

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingService : Service() {

    companion object {
        const val ACTION_START = "com.example.screenrecorder.START"
        const val ACTION_STOP = "com.example.screenrecorder.STOP"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        private const val TAG = "RecordingService"
        private const val CHANNEL_ID = "recording_channel_quiet"
        private const val NOTIFICATION_ID = 1001
        private const val SUBFOLDER = "ScreenRecorder"

        // V2 uses MediaCodec directly so we can request CBR and configure the
        // encoder's operating rate instead of relying on MediaRecorder's
        // opaque encoder policy.
        private const val VIDEO_BITRATE = 24_000_000
        private const val VIDEO_FRAME_RATE = 60
        private const val I_FRAME_INTERVAL_SECONDS = 2

        @Volatile
        var isRunning = false
            private set
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var encoder: MediaCodec? = null
    private var encoderSurface: Surface? = null
    private var muxer: MediaMuxer? = null
    private var muxerStarted = false
    private var encoderThread: Thread? = null
    private var videoTrack = -1
    private var videoUri: Uri? = null
    private var outputPfd: ParcelFileDescriptor? = null
    private var legacyFile: File? = null
    private var legacyOutput: FileOutputStream? = null
    private var stopping = false

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            stopCapture()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (isRunning) return START_NOT_STICKY

                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE)
                val data = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                startAsForeground()
                if (resultCode == Activity.RESULT_OK && data != null) {
                    beginCapture(resultCode, data)
                } else {
                    showError("Screen capture permission was not granted")
                    stopCapture()
                }
            }
            ACTION_STOP -> stopCapture()
        }
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun beginCapture(resultCode: Int, data: Intent) {
        try {
            val projectionManager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
                ?: throw IllegalStateException("Could not create MediaProjection")
            mediaProjection!!.registerCallback(projectionCallback, mainHandler)

            val metrics = resources.displayMetrics
            val width = metrics.widthPixels and 1.inv()
            val height = metrics.heightPixels and 1.inv()
            val density = metrics.densityDpi
            require(width >= 2 && height >= 2) { "Invalid display size" }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "Recording_$timestamp.mp4"
            val output = openRecordingTarget(fileName)
                ?: throw IllegalStateException("Could not create output file")

            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BITRATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAME_RATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL_SECONDS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setInteger(MediaFormat.KEY_PRIORITY, 0)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setInteger(MediaFormat.KEY_OPERATING_RATE, VIDEO_FRAME_RATE)
                }
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoderSurface = encoder!!.createInputSurface()

            // Ask Android's SurfaceFlinger/producer path to treat this as a fixed
            // 60 FPS source. This is a pacing hint, not a promise from Android.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                encoderSurface!!.setFrameRate(
                    VIDEO_FRAME_RATE.toFloat(),
                    Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
                    Surface.CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS
                )
            }

            muxer = MediaMuxer(output, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            encoder!!.start()

            virtualDisplay = mediaProjection!!.createVirtualDisplay(
                "ScreenRecorderV2",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                encoderSurface,
                null,
                mainHandler
            ) ?: throw IllegalStateException("Could not create virtual display")

            isRunning = true
            stopping = false
            encoderThread = Thread({ drainEncoderUntilStopped() }, "ScreenRecorder-Encoder").apply { start() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            showError("Recording failed: ${e.message ?: e.javaClass.simpleName}")
            stopCapture()
        }
    }

    private fun drainEncoderUntilStopped() {
        val codec = encoder ?: return
        val bufferInfo = MediaCodec.BufferInfo()
        var sawEos = false

        try {
            while (!sawEos) {
                val index = codec.dequeueOutputBuffer(bufferInfo, 100_000)
                when {
                    index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (muxerStarted) throw IllegalStateException("Encoder format changed twice")
                        videoTrack = muxer!!.addTrack(codec.outputFormat)
                        muxer!!.start()
                        muxerStarted = true
                    }
                    index >= 0 -> {
                        val buffer = codec.getOutputBuffer(index)
                            ?: throw IllegalStateException("Encoder returned null output buffer")

                        if (bufferInfo.size > 0 && muxerStarted) {
                            buffer.position(bufferInfo.offset)
                            buffer.limit(bufferInfo.offset + bufferInfo.size)
                            muxer!!.writeSampleData(videoTrack, buffer, bufferInfo)
                        }

                        sawEos = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                        codec.releaseOutputBuffer(index, false)
                    }
                }
            }
        } catch (e: Exception) {
            if (!stopping) Log.e(TAG, "Encoder drain failed", e)
        }
    }

    private fun openRecordingTarget(fileName: String): FileDescriptor? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$SUBFOLDER")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = contentResolver.insert(collection, values) ?: return null
            videoUri = uri
            outputPfd = contentResolver.openFileDescriptor(uri, "rw")
            if (outputPfd == null) {
                contentResolver.delete(uri, null, null)
                videoUri = null
                return null
            }
            return outputPfd!!.fileDescriptor
        }

        @Suppress("DEPRECATION")
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            SUBFOLDER
        )
        if (!dir.exists() && !dir.mkdirs()) return null
        val file = File(dir, fileName)
        legacyFile = file
        legacyOutput = FileOutputStream(file)
        return legacyOutput!!.fd
    }

    private fun stopCapture() {
        if (stopping) return
        stopping = true

        try {
            isRunning = false

            // Stop the producer first so no more frames are submitted to the
            // encoder. Then signal EOS and drain the remaining encoded frames.
            virtualDisplay?.release()
            virtualDisplay = null

            try {
                encoder?.signalEndOfInputStream()
            } catch (e: Exception) {
                Log.w(TAG, "Could not signal encoder EOS", e)
            }

            encoderThread?.join(2_000L)
            encoderThread = null

            try {
                encoder?.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping encoder", e)
            }
            try {
                encoder?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing encoder", e)
            }
            encoder = null

            encoderSurface?.release()
            encoderSurface = null

            if (muxerStarted) {
                try { muxer?.stop() } catch (e: Exception) { Log.w(TAG, "Error stopping muxer", e) }
            }
            try { muxer?.release() } catch (e: Exception) { Log.w(TAG, "Error releasing muxer", e) }
            muxer = null
            muxerStarted = false
            videoTrack = -1

            try { mediaProjection?.unregisterCallback(projectionCallback) } catch (_: Exception) { }
            mediaProjection?.stop()
            mediaProjection = null

            finalizeOutput()
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopping = false
            stopSelf()
        }
    }

    private fun finalizeOutput() {
        try { legacyOutput?.fd?.sync() } catch (_: Exception) { }
        try { legacyOutput?.close() } catch (_: Exception) { }
        legacyOutput = null

        outputPfd?.let { try { it.close() } catch (_: Exception) { } }
        outputPfd = null

        videoUri?.let { uri ->
            try {
                contentResolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) },
                    null,
                    null
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to finalize MediaStore video", e)
            }
        }
        videoUri = null

        legacyFile?.let { file ->
            if (file.exists() && file.length() > 0L) {
                MediaScannerConnection.scanFile(this, arrayOf(file.absolutePath), arrayOf("video/mp4"), null)
            }
        }
        legacyFile = null
    }

    private fun showError(message: String) {
        mainHandler.post {
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Recording",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Screen recording status"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, RecordingService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentTitle("Screen recording")
            .setContentText("1080p60 • H.264 CBR • 24 Mbps")
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MIN)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .build()
    }

    override fun onDestroy() {
        if (isRunning || encoder != null || mediaProjection != null) stopCapture()
        super.onDestroy()
    }
}
