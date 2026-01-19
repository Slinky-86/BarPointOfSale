/* Copyright 2024 anyone-Hub */
package com.anyonehub.barpos.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anyonehub.barpos.R

data class TutorialStep(
    val title: String,
    val description: String,
    val iconRes: Int? = null,
    val alignment: Alignment = Alignment.Center
)

@Composable
fun TutorialOverlay(
    onComplete: () -> Unit
) {
    val steps = listOf(
        TutorialStep(
            "Welcome to MidTown POS",
            "Experience the new Pure Compose Material 3 engine. Fast, fluid, and built for high-volume service.",
            R.drawable.ic_midtown_3d,
            Alignment.Center
        ),
        TutorialStep(
            "The Shot Wall",
            "Quick-access spirits on the left. Real-time inventory tracking ensures you never sell a bottle you don\'t have.",
            R.drawable.ic_shot_3d,
            Alignment.CenterStart
        ),
        TutorialStep(
            "The Active Tab",
            "Your current customer\'s ticket is always visible on the right. Tap items to add notes, void, or edit prices in real-time.",
            R.drawable.ic_customers_3d,
            Alignment.CenterEnd
        ),
        TutorialStep(
            "Cloud Heartbeat",
            "The pulsing cloud icon in the top bar monitors your Supabase connection. It ensures your data is synced and backed up every second.",
            R.drawable.ic_cloud_upload,
            Alignment.TopEnd
        ),
        TutorialStep(
            "Navigation Drawer",
            "Tap the menu icon (top left) to access Sales History, Z-Reports, and the Staff Time Clock.",
            R.drawable.ic_menu_3d,
            Alignment.TopStart
        ),
        TutorialStep(
            "Admin Engine",
            "Toggle the 3D Admin icon (bottom right) to enter Edit Mode. Update prices, restock items, or modify the menu on the fly.",
            R.drawable.ic_admin_3d,
            Alignment.BottomEnd
        ),
        TutorialStep(
            "System Ready",
            "You\'re logged in and synced. Let\'s get to work!",
            R.drawable.ic_3d_lock,
            Alignment.Center
        )
    )

    var currentStepIdx by remember { mutableIntStateOf(0) }
    val currentStep = steps[currentStepIdx]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(24.dp),
        contentAlignment = currentStep.alignment
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentStep.iconRes != null) {
                    Icon(
                        painter = painterResource(id = currentStep.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = currentStep.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentStep.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onComplete) {
                        Text("SKIP", color = Color.Gray)
                    }

                    Button(
                        onClick = {
                            if (currentStepIdx < steps.size - 1) {
                                currentStepIdx++
                            } else {
                                onComplete()
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (currentStepIdx == steps.size - 1) "FINISH" else "NEXT")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(id = if (currentStepIdx == steps.size - 1) R.drawable.ic_3d_lock else R.drawable.ic_menu_rotate),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.Unspecified
                        )
                    }
                }
            }
        }

        // Close Button in Top Right
        IconButton(
            onClick = onComplete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_3d_delete), 
                contentDescription = "Close Tutorial", 
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
