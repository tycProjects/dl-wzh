package com.vyrx.teacherstudio

import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONArray
import org.json.JSONObject

data class Question(val prompt:String, val answer:String, val explanation:String)

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("vyrx_teacher", MODE_PRIVATE) }
    private val questions = mutableListOf<Question>()
    private lateinit var listBox: LinearLayout
    private lateinit var status: TextView
    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) runOcr(uri) }

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); loadQuestions(); buildUi() }
    private fun dp(v:Int) = (v * resources.displayMetrics.density).toInt()
    private fun label(t:String, s:Float=15f) = TextView(this).apply { text=t; textSize=s; setTextColor(Color.WHITE) }
    private fun button(t:String, action:()->Unit) = Button(this).apply { text=t; isAllCaps=false; setOnClickListener { action() } }
    private fun gap() = View(this).apply { layoutParams=LinearLayout.LayoutParams(1,dp(12)) }

    private fun buildUi() {
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(20),dp(24),dp(20),dp(20)); setBackgroundColor(Color.rgb(15,13,25)) }
        val scroll=ScrollView(this)
        val content=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        scroll.addView(content); root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        content.addView(label("VYRX",30f).apply { setTypeface(null,Typeface.BOLD); setTextColor(Color.rgb(190,150,255)) })
        content.addView(label("TEACHER STUDIO  •  OFFLINE",12f).apply { setTextColor(Color.LTGRAY) })
        content.addView(gap())
        content.addView(label("Buat dan kelola bank soal",21f).apply { setTypeface(null,Typeface.BOLD) })
        content.addView(label("Mode guru: susun soal, kunci jawaban, pembahasan, dan pindai lembar soal tanpa mengirim data ke server.",14f))
        content.addView(gap())
        content.addView(button("＋  Buat soal baru") { showEditor() })
        content.addView(button("▧  Pindai soal dari gambar (OCR lokal)") { picker.launch("image/*") })
        content.addView(button("✦  Analisis soal offline") { analyzeAll() })
        content.addView(gap())
        status=label("Siap • ${questions.size} soal tersimpan di perangkat",13f).apply { setTextColor(Color.rgb(180,160,220)) }
        content.addView(status); content.addView(gap())
        content.addView(label("BANK SOAL",13f).apply { setTextColor(Color.rgb(190,150,255)); setTypeface(null,Typeface.BOLD) })
        listBox=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; content.addView(listBox)
        setContentView(root); renderQuestions()
    }

    private fun showEditor(prefill:String="") {
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(8),dp(8),dp(8),dp(8)) }
        val prompt=EditText(this).apply { hint="Tulis pertanyaan"; setText(prefill); minLines=3; gravity=Gravity.TOP }
        val answer=EditText(this).apply { hint="Kunci jawaban" }
        val explanation=EditText(this).apply { hint="Pembahasan / alasan"; minLines=2; gravity=Gravity.TOP }
        box.addView(prompt); box.addView(answer); box.addView(explanation)
        val dialog=android.app.AlertDialog.Builder(this).setTitle("Editor soal").setView(box).setNegativeButton("Batal",null).setPositiveButton("Simpan",null).create()
        dialog.setOnShowListener { dialog.getButton(-1).setOnClickListener {
            if(prompt.text.isBlank() || answer.text.isBlank()) Toast.makeText(this,"Isi pertanyaan dan kunci jawaban.",Toast.LENGTH_SHORT).show()
            else { questions.add(Question(prompt.text.toString(),answer.text.toString(),explanation.text.toString())); saveQuestions(); renderQuestions(); dialog.dismiss() }
        } }
        dialog.show()
    }

    private fun runOcr(uri:Uri) {
        status.text="Memindai gambar secara lokal…"
        try {
            val image=InputImage.fromFilePath(this,uri)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                .addOnSuccessListener { result -> status.text="OCR selesai • ${result.text.length} karakter terbaca"; showEditor(result.text) }
                .addOnFailureListener { status.text="OCR gagal: ${it.localizedMessage}" }
        } catch(e:Exception) { status.text="Gagal membuka gambar: ${e.localizedMessage}" }
    }

    private fun analyzeAll() {
        if(questions.isEmpty()) { Toast.makeText(this,"Tambahkan soal terlebih dahulu.",Toast.LENGTH_SHORT).show(); return }
        val mathCount=questions.count { Regex("\\d+\\s*[+\\-×x*/÷]\\s*\\d+").containsMatchIn(it.prompt) }
        val msg="Analisis offline selesai.\n\nJumlah soal: ${questions.size}\nPola hitung sederhana: $mathCount\nSoal lain: ${questions.size-mathCount}\n\nCatatan: ini pemeriksaan pola lokal, bukan model bahasa generatif. Guru tetap perlu memeriksa kunci dan pembahasan."
        android.app.AlertDialog.Builder(this).setTitle("Laporan analisis").setMessage(msg).setPositiveButton("Tutup",null).show()
    }

    private fun renderQuestions() {
        if(!::listBox.isInitialized) return
        listBox.removeAllViews()
        if(questions.isEmpty()) listBox.addView(label("Belum ada soal. Buat soal pertama untuk memulai.",14f))
        questions.forEachIndexed { i,q ->
            val card=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(12),dp(12),dp(12),dp(12)); setBackgroundColor(Color.rgb(35,29,49)) }
            card.addView(label("${i+1}. ${q.prompt}",16f).apply { setTypeface(null,Typeface.BOLD) })
            card.addView(label("Kunci: ${q.answer}",14f).apply { setTextColor(Color.rgb(195,175,235)) })
            if(q.explanation.isNotBlank()) card.addView(label("Pembahasan: ${q.explanation}",13f))
            card.addView(button("Hapus soal") { questions.removeAt(i); saveQuestions(); renderQuestions() })
            val lp=LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=dp(10); listBox.addView(card,lp)
        }
        if(::status.isInitialized) status.text="Siap • ${questions.size} soal tersimpan di perangkat"
    }
    private fun saveQuestions() {
        val arr=JSONArray(); questions.forEach { q -> arr.put(JSONObject().put("prompt",q.prompt).put("answer",q.answer).put("explanation",q.explanation)) }
        prefs.edit().putString("questions",arr.toString()).apply()
    }
    private fun loadQuestions() {
        try { val arr=JSONArray(prefs.getString("questions","[]")); for(i in 0 until arr.length()) { val o=arr.getJSONObject(i); questions.add(Question(o.optString("prompt"),o.optString("answer"),o.optString("explanation"))) } }
        catch(_:Exception) { questions.clear() }
    }
}
