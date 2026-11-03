package com.camstream.mobile

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.media.MediaMetadataRetriever
import android.text.format.Formatter
import java.text.SimpleDateFormat
import java.util.*

private val Bg = Color(0xFF0B0D13)
private val Panel = Color(0xFF151923)
private val Stroke = Color(0xFF292E3B)
private val Accent = Color(0xFFFF3655)
private val Muted = Color(0xFF9299AA)

class MainActivity : ComponentActivity() {
    private val micPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchProjection() else Toast.makeText(this, "إذن الميكروفون مطلوب لهذا الخيار", Toast.LENGTH_LONG).show()
    }
    private val projectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val svc = Intent(this, ScreenRecordService::class.java).apply {
                action = ScreenRecordService.ACTION_START
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data)
                putExtra("fps", prefs(this@MainActivity).getInt("fps", 30))
                putExtra("resolution", prefs(this@MainActivity).getString("resolution", "1080p"))
                putExtra("quality", prefs(this@MainActivity).getString("quality", "High"))
                putExtra("sound", prefs(this@MainActivity).getString("sound", "Mic"))
            }
            ContextCompat.startForegroundService(this, svc)
            Toast.makeText(this, "بدأ التسجيل", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CamApp(onRecord = { requestRecording() }) }
    }
    private fun requestRecording() {
        val sound = prefs(this).getString("sound", "Mic")
        if ((sound == "Mic" || sound == "Both") && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else launchProjection()
    }
    private fun launchProjection() {
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(mgr.createScreenCaptureIntent())
    }
}

private fun prefs(c: Context) = c.getSharedPreferences("cam_settings", Context.MODE_PRIVATE)

