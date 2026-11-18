package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ui.components.CameraBarcodeScannerCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.NexoraBlue
import com.example.ui.theme.NexoraDanger
import com.example.ui.theme.NexoraSuccess
import com.example.viewmodel.NexoraUiState
import com.example.viewmodel.NexoraViewModel

@Composable
fun ScannerScreen(
    state: NexoraUiState,
    viewModel: NexoraViewModel,
    onGoToCashier: () -> Unit
) {
    var manualCode by remember { mutableStateOf("") }
    var lastDetectedCode by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Scanner Barcode & QR", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Mendukung EAN-13, EAN-8, UPC, Code 128, Code 39, dan QR Code melalui Kamera Android maupun Scanner USB/Bluetooth.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // CameraX + ML Kit Live Preview
        CameraBarcodeScannerCard(
            onBarcodeDetected = { code ->
                lastDetectedCode = code
                viewModel.handleScannedBarcode(code, addToCartImmediately = true)
            }
        )

        // External USB/Bluetooth HID Scanner & Manual Input
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Usb, contentDescription = null, tint = NexoraBlue)
                    Text(
                        "Scanner USB/Bluetooth & Input Manual",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "Fokuskan kursor pada kolom di bawah untuk menerima ketikan otomatis dari scanner eksternal (diakhiri Enter) atau ketik kode barcode secara manual.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualCode,
                        onValueChange = { manualCode = it },
                        label = { Text("Kode Barcode / SKU") },
                        placeholder = { Text("Contoh: 8991001000011") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (manualCode.isNotBlank()) {
                                    lastDetectedCode = manualCode.trim()
                                    viewModel.handleScannedBarcode(manualCode, addToCartImmediately = true)
                                    manualCode = ""
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_barcode_input")
                    )
                    Button(
                        onClick = {
                            if (manualCode.isNotBlank()) {
                                lastDetectedCode = manualCode.trim()
                                viewModel.handleScannedBarcode(manualCode, addToCartImmediately = true)
                                manualCode = ""
                            }
                        },
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("submit_manual_barcode_button")
                    ) {
                        Text("Cari")
                    }
                }
            }
        }

        // Not Found -> Offer Register New Product
        state.lastScannedNotFoundBarcode?.let { missingCode ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, NexoraDanger.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = NexoraDanger)
                        Text(
                            "Barcode Tidak Terdaftar: $missingCode",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Produk dengan kode tersebut belum ada di katalog aktif cabang ini.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.permissions.canManageProducts) {
                            Button(
                                onClick = { viewModel.startRegisterProductWithBarcode(missingCode) },
                                modifier = Modifier.testTag("register_missing_barcode_button")
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Daftarkan Produk Baru")
                            }
                        }
                        OutlinedButton(onClick = { viewModel.dismissNotFoundBarcode() }) {
                            Text("Tutup")
                        }
                    }
                }
            }
        }

        // Cart Summary Shortcut
        if (state.cartLines.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        StatusBadge("Keranjang: ${state.cartItemCount} item", NexoraSuccess)
                        Spacer(Modifier.height(4.dp))
                        lastDetectedCode?.let {
                            Text("Terakhir dipindai: $it", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Button(onClick = onGoToCashier) {
                        Text("Ke Kasir")
                    }
                }
            }
        }
    }
}
