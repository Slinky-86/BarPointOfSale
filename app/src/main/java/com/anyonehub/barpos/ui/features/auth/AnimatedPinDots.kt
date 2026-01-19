// Copyright 2024 anyone-Hub
package com.anyonehub.barpos.ui.features.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedPinDots(
    pinLength: Int,
    maxDots: Int = 6,
    isError: Boolean = false,
    isSuccess: Boolean = false
) {
    val shakeOffset = remember { Animatable(0f) }
    val successScale = remember { Animatable(1f) }

    // Error Shake Effect
    LaunchedEffect(isError) {
        if (isError) {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    (-20f) at 50
                    20f at 100
                    (-20f) at 150
                    20f at 200
                    (-10f) at 250
                    10f at 300
                    (-5f) at 350
                }
            )
        }
    }

    // Success Pulse Effect
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            successScale.animateTo(
                targetValue = 1.3f,
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            )
            successScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .offset { IntOffset(shakeOffset.value.toInt(), 0) }
            .scale(successScale.value),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxDots) { index ->
            val isFilled = index < pinLength
            
            // Smoother color transition
            val dotColor by animateColorAsState(
                targetValue = when {
                    isError -> Color.Red
                    isSuccess -> Color(0xFF4CAF50)
                    isFilled -> MaterialTheme.colorScheme.primary
                    else -> Color.Gray.copy(alpha = 0.2f)
                },
                animationSpec = tween(250),
                label = "DotColor"
            )

            // Individual Dot Entrance Animation
            val dotScale by animateFloatAsState(
                targetValue = if (isFilled) 1.2f else 1.0f,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "IndividualDotScale"
            )

            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp)
                    .scale(dotScale)
                    .clip(CircleShape)
                    .background(dotColor)
                    .then(
                        if (!isFilled && !isError && !isSuccess) {
                            Modifier.border(1.dp, Color.Gray.copy(alpha = 0.4f), CircleShape)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Glow effect for filled dots
                if (isFilled || isSuccess) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
                                )
                            )
                    )
                }
            }
        }
    }
}