@Composable fun CamApp(onRecord: () -> Unit) {
    val ctx = LocalContext.current
    var page by remember { mutableStateOf("Record") }
    var recording by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }
    val p = prefs(ctx)
    var resolution by remember { mutableStateOf(p.getString("resolution", "1080p") ?: "1080p") }
    var fps by remember { mutableStateOf(p.getInt("fps", 30).toString()) }
    var quality by remember { mutableStateOf(p.getString("quality", "High") ?: "High") }
    var sound by remember { mutableStateOf(p.getString("sound", "Mic") ?: "Mic") }
    var camera by remember { mutableStateOf(p.getString("camera", "Off") ?: "Off") }
    var floating by remember { mutableStateOf(p.getString("floating", "Off") ?: "Off") }
    var countdown by remember { mutableStateOf(p.getString("countdown", "3 s") ?: "3 s") }
    var light by remember { mutableStateOf(p.getBoolean("light", false)) }
    var language by remember { mutableStateOf(p.getString("language", "English") ?: "English") }
    val bg = if (light) Color(0xFFF2F3F8) else Bg
    val panel = if (light) Color.White else Panel
    val fg = if (light) Color(0xFF171A23) else Color(0xFFF4F5FA)
    val recordings = remember(refresh) { loadVideos(ctx) }
    LaunchedEffect(recording, paused) {
        while (recording && !paused) { kotlinx.coroutines.delay(1000); seconds++ }
    }
    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                when(i?.action) {
                    ScreenRecordService.ACTION_STATE -> { recording = i.getBooleanExtra("recording", false); paused = i.getBooleanExtra("paused", false); if (!recording) { seconds = 0; refresh++ } }
                    ScreenRecordService.ACTION_FINISHED -> { recording = false; paused = false; seconds = 0; refresh++ }
                }
            }
        }
        val filter = android.content.IntentFilter().apply { addAction(ScreenRecordService.ACTION_STATE); addAction(ScreenRecordService.ACTION_FINISHED) }
        if (Build.VERSION.SDK_INT >= 33) ctx.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED) else @Suppress("DEPRECATION") ctx.registerReceiver(receiver, filter)
        onDispose { runCatching { ctx.unregisterReceiver(receiver) } }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = bg, surface = panel, onSurface = fg)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(bg, if(light) Color(0xFFE7EAF2) else Color(0xFF101C28))))) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal=22.dp, vertical=18.dp), verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).background(Accent, CircleShape), Alignment.Center) { Icon(Icons.Default.FiberManualRecord, null, tint=Color.White) }
                    Text("  CAM STREAM MOBILE", color=fg, fontSize=19.sp, fontWeight=FontWeight.ExtraBold, modifier=Modifier.weight(1f))
                    Surface(color=panel, shape=RoundedCornerShape(30.dp), border=androidx.compose.foundation.BorderStroke(1.dp, Stroke)) {
                        Text(if(recording) if(paused) "Paused" else "Recording" else "Ready", Modifier.padding(horizontal=14.dp, vertical=8.dp), color=if(recording) Accent else Muted, fontWeight=FontWeight.Bold, fontSize=12.sp)
                    }
                }
                when(page) {
                    "Record" -> LazyColumn(Modifier.weight(1f), contentPadding=PaddingValues(start=20.dp,end=20.dp,bottom=18.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
                        item {
                            Column(Modifier.fillMaxWidth().padding(vertical=12.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                                Box(Modifier.size(176.dp).background(Accent.copy(alpha=.09f), CircleShape).clickable { if(!recording) onRecord() }, Alignment.Center) {
                                    Box(Modifier.size(132.dp).background(Color(0xFF25202A), CircleShape), Alignment.Center) {
                                        Box(Modifier.size(104.dp).background(Accent, CircleShape), Alignment.Center) {
                                            Icon(if(recording) Icons.Default.Stop else Icons.Default.FiberManualRecord, null, tint=Color.White, modifier=Modifier.size(42.dp))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text("%02d:%02d".format(seconds/60, seconds%60), color=fg, fontSize=42.sp, fontWeight=FontWeight.Black)
                                Text(if(recording) if(paused) "Recording paused" else "Screen recording active" else "Tap to start recording", color=Muted)
                                Row(horizontalArrangement=Arrangement.spacedBy(10.dp), modifier=Modifier.padding(top=12.dp)) {
                                    Button(onClick={ if(!recording) onRecord() else serviceAction(ctx, ScreenRecordService.ACTION_STOP) }, colors=ButtonDefaults.buttonColors(containerColor=Accent)) { Icon(if(recording) Icons.Default.Stop else Icons.Default.FiberManualRecord, null); Text(if(recording) " Stop" else " Record") }
                                    OutlinedButton(onClick={ if(recording) { serviceAction(ctx, ScreenRecordService.ACTION_PAUSE); paused = !paused } }, enabled=recording) { Icon(if(paused) Icons.Default.PlayArrow else Icons.Default.Pause, null); Text(if(paused) " Resume" else " Pause") }
                                }
                            }
                        }
                        item { ChoiceCard("RESOLUTION", listOf("480p","720p","1080p","1440p","Native"), resolution, {resolution=it;p.edit().putString("resolution",it).apply()}, panel, fg) }
                        item { ChoiceCard("FRAME RATE", listOf("30","60","90","120"), fps, {fps=it;p.edit().putInt("fps",it.toInt()).apply()}, panel, fg) }
                        item { ChoiceCard("QUALITY", listOf("Standard","High","Ultra"), quality, {quality=it;p.edit().putString("quality",it).apply()}, panel, fg) }
                        item { ChoiceCard("SOUND", listOf("Off","Mic"), sound, {sound=it;p.edit().putString("sound",it).apply()}, panel, fg) }
                    }
                    "Videos" -> VideosPage(recordings, fg, panel, {refresh++}, Modifier.weight(1f))
                    "Camera" -> CameraPage(fg, panel, Modifier.weight(1f))
                    else -> SettingsPage(light, {light=it;p.edit().putBoolean("light",it).apply()}, language, {language=it;p.edit().putString("language",it).apply()}, fg, panel, {refresh++}, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth().padding(horizontal=18.dp, vertical=12.dp).background(panel, RoundedCornerShape(24.dp)).padding(6.dp), horizontalArrangement=Arrangement.SpaceAround) {
                    listOf("Record" to Icons.Default.FiberManualRecord, "Videos" to Icons.Default.VideoLibrary, "Camera" to Icons.Default.CameraAlt, "Settings" to Icons.Default.Settings).forEach { (name, icon) ->
                        Column(Modifier.weight(1f).clickable { page=name }.background(if(page==name) Color(0xFF293047) else Color.Transparent, RoundedCornerShape(17.dp)).padding(vertical=10.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                            Icon(icon, null, tint=if(page==name) Color.White else Muted)
                            Text(name, color=if(page==name) Color.White else Muted, fontSize=10.sp, fontWeight=FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
@Composable private fun ChoiceCard(title:String, options:List<String>, selected:String, onSelect:(String)->Unit, panel:Color, fg:Color) {
    Column(Modifier.fillMaxWidth().background(panel, RoundedCornerShape(22.dp)).padding(14.dp)) {
        Text(title, color=Muted, fontWeight=FontWeight.Bold, fontSize=13.sp, modifier=Modifier.padding(bottom=10.dp))
        Row(Modifier.fillMaxWidth().background(if(panel==Color.White) Color(0xFFF0F1F5) else Color(0xFF0D1018), RoundedCornerShape(16.dp)).padding(3.dp)) {
            options.forEach { option ->
                Box(Modifier.weight(1f).background(if(selected==option) Color(0xFF2B334B) else Color.Transparent, RoundedCornerShape(12.dp)).clickable { onSelect(option) }.padding(vertical=13.dp), Alignment.Center) {
                    Text(option, color=if(selected==option) Color.White else Muted, fontSize=12.sp, fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}
@Composable private fun VideosPage(videos:List<Uri>, fg:Color, panel:Color, refresh:()->Unit, modifier:Modifier=Modifier) {
    val ctx= LocalContext.current
    var selected by remember { mutableStateOf<Uri?>(null) }
    LazyColumn(modifier.padding(horizontal=20.dp), contentPadding=PaddingValues(bottom=20.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { Text("YOUR RECORDINGS", color=fg, fontSize=20.sp, fontWeight=FontWeight.Bold, modifier=Modifier.padding(vertical=14.dp)) }
        if(videos.isEmpty()) item { Text("No recordings yet. Your saved videos will appear here.", color=Muted, modifier=Modifier.fillMaxWidth().background(panel, RoundedCornerShape(18.dp)).padding(24.dp)) }
        items(videos) { uri ->
            val name=runCatching { queryName(ctx,uri) }.getOrDefault("Screen recording")
            Row(Modifier.fillMaxWidth().background(panel, RoundedCornerShape(16.dp)).clickable { selected=uri }.padding(14.dp), verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Default.PlayCircle, null, tint=Accent, modifier=Modifier.size(36.dp))
                Column(Modifier.weight(1f).padding(start=10.dp)) { Text(name, color=fg, fontWeight=FontWeight.Bold); Text("Tap to play • ${videoSize(ctx,uri)}", color=Muted, fontSize=12.sp) }
                IconButton(onClick={ val i=Intent(Intent.ACTION_SEND).setType("video/mp4").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); ctx.startActivity(Intent.createChooser(i,"Share video")) }) { Icon(Icons.Default.Share,null,tint=Muted) }
                IconButton(onClick={ selected=uri }) { Icon(Icons.Default.MoreVert,null,tint=Muted) }
            }
        }
    }
    selected?.let { uri ->
        AlertDialog(onDismissRequest={selected=null}, title={Text("Video actions")}, text={Text("Open or share this recording. To delete it, confirm below.")}, confirmButton={TextButton(onClick={ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));selected=null){Text("Play")}}, dismissButton={Row { TextButton(onClick={ctx.contentResolver.delete(uri,null,null);selected=null;refresh()}){Text("Delete",color=Accent)}; TextButton(onClick={selected=null}){Text("Close")} }})
    }
}
@Composable private fun CameraPage(fg:Color,panel:Color,modifier:Modifier=Modifier) {
    val ctx=LocalContext.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) }
    var front by remember { mutableStateOf(true) }
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted=it}
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("CAMERA", color=fg, fontSize=22.sp, fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if(!granted) Button(onClick={launcher.launch(Manifest.permission.CAMERA)}){Text("Grant camera permission")}
        else CameraPreview(front, Modifier.weight(1f).fillMaxWidth())
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Button(onClick={front=true},modifier=Modifier.weight(1f)){Text("Front")}
            OutlinedButton(onClick={front=false},modifier=Modifier.weight(1f)){Text("Back")}
        }
    }
}
@Composable private fun CameraPreview(front:Boolean, modifier:Modifier) {
    val ctx=LocalContext.current; val owner=LocalLifecycleOwner.current
    AndroidView(modifier=modifier, factory={ c ->
        val view=PreviewView(c); val provider=ProcessCameraProvider.getInstance(c)
        provider.addListener({ runCatching { val p=provider.get(); val preview=Preview.Builder().build().also{it.setSurfaceProvider(view.surfaceProvider)}; p.unbindAll(); p.bindToLifecycle(owner,if(front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA,preview) } },ContextCompat.getMainExecutor(c)); view
    }, update={ view ->
        val provider=ProcessCameraProvider.getInstance(ctx)
        provider.addListener({runCatching {val p=provider.get();val preview=Preview.Builder().build().also{it.setSurfaceProvider(view.surfaceProvider)};p.unbindAll();p.bindToLifecycle(owner,if(front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA,preview)}},ContextCompat.getMainExecutor(ctx))
    })
}
@Composable private fun SettingsPage(light:Boolean,onLight:(Boolean)->Unit,language:String,onLanguage:(String)->Unit,fg:Color,panel:Color,refresh:()->Unit,modifier:Modifier=Modifier) {
    val ctx=LocalContext.current
    LazyColumn(modifier.fillMaxSize().padding(horizontal=20.dp),contentPadding=PaddingValues(bottom=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text("SETTINGS",color=fg,fontSize=22.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(vertical=12.dp)) }
        item { Row(Modifier.fillMaxWidth().background(panel,RoundedCornerShape(16.dp)).clickable{onLight(!light)}.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Text("Light theme",color=fg,modifier=Modifier.weight(1f));Switch(light,onLight)} }
        item { Row(Modifier.fillMaxWidth().background(panel,RoundedCornerShape(16.dp)).padding(18.dp),verticalAlignment=Alignment.CenterVertically){Text("Language",color=fg,modifier=Modifier.weight(1f));TextButton(onClick={onLanguage(if(language=="English") "العربية" else "English")}){Text(language)} } }
        item { SettingsAction("Storage usage",fg,panel){ Toast.makeText(ctx,"Saved in Movies/CamStreamMobile",Toast.LENGTH_LONG).show() } }
        item { SettingsAction("Clear cache",fg,panel){ val n=ctx.cacheDir.listFiles()?.sumOf{it.length()}?:0;ctx.cacheDir.deleteRecursively();ctx.cacheDir.mkdirs();Toast.makeText(ctx,"Cache cleared (${Formatter.formatFileSize(ctx,n)})",Toast.LENGTH_LONG).show() } }
        item { SettingsAction("Privacy policy",fg,panel){ Toast.makeText(ctx,"Videos stay on your device. Camera and microphone are used only after permission.",Toast.LENGTH_LONG).show() } }
        item { SettingsAction("App information",fg,panel){ Toast.makeText(ctx,"CAM STREAM MOBILE 1.0.0",Toast.LENGTH_LONG).show() } }
        item { SettingsAction("Reset settings",fg,panel){ prefs(ctx).edit().clear().apply();Toast.makeText(ctx,"Settings reset. Restart app to apply all defaults.",Toast.LENGTH_LONG).show() } }
    }
}
@Composable private fun SettingsAction(title:String,fg:Color,panel:Color,action:()->Unit){Row(Modifier.fillMaxWidth().background(panel,RoundedCornerShape(16.dp)).clickable{action()}.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Text(title,color=fg,modifier=Modifier.weight(1f));Icon(Icons.Default.ChevronRight,null,tint=Muted)}}
private fun serviceAction(ctx:Context,action:String){ctx.startService(Intent(ctx,ScreenRecordService::class.java).setAction(action))}
private fun loadVideos(ctx:Context):List<Uri>{val out=mutableListOf<Uri>();val c=ctx.contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,arrayOf(MediaStore.Video.Media._ID), "${MediaStore.Video.Media.RELATIVE_PATH}=?", arrayOf("Movies/CamStreamMobile/"),"${MediaStore.Video.Media.DATE_ADDED} DESC");c?.use{while(it.moveToNext())out+=Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,it.getLong(0).toString())};return out}
private fun queryName(ctx:Context,u:Uri):String{ctx.contentResolver.query(u,arrayOf(MediaStore.Video.Media.DISPLAY_NAME),null,null,null)?.use{if(it.moveToFirst())return it.getString(0)};return "Recording"}
private fun videoSize(ctx:Context,u:Uri):String{ctx.contentResolver.query(u,arrayOf(MediaStore.Video.Media.SIZE),null,null,null)?.use{if(it.moveToFirst())return Formatter.formatFileSize(ctx,it.getLong(0))};return "Video"}
