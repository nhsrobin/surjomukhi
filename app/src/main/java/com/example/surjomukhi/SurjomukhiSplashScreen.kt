package com.example.surjomukhi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun SurjomukhiSplashScreen(
    onInitializationComplete: () -> Unit
) {
    // Animation scale and opacity states
    val logoScale = remember { Animatable(0.3f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val watermarkAlpha = remember { Animatable(0f) }

    // Infinite breathing and rotation animations for ultra-modern vibe
    val infiniteTransition = rememberInfiniteTransition(label = "SplashInfinite")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "HaloRotation"
    )

    val auraGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraGlow"
    )

    // Trigger sequential entrance animations on initial compose
    LaunchedEffect(Unit) {
        // Scale & fade in logo
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        logoAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )
        // Fade in text title
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )
        // Fade in watermark
        watermarkAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500)
        )

        // Ensure splash stays active until properly initialized (min 2.2 seconds display)
        delay(2200)
        onInitializationComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E3A8A), // Deep radiant blue/indigo center
                        Color(0xFF0F172A), // Dark slate
                        Color(0xFF030712)  // Ultra dark midnight background
                    ),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // BACKGROUND ROTATING GLOW HALO
        Box(
            modifier = Modifier
                .size(240.dp)
                .rotate(haloRotation)
                .scale(pulseScale)
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = auraGlowAlpha),
                            Color(0xFF38BDF8).copy(alpha = auraGlowAlpha),
                            Color(0xFFF43F5E).copy(alpha = auraGlowAlpha),
                            Color(0xFFFFD700).copy(alpha = auraGlowAlpha)
                        )
                    ),
                    shape = CircleShape
                )
                .blur(28.dp)
        )

        // CENTER CONTAINER FOR LOGO & APP BRANDING
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            // CIRCLE MASKED ICON WITH GRADIENT BORDER
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(logoScale.value * pulseScale)
                    .alpha(logoAlpha.value)
            ) {
                // Outer ring border
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .rotate(haloRotation)
                        .border(
                            width = 2.5.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFFF59E0B),
                                    Color(0xFF10B981),
                                    Color(0xFF6366F1),
                                    Color(0xFFF59E0B)
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Circle Masked App Icon
                Image(
                    painter = painterResource(id = R.drawable.icon),
                    contentDescription = "Surjomukhi App Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // APP TITLE & TAGLINE WITH FADE ANIMATION
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(textAlpha.value)
            ) {
                Text(
                    text = "সূর্যমুখী",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "আপনার ভালোবাসার সাথেই সংযোগ...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                )
            }
        }

        // BOTTOM WATERMARK "by TasfiwnLabs"
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "by TasfiwnLabs",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.SansSerif,
                color = Color.White.copy(alpha = 0.55f),
                letterSpacing = 0.8.sp,
                modifier = Modifier.alpha(watermarkAlpha.value)
            )
        }
    }
}
