/* Copyright 2024 anyone-Hub */
package com.anyonehub.barpos.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anyonehub.barpos.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    // 1. Animation States
    val entranceProgress = remember { Animatable(0f) }
    val fillProgress = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Step 1: High-Energy Rotating Entrance (0s - 1.8s)
        launch {
            entranceProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1800, easing = OvershootInterpolator(1.5f).toEasing())
            )
        }

        // Wait for the logo to finish spinning and settle
        delay(2000)

        // Step 2: The Mug Fill (Liquid rises once logo is locked)
        fillProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2500, easing = LinearOutSlowInEasing)
        )

        // Step 3: Text Reveal
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )

        delay(1200)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F)), // Deeper Onyx for better contrast
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            
            Box(
                modifier = Modifier
                    .size(220.dp) // Slightly larger for better 3D impact
                    .graphicsLayer {
                        scaleX = entranceProgress.value
                        scaleY = entranceProgress.value
                        // Spins 2.5 full rotations (900 degrees) during the fade-in
                        rotationZ = (1f - entranceProgress.value) * 900f
                        alpha = entranceProgress.value
                    },
                contentAlignment = Alignment.Center
            ) {
                // Background Base Logo (The "Empty" Glass look)
                Image(
                    painter = painterResource(id = R.drawable.ic_midtown_3d),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().alpha(0.2f)
                )

                // THE FILLING LAYER (Liquid rises)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 20.dp) // Adjust for 3D glass bottom thickness
                        .drawWithContent {
                            val fillHeight = size.height * fillProgress.value
                            clipRect(
                                top = size.height - fillHeight,
                                bottom = size.height,
                                clipOp = ClipOp.Intersect
                            ) {
                                this@drawWithContent.drawContent()
                            }
                        }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_midtown_3d),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Brand Text with Modern Spacing
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .offset(y = ((1f - textAlpha.value) * 15).dp)
            ) {
                Text(
                    text = "MidTown POS",
                    color = Color(0xFF00E676),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp
                )
                Text(
                    text = "AUTHORITATIVE CLOUD TERMINAL",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
            }
        }

        // Loading Progress Indicator
        if (fillProgress.value < 1f && fillProgress.value > 0f) {
            LinearProgressIndicator(
                progress = { fillProgress.value },
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp),
                color = Color(0xFF00E676),
                trackColor = Color.White.copy(alpha = 0.05f)
            )
        }

        Text(
            text = "ESTABLISHING SECURE CLOUD HANDSHAKE...",
            color = Color.White.copy(alpha = 0.2f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

/**
 * Helper to convert Android Interpolator to Compose Easing
 */
fun android.view.animation.Interpolator.toEasing() = Easing { x -> getInterpolation(x) }
class OvershootInterpolator(val tension: Float) : android.view.animation.Interpolator {
    override fun getInterpolation(t: Float): Float {
        val it = t - 1.0f
        return it * it * ((tension + 1) * it + tension) + 1.0f
    }
}
