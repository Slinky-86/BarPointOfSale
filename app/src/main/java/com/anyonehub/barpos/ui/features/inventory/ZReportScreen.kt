/* Copyright 2024 anyone-Hub */
package com.anyonehub.barpos.ui.features.inventory

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anyonehub.barpos.util.ZReportResult
import java.util.Locale

@Composable
fun ZReportScreen(
    viewModel: ZReportViewModel,
    inventoryViewModel: InventoryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val total by viewModel.shiftTotal.collectAsState()
    val items by viewModel.unreportedItems.collectAsState()

    // Observe Inventory events for sync/cloud feedback
    LaunchedEffect(Unit) {
        inventoryViewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Collect the result to trigger navigation and show feedback once finalized
    LaunchedEffect(Unit) {
        viewModel.reportResult.collect { result ->
            when (result) {
                is ZReportResult.Success -> {
                    Toast.makeText(context, "Z-Report Finalized Successfully", Toast.LENGTH_LONG).show()
                    onNavigateBack()
                }
                is ZReportResult.Error -> {
                    Toast.makeText(context, "Error: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp)
    ) {
        // Header
        Text(
            text = "SHIFT END AUDIT (Z-REPORT)",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Total Shift Revenue", color = Color.Gray, fontSize = 14.sp)
                Text(
                    text = String.format(Locale.US, "$%.2f", total),
                    color = Color(0xFF4CAF50),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${items.size} unique items in this batch",
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Item Breakdown List
        Text("Itemized Breakdown", color = Color.White, fontWeight = FontWeight.SemiBold)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            items(items.toList()) { (tabItem, menuItem) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(menuItem.name, color = Color.LightGray)
                    Text(
                        text = String.format(Locale.US, "$%.2f", tabItem.priceAtTimeOfSale),
                        color = Color.White
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 0.5.dp,
                    color = Color(0xFF333333)
                )
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.weight(1f).height(60.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("CANCEL")
            }

            Button(
                onClick = {
                    viewModel.runZReport(managerId = 1) // 1 = Seeded Manager ID from AppDatabase
                },
                enabled = items.isNotEmpty(),
                modifier = Modifier.weight(1f).height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Text("FINALIZE & CLOSE", fontWeight = FontWeight.Bold)
            }
        }
    }
}
