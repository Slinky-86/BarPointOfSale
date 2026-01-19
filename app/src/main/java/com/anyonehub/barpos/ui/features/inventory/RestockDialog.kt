// Copyright 2024 anyone-Hub
package com.anyonehub.barpos.ui.features.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.anyonehub.barpos.data.MenuItem

@Composable
fun RestockDialog(
    item: MenuItem,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var countText by remember { mutableStateOf(item.inventoryCount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("RESTOCK: ${item.name.uppercase()}", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Enter the new inventory count for this item.")
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = countText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) countText = it },
                    label = { Text("Total Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newCount = countText.toIntOrNull() ?: item.inventoryCount
                    onConfirm(newCount)
                }
            ) { Text("UPDATE STOCK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}