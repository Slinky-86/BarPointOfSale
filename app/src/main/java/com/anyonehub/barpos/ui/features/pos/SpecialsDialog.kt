// Copyright 2024 anyone-Hub
@file:OptIn(ExperimentalTime::class)

package com.anyonehub.barpos.ui.features.pos

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource // Required for XMLs
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anyonehub.barpos.R // Required to find your new XMLs
import com.anyonehub.barpos.data.MenuItem
import org.json.JSONObject
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialsDialog(
    allMenuItems: List<MenuItem>,
    specialsJson: String,
    onDismiss: () -> Unit,
    onUpdateSpecial: (Int, Double) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val currentSpecials = remember(specialsJson) {
        try {
            JSONObject(specialsJson.ifBlank { "{}" })
        } catch (_: Exception) {
            JSONObject("{}")
        }
    }

    val filteredItems = remember(searchQuery, allMenuItems) {
        allMenuItems.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // FIXED: Using 3D Tag Icon
                Icon(
                    painter = painterResource(id = R.drawable.ic_tag_3d),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = androidx.compose.ui.graphics.Color.Unspecified // Keep 3D colors
                )
                Spacer(Modifier.width(12.dp))
                Text("Daily Specials Manager")
            }
        },
        text = {
            Column {
                Text(
                    "Assign fixed prices to specific items. Enter 0 to remove.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search menu...") },
                    modifier = Modifier.fillMaxWidth(),
                    // FIXED: Using 3D Search Icon
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search_3d),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = androidx.compose.ui.graphics.Color.Unspecified
                        )
                    },
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredItems) { item ->
                        val currentSpecialPrice = if (currentSpecials.has(item.id.toString())) {
                            currentSpecials.getDouble(item.id.toString()).toString()
                        } else ""

                        var localPriceInput by remember(item.id) { mutableStateOf(currentSpecialPrice) }

                        Card(
                            modifier = Modifier.padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (localPriceInput.isNotEmpty() && localPriceInput != "0.0")
                                    MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Bold)
                                    Text("Reg: $${String.format(Locale.US, "%.2f", item.price)}", fontSize = 12.sp)
                                }

                                OutlinedTextField(
                                    value = localPriceInput,
                                    onValueChange = {
                                        localPriceInput = it
                                        val price = it.toDoubleOrNull() ?: 0.0
                                        
                                        // High-Precision Audit: Capture exact time special price was assigned
                                        val updateTime: Instant = Clock.System.now()
                                        Log.d("SpecialsDialog", "Price update for '${item.name}' initiated at $updateTime")
                                        
                                        onUpdateSpecial(item.id, price)
                                    },
                                    label = { Text("Special $") },
                                    modifier = Modifier.width(100.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("FINISH") }
        }
    )
}
