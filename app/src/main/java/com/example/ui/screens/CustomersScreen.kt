package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CustomerEntity
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.NexoraBlue
import com.example.ui.theme.NexoraDanger
import com.example.ui.theme.NexoraViolet
import com.example.util.formatCurrency
import com.example.util.formatDateTime
import com.example.viewmodel.NexoraUiState
import com.example.viewmodel.NexoraViewModel

@Composable
fun CustomersScreen(
    state: NexoraUiState,
    viewModel: NexoraViewModel
) {
    var search by remember { mutableStateOf("") }
    var editingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var historyCustomer by remember { mutableStateOf<CustomerEntity?>(null) }

    val customers = remember(state.customers, search) {
        state.customers.filter {
            search.isBlank() || it.name.contains(search, ignoreCase = true) || it.phone.contains(search)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingCustomer = null; showForm = true },
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Tambah Pelanggan")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Cari nama atau nomor HP pelanggan...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (customers.isEmpty()) {
                EmptyStatePanel(
                    icon = Icons.Default.People,
                    title = "Belum Ada Pelanggan",
                    description = "Simpan pelanggan tetap untuk melacak riwayat belanja dan poin loyalitas.",
                    actionLabel = "Tambah Pelanggan",
                    onAction = { editingCustomer = null; showForm = true }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 84.dp)
                ) {
                    items(customers, key = { it.id }) { cust ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { historyCustomer = cust }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cust.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${cust.phone} • ${cust.address.ifBlank { "-" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusBadge("${cust.points} poin", NexoraViolet, Icons.Default.Stars)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${cust.transactionCount} transaksi • Total ${formatCurrency(cust.totalSpent, state.settings.currencyCode)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NexoraBlue,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row {
                                        IconButton(onClick = { editingCustomer = cust; showForm = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Pelanggan")
                                        }
                                        IconButton(onClick = { viewModel.deleteCustomer(cust) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = NexoraDanger)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        var name by remember { mutableStateOf(editingCustomer?.name ?: "") }
        var phone by remember { mutableStateOf(editingCustomer?.phone ?: "") }
        var address by remember { mutableStateOf(editingCustomer?.address ?: "") }
        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text(if (editingCustomer == null) "Tambah Pelanggan" else "Edit Pelanggan", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama*") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Nomor Telepon") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat") }, maxLines = 2, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCustomer(
                            CustomerEntity(
                                id = editingCustomer?.id ?: "",
                                name = name.trim(),
                                phone = phone.trim(),
                                address = address.trim(),
                                points = editingCustomer?.points ?: 0,
                                totalSpent = editingCustomer?.totalSpent ?: 0.0,
                                transactionCount = editingCustomer?.transactionCount ?: 0,
                                demoData = editingCustomer?.demoData ?: false
                            )
                        )
                        showForm = false
                    },
                    enabled = name.isNotBlank()
                ) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showForm = false }) { Text("Batal") } }
        )
    }

    historyCustomer?.let { cust ->
        val custSales = state.sales.filter { it.customerId == cust.id }
        AlertDialog(
            onDismissRequest = { historyCustomer = null },
            title = { Text("Riwayat: ${cust.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (custSales.isEmpty()) {
                        Text("Belum ada riwayat transaksi untuk pelanggan ini.")
                    } else {
                        custSales.take(6).forEach { s ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(s.transactionNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        Text(formatDateTime(s.createdAtEpochMs), style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(formatCurrency(s.total, state.settings.currencyCode), fontWeight = FontWeight.Bold, color = NexoraBlue)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { historyCustomer = null }) { Text("Tutup") } }
        )
    }
}
