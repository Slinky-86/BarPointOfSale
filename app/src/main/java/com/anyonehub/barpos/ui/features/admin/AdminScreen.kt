// Copyright 2024 anyone-Hub
@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.anyonehub.barpos.ui.features.admin

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anyonehub.barpos.R
import com.anyonehub.barpos.ui.IconUtils
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Locale
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val staff by viewModel.staffProfiles.collectAsState()
    val sales by viewModel.liveSales.collectAsState()
    val lowStock by viewModel.lowStockItems.collectAsState()
    val totalRev by viewModel.totalRevenue.collectAsState()
    val lastSync by viewModel.lastSyncTime.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("ADMIN INSIGHTS", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        // Online-First Status Indicator
                        Text(
                            text = "Cloud Authoritative: ${lastSync.formatToLocalTime()}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshCloudStats() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Cloud Data")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. REVENUE OVERVIEW (Online-First)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_zreport_3d),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("SHIFT TOTAL REVENUE (Cloud)", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                        Text(
                            text = String.format(Locale.US, "$%.2f", totalRev),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // 2. LIVE STAFF MONITOR
            item {
                Text("STAFF ACTIVITY (Real-time)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            items(staff) { user ->
                ListItem(
                    headlineContent = { Text(user.name, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Role: ${user.role.label}") },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = IconUtils.getIconResource("customers")),
                            null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                    },
                    trailingContent = {
                        Badge(containerColor = if (user.isActive) Color(0xFF4CAF50) else Color.Gray) {
                            Text(if (user.isActive) "ACTIVE" else "OFFLINE")
                        }
                    }
                )
                HorizontalDivider()
            }

            // 3. LOW STOCK ALERTS
            if (lowStock.isNotEmpty()) {
                item {
                    Text("INVENTORY ALERTS", fontWeight = FontWeight.Bold, color = Color.Red)
                }
                items(lowStock) { item ->
                    ListItem(
                        headlineContent = { Text(item.name) },
                        supportingContent = { Text("Only ${item.inventoryCount} left in stock") },
                        leadingContent = { 
                            Icon(
                                painter = painterResource(id = R.drawable.ic_3d_refill), 
                                contentDescription = null, 
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            ) 
                        }
                    )
                }
            }

            // 4. LIVE SALES FEED
            item {
                Text("RECENT TRANSACTIONS (Synced)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            items(sales) { sale ->
                ListItem(
                    headlineContent = { Text("Sale #${sale.id}") },
                    supportingContent = { Text("Payment: ${sale.paymentType}") },
                    trailingContent = {
                        Text(
                            String.format(Locale.US, "$%.2f", sale.totalAmount),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
        }
    }
}

/**
 * Extension to format kotlin.time.Instant into a human-readable local time string.
 */
@OptIn(kotlin.time.ExperimentalTime::class)
private fun Instant.formatToLocalTime(): String {
    // In kotlinx-datetime 0.6+, Instant IS kotlin.time.Instant.
    // We can call toLocalDateTime directly on it using the extension from the library.
    val localDateTime = this.toLocalDateTime(TimeZone.currentSystemDefault())
    return String.format(Locale.US, "%02d:%02d:%02d", localDateTime.hour, localDateTime.minute, localDateTime.second)
}
