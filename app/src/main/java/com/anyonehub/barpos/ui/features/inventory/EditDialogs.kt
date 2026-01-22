// Copyright 2024 anyone-Hub
package com.anyonehub.barpos.ui.features.inventory

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anyonehub.barpos.data.MenuItem
import com.anyonehub.barpos.ui.IconUtils
import org.json.JSONObject

@Composable
fun AddMenuGroupDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("NEW MENU GROUP", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Example: 'Liquor', 'Drafts', 'Appetizers'", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Group Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onConfirm(name) }, enabled = name.isNotBlank()) { Text("CREATE GROUP") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
fun AddCategoryDialog(onDismiss: () -> Unit, onConfirm: (String, String, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("beer") }
    var requiresBuilder by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("NEW CATEGORY", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category Name") }, placeholder = { Text("e.g. Mixed Drinks") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Select Icon Style:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(IconUtils.availableIcons) { (iconKey, resId) ->
                        Box(modifier = Modifier.size(50.dp).border(width = if (selectedIcon == iconKey) 2.dp else 1.dp, color = if (selectedIcon == iconKey) MaterialTheme.colorScheme.primary else Color.LightGray, shape = CircleShape).clickable { selectedIcon = iconKey }.padding(10.dp)) {
                            Image(painter = painterResource(id = resId), contentDescription = null, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
                Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) { Text("Drink Builder Required", fontWeight = FontWeight.Bold); Text("Prompts for mixers/garnish.", fontSize = 11.sp) }
                        Switch(checked = requiresBuilder, onCheckedChange = { requiresBuilder = it })
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onConfirm(name, selectedIcon, requiresBuilder) }, enabled = name.isNotBlank()) { Text("CREATE") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
fun AddEditItemDialog(
    existingItem: MenuItem? = null,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onConfirm: (MenuItem) -> Unit
) {
    var name by remember { mutableStateOf(existingItem?.name ?: "") }
    var description by remember { mutableStateOf(existingItem?.description ?: "") }
    var selectedIcon by remember { mutableStateOf(existingItem?.iconName ?: "beer") }
    var basePriceStr by remember { mutableStateOf(existingItem?.price?.toString() ?: "0.0") }
    var inventoryStr by remember { mutableStateOf(existingItem?.inventoryCount?.toString() ?: "999") }
    var isShotWall by remember { mutableStateOf(existingItem?.isShotWallItem ?: false) }
    var isFood by remember { mutableStateOf(existingItem?.isFood ?: false) }
    var isModifier by remember { mutableStateOf(existingItem?.isModifier ?: false) }

    // Dynamic Price Management
    var dynamicPrices by remember { 
        mutableStateOf(
            try {
                val json = JSONObject(existingItem?.pricesJson ?: "{}")
                json.keys().asSequence().associateWith { json.getDouble(it) }.toMutableMap()
            } catch (e: Exception) { mutableMapOf<String, Double>() }
        )
    }
    var newTierName by remember { mutableStateOf("") }
    var newTierPrice by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingItem != null) "EDIT PRODUCT" else "NEW PRODUCT", fontWeight = FontWeight.ExtraBold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.heightIn(max = 600.dp)) {
                item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item {
                    Text("Visual Style / Icon:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(IconUtils.availableIcons) { (iconKey, resId) ->
                            Box(modifier = Modifier.size(50.dp).border(width = if (selectedIcon == iconKey) 2.dp else 1.dp, color = if (selectedIcon == iconKey) MaterialTheme.colorScheme.primary else Color.LightGray, shape = CircleShape).clickable { selectedIcon = iconKey }.padding(10.dp)) {
                                Image(painter = painterResource(id = resId), contentDescription = null, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
                item { OutlinedTextField(value = basePriceStr, onValueChange = { basePriceStr = it }, label = { Text("Base Price ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }
                
                // DYNAMIC PRICING SECTION
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("PRICING TIERS (Optional)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            dynamicPrices.forEach { (tier, price) ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text("$tier: $${String.format("%.2f", price)}", modifier = Modifier.weight(1f))
                                    IconButton(onClick = { dynamicPrices = dynamicPrices.toMutableMap().apply { remove(tier) } }) {
                                        Icon(painterResource(id = com.anyonehub.barpos.R.drawable.ic_3d_delete), null, modifier = Modifier.size(16.dp), tint = Color.Unspecified)
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(value = newTierName, onValueChange = { newTierName = it }, label = { Text("Tier Name") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(value = newTierPrice, onValueChange = { newTierPrice = it }, label = { Text("$") }, modifier = Modifier.width(70.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                                Button(onClick = {
                                    if (newTierName.isNotBlank() && newTierPrice.toDoubleOrNull() != null) {
                                        dynamicPrices = dynamicPrices.toMutableMap().apply { put(newTierName, newTierPrice.toDouble()) }
                                        newTierName = ""; newTierPrice = ""
                                    }
                                }, contentPadding = PaddingValues(0.dp)) { Text("ADD") }
                            }
                        }
                    }
                }

                item { OutlinedTextField(value = inventoryStr, onValueChange = { inventoryStr = it }, label = { Text("Stock Level") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = isFood, onClick = { isFood = !isFood }, label = { Text("Is Food") })
                        FilterChip(selected = isModifier, onClick = { isModifier = !isModifier }, label = { Text("Modifier") })
                        FilterChip(selected = isShotWall, onClick = { isShotWall = !isShotWall }, label = { Text("Inventory") })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pricesJson = JSONObject().apply { dynamicPrices.forEach { (k, v) -> put(k, v) } }.toString()
                    val newItem = (existingItem ?: MenuItem(categoryId = 0, name = "", price = 0.0)).copy(
                        name = name, price = basePriceStr.toDoubleOrNull() ?: 0.0, description = description, 
                        iconName = selectedIcon, inventoryCount = inventoryStr.toIntOrNull() ?: 999,
                        isShotWallItem = isShotWall, isFood = isFood, isModifier = isModifier, pricesJson = pricesJson
                    )
                    onConfirm(newItem)
                },
                modifier = Modifier.fillMaxWidth(), enabled = name.isNotBlank() && basePriceStr.toDoubleOrNull() != null
            ) { Text(if (existingItem != null) "UPDATE ITEM" else "CREATE ITEM", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (existingItem != null) { TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("DELETE") } }
                TextButton(onClick = onDismiss) { Text("CANCEL") }
            }
        }
    )
}
