package com.leon.htmlreader

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var root: View
    private lateinit var code: EditText
    private lateinit var status: TextView
    private lateinit var stats: TextView
    private lateinit var url: EditText
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var accountStatus: TextView
    private lateinit var authFields: View
    private lateinit var logoutBtn: Button
    private val executor = Executors.newSingleThreadExecutor()
    private val pick = 20
    private val save = 21
    private val prefs by lazy { getSharedPreferences("html_studio_auth", Context.MODE_PRIVATE) }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_main)
        root = findViewById(R.id.root); code = findViewById(R.id.codeView)
        status = findViewById(R.id.status); stats = findViewById(R.id.stats); url = findViewById(R.id.urlInput)
        username = findViewById(R.id.usernameInput); password = findViewById(R.id.passwordInput)
        accountStatus = findViewById(R.id.accountStatus); authFields = findViewById(R.id.authFields); logoutBtn = findViewById(R.id.logoutBtn)
        findViewById<Button>(R.id.pickBtn).setOnClickListener { pulse(it); pickFile() }
        findViewById<Button>(R.id.fetchBtn).setOnClickListener { pulse(it); fetch() }
        findViewById<Button>(R.id.saveBtn).setOnClickListener { pulse(it); saveFile() }
        findViewById<Button>(R.id.apkBtn).setOnClickListener { pulse(it); exportApkProject() }
        findViewById<Button>(R.id.clearBtn).setOnClickListener { pulse(it); code.setText(""); status.text="Đã xóa"; stats.text="Chưa có nội dung"; animateCards() }
        findViewById<Button>(R.id.loginBtn).setOnClickListener { pulse(it); login() }
        findViewById<Button>(R.id.registerBtn).setOnClickListener { pulse(it); register() }
        logoutBtn.setOnClickListener { pulse(it); logout() }
        updateAuthUi(); animateIn()
    }

    private fun hash(s: String): String = MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun userKey(u: String) = "user_${u.lowercase().trim()}"

    private fun register() {
        val u = username.text.toString().trim(); val p = password.text.toString()
        if (!u.matches(Regex("[A-Za-z0-9_]{3,24}"))) { accountStatus.text="Tên tài khoản 3–24 ký tự, chỉ chữ/số/_"; return }
        if (p.length < 6) { accountStatus.text="Mật khẩu cần ít nhất 6 ký tự"; return }
        if (prefs.contains(userKey(u))) { accountStatus.text="Tài khoản đã tồn tại trên thiết bị"; return }
        prefs.edit().putString(userKey(u), hash(p)).putString("session", u).apply()
        password.text.clear(); updateAuthUi(); accountStatus.text="Đăng ký thành công • đã đăng nhập"; toast("Tạo tài khoản thành công")
    }

    private fun login() {
        val u = username.text.toString().trim(); val p = password.text.toString()
        val saved = prefs.getString(userKey(u), null)
        if (saved == null || saved != hash(p)) { accountStatus.text="Sai tài khoản hoặc mật khẩu"; return }
        prefs.edit().putString("session", u).apply(); password.text.clear(); updateAuthUi(); accountStatus.text="Đăng nhập thành công"; toast("Chào mừng $u")
    }

    private fun logout() { prefs.edit().remove("session").apply(); username.text.clear(); password.text.clear(); updateAuthUi(); accountStatus.text="Đã đăng xuất" }

    private fun updateAuthUi() {
        val session = prefs.getString("session", null)
        if (session == null) { authFields.visibility=View.VISIBLE; logoutBtn.visibility=View.GONE; accountStatus.text="Đăng nhập để sử dụng đầy đủ tính năng" }
        else { authFields.visibility=View.GONE; logoutBtn.visibility=View.VISIBLE; accountStatus.text="Đang đăng nhập: $session" }
    }

    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    private fun animateIn(){root.alpha=0f;root.translationY=40f;root.animate().alpha(1f).translationY(0f).setDuration(650).setInterpolator(OvershootInterpolator(.8f)).start();animateCards()}
    private fun animateCards(){intArrayOf(R.id.badge,R.id.accountCard,R.id.title,R.id.actionCard,R.id.urlCard,R.id.statusCard,R.id.codeView).forEachIndexed{i,id->findViewById<View>(id).apply{alpha=0f;translationY=18f;animate().alpha(1f).translationY(0f).setStartDelay((i*45).toLong()).setDuration(360).setInterpolator(DecelerateInterpolator()).start()}}}
    private fun pulse(v:View){v.animate().scaleX(.96f).scaleY(.96f).setDuration(70).withEndAction{v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()}.start()}
    private fun pickFile(){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="text/html"},pick)}
    @Deprecated("compat") override fun onActivityResult(r:Int,c:Int,d:Intent?){super.onActivityResult(r,c,d);if(c!=Activity.RESULT_OK||d?.data==null)return;try{if(r==pick){contentResolver.openInputStream(d.data!!)?.use{code.setText(it.bufferedReader().readText())};updateStats("Đã mở file HTML");animateCards()}else if(r==save){contentResolver.openOutputStream(d.data!!)?.use{it.write(code.text.toString().toByteArray())};status.text="Đã lưu HTML"}}catch(e:Exception){status.text="Lỗi: ${e.message}"}}
    private fun fetch(){var s=url.text.toString().trim();if(s.isEmpty()){status.text="Nhập URL trước";return};if(!s.startsWith("http://")&&!s.startsWith("https://"))s="https://$s";status.text="Đang tải HTML…";stats.text="Kết nối website";findViewById<Button>(R.id.fetchBtn).isEnabled=false;executor.execute{try{val c=URL(s).openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=20000;c.instanceFollowRedirects=true;c.setRequestProperty("User-Agent","Mozilla/5.0 HTMLStudio/2.1");val response=c.responseCode;val body=c.inputStream.bufferedReader(Charsets.UTF_8).readText();c.disconnect();runOnUiThread{code.setText(body);status.text="Đã tải HTML • HTTP $response";updateStats("Kích thước");animateCards();findViewById<Button>(R.id.fetchBtn).isEnabled=true}}catch(e:Exception){runOnUiThread{status.text="Lỗi: ${e.message}";findViewById<Button>(R.id.fetchBtn).isEnabled=true}}}}
    private fun updateStats(prefix:String){stats.text="$prefix: ${code.text.length} ký tự • ${code.text.lines().size} dòng"}
    private fun saveFile(){if(code.text.isEmpty()){status.text="Chưa có HTML để lưu";return};startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="text/html";putExtra(Intent.EXTRA_TITLE,"index.html")},save)}
    private fun exportApkProject(){
        val html=code.text.toString(); if(html.isBlank()){status.text="Chưa có HTML để đóng gói";return}; status.text="Đang tạo project APK…";stats.text="Đang đóng gói HTML"
        executor.execute{try{val zip=File(cacheDir,"HTML_to_APK_Project.zip");val files=linkedMapOf<String,String>();files["settings.gradle.kts"]="""pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name="HTMLToAPK"; include(":app")""";files["build.gradle.kts"]="""plugins { id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false }""";files["gradle.properties"]="org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8\nandroid.useAndroidX=true\nkotlin.code.style=official\n";files["app/build.gradle.kts"]="""plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android { namespace="com.htmlstudio.generated"; compileSdk=35
 defaultConfig { applicationId="com.htmlstudio.generated"; minSdk=23; targetSdk=35; versionCode=1; versionName="1.0" }
}
dependencies { implementation("androidx.core:core-ktx:1.13.1"); implementation("androidx.appcompat:appcompat:1.7.0") }""";files["app/src/main/AndroidManifest.xml"]="""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"><uses-permission android:name="android.permission.INTERNET"/><application android:theme="@style/AppTheme" android:label="My HTML App"><activity android:name=".MainActivity" android:screenOrientation="portrait" android:exported="true"><intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter></activity></application></manifest>""";files["app/src/main/res/values/styles.xml"]="<resources><style name=\"AppTheme\" parent=\"Theme.AppCompat.DayNight.NoActionBar\"><item name=\"android:colorAccent\">#8D7AFF</item></style></resources>";files["app/src/main/java/com/htmlstudio/generated/MainActivity.kt"]="""package com.htmlstudio.generated
import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
class MainActivity: Activity(){override fun onCreate(b:Bundle?){super.onCreate(b);val w=WebView(this);w.settings.javaScriptEnabled=true;w.settings.domStorageEnabled=true;w.webViewClient=WebViewClient();w.loadUrl(\"file:///android_asset/index.html\");setContentView(w)}}""";files["README_BUILD.md"]="""# HTML → APK\nOpen in AndroidIDE/Android Studio, Sync Gradle and Build APK. HTML is bundled at app/src/main/assets/index.html.\n""";ZipOutputStream(zip.outputStream().buffered()).use{out->files.forEach{(n,data)->out.putNextEntry(ZipEntry(n));out.write(data.toByteArray());out.closeEntry()};out.putNextEntry(ZipEntry("app/src/main/assets/index.html"));out.write(html.toByteArray());out.closeEntry()};runOnUiThread{status.text="Đã tạo project HTML → APK";stats.text="ZIP: ${zip.length()/1024} KB";val uri=FileProvider.getUriForFile(this@MainActivity,"${packageName}.fileprovider",zip);startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="application/zip";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Lưu project APK"))}}catch(e:Exception){runOnUiThread{status.text="Lỗi: ${e.message}"}}}}
}
