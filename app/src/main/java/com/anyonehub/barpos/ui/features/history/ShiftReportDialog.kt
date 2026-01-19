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
fun ShiftReportDialog(
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
                .fillMaxWidth(0.65f)
                .padding(16.dp)
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFECEFF1), Color(0xFF455A64))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    color = Color(0xFF1C1C1E),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 3D Visual Header
                Image(
                    painter = painterResource(id = R.drawable.ic_3d_lock),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )

                Text(
                    text = "SHIFT SUMMARY",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = serverName.uppercase(Locale.US),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Stats Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ReportStatItem("TOTAL SALES", salesCount.toString())
                    ReportStatItem(
                        "TOTAL TIPS",
                        String.format(Locale.US, "$%.2f", totalTips),
                        isGlow = true
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("BACK TO POS", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReportStatItem(label: String, value: String, isGlow: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(
            value,
            color = if (isGlow) Color(0xFF69F0AE) else Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black
        )
    }
}