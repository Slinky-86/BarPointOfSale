// Copyright 2024 anyone-Hub
package com.anyonehub.barpos.ui.shared.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anyonehub.barpos.R

@Composable
fun LineItemActionDialog(
    itemName: String,
    currentNote: String,
    onDismiss: () -> Unit,
    onVoid: () -> Unit,
    onSaveNote: (String) -> Unit
) {
    var note by remember { mutableStateOf(currentNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(itemName, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Manage Item", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Kitchen/Bar Note") },
                    placeholder = { Text("e.g. No Salt, Neat") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveNote(note) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_3d_lock), // Representing "save/lock"
                    contentDescription = null, 
                    modifier = Modifier.size(16.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Note")
            }
        },
        dismissButton = {
            Row {
                // VOID BUTTON (Destructive)
                TextButton(
                    onClick = onVoid,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_3d_delete), 
                        contentDescription = null, 
                        modifier = Modifier.size(16.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("VOID ITEM")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}