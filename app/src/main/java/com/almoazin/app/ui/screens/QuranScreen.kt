package com.almoazin.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almoazin.app.data.QuranData
import com.almoazin.app.data.SurahMeta
import com.almoazin.app.ui.theme.*
import com.almoazin.app.utils.AthanAudioPlayer

@Composable
fun QuranScreen(
    audioPlayer: AthanAudioPlayer,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val isPlaying by audioPlayer.isPlaying.collectAsState()
    val currentTrack by audioPlayer.currentTrackName.collectAsState()

    val filteredSurahs = remember(searchQuery) {
        if (searchQuery.isBlank()) QuranData.ALL_SURAHS
        else QuranData.ALL_SURAHS.filter {
            it.nameAr.contains(searchQuery) || it.nameEn.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SkyBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "القرآن الكريم كاملاً",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
        )
        Text(
            text = "١١٤ سورة مع التلاوة المرتلة بأصوات كبار القراء",
            fontSize = 13.sp,
            color = SlateTextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث عن اسم السورة...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RoyalBluePrimary) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = PureWhite,
                focusedBorderColor = RoyalBluePrimary,
                unfocusedBorderColor = BorderLightBlue
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (isPlaying) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SoftBorderBlue, RoundedCornerShape(16.dp))
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Column {
                        Text(text = "جارٍ الاستماع الآن:", fontSize = 11.sp, color = RoyalBluePrimary)
                        Text(text = currentTrack, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                    }
                    IconButton(
                        onClick = { audioPlayer.stop() },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = RoyalBluePrimary)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "إيقاف", tint = PureWhite)
                    }
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredSurahs, key = { it.number }) { surah ->
                SurahItemCard(
                    surah = surah,
                    onPlayAudio = {
                        val padded = surah.number.toString().padStart(3, '0')
                        val streamUrl = "https://server8.mp3quran.net/afs/$padded.mp3"
                        audioPlayer.playAudioUrl(streamUrl, "سورة ${surah.nameAr} - الشيخ مشاري العفاسي")
                    }
                )
            }
        }
    }
}

@Composable
private fun SurahItemCard(
    surah: SurahMeta,
    onPlayAudio: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderLightBlue, RoundedCornerShape(16.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEFF6FF))
                        .border(1.dp, SoftBorderBlue, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "${surah.number}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBluePrimary
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "سورة ${surah.nameAr}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                    Text(
                        text = "${if (surah.revelationType == "Meccan") "مكية" else "مدنية"} · ${surah.numberOfAyahs} آيات",
                        fontSize = 12.sp,
                        color = SlateTextSecondary
                    )
                }
            }

            IconButton(
                onClick = onPlayAudio,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "استماع",
                    tint = RoyalBluePrimary
                )
            }
        }
    }
}
