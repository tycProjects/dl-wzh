package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Custom Splash Screen designed so your own Lottie/video splash animation
 * can be dropped directly into [CustomSplashAnimationSlot].
 */
@Composable
fun NexoraSplashScreen(
    onFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraBlack)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_esports_hero),
            contentDescription = "NexoraHub splash background",
            contentScale = ContentScale.Crop,
            alpha = 0.42f,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NexoraBlack.copy(alpha = 0.72f),
                            NexoraBlack.copy(alpha = 0.55f),
                            NexoraBlack
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(12.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CustomSplashAnimationSlot()
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "NEXORAHUB",
                    style = MaterialTheme.typography.displayLarge.copy(
                        letterSpacing = 3.sp
                    ),
                    color = SoftWhite
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "PLAY  •  COMPETE  •  CONQUER",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 2.sp
                    ),
                    color = MetallicGold
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(34.dp),
                    color = ElectricViolet,
                    trackColor = NexoraClay,
                    strokeWidth = 3.dp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Initializing NexoraHub Arena...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoolGrey
                )
            }
        }
    }
}

/**
 * Isolated animation container: replace this body with LottieAnimation or ExoPlayer
 * when integrating your custom NexoraHub splash asset.
 */
@Composable
fun CustomSplashAnimationSlot() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_ring")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier.size(164.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = (size.minDimension / 2f) - 10f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonPurple.copy(alpha = 0.35f * pulse), Color.Transparent)
                ),
                radius = radius * 1.1f
            )
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(ElectricViolet, MetallicGold, NeonPurple, ElectricViolet)
                ),
                startAngle = angle,
                sweepAngle = 250f,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(10f, 10f),
                size = androidx.compose.ui.geometry.Size(size.width - 20f, size.height - 20f)
            )
        }

        Image(
            painter = painterResource(id = R.drawable.ic_nexora_icon),
            contentDescription = "NexoraHub Emblem",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(116.dp)
                .clip(CircleShape)
                .border(2.dp, GoldBorder, CircleShape)
        )
    }
}

@Composable
fun WelcomeAuthScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onSignInWithGoogle: () -> Unit
) {
    var acceptedTerms by remember { mutableStateOf(false) }
    var showConsentHint by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraBlack)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_esports_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.38f,
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NexoraBlack.copy(alpha = 0.25f),
                            NexoraBlack.copy(alpha = 0.85f),
                            NexoraBlack
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .padding(top = 18.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_nexora_icon),
                    contentDescription = "NexoraHub Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .border(2.dp, GoldBorder, CircleShape)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Welcome to",
                    style = MaterialTheme.typography.titleMedium,
                    color = CoolGrey
                )
                Text(
                    text = "NEXORAHUB",
                    style = MaterialTheme.typography.displayLarge.copy(letterSpacing = 2.sp),
                    color = SoftWhite
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "PLAY  •  COMPETE  •  CONQUER",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    color = MetallicGold
                )
            }

            Spacer(Modifier.height(24.dp))

            NexoraGlassCard(
                modifier = Modifier.widthIn(max = 520.dp),
                borderColor = VioletBorder,
                contentPadding = PaddingValues(20.dp)
            ) {
                Text(
                    text = "Player Sign In & Consent",
                    style = MaterialTheme.typography.titleMedium,
                    color = SoftWhite
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Please review and accept our Terms & Conditions and Privacy Policy to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoolGrey
                )
                Spacer(Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NexoraSurface,
                    border = BorderStroke(1.dp, DividerDark)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenTerms() }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = ElectricViolet)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Terms & Conditions",
                                style = MaterialTheme.typography.titleSmall,
                                color = SoftWhite,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = CoolGrey)
                        }
                        HorizontalDivider(color = DividerDark)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenPrivacy() }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = ElectricViolet)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Privacy Policy",
                                style = MaterialTheme.typography.titleSmall,
                                color = SoftWhite,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = CoolGrey)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            acceptedTerms = !acceptedTerms
                            if (acceptedTerms) showConsentHint = false
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = acceptedTerms,
                        onCheckedChange = {
                            acceptedTerms = it
                            if (it) showConsentHint = false
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = NeonPurple,
                            uncheckedColor = CoolGrey,
                            checkmarkColor = SoftWhite
                        ),
                        modifier = Modifier.testTag("accept_terms_checkbox")
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "I accept the Terms & Conditions and Privacy Policy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftWhite
                    )
                }

                AnimatedVisibility(visible = showConsentHint) {
                    Text(
                        text = "Please accept the Terms & Conditions and Privacy Policy first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningAmber,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = ErrorContainer,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorRed,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (acceptedTerms) {
                            onSignInWithGoogle()
                        } else {
                            showConsentHint = true
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (acceptedTerms) SoftWhite else NexoraSurface,
                        contentColor = if (acceptedTerms) NexoraBlack else CoolGrey
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = NeonPurple,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Authenticating...", style = MaterialTheme.typography.labelLarge)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(Color(0xFF4285F4), Color(0xFF34A853), Color(0xFFFBBC05), Color(0xFFEA4335))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "G",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Secure Google Auth • Built for Free Fire Community",
                        style = MaterialTheme.typography.bodySmall,
                        color = CoolGrey,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
