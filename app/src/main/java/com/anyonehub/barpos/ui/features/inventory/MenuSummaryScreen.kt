/* Copyright 2024 anyone-Hub */
@file:Suppress("AssignedValueIsNeverRead")

package com.anyonehub.barpos.ui.features.inventory

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anyonehub.barpos.R
import com.anyonehub.barpos.data.MenuItem
import com.anyonehub.barpos.ui.IconUtils
import com.anyonehub.barpos.util.MenuExportManager
import com.anyonehub.barpos.util.shareFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuSummaryScreen(
    viewModel: InventoryViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val menuGroups by viewModel.menuGroups.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val allItems by viewModel.allItems.collectAsState()

    var expandedExport by remember { mutableStateOf(false) }

    // --- DIALOG STATES ---
    var showEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<MenuItem?>(null) }
    var itemToRestock by remember { mutableStateOf<MenuItem?>(null) }

    // Use events from InventoryViewModel to show feedback
    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MASTER MENU AUDIT", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.triggerCloudSync() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_3d_refill), 
                            contentDescription = "Sync Cloud",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                    }
                    IconButton(onClick = {
                        itemToEdit = null
                        showEditDialog = true
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_3d_add), 
                            contentDescription = "Add Item",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                    }

                    Box {
                        IconButton(onClick = { expandedExport = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_share_3d), 
                                contentDescription = "Export",
                                modifier = Modifier.size(24.dp),
                                tint = Color.Unspecified
                            )
                        }
                        DropdownMenu(expanded = expandedExport, onDismissRequest = { expandedExport = false }) {
                            DropdownMenuItem(
                                text = { Text("Share PDF") },
                                onClick = {
                                    val file = MenuExportManager.exportToPdf(context, allCategories, allItems)
                                    if (file != null) {
                                        shareFile(context, file, "application/pdf")
                                    }
                                    expandedExport = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export CSV") },
                                onClick = {
                                    val file = MenuExportManager.exportToCsv(context, allCategories, allItems)
                                    if (file != null) {
                                        shareFile(context, file, "text/csv")
                                    }
                                    expandedExport = false
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("MIDTOWN MENU AUDIT", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text("Date: ${SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date())}", color = Color.Gray)
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            menuGroups.forEach { group ->
                item(key = "g_${group.id}") {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = group.name.uppercase(Locale.US),
                            modifier = Modifier.padding(8.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                val groupCategories = allCategories.filter { it.menuGroupId == group.id }
                groupCategories.forEach { category ->
                    item(key = "c_${category.id}") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = IconUtils.getIconResource(category.iconName)),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(category.name, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        }
                    }

                    val items = allItems.filter { it.categoryId == category.id }
                    items(items, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).clickable {
                                itemToEdit = item
                                showEditDialog = true
                            }) {
                                Text(item.name)
                                if (item.description.isNotBlank()) {
                                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Text("Stock: ${item.inventoryCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if(item.inventoryCount < 10) Color.Red else Color.Gray
                                )
                            }

                            IconButton(onClick = { itemToRestock = item }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_3d_refill), 
                                    contentDescription = "Restock", 
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.Unspecified
                                )
                            }

                            Text(String.format(Locale.US, "$%.2f", item.price), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(50.dp)) }
        }
    }

    if (showEditDialog) {
        AddEditItemDialog(
            existingItem = itemToEdit,
            onDismiss = { showEditDialog = false },
            onDelete = {
                itemToEdit?.let { viewModel.deleteMenuItem(it.id) }
                showEditDialog = false
            },
            onConfirm = { n, p, s, st, d, i, hp, bp, hhb ->
                viewModel.saveMenuItem(
                    id = itemToEdit?.id,
                    categoryId = itemToEdit?.categoryId ?: 1,
                    name = n,
                    price = p,
                    isShot = s,
                    stock = st,
                    description = d,
                    iconName = i,
                    hhPrice = hp,
                    bucketPrice = bp,
                    hhBucketPrice = hhb
                )
                showEditDialog = false
            }
        )
    }

    if (itemToRestock != null) {
        RestockDialog(
            item = itemToRestock!!,
            onDismiss = { itemToRestock = null },
            onConfirm = { newCount ->
                viewModel.adjustStock(itemToRestock!!.id, newCount)
                itemToRestock = null
            }
        )
    }
}
