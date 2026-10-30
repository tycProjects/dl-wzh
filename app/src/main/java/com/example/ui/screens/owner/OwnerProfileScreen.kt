package com.example.ui.screens.owner

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBg
import com.example.ui.theme.GoldStar
import com.example.ui.theme.HindSiliguriFontFamily
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SecondaryViolet
import com.example.ui.theme.VerifiedBlue
import com.example.ui.viewmodel.AppViewModel

@Composable
fun OwnerProfileScreen(
    viewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "ফিরে যান",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "অ্যাপ নির্মাতা পরিচিতি",
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar with verified check
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(PrimaryPurple, SecondaryViolet)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "MP",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "মাহিন পারভেজ (Mahin Parvez)",
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "অফিসিয়াল ভেরিফায়েড",
                                tint = VerifiedBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = "Founder, Full-Stack Architect & Author",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryPurple,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            color = GoldStar.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "✨ Made by Mahin Parvez",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldStar,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "বাংলা সাহিত্যের সৌন্দর্য ও ডিজিটাল প্রযুক্তির মেলবন্ধনে 'গল্পকথা' একটি অনন্য সৃষ্টি। কোটি বাংলাভাষী মানুষের হৃদয়ে বই পড়ার অভ্যাস ও আনন্দ ফিরিয়ে আনাই এই অ্যাপ্লিকেশনের মূল লক্ষ্য। প্রতিটি গল্প এবং প্রতিটি পাতার অভিজ্ঞতা পাঠকদের জন্য সর্বোচ্চ মমতায় তৈরি করা হয়েছে।",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 24.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Key Highlights Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "প্রজেক্টের বিশেষত্ব",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        HighlightItem(
                            icon = Icons.Default.Code,
                            title = "মডার্ন আর্কিটেকচার",
                            desc = "Kotlin, Jetpack Compose, Room Database এবং M3 ডিজাইনের সমন্বয়"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HighlightItem(
                            icon = Icons.Default.AutoStories,
                            title = "অরিজিনাল বাংলা সাহিত্য",
                            desc = "সম্পূর্ণ প্রাকৃতিক ও সাবলীল বাংলায় লেখা দীর্ঘ বহু-অধ্যায়ের গল্প"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HighlightItem(
                            icon = Icons.Default.Star,
                            title = "উন্নত পঠন অভিজ্ঞতা",
                            desc = "কাস্টম থিম, ফন্ট সাইজ নিয়ন্ত্রণ, সেপিয়া মোড এবং অটো-স্ক্রোল সুবিধা"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Stories by Mahin Parvez
            item {
                Text(
                    text = "মাহিন পারভেজের রচিত গল্পসমূহ",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "কুয়াশার ওপারে এক নদী",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "পদ্মা নদীর পাড়ের এক নিঃসঙ্গ চিত্রশিল্পী এবং অচেনা এক নারীর চিঠিকে কেন্দ্র করে গড়ে ওঠা রোমান্টিক উপন্যাস।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.startReading("story_1", 1)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Text("গল্পটি পড়ুন")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun HighlightItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(PrimaryPurple.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
        }
    }
}
