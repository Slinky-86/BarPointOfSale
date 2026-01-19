/* Copyright 2024 anyone-Hub */
@file:Suppress("AssignedValueIsNeverRead")

package com.anyonehub.barpos.ui.shared.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anyonehub.barpos.R
import com.anyonehub.barpos.data.ActiveTab
import com.anyonehub.barpos.data.Customer
import com.anyonehub.barpos.ui.IconUtils
import com.anyonehub.barpos.ui.features.pos.PosViewModel

@Composable
fun TabSelectionDialog(
    openTabs: List<ActiveTab>,
    currentTabId: Long,
    onDismiss: () -> Unit,
    onTabSelected: (Long) -> Unit,
    onCreateNew: (String) -> Unit,
    onRenameTab: (Long, String) -> Unit,
    // Add ViewModel for search logic
    posViewModel: PosViewModel? = null 
) {
    var isCreatingNew by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }
    val suggestions by posViewModel?.customerSuggestions?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList<Customer>()) }

    var tabToRename by remember { mutableStateOf<ActiveTab?>(null) }
    var renameText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isCreatingNew) "FIND OR CREATE CUSTOMER" else "SELECT ACTIVE TAB",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            if (isCreatingNew) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Search existing or enter new name:", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { 
                            searchInput = it
                            posViewModel?.updateCustomerSearch(it)
                        },
                        label = { Text("Customer Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { 
                            if (searchInput.isNotEmpty()) {
                                IconButton(onClick = { searchInput = "" }) { 
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_3d_delete), 
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.Unspecified
                                    ) 
                                } 
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_search_3d), 
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.Unspecified
                                )
                            }
                        }
                    )

                    // --- SEARCH SUGGESTIONS ---
                    if (suggestions.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Existing Customers:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.heightIn(max = 200.dp).padding(vertical = 4.dp)
                        ) {
                            LazyColumn {
                                items(suggestions) { customer ->
                                    ListItem(
                                        headlineContent = { Text(customer.name, fontWeight = FontWeight.Bold) },
                                        supportingContent = { customer.phone?.let { Text(it) } },
                                        modifier = Modifier.clickable {
                                            onCreateNew(customer.name)
                                            onDismiss()
                                        }
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.heightIn(max = 400.dp)) {
                    if (openTabs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No open tabs found.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn {
                            items(openTabs, key = { it.id }) { tab ->
                                val isActive = tab.id == currentTabId

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            onTabSelected(tab.id)
                                            onDismiss()
                                        },
                                    color = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(
                                                painter = if (isActive) painterResource(id = R.drawable.ic_3d_lock) else painterResource(id = R.drawable.ic_customers_3d),
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp),
                                                tint = Color.Unspecified
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(tab.customerName.uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                                Text("TAB #${tab.id}", fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }

                                        IconButton(onClick = {
                                            renameText = tab.customerName
                                            tabToRename = tab
                                        }) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_3d_lock), 
                                                contentDescription = "Rename", 
                                                modifier = Modifier.size(20.dp),
                                                tint = Color.Unspecified
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isCreatingNew) {
                Button(
                    onClick = {
                        if (searchInput.isNotBlank()) {
                            onCreateNew(searchInput)
                            onDismiss()
                        }
                    },
                    enabled = searchInput.isNotBlank()
                ) {
                    Text("START TAB")
                }
            } else {
                Button(onClick = { isCreatingNew = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_3d_add), 
                        contentDescription = null, 
                        modifier = Modifier.size(18.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("NEW TAB")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (isCreatingNew) {
                    isCreatingNew = false
                } else {
                    onDismiss()
                }
            }) {
                Text("CANCEL")
            }
        }
    )

    if (tabToRename != null) {
        AlertDialog(
            onDismissRequest = { tabToRename = null },
            title = { Text("RENAME TAB #${tabToRename?.id}") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("New Customer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val target = tabToRename
                    if (target != null && renameText.isNotBlank()) {
                        onRenameTab(target.id, renameText)
                        tabToRename = null
                    }
                }) {
                    Text("UPDATE")
                }
            },
            dismissButton = {
                TextButton(onClick = { tabToRename = null }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
fun LineItemActionDialog(
    itemName: String,
    currentNote: String,
    isAdmin: Boolean = false,
    onDismiss: () -> Unit,
    onVoid: () -> Unit,
    onSaveNote: (String) -> Unit,
    onEditPrice: () -> Unit
) {
    var note by remember { mutableStateOf(currentNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(itemName.uppercase(), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Add Item Note") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (isAdmin) {
                    Button(
                        onClick = onEditPrice,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_3d_lock), 
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("OVERRIDE PRICE")
                    }
                } else {
                    Text(
                        "Price editing and voiding requires Manager PIN.", 
                        fontSize = 11.sp, 
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSaveNote(note) }) {
                Text("SAVE NOTE")
            }
        },
        dismissButton = {
            Row {
                if (isAdmin) {
                    TextButton(onClick = onVoid, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_3d_delete),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("VOID ITEM")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("CLOSE")
                }
            }
        }
    )
}