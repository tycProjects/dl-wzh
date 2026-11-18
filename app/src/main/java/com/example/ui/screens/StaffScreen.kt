package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.StaffMemberEntity
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.StaffSwitchDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.formatDateTime
import com.example.viewmodel.NexoraUiState
import com.example.viewmodel.NexoraViewModel
import com.example.viewmodel.RolePermissions
import com.example.viewmodel.StaffRole

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StaffScreen(
    state: NexoraUiState,
    viewModel: NexoraViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Staf & Role, 1=Log Aktivitas
    var editingStaff by remember { mutableStateOf<StaffMemberEntity?>(null) }
    var showStaffDialog by remember { mutableStateOf(false) }
    var showSwitchDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Staf, Role & Log Aktivitas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Aktif: ${state.activeStaff?.name ?: state.ownerDisplayName} (${state.activeRole.displayName})",
                    style = MaterialTheme.typography.bodySmall,
                    color = NexoraBlue
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { showSwitchDialog = true },
                    modifier = Modifier.testTag("open_switch_role_button")
                ) {
                    Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ganti")
                }
                if (state.permissions.canManageStaff) {
                    Button(
                        onClick = { editingStaff = null; showStaffDialog = true },
                        modifier = Modifier.testTag("add_staff_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Staf")
                    }
                }
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Daftar Staf (${state.staffMembers.size})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Aktivitas (${state.activityLogs.size})") })
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    RolePermissionMatrixCard()
                }
                items(state.staffMembers, key = { it.id }) { member ->
                    val role = StaffRole.fromString(member.role)
                    val isActiveOperator = member.id == state.activeStaff?.id
                    val roleColor = when (role) {
                        StaffRole.OWNER -> NexoraBlue
                        StaffRole.ADMIN -> NexoraViolet
                        StaffRole.MANAJER -> NexoraSuccess
                        StaffRole.KASIR -> NexoraWarning
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            if (isActiveOperator) 1.5.dp else 1.dp,
                            if (isActiveOperator) NexoraBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(member.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    StatusBadge(role.displayName, roleColor)
                                }
                                val outletName = state.outlets.firstOrNull { it.id == member.outletId }?.name ?: "Cabang Utama"
                                Text(
                                    "Cabang: $outletName • Status: ${if (member.active) "Aktif" else "Nonaktif"}" +
                                        if (isActiveOperator) " • Sedang Bertugas" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (state.permissions.canManageStaff) {
                                Row {
                                    IconButton(onClick = { editingStaff = member; showStaffDialog = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Staf")
                                    }
                                    if (member.role != "OWNER" || state.staffMembers.count { it.role == "OWNER" } > 1) {
                                        IconButton(onClick = { viewModel.deleteStaff(member) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Staf", tint = NexoraDanger)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (state.activityLogs.isEmpty()) {
                EmptyStatePanel(
                    icon = Icons.Default.History,
                    title = "Belum Ada Catatan Aktivitas",
                    description = "Setiap transaksi, perubahan stok, void, retur, dan pergantian operator akan tercatat otomatis."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.activityLogs, key = { it.id }) { log ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(log.action, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = NexoraBlue)
                                    Text(formatDateTime(log.createdAtEpochMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(log.details, style = MaterialTheme.typography.bodySmall)
                                Text("Oleh: ${log.actorName} (${log.actorRole})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSwitchDialog) {
        StaffSwitchDialog(
            staffMembers = state.staffMembers,
            activeStaffId = state.activeStaff?.id,
            onDismiss = { showSwitchDialog = false },
            onSwitch = { member, pin -> viewModel.switchStaffWithPin(member, pin) }
        )
    }

    if (showStaffDialog) {
        var name by remember(editingStaff) { mutableStateOf(editingStaff?.name ?: "") }
        var role by remember(editingStaff) { mutableStateOf(editingStaff?.role ?: "KASIR") }
        var pin by remember(editingStaff) { mutableStateOf(editingStaff?.pinCode ?: "1234") }
        var outletId by remember(editingStaff) { mutableStateOf(editingStaff?.outletId ?: (state.activeOutlet?.id ?: "outlet_main")) }
        var active by remember(editingStaff) { mutableStateOf(editingStaff?.active ?: true) }

        AlertDialog(
            onDismissRequest = { showStaffDialog = false },
            title = { Text(if (editingStaff == null) "Tambah Akun Staf" else "Edit Akun Staf", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Staf *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Role Akses:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StaffRole.entries.forEach { r ->
                            FilterChip(
                                selected = role == r.name,
                                onClick = { role = r.name },
                                label = { Text(r.displayName) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 8) pin = it.filter { c -> c.isDigit() } },
                        label = { Text("PIN Akses (min 4 digit) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.outlets.isNotEmpty()) {
                        Text("Cabang Penugasan:", style = MaterialTheme.typography.labelMedium)
                        state.outlets.forEach { o ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = outletId == o.id, onClick = { outletId = o.id })
                                Text(o.name, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Akun Aktif")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveStaff(
                            StaffMemberEntity(
                                id = editingStaff?.id ?: "",
                                name = name.trim(),
                                role = role,
                                outletId = outletId,
                                pinCode = pin,
                                active = active,
                                demoData = editingStaff?.demoData ?: false
                            )
                        )
                        showStaffDialog = false
                    },
                    enabled = name.isNotBlank() && pin.length >= 4
                ) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showStaffDialog = false }) { Text("Batal") } }
        )
    }
}

@Composable
private fun RolePermissionMatrixCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Hak Akses Role (RBAC)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            StaffRole.entries.forEach { r ->
                val perms = RolePermissions.forRole(r)
                val summary = buildList {
                    add("Kasir & Pelanggan")
                    if (perms.canManageProducts) add("Produk")
                    if (perms.canManageInventory) add("Stok")
                    if (perms.canVoidOrReturn) add("Void/Retur")
                    if (perms.canViewProfitReports) add("Laba")
                    if (perms.canManageOutlets) add("Cabang")
                    if (perms.canManageStaff) add("Staf")
                    if (perms.canManageSettings) add("Pengaturan")
                }.joinToString(" • ")
                Text("${r.displayName}: $summary", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
