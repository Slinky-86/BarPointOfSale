// Copyright 2024 anyone-Hub
package com.anyonehub.barpos.ui.features.inventory

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@Composable
fun AddMenuGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("NEW MENU GROUP", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Example: 'Liquor', 'Drafts', 'Appetizers'", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("CREATE GROUP")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("beer") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("NEW CATEGORY", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Domestic") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Select Icon Style:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Icon Picker for better UX in live environments
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(IconUtils.availableIcons) { (iconKey, resId) ->
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .border(
                                    width = if (selectedIcon == iconKey) 2.dp else 1.dp,
                                    color = if (selectedIcon == iconKey) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { selectedIcon = iconKey }
                                .padding(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedIcon) },
                enabled = name.isNotBlank()
            ) {
                Text("CREATE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun AddEditItemDialog(
    existingItem: MenuItem? = null,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onConfirm: (String, Double, Boolean, Int, String, String, Double?, Double?, Double?) -> Unit
) {
    var name by remember { mutableStateOf(existingItem?.name ?: "") }
    var description by remember { mutableStateOf(existingItem?.description ?: "") }
    var selectedIcon by remember { mutableStateOf(existingItem?.iconName ?: "beer") }
    var priceStr by remember { mutableStateOf(existingItem?.price?.toString() ?: "") }
    var hhPriceStr by remember { mutableStateOf(existingItem?.hhPrice?.toString() ?: "") }
    var bucketPriceStr by remember { mutableStateOf(existingItem?.bucketPrice?.toString() ?: "") }
    var hhBucketPriceStr by remember { mutableStateOf(existingItem?.hhBucketPrice?.toString() ?: "") }
    var isShotWall by remember { mutableStateOf(existingItem?.isShotWallItem ?: false) }
    var inventoryStr by remember { mutableStateOf(existingItem?.inventoryCount?.toString() ?: "999") }

    val isEditMode = existingItem != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditMode) "EDIT PRODUCT" else "NEW PRODUCT", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // --- ICON PICKER (On-the-fly configuration) ---
                Text("Visual Style / Icon:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(IconUtils.availableIcons) { (iconKey, resId) ->
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .border(
                                    width = if (selectedIcon == iconKey) 2.dp else 1.dp,
                                    color = if (selectedIcon == iconKey) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { selectedIcon = iconKey }
                                .padding(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Proof / Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) priceStr = it },
                        label = { Text("Reg Price ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = hhPriceStr,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) hhPriceStr = it },
                        label = { Text("HH Price ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bucketPriceStr,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) bucketPriceStr = it },
                        label = { Text("Bucket ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = hhBucketPriceStr,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) hhBucketPriceStr = it },
                        label = { Text("HH Bucket ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = inventoryStr,
                    onValueChange = { if (it.all { c -> c.isDigit() }) inventoryStr = it },
                    label = { Text("Stock Level") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Shot Wall / Limited Stock", fontWeight = FontWeight.Bold)
                            Text("Enables inventory tracking.", fontSize = 11.sp)
                        }
                        Switch(checked = isShotWall, onCheckedChange = { isShotWall = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    val stock = inventoryStr.toIntOrNull() ?: 999
                    val hhPrice = hhPriceStr.toDoubleOrNull()
                    val bPrice = bucketPriceStr.toDoubleOrNull()
                    val hhbPrice = hhBucketPriceStr.toDoubleOrNull()
                    onConfirm(name, price, isShotWall, stock, description, selectedIcon, hhPrice, bPrice, hhbPrice)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && priceStr.isNotBlank()
            ) {
                Text(if (isEditMode) "UPDATE ITEM" else "CREATE ITEM", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (isEditMode) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("DELETE")
                    }
                }
                TextButton(onClick = onDismiss) { Text("CANCEL") }
            }
        }
    )
}
