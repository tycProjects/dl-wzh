package com.fakesocial.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fakesocial.app.HistoryViewModel
import com.fakesocial.app.R
import com.fakesocial.app.data.Kind
import com.fakesocial.app.data.MockData
import com.fakesocial.app.ui.templates.Avatar
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit, onOpen: (MockData) -> Unit) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history)) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } }
            )
        }
    ) { inner ->
        if (projects.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.history_empty))
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(projects, key = { it.id }) { p -> ProjectRow(p, onOpen, viewModel::delete) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectRow(p: MockData, onOpen: (MockData) -> Unit, onDelete: (Long) -> Unit) {
    val snippet = if (p.effectiveKind == Kind.CHAT) p.items.firstOrNull()?.text.orEmpty() else p.text
    Card(onClick = { onOpen(p) }, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(p.avatarPath, p.name, 44.dp, Color(p.platform.accent))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("${p.platform.label} • ${p.name}", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(snippet, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(DateFormat.getDateTimeInstance().format(Date(p.updatedAt)))
            }
            TextButton(onClick = { onDelete(p.id) }) { Text(stringResource(R.string.delete)) }
        }
    }
}
