package com.sensigreen.ffoverlay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val ComponentActivity.dataStore by preferencesDataStore(name = "sensigreen_settings")
private val Neon = Color(0xFF7CFF4E)
private val Bg = Color(0xFF080D0B)
private val Panel = Color(0xFF141C18)
private val Muted = Color(0xFF9EADA4)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SensiGreenApp(this) }
    }
}

@Composable
private fun SensiGreenApp(activity: MainActivity) {
    val scope = rememberCoroutineScope()
    var savedPin by remember { mutableStateOf("123") }
    var loggedIn by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var page by remember { mutableStateOf("Home") }
    var overlayOn by remember { mutableStateOf(false) }
    var crosshairOn by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(true) }
    var crosshairSize by remember { mutableFloatStateOf(18f) }
    var opacity by remember { mutableFloatStateOf(0.85f) }
    var crosshairColor by remember { mutableStateOf("Merah") }
    var profile by remember { mutableStateOf("Drag Headshot") }
    val sensNames = listOf("General", "Red Dot", "2x Scope", "4x Scope", "Sniper Scope", "Free Look", "Drag Sensitivity", "Touch Response", "Camera Movement", "Aim Comfort", "Gyroscope", "Custom")
    val sens = remember { mutableStateMapOf<String, Float>().apply { sensNames.forEach { put(it, 100f) } } }
    val anime = listOf("Gojo Satoru", "Gojo Six Eyes", "Gojo Blindfold", "Yuji Itadori", "Megumi Fushiguro", "Ryomen Sukuna", "Killua Zoldyck", "Gon Freecss", "Naruto Uzumaki", "Sasuke Uchiha", "Kakashi Hatake", "Monkey D. Luffy", "Roronoa Zoro", "Ichigo Kurosaki")
    var favorite by remember { mutableStateOf("Gojo Satoru") }

    LaunchedEffect(Unit) {
        val prefs = activity.dataStore.data.first()
        savedPin = prefs[stringPreferencesKey("pin")] ?: "123"
        sensNames.forEach { name -> sens[name] = prefs[stringPreferencesKey("sens_$name")]?.toFloatOrNull() ?: 100f }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Neon, background = Bg, surface = Panel, onPrimary = Color.Black, onBackground = Color.White, onSurface = Color.White)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
            if (!loggedIn) {
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(Modifier.size(112.dp).clip(CircleShape).background(Neon.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                        Text("五", fontSize = 54.sp, color = Neon, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(22.dp))
                    Text("SENSIGREEN", fontSize = 29.sp, color = Color.White, fontWeight = FontWeight.Black)
                    Text("FF OVERLAY PRO MAX", color = Neon, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(28.dp))
                    Text("LOGIN PLAYER", color = Muted)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = pin, onValueChange = { pin = it; error = "" }, label = { Text("Password / PIN") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { showPin = !showPin }) { Icon(if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Tampilkan password") } }, modifier = Modifier.fillMaxWidth())
                    if (error.isNotEmpty()) Text(error, color = Color(0xFFFF6868), modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { if (pin == savedPin) loggedIn = true else error = "PIN salah. Coba lagi." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Neon), shape = RoundedCornerShape(14.dp)) { Text("LOGIN", color = Color.Black, fontWeight = FontWeight.Black) }
                    Spacer(Modifier.height(12.dp))
                    Text("PIN awal: 123  •  Simpan pengaturan lokal", color = Muted, fontSize = 12.sp)
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("SENSIGREEN", color = Neon, fontWeight = FontWeight.Black, fontSize = 21.sp)
                            Text("PRO MAX  •  v1.0.0", color = Muted, fontSize = 11.sp)
                        }
                        Box(Modifier.size(38.dp).clip(CircleShape).background(Neon.copy(alpha = .16f)), contentAlignment = Alignment.Center) { Text("五", color = Neon, fontSize = 22.sp, fontWeight = FontWeight.Black) }
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Home", "Sensi", "Aim", "Overlay", "Anime", "Settings").forEach { item ->
                            val selected = page == item
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (selected) Neon else Panel).clickable { page = item }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(item, color = if (selected) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    when (page) {
                        "Home" -> LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                            item { Box(Modifier.fillMaxWidth().height(158.dp).clip(RoundedCornerShape(20.dp)).background(Neon.copy(alpha = .12f)).padding(18.dp), contentAlignment = Alignment.CenterStart) { Column { Text("DOMAIN EXPANSION", color = Muted, fontSize = 11.sp); Text("ANIME GAMING MODE", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black); Text("Aim • Sensi • Overlay", color = Neon, fontSize = 13.sp); Text("Favorit: $favorite", color = Muted, fontSize = 12.sp) } } }
                            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatusCard("Overlay", if (overlayOn) "AKTIF" else "MATI", Modifier.weight(1f)); StatusCard("Crosshair", if (crosshairOn) "AKTIF" else "MATI", Modifier.weight(1f)) } }
                            item { SectionTitle("QUICK ACTION") }
                            item { Button(onClick = { if (Settings.canDrawOverlays(activity)) { overlayOn = true; activity.startService(Intent(activity, FloatingOverlayService::class.java)) } else { activity.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${activity.packageName}"))) } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Neon)) { Icon(Icons.Default.Layers, null, tint = Color.Black); Spacer(Modifier.width(8.dp)); Text(if (overlayOn) "OVERLAY AKTIF" else "MULAI OVERLAY", color = Color.Black, fontWeight = FontWeight.Black) } }
                            item { OutlinedButton(onClick = { overlayOn = false; activity.stopService(Intent(activity, FloatingOverlayService::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("STOP OVERLAY") } }
                            item { Button(onClick = { val intent = activity.packageManager.getLaunchIntentForPackage("com.dts.freefireth") ?: activity.packageManager.getLaunchIntentForPackage("com.dts.freefiremax"); if (intent != null) activity.startActivity(intent) else page = "Tools" }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Panel)) { Icon(Icons.Default.SportsEsports, null); Spacer(Modifier.width(8.dp)); Text("BUKA FREE FIRE") } }
                            item { SectionTitle("PROFIL AKTIF") }
                            item { ProfilePicker(profile) { profile = it } }
                            item { Text("Catatan: profil menyimpan rekomendasi nilai, bukan mengubah pengaturan internal game otomatis.", color = Muted, fontSize = 12.sp) }
                        }
                        "Sensi" -> LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                            item { SectionTitle("SENSITIVITY LAB") }
                            items(sensNames) { name -> Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).padding(14.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Text("${sens[name]?.toInt() ?: 100}", color = Neon, fontWeight = FontWeight.Black) }; Slider(value = sens[name] ?: 100f, onValueChange = { sens[name] = it }, valueRange = 0f..200f, colors = SliderDefaults.colors(thumbColor = Neon, activeTrackColor = Neon)) } }
                            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { sensNames.forEach { sens[it] = 100f } }, modifier = Modifier.weight(1f)) { Text("RESET") }; Button(onClick = { scope.launch { activity.dataStore.edit { prefs -> sensNames.forEach { prefs[stringPreferencesKey("sens_$it")] = (sens[it] ?: 100f).toString() } }; error = "" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Neon)) { Text("SIMPAN", color = Color.Black) } } }
                            item { OutlinedButton(onClick = { val lines = sensNames.joinToString("\n") { "$it=${sens[it]?.toInt() ?: 100}" }; val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "SensiGreen Profile: $profile\n$lines"); activity.startActivity(Intent.createChooser(send, "Ekspor profil")) }, modifier = Modifier.fillMaxWidth()) { Text("EKSPOR PROFIL") } }
                        }
                        "Aim" -> LazyColumn(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            item { SectionTitle("AIM & CROSSHAIR") }
                            item { Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF101815)), contentAlignment = Alignment.Center) { Text(if (crosshairOn) if (crosshairColor == "Merah") "⊕" else "＋" else "PREVIEW OFF", color = when (crosshairColor) { "Merah" -> Color.Red; "Hijau" -> Neon; "Biru" -> Color.Cyan; "Kuning" -> Color.Yellow; else -> Color.White }, fontSize = (crosshairSize * 2).sp, fontWeight = FontWeight.Black) } }
                            item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Aktifkan crosshair", Modifier.weight(1f)); Switch(checked = crosshairOn, onCheckedChange = { crosshairOn = it }) } }
                            item { Text("Warna: $crosshairColor", color = Neon); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Merah", "Hijau", "Biru", "Putih", "Kuning").forEach { c -> FilterChip(selected = crosshairColor == c, onClick = { crosshairColor = c }, label = { Text(c) }) } } }
                            item { Text("Ukuran", color = Color.White); Slider(value = crosshairSize, onValueChange = { crosshairSize = it }, valueRange = 5f..30f) }
                            item { Text("Transparansi overlay: ${(opacity * 100).toInt()}%", color = Color.White); Slider(value = opacity, onValueChange = { opacity = it }, valueRange = .2f..1f) }
                            item { Text("Crosshair ini hanya panduan visual. Preview dalam aplikasi; overlay game bergantung pada dukungan Android.", color = Muted, fontSize = 12.sp) }
                        }
                        "Overlay" -> LazyColumn(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            item { SectionTitle("FLOATING OVERLAY") }
                            item { StatusCard("Izin Android", if (Settings.canDrawOverlays(activity)) "DIIZINKAN" else "BELUM DIIZINKAN", Modifier.fillMaxWidth()) }
                            item { Button(onClick = { activity.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${activity.packageName}"))) }, modifier = Modifier.fillMaxWidth()) { Text("BUKA IZIN OVERLAY") } }
                            item { Button(onClick = { if (Settings.canDrawOverlays(activity)) { overlayOn = true; activity.startService(Intent(activity, FloatingOverlayService::class.java)) } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Neon)) { Text("MULAI OVERLAY", color = Color.Black) } }
                            item { OutlinedButton(onClick = { overlayOn = false; activity.stopService(Intent(activity, FloatingOverlayService::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("HENTIKAN OVERLAY") } }
                            item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Posisi tombol overlay", Modifier.weight(1f)); Text(if (locked) "LOCK" else "UNLOCK", color = Neon) } }
                            item { Button(onClick = { locked = !locked }, modifier = Modifier.fillMaxWidth()) { Text(if (locked) "UNLOCK POSISI" else "LOCK POSISI") } }
                            item { Text("LOCK/UNLOCK mengatur perpindahan tombol overlay, bukan mengunci target di game.", color = Muted, fontSize = 12.sp) }
                        }
                        "Anime" -> LazyColumn(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            item { SectionTitle("ANIME WALLPAPER GALLERY") }
                            item { Text("Pilih karakter tema. Kartu ini adalah pilihan tema; gambar aset khusus dapat ditambahkan ke folder drawable.", color = Muted, fontSize = 12.sp) }
                            items(anime) { character -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (favorite == character) Neon.copy(alpha = .13f) else Panel).clickable { favorite = character }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF172B22)), contentAlignment = Alignment.Center) { Text(if (character.contains("Gojo")) "五" else "★", color = Neon, fontSize = 24.sp, fontWeight = FontWeight.Black) }; Spacer(Modifier.width(12.dp)); Text(character, Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.SemiBold); if (favorite == character) Icon(Icons.Default.Favorite, null, tint = Neon) else Icon(Icons.Default.ChevronRight, null, tint = Muted) } }
                        }
                        "Settings" -> LazyColumn(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            item { SectionTitle("SETTINGS") }
                            item { var newPin by remember { mutableStateOf("") }; OutlinedTextField(value = newPin, onValueChange = { newPin = it }, label = { Text("PIN baru") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth()); Button(onClick = { if (newPin.length >= 3) scope.launch { activity.dataStore.edit { it[stringPreferencesKey("pin")] = newPin }; savedPin = newPin; newPin = "" } }, modifier = Modifier.fillMaxWidth()) { Text("SIMPAN PIN BARU") } }
                            item { SettingRow("Nama aplikasi", "SensiGreen FF Overlay PRO MAX") }
                            item { SettingRow("Versi", "1.0.0") }
                            item { SettingRow("Penyimpanan", "Lokal di perangkat") }
                            item { SettingRow("Privasi", "Tidak meminta akun/password game") }
                            item { OutlinedButton(onClick = { loggedIn = false; pin = "" }, modifier = Modifier.fillMaxWidth()) { Text("KELUAR / KUNCI APLIKASI") } }
                        }
                        else -> LazyColumn(Modifier.weight(1f).padding(16.dp)) { item { SectionTitle("GAMING TOOLS") }; item { SettingRow("Resolusi layar", "${activity.resources.displayMetrics.widthPixels} × ${activity.resources.displayMetrics.heightPixels}") }; item { SettingRow("Refresh rate", "${activity.display?.refreshRate?.toInt() ?: 0} Hz (perkiraan layar)") }; item { SettingRow("Overlay permission", if (Settings.canDrawOverlays(activity)) "Granted" else "Belum diberikan") }; item { Text("Pengukuran FPS game tidak tersedia secara universal untuk aplikasi biasa. Tidak ada tombol booster palsu.", color = Muted, fontSize = 12.sp) } }
                    }
                }
            }
        }
    }
}

@Composable private fun SectionTitle(text: String) { Text(text, color = Neon, fontSize = 15.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)) }
@Composable private fun StatusCard(title: String, value: String, modifier: Modifier = Modifier) { Column(modifier.clip(RoundedCornerShape(14.dp)).background(Panel).padding(16.dp)) { Text(title, color = Muted, fontSize = 12.sp); Spacer(Modifier.height(5.dp)); Text(value, color = Neon, fontWeight = FontWeight.Black, fontSize = 17.sp) } }
@Composable private fun SettingRow(title: String, value: String) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Panel).padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.weight(1f), color = Color.White); Text(value, color = Neon, fontSize = 12.sp) } }
@Composable private fun ProfilePicker(selected: String, onSelect: (String) -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { listOf("HP Low-End", "HP Mid-Range", "HP High-End", "FPS Stabil", "Drag Headshot", "2 Jari", "3 Jari", "4 Jari").forEach { label -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (selected == label) Neon.copy(alpha = .12f) else Panel).clickable { onSelect(label) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); if (selected == label) Icon(Icons.Default.CheckCircle, null, tint = Neon) } } } }
