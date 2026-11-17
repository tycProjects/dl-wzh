package com.droidstore.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.droidstore.app.data.Api
import com.droidstore.app.data.AppItem
import com.droidstore.app.ui.components.AppIcon
import com.droidstore.app.ui.components.ErrorText
import com.droidstore.app.ui.components.Stars
import kotlinx.coroutines.launch

@Composable
fun CatalogScreen(onOpenApp: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("downloads") }
    var apps by remember { mutableStateOf<List<AppItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            error = null
            try { apps = Api.catalog(query, sort) } catch (e: Exception) { error = e.message }
        }
    }
    LaunchedEffect(sort) { load() }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Поиск приложений") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("downloads" to "Популярные", "rating" to "Рейтинг", "new" to "Новые").forEach { (key, label) ->
                FilterChip(selected = sort == key, onClick = { sort = key }, label = { Text(label) })
            }
        }
        ErrorText(error)
        when {
            apps == null && error == null -> CircularProgressIndicator(Modifier.padding(16.dp))
            apps?.isEmpty() == true -> Text("Пока нет опубликованных приложений.", modifier = Modifier.padding(16.dp))
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(apps.orEmpty(), key = { _, a -> a.id }) { index, app ->
                    // Появление карточек по очереди: каждая немного позже предыдущей.
                    var shown by remember(app.id) { mutableStateOf(false) }
                    LaunchedEffect(app.id) { shown = true }
                    AnimatedVisibility(
                        visible = shown,
                        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(300, delayMillis = minOf(index, 8) * 40)) +
                            slideInVertically(initialOffsetY = { it / 6 }),
                    ) {
                        AppCard(app, onClick = { onOpenApp(app.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun AppCard(app: AppItem, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(Api.imageUrl(app.iconId), 56)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.title, style = MaterialTheme.typography.titleMedium)
                Text(app.studioName ?: "", style = MaterialTheme.typography.bodySmall)
                Text(app.shortDescription, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Stars(app.ratingAverage, app.ratingCount)
                    Text("⬇ ${app.downloads}", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
