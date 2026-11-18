package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.example.viewmodel.NexoraUiState

data class HubMenuItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val testTag: String
)

@Composable
fun MoreHubScreen(
    state: NexoraUiState,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        HubMenuItem("scanner", "Scanner Barcode", "Kamera & USB/BT", Icons.Default.QrCodeScanner, NexoraViolet, "hub_scanner"),
        HubMenuItem("restaurant", "Restoran & Dapur", "Meja, Dine-in & KDS", Icons.Default.Restaurant, NexoraWarning, "hub_restaurant"),
        HubMenuItem("digital", "Toko Produk Digital", "Pulsa, Voucher & Antrean", Icons.Default.PhonelinkSetup, NexoraInfo, "hub_digital"),
        HubMenuItem("customers", "Pelanggan & Poin", "${state.customers.size} pelanggan", Icons.Default.Groups, NexoraSuccess, "hub_customers"),
        HubMenuItem("transactions", "Riwayat & Retur", "${state.sales.size} transaksi", Icons.Default.ReceiptLong, NexoraBlue, "hub_transactions"),
        HubMenuItem("reports", "Laporan Bisnis", "Omzet, Laba, CSV & Print", Icons.Default.Assessment, NexoraViolet, "hub_reports"),
        HubMenuItem("outlets", "Multi-Outlet", "${state.outlets.size} cabang usaha", Icons.Default.Store, NexoraBlue, "hub_outlets"),
        HubMenuItem("staff", "Staf & Log Aktivitas", "Role ${state.activeRole.displayName}", Icons.Default.AdminPanelSettings, NexoraSuccess, "hub_staff"),
        HubMenuItem("settings", "Pengaturan & Profil", "Pajak, Struk, Sync & Backup", Icons.Default.Settings, NexoraTextMutedLight, "hub_settings")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Semua Modul Usaha", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Akses lengkap fitur POS, operasional restoran, produk digital, cabang, hingga laporan.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 155.dp),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.route }) { item ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 112.dp)
                        .clickable { onNavigate(item.route) }
                        .testTag(item.testTag),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(item.color.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(item.icon, contentDescription = item.title, tint = item.color, modifier = Modifier.size(22.dp))
                        }
                        Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
