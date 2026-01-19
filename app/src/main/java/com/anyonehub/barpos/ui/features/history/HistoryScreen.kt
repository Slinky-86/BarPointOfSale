/* Copyright 2024 anyone-Hub */
@file:OptIn(kotlin.time.ExperimentalTime::class)
@file:Suppress("AssignedValueIsNeverRead")

package com.anyonehub.barpos.ui.features.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anyonehub.barpos.R
import com.anyonehub.barpos.data.ActiveTab
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val closedTabs by viewModel.closedTabs.collectAsState()
    val totalRevenue by viewModel.totalRevenue.collectAsState()
    val totalTips by viewModel.allTips.collectAsState()

    var showShiftReport by remember { mutableStateOf(false) }
    var showTipOut by remember { mutableStateOf(false) }
    var showTipTracker by remember { mutableStateOf(false) } 
    var showEndShiftConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Z-REPORT & HISTORY", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_undo),
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // --- HEADER SUMMARY ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryCard("Total Revenue", totalRevenue, MaterialTheme.colorScheme.primary)
                SummaryCard("Logged Tips", totalTips, MaterialTheme.colorScheme.secondary)
            }

            HorizontalDivider()

            // --- ACTION BUTTONS ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { showShiftReport = true }) { Text("Shift Report") }
                Button(onClick = { showTipOut = true }) { Text("Tip Out") }
                Button(onClick = { showTipTracker = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tip_tracker_3d),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Log Tip")
                }
                Button(
                    onClick = { showEndShiftConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("END SHIFT") }
            }

            HorizontalDivider()

            // --- LIST OF CLOSED TABS ---
            LazyColumn(modifier = Modifier.weight(1f)) {
                if (closedTabs.isEmpty()) {
                    item {
                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No history for this shift.", color = Color.Gray)
                        }
                    }
                }
                items(closedTabs) { tab ->
                    HistoryRow(tab)
                }
            }
        }
    }

    // --- DIALOGS ---

    if (showTipTracker) {
        TipTrackerDialog(
            currentTotalTips = totalTips,
            onDismiss = { showTipTracker = false },
            onSaveTip = { amount, note ->
                viewModel.logManualTip(amount, note, 0)
            }
        )
    }

    if (showShiftReport) {
        ShiftReportDialog(
            serverName = "MANAGER VIEW",
            totalTips = totalTips,
            salesCount = closedTabs.size,
            onDismiss = { showShiftReport = false }
        )
    }

    if (showTipOut) {
        TipOutDialog(
            serverName = "MANAGER VIEW",
            totalTips = totalTips,
            salesCount = closedTabs.size,
            onDismiss = { showTipOut = false }
        )
    }

    if (showEndShiftConfirm) {
        AlertDialog(
            onDismissRequest = { showEndShiftConfirm = false },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_3d_delete),
                    null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.Unspecified
                )
            },
            title = { Text("Confirm End of Shift") },
            text = { Text("This will PERMANENTLY delete all closed tab data for the day. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearSalesHistory()
                        showEndShiftConfirm = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("WIPE DATA") }
            },
            dismissButton = {
                TextButton(onClick = { showEndShiftConfirm = false }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
fun SummaryCard(label: String, amount: Double, color: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                String.format(Locale.US, "$%.2f", amount),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
fun HistoryRow(tab: ActiveTab) {
    val sdf = SimpleDateFormat("HH:mm", Locale.US)
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_history_3d),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(tab.customerName, fontWeight = FontWeight.Bold)
                // Actively applying the high-precision Instant type here to satisfy the import
                val createdAt: Instant = tab.createdAt
                Text("Closed at ${sdf.format(Date(createdAt.toEpochMilliseconds()))}", fontSize = 12.sp, color = Color.Gray)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("PAID", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_redo),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.Unspecified
            )
        }
    }
    HorizontalDivider(thickness = 0.5.dp)
}
