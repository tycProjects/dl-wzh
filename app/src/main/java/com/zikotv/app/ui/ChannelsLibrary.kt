package com.zikotv.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zikotv.app.R
import com.zikotv.app.player.PlayerActivity
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class TvChannel(
    val title: String,
    val streamUrl: String,
    val logoUrl: String,
    val groupName: String
)

private data class ChannelGroup(
    val name: String,
    val channels: List<TvChannel>,
    val logoUrl: String
)

private object ChannelLogoCache {
    val images = ConcurrentHashMap<String, Bitmap>()
}

private fun playlistAttribute(line: String, attribute: String): String {
    return Regex("(?<![\\w-])${Regex.escape(attribute)}=\"([^\"]*)\"")
        .find(line)?.groupValues?.getOrNull(1).orEmpty()
}

private fun parseChannelPlaylist(contents: String): List<TvChannel> {
    val result = ArrayList<TvChannel>(14_600)
    var metadata: String? = null

    contents.lineSequence().forEach { sourceLine ->
        val line = sourceLine.trim().removePrefix("\uFEFF")
        when {
            line.startsWith("#EXTINF", ignoreCase = true) -> metadata = line
            line.isNotBlank() && !line.startsWith("#") -> {
                val current = metadata
                if (current != null) {
                    val nameFromAttribute = playlistAttribute(current, "tvg-name")
                    val displayName = current.substringAfterLast(',', "").trim()
                    val title = nameFromAttribute.ifBlank { displayName }
                    val group = playlistAttribute(current, "group-title").ifBlank { "Other Channels" }
                    if (title.isNotBlank()) {
                        result += TvChannel(
                            title = title,
                            streamUrl = line,
                            logoUrl = playlistAttribute(current, "tvg-logo"),
                            groupName = group
                        )
                    }
                }
                metadata = null
            }
        }
    }
    return result
}

@Composable
fun ChannelsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val channels = remember(context) {
        runCatching {
            context.assets.open("all_channels.m3u8").bufferedReader().use { parseChannelPlaylist(it.readText()) }
        }.getOrDefault(emptyList())
    }
    val groups = remember(channels) {
        channels.groupBy { it.groupName }
            .map { (name, entries) ->
                ChannelGroup(
                    name = name,
                    channels = entries,
                    logoUrl = entries.firstOrNull { it.logoUrl.isNotBlank() }?.logoUrl.orEmpty()
                )
            }
            .sortedWith(
                compareBy<ChannelGroup> {
                    when (it.name) {
                        "TOD Entertainment" -> 0
                        "TOD Sports" -> 1
                        "beIN Sports & Entertainment" -> 2
                        "MBC & Rotana" -> 3
                        else -> 4
                    }
                }.thenBy { it.name.lowercase(Locale.ROOT) }
            )
    }

    var selectedGroup by remember { mutableStateOf<ChannelGroup?>(null) }
    var search by remember { mutableStateOf("") }
    val filteredGroups = remember(groups, search, selectedGroup) {
        if (selectedGroup == null) groups.filter { it.name.contains(search, ignoreCase = true) }
        else emptyList()
    }
    val filteredChannels = remember(selectedGroup, search) {
        selectedGroup?.channels?.filter { it.title.contains(search, ignoreCase = true) }.orEmpty()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.Black, Color(0xFF080808), Color.Black)))
    ) {
        Box(
            modifier = Modifier
                .size(270.dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF1D8B67).copy(alpha = 0.14f), Color.Transparent),
                        radius = 430f
                    ),
                    CircleShape
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(15.dp))
                            .clickable {
                                if (selectedGroup != null) {
                                    selectedGroup = null
                                    search = ""
                                } else {
                                    onBack()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                    Column(Modifier.weight(1f).padding(start = 13.dp)) {
                        Text(
                            text = selectedGroup?.name ?: "Channels",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 25.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (selectedGroup == null) {
                                "${channels.size} channels  •  ${groups.size} groups"
                            } else {
                                "${selectedGroup?.channels?.size ?: 0} channels"
                            },
                            color = Color.White.copy(alpha = 0.52f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }
            }

            item { SearchBox(value = search, onValueChange = { search = it }) }

            if (channels.isEmpty()) {
                item {
                    Text(
                        text = "The channel list could not be loaded.",
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            } else if (selectedGroup == null) {
                if (filteredGroups.isEmpty()) {
                    item { EmptySearchResult() }
                } else {
                    items(filteredGroups.chunked(2), key = { row -> row.joinToString("|") { it.name } }) { rowGroups ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            rowGroups.forEach { group ->
                                GroupCard(
                                    group = group,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedGroup = group
                                        search = ""
                                    }
                                )
                            }
                            if (rowGroups.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else {
                if (filteredChannels.isEmpty()) {
                    item { EmptySearchResult() }
                } else {
                    items(filteredChannels, key = { "${it.groupName}|${it.title}|${it.streamUrl}" }) { channel ->
                        ChannelRow(
                            channel = channel,
                            fallbackLogo = selectedGroup?.logoUrl.orEmpty(),
                            onClick = {
                                context.startActivity(
                                    PlayerActivity.createIntent(
                                        context = context,
                                        url = channel.streamUrl,
                                        title = channel.title
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBox(value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = "Search",
            tint = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
            cursorBrush = SolidColor(Color(0xFF74E1B4)),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = "Search groups or channels",
                            color = Color.White.copy(alpha = 0.40f),
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun GroupCard(group: ChannelGroup, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(21.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.035f))))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(21.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        ChannelLogo(
            url = group.logoUrl,
            title = group.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.15f)
                .clip(RoundedCornerShape(15.dp))
        )
        Text(
            text = group.name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 11.dp, start = 2.dp, end = 2.dp)
        )
        Text(
            text = "${group.channels.size} channels",
            color = Color.White.copy(alpha = 0.48f),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp, start = 2.dp, bottom = 2.dp)
        )
    }
}

@Composable
private fun ChannelRow(channel: TvChannel, fallbackLogo: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChannelLogo(
            url = channel.logoUrl.ifBlank { fallbackLogo },
            title = channel.title,
            modifier = Modifier
                .size(66.dp)
                .clip(RoundedCornerShape(13.dp))
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = channel.title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Tap to play",
                color = Color.White.copy(alpha = 0.42f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 5.dp)
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_play),
            contentDescription = "Play ${channel.title}",
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.padding(horizontal = 4.dp).size(23.dp)
        )
    }
}

@Composable
private fun ChannelLogo(url: String, title: String, modifier: Modifier = Modifier) {
    var bitmap by remember(url) { mutableStateOf(ChannelLogoCache.images[url]) }

    LaunchedEffect(url) {
        if (url.isBlank() || bitmap != null) return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            ChannelLogoCache.images[url] ?: runCatching {
                URL(url).openConnection().apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", "ZikoTV/1.0 Android")
                }.getInputStream().use { input -> BitmapFactory.decodeStream(input) }
            }.getOrNull()?.also { decoded -> ChannelLogoCache.images[url] = decoded }
        }
    }

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(Color(0xFF282828), Color(0xFF111111), Color.Black))
        ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "$title logo",
                modifier = Modifier.fillMaxSize().padding(5.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(R.drawable.ic_channels),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.70f),
                    modifier = Modifier.size(27.dp)
                )
                Text(
                    text = title.trim().split(Regex("\\s+")).take(2).joinToString(" ").take(13).uppercase(Locale.ROOT),
                    color = Color.White.copy(alpha = 0.77f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptySearchResult() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No matching channels found.",
            color = Color.White.copy(alpha = 0.52f),
            fontSize = 14.sp
        )
    }
}
