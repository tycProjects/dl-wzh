package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song
import com.example.model.SongSortOption
import com.example.ui.theme.JnmfBorder
import com.example.ui.theme.JnmfDarkBg
import com.example.ui.theme.JnmfSurfaceElevated
import com.example.ui.theme.JnmfTextMuted
import com.example.ui.theme.JnmfTextPrimary
import com.example.ui.theme.JnmfTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun FastScrollMusicList(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    sortOption: SongSortOption,
    listState: LazyListState,
    onPlaySong: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onKickSong: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = JnmfTheme.colors
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalCount = songs.size
        // Show fast-scrolling handle when there are 6 or more tracks
        val showFastScroll = totalCount >= 6

        // Music list with right padding clearance for fast scroll handle
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(end = if (showFastScroll) 18.dp else 0.dp)
                .testTag("song_list"),
            contentPadding = PaddingValues(bottom = 76.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                val isSelected = currentSong?.id == song.id
                SongItemCard(
                    song = song,
                    isSelected = isSelected,
                    isPlaying = isPlaying,
                    onClick = { onPlaySong(song) },
                    onAddToPlaylist = { onAddToPlaylist(song) },
                    onKickSong = { onKickSong(song) }
                )
            }
        }

        if (showFastScroll) {
            // Reserve bottom space for MiniPlayer & Bottom Nav
            val trackHeightDp = (maxHeight - 82.dp).coerceAtLeast(100.dp)
            val thumbHeightDp = 42.dp
            val trackHeightPx = with(density) { trackHeightDp.toPx() }
            val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
            val availableTravelPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)

            var isDragging by remember { mutableStateOf(false) }
            var dragFraction by remember { mutableFloatStateOf(0f) }

            // Calculate natural scroll fraction from lazy list state
            val firstVisibleIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
            val firstVisibleOffset by remember { derivedStateOf { listState.firstVisibleItemScrollOffset } }
            val visibleCount by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.size } }

            val maxScrollIndex = remember(totalCount, visibleCount) {
                maxOf(1, totalCount - visibleCount)
            }

            val naturalFraction by remember {
                derivedStateOf {
                    (firstVisibleIndex.toFloat() / maxScrollIndex).coerceIn(0f, 1f)
                }
            }

            val effectiveFraction = if (isDragging) dragFraction else naturalFraction
            val thumbOffsetY = with(density) { (availableTravelPx * effectiveFraction).toDp() }

            val activeIndex = remember(effectiveFraction, totalCount) {
                (effectiveFraction * (totalCount - 1)).roundToInt().coerceIn(0, totalCount - 1)
            }
            val activeSong = remember(activeIndex, songs) {
                songs.getOrNull(activeIndex)
            }

            // Fast scroll container on right edge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(28.dp)
                    .height(trackHeightDp)
                    .testTag("fast_scroll_container")
                    .pointerInput(totalCount, trackHeightPx, thumbHeightPx) {
                        detectTapGestures { offset ->
                            val fraction = ((offset.y - thumbHeightPx / 2) / availableTravelPx).coerceIn(0f, 1f)
                            dragFraction = fraction
                            val targetIndex = (fraction * (totalCount - 1)).roundToInt().coerceIn(0, totalCount - 1)
                            coroutineScope.launch {
                                listState.scrollToItem(targetIndex)
                            }
                        }
                    }
                    .pointerInput(totalCount, trackHeightPx, thumbHeightPx) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                val fraction = ((offset.y - thumbHeightPx / 2) / availableTravelPx).coerceIn(0f, 1f)
                                dragFraction = fraction
                                val targetIndex = (fraction * (totalCount - 1)).roundToInt().coerceIn(0, totalCount - 1)
                                coroutineScope.launch {
                                    listState.scrollToItem(targetIndex)
                                }
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onVerticalDrag = { change, _ ->
                                change.consume()
                                val fraction = ((change.position.y - thumbHeightPx / 2) / availableTravelPx).coerceIn(0f, 1f)
                                dragFraction = fraction
                                val targetIndex = (fraction * (totalCount - 1)).roundToInt().coerceIn(0, totalCount - 1)
                                coroutineScope.launch {
                                    listState.scrollToItem(targetIndex)
                                }
                            }
                        )
                    }
            ) {
                // Subtle vertical guide track line
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(
                            color = if (isDragging) colors.primary.copy(alpha = 0.4f) else JnmfBorder.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(1.dp)
                        )
                )

                // Draggable Cyber Handle / Thumb
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = thumbOffsetY)
                        .size(width = if (isDragging) 10.dp else 8.dp, height = thumbHeightDp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDragging) colors.bright else colors.primary)
                        .border(
                            width = 1.dp,
                            color = if (isDragging) colors.bright else colors.borderGlow,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .testTag("fast_scroll_handle"),
                    contentAlignment = Alignment.Center
                ) {
                    // Tactile grip grooves on the handle
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.size(width = 4.dp, height = 1.5.dp).background(JnmfDarkBg, CircleShape))
                        Box(modifier = Modifier.size(width = 4.dp, height = 1.5.dp).background(JnmfDarkBg, CircleShape))
                        Box(modifier = Modifier.size(width = 4.dp, height = 1.5.dp).background(JnmfDarkBg, CircleShape))
                    }
                }

                // Floating letter/index badge bubble visible while dragging
                AnimatedVisibility(
                    visible = isDragging,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(
                            x = (-34).dp,
                            y = (thumbOffsetY - 12.dp).coerceIn(4.dp, trackHeightDp - 60.dp)
                        )
                ) {
                    val badgePrimary = remember(activeSong, sortOption) {
                        when (sortOption) {
                            SongSortOption.A_TO_Z, SongSortOption.Z_TO_A -> {
                                activeSong?.title?.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                            }
                            SongSortOption.DATE_ASC, SongSortOption.DATE_DESC -> {
                                val dateAdded = activeSong?.dateAdded ?: 0L
                                if (dateAdded > 0) {
                                    SimpleDateFormat("MMM yyyy", Locale.US).format(Date(dateAdded))
                                } else {
                                    "Track ${activeIndex + 1}"
                                }
                            }
                            SongSortOption.SIZE_ASC, SongSortOption.SIZE_DESC -> {
                                activeSong?.formattedFileSize ?: "--"
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .shadow(8.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(JnmfSurfaceElevated)
                            .border(1.5.dp, colors.primary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("fast_scroll_bubble")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = badgePrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = colors.bright
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "#${activeIndex + 1} / $totalCount",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = JnmfTextMuted
                            )
                            if (activeSong != null) {
                                Text(
                                    text = activeSong.title,
                                    fontSize = 10.sp,
                                    color = JnmfTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
