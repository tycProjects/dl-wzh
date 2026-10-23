package com.zikotv.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zikotv.app.R
import kotlinx.coroutines.delay

private val AppBackground = Color(0xFF070707)
private val AccentGreen = Color(0xFF70E2B5)

@Composable
fun ZikoTvApp() {
    ZikoTheme {
        var screen by remember { mutableIntStateOf(0) }

        LaunchedEffect(Unit) {
            delay(2450)
            screen = 1
        }

        BackHandler(enabled = screen == 2) { screen = 1 }

        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                (fadeIn(tween(420)) + scaleIn(initialScale = 0.975f, animationSpec = tween(420)))
                    .togetherWith(fadeOut(tween(260)))
            },
            label = "root-transition"
        ) { target ->
            when (target) {
                0 -> SplashScreen()
                2 -> ChannelsScreen(onBack = { screen = 1 })
                3 -> MoviesScreen(onBack = { screen = 1 })
                else -> HomeScreen(
                    onChannelsClick = { screen = 2 },
                    onMoviesClick = { screen = 3 }
                )
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.72f,
        animationSpec = tween(1050),
        label = "splash-logo-scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(900),
        label = "splash-logo-alpha"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF030505), Color(0xFF111816), Color.Black))
        ),
        contentAlignment = Alignment.Center
    ) {
        DecorativeGlow()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale).alpha(alpha).padding(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(RoundedCornerShape(44.dp))
                    .background(Color.White.copy(alpha = 0.035f))
                    .border(1.dp, AccentGreen.copy(alpha = 0.25f), RoundedCornerShape(44.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ziko_logo),
                    contentDescription = "Ziko TV logo",
                    modifier = Modifier.fillMaxSize().padding(9.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(Modifier.height(26.dp))
            Text(
                text = "ZIKO TV",
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(9.dp))
            Text(
                text = "YOUR ENTERTAINMENT UNIVERSE",
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 10.sp,
                letterSpacing = 2.4.sp
            )
            Spacer(Modifier.height(29.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { index ->
                    Box(
                        Modifier
                            .size(if (index == 1) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(AccentGreen.copy(alpha = if (index == 1) 0.95f else 0.40f))
                    )
                }
            }
        }
        Text(
            text = "MOVIES  •  ENTERTAINMENT",
            color = Color.White.copy(alpha = 0.32f),
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 25.dp)
        )
    }
}

@Composable
private fun HomeScreen(onChannelsClick: () -> Unit, onMoviesClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Black, Color(0xFF0A100E), Color.Black))
        )
    ) {
        DecorativeGlow()
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(R.drawable.ziko_logo),
                        contentDescription = "Ziko TV logo",
                        modifier = Modifier.size(94.dp).clip(RoundedCornerShape(28.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.height(13.dp))
                    Text("ZIKO TV", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("A BETTER WAY TO WATCH", color = Color.White.copy(alpha = 0.48f), fontSize = 11.sp, letterSpacing = 2.sp)
                }
            }
            item { Spacer(Modifier.height(7.dp)) }
            item {
                HomeSectionCard(
                    title = "Movies",
                    subtitle = "Explore movies, collections and genres",
                    iconRes = R.drawable.ic_movies,
                    onClick = onMoviesClick
                )
            }
            item {
                HomeSectionCard(
                    title = "Channels",
                    subtitle = "Live channels section",
                    iconRes = R.drawable.ic_channels,
                    onClick = onChannelsClick
                )
            }
            item {
                Text(
                    text = "DISCOVER • SEARCH • WATCH",
                    color = AccentGreen.copy(alpha = 0.50f),
                    fontSize = 10.sp,
                    letterSpacing = 2.2.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun HomeSectionCard(title: String, subtitle: String, iconRes: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.55f)
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1B2924), Color(0xFF101311), Color(0xFF080909))))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(30.dp))
            .clickable(onClick = onClick)
            .padding(23.dp)
    ) {
        Box(
            modifier = Modifier.align(Alignment.Center).size(94.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(AccentGreen.copy(alpha = 0.09f))
                .border(1.dp, AccentGreen.copy(alpha = 0.22f), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(iconRes), contentDescription = title, tint = Color.Unspecified, modifier = Modifier.size(55.dp))
        }
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(title, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = Color.White.copy(alpha = 0.54f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Icon(
            painter = painterResource(R.drawable.ic_arrow_forward),
            contentDescription = "Open $title",
            tint = AccentGreen.copy(alpha = 0.80f),
            modifier = Modifier.align(Alignment.TopEnd).size(22.dp)
        )
    }
}

@Composable
fun ChannelsScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Black, Color(0xFF080D0B), Color.Black))
        )
    ) {
        DecorativeGlow()
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            BackButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(18.dp))
            Column(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.ziko_logo),
                    contentDescription = "Ziko TV logo",
                    modifier = Modifier.size(78.dp).clip(RoundedCornerShape(24.dp))
                )
                Spacer(Modifier.height(12.dp))
                Text("CHANNELS", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            }
            // Intentionally empty: the Channels screen contains no channel cards or channel data.
        }
    }
}

@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(44.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.13f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(painter = painterResource(R.drawable.ic_back), contentDescription = "Back", tint = Color.White, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun DecorativeGlow() {
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.size(300.dp).align(Alignment.TopEnd).background(
                Brush.radialGradient(listOf(AccentGreen.copy(alpha = 0.12f), Color.Transparent), radius = 470f), CircleShape
            )
        )
        Box(
            modifier = Modifier.size(230.dp).align(Alignment.BottomStart).background(
                Brush.radialGradient(listOf(Color(0xFF316B55).copy(alpha = 0.09f), Color.Transparent)), CircleShape
            )
        )
    }
}
