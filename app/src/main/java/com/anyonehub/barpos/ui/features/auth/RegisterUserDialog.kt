/* Copyright 2024 anyone-Hub */
package com.anyonehub.barpos.ui.features.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anyonehub.barpos.R
import com.anyonehub.barpos.data.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterUserDialog(
    onDismiss: () -> Unit,
    onRegister: (String, String, String, UserRole, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.SERVER) }
    var isManager by remember { mutableStateOf(false) }
    var roleMenuExpanded by remember { mutableStateOf(false) }

    // Minimum 6 digits for Supabase auth
    val isValid = name.isNotBlank() && email.contains("@") && pin.length >= 6

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "STAFF REGISTRATION",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    placeholder = { Text("staff@bar.com") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 8) {
                            pin = input
                        }
                    },
                    label = { Text("Access PIN (Min 6 Digits)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                // --- ROLE SELECTION ---
                Box {
                    OutlinedTextField(
                        value = selectedRole.label,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Positional Category / Role") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { roleMenuExpanded = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_menu_rotate), // 3D representation of selection/rotate
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.Unspecified
                                )
                            }
                        }
                    )
                    // Invisible clickable area over the field
                    Box(modifier = Modifier.matchParentSize().clickable { roleMenuExpanded = true })
                    
                    DropdownMenu(
                        expanded = roleMenuExpanded,
                        onDismissRequest = { roleMenuExpanded = false }
                    ) {
                        UserRole.entries.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.label) },
                                onClick = {
                                    selectedRole = role
                                    if (role == UserRole.MANAGER || role == UserRole.ADMIN) {
                                        isManager = true
                                    }
                                    roleMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isManager,
                            onCheckedChange = { isManager = it }
                        )
                        Column {
                            Text("Manager Permissions", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Allows menu editing & system configs", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onRegister(name, email, pin, selectedRole, isManager)
                    }
                },
                enabled = isValid
            ) {
                Text("CREATE ACCOUNT")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray)
            }
        }
    )
}