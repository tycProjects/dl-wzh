package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.remote.signOutFromGoogle
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.viewmodel.NexoraUiState
import com.example.viewmodel.NexoraViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsAndProfileScreen(
    state: NexoraUiState,
    viewModel: NexoraViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var businessName by remember(state.settings.businessName) { mutableStateOf(state.settings.businessName) }
    var businessType by remember(state.settings.businessType) { mutableStateOf(state.settings.businessType) }
    var address by remember(state.settings.address) { mutableStateOf(state.settings.address) }
    var phone by remember(state.settings.phone) { mutableStateOf(state.settings.phone) }
    var taxPercent by remember(state.settings.taxPercent) { mutableStateOf(state.settings.taxPercent.toString()) }
    var currencyCode by remember(state.settings.currencyCode) { mutableStateOf(state.settings.currencyCode) }
    var receiptFooter by remember(state.settings.receiptFooter) { mutableStateOf(state.settings.receiptFooter) }
    var receiptWidthMm by remember(state.settings.receiptWidthMm) { mutableIntStateOf(state.settings.receiptWidthMm) }
    var taxEnabled by remember(state.settings.taxEnabled) { mutableStateOf(state.settings.taxEnabled) }
    var loyaltyEnabled by remember(state.settings.loyaltyEnabled) { mutableStateOf(state.settings.loyaltyEnabled) }
    var scannerSoundEnabled by remember(state.settings.scannerSoundEnabled) { mutableStateOf(state.settings.scannerSoundEnabled) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = viewModel.createBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                    statusMessage = "Cadangan JSON berhasil disimpan"
                } catch (e: Exception) {
                    statusMessage = "Gagal menyimpan cadangan: ${e.localizedMessage}"
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (!json.isNullOrBlank()) {
                        viewModel.restoreFromBackupJson(json)
                    }
                } catch (e: Exception) {
                    statusMessage = "Gagal membaca file JSON: ${e.localizedMessage}"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Pengaturan & Profil Akun", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        if (statusMessage != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NexoraInfoContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(statusMessage!!, style = MaterialTheme.typography.bodySmall, color = NexoraInfo, modifier = Modifier.padding(12.dp))
            }
        }

        // User Profile & Google Sign-Out Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = NexoraBlue, modifier = Modifier.size(40.dp))
                        Column {
                            Text(state.ownerDisplayName.ifBlank { "Pemilik Usaha" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(state.ownerEmail.ifBlank { "Google Account Terhubung" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    StatusBadge(state.activeRole.displayName, NexoraViolet)
                }
                Text(
                    "Operator Aktif: ${state.activeStaff?.name ?: state.ownerDisplayName} • Cabang: ${state.activeOutlet?.name ?: "-"}",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(
                    onClick = { signOutFromGoogle(credentialManager, scope) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoraDanger),
                    border = BorderStroke(1.dp, NexoraDanger.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Keluar dari Akun Google")
                }
            }
        }

        // Theme Selector
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tampilan & Tema", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        AppThemeMode.SYSTEM to "Sistem",
                        AppThemeMode.LIGHT to "Terang",
                        AppThemeMode.DARK to "Gelap"
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = state.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Offline Mode & Sync Queue Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sinkronisasi Cloud & Mode Offline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    StatusBadge(
                        label = if (state.isOnline) "Online" else "Offline",
                        color = if (state.isOnline) NexoraSuccess else NexoraWarning
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Simulasi Mode Offline Manual", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Transaksi disimpan di Room & antrean sinkronisasi lokal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = state.manualOfflineMode,
                        onCheckedChange = { viewModel.setManualOfflineMode(it) },
                        modifier = Modifier.testTag("manual_offline_switch")
                    )
                }
                Text("Antrean Belum Sinkron: ${state.pendingSyncCount} item", style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.syncNow() },
                        enabled = !state.isSyncing,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("sync_now_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (state.isSyncing) "Menyinkronkan..." else "Sync Sekarang")
                    }
                    OutlinedButton(
                        onClick = { viewModel.pullCloudData() },
                        enabled = !state.isSyncing,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("pull_cloud_button")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pulihkan Cloud")
                    }
                }
            }
        }

        // Business Settings Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            val editable = state.permissions.canManageSettings
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Profil Bisnis, Pajak & Struk", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (!editable) {
                    Text(
                        "Hanya Owner atau Admin yang dapat mengubah pengaturan bisnis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NexoraWarning
                    )
                }
                OutlinedTextField(value = businessName, onValueChange = { businessName = it }, enabled = editable, label = { Text("Nama Usaha") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = businessType, onValueChange = { businessType = it }, enabled = editable, label = { Text("Jenis Usaha") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, enabled = editable, label = { Text("Alamat Usaha") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, enabled = editable, label = { Text("Telepon") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = taxPercent,
                        onValueChange = { taxPercent = it.filter { c -> c.isDigit() || c == '.' } },
                        enabled = editable,
                        label = { Text("Pajak (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = currencyCode == "IDR", onClick = { if (editable) currencyCode = "IDR" }, label = { Text("IDR") }, modifier = Modifier.weight(1f))
                        FilterChip(selected = currencyCode == "USD", onClick = { if (editable) currencyCode = "USD" }, label = { Text("USD") }, modifier = Modifier.weight(1f))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Lebar Struk Thermal:", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    FilterChip(selected = receiptWidthMm == 58, onClick = { if (editable) receiptWidthMm = 58 }, label = { Text("58mm") })
                    FilterChip(selected = receiptWidthMm == 80, onClick = { if (editable) receiptWidthMm = 80 }, label = { Text("80mm") })
                }

                OutlinedTextField(value = receiptFooter, onValueChange = { receiptFooter = it }, enabled = editable, label = { Text("Footer Struk") }, modifier = Modifier.fillMaxWidth())

                SettingSwitchRow("Aktifkan Pajak Otomatis", taxEnabled, editable) { taxEnabled = it }
                SettingSwitchRow("Aktifkan Poin Loyalitas Pelanggan", loyaltyEnabled, editable) { loyaltyEnabled = it }
                SettingSwitchRow("Aktifkan Notifikasi Scanner", scannerSoundEnabled, editable) { scannerSoundEnabled = it }

                if (editable) {
                    Button(
                        onClick = {
                            viewModel.saveSettings(
                                state.settings.copy(
                                    businessName = businessName.trim().ifBlank { "NEXORA POS Store" },
                                    businessType = businessType.trim(),
                                    address = address.trim(),
                                    phone = phone.trim(),
                                    taxPercent = taxPercent.toDoubleOrNull() ?: 11.0,
                                    currencyCode = currencyCode,
                                    receiptFooter = receiptFooter.trim(),
                                    receiptWidthMm = receiptWidthMm,
                                    taxEnabled = taxEnabled,
                                    loyaltyEnabled = loyaltyEnabled,
                                    scannerSoundEnabled = scannerSoundEnabled
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("save_settings_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Simpan Pengaturan")
                    }
                }
            }
        }

        // Backup & Demo Data Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Cadangan Data & Data Demo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { backupLauncher.launch("nexora_backup_${System.currentTimeMillis()}.json") },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("backup_json_button")
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Backup JSON")
                    }
                    OutlinedButton(
                        onClick = { restoreLauncher.launch(arrayOf("application/json", "text/*")) },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("restore_json_button")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Restore JSON")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { viewModel.loadDemoData() },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("settings_load_demo_button")
                    ) {
                        Text("Muat Data Demo")
                    }
                    if (state.hasDemoData) {
                        OutlinedButton(
                            onClick = { viewModel.clearDemoData() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoraDanger),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("settings_clear_demo_button")
                        ) {
                            Text("Hapus Data Demo")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
