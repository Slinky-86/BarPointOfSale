// Copyright 2024 anyone-Hub
package com.anyonehub.barpos.ui.features.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anyonehub.barpos.R
import java.util.Locale

@Composable
fun TipOutDialog(
    serverName: String,
    totalTips: Double,
    salesCount: Int,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f) // Adjusted for landscape POS tablet view
                .padding(16.dp)
                // 3D Metallic Border Effect
                .border(
                    width = 3.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFECEFF1), // Light reflection (Top)
                            Color(0xFF607D8B), // Mid-tone
                            Color(0xFF263238)  // Shadow (Bottom)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    color = Color(0xFF1C1C1E), // Industrial deep black
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // High-Fi 3D Icon Header
                Image(
                    painter = painterResource(id = R.drawable.ic_3d_lock),
                    contentDescription = "Session Summary",
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "SERVER SHIFT REPORT",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )

                Text(
                    text = serverName.uppercase(Locale.US),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Data Rows - Industrial Style
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryStatItem("TRANSACTIONS", salesCount.toString())
                    SummaryStatItem(
                        label = "TIPS EARNED",
                        value = String.format(Locale.US, "$%.2f", totalTips),
                        isGlow = true
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "DISMISS SUMMARY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStatItem(
    label: String,
    value: String,
    isGlow: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = if (isGlow) Color(0xFF69F0AE) else Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black
        )
    }
}