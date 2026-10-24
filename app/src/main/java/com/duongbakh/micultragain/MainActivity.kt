package com.duongbakh.micultragain

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Process
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import kotlin.concurrent.thread
import kotlin.math.sqrt

class MainActivity : Activity() {
    private var recorder: AudioRecord? = null
    @Volatile private var running = false
    @Volatile private var gainUi = 100
    @Volatile private var eqMode = "Giọng rõ"
    private lateinit var status: TextView
    private lateinit var meter: TextView
    private lateinit var gainLabel: TextView
    private lateinit var gainSeek: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(Color.rgb(7, 11, 20))
        buildUi()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 10)
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
        }
        val title = TextView(this).apply {
            text = "DƯƠNG BÁ KHÍ"
            textSize = 26f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
        }
        val subtitle = TextView(this).apply {
            text = "MIC ULTRA GAIN  •  VOICE LAB"
            textSize = 13f; setTextColor(Color.rgb(54, 199, 255))
        }
        root.addView(title)
        root.addView(subtitle)
        root.addView(space(20))
        root.addView(cardHeader("ULTRA GAIN"))
        gainLabel = TextView(this).apply {
            text = "GAIN: 100 / 1000"; textSize = 17f; setTextColor(Color.WHITE)
        }
        root.addView(gainLabel)
        gainSeek = SeekBar(this).apply {
            max = 999; progress = 99
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) {
                    gainUi = p + 1
                    gainLabel.text = "GAIN: ${p + 1} / 1000"
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        root.addView(gainSeek)
        root.addView(space(14))
        root.addView(cardHeader("EQ GIỌNG NÓI"))
        val eq = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                listOf("Giọng rõ", "Giọng ấm", "Giọng sáng", "Livestream"))
            setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    eqMode = listOf("Giọng rõ", "Giọng ấm", "Giọng sáng", "Livestream")[position]
                }
            })
        }
        root.addView(eq)
        root.addView(space(18))
        meter = TextView(this).apply {
            text = "MỨC MIC: --"; textSize = 18f; setTextColor(Color.rgb(54, 199, 255))
        }
        status = TextView(this).apply {
            text = "Sẵn sàng • Chưa nghe mic"; textSize = 14f; setTextColor(Color.LTGRAY
            )
        }
        root.addView(meter)
        root.addView(status)
        root.addView(space(18))
        val start = Button(this).apply {
            text = "BẮT ĐẦU NGHE THỬ"
            setOnClickListener { startMic() }
        }
        val stop = Button(this).apply {
            text = "DỪNG"
            setOnClickListener { stopMic() }
        }
        root.addView(start, LinearLayout.LayoutParams(-1, -2))
        root.addView(stop, LinearLayout.LayoutParams(-1, -2))
        root.addView(space(12))
        val note = TextView(this).apply {
            text = "Lưu ý: bản thử này đo mic và giao diện Gain/EQ. Chưa đưa âm thanh đã xử lý vào TikTok, Messenger, Discord hoặc cuộc gọi hệ thống."
            textSize = 12f; setTextColor(Color.rgb(180, 190, 205))
        }
        root.addView(note)
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun cardHeader(text: String) = TextView(this).apply {
        this.text = text
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(54, 199, 255))
        setPadding(0, 8, 0, 8)
    }

    private fun space(h: Int) = TextView(this).apply { height = h }

    private fun startMic() {
        if (running) return
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 10)
            status.text = "Hãy cấp quyền microphone rồi nhấn lại."
            return
        }
        val sampleRate = 44100
        val min = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) {
            status.text = "Thiết bị không hỗ trợ cấu hình mic này."
            return
        }
        try {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                min * 2
            )
            if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
                status.text = "Không khởi tạo được mic. Thử đóng app đang dùng mic."
                recorder?.release(); recorder = null; return
            }
            recorder?.startRecording()
            running = true
            status.text = "Đang đo mic • EQ: $eqMode"
            thread(name = "MicMeter") {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
                val buffer = ShortArray(min / 2)
                while (running) {
                    val n = recorder?.read(buffer, 0, buffer.size) ?: -1
                    if (n > 0) {
                        var sum = 0.0
                        val gain = 1.0 + ((gainUi - 1) / 999.0) * 7.0
                        for (i in 0 until n) {
                            val v = (buffer[i].toDouble() * gain).coerceIn(-32768.0, 32767.0)
                            sum += v * v
                        }
                        val rms = sqrt(sum / n).toInt()
                        val pct = (rms * 100 / 32768).coerceIn(0, 100)
                        runOnUiThread {
                            meter.text = "MỨC MIC: $pct%  •  GAIN ${gainUi}/1000"
                            status.text = if (pct > 90) "Âm rất lớn — giảm Gain để tránh méo tiếng." else "Đang nghe mic • EQ: $eqMode"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            running = false
            status.text = "Lỗi mic: ${e.message ?: "khongro"}"
        }
    }

    private fun stopMic() {
        running = false
        try { recorder?.stop() } catch (_: Exception) {}
        recorder?.release()
        recorder = null
        meter.text = "MỨC MIC: --"
        status.text = "Đã dừng nghe mic."
    }

    override fun onDestroy() {
        stopMic()
        super.onDestroy()
    }
}
