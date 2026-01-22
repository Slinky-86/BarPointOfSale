/* Copyright 2024 anyone-Hub */
package com.anyonehub.barpos.ui.features.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anyonehub.barpos.R
import com.anyonehub.barpos.data.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onBackToLogin: () -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.SERVER) }
    var showPinPad by remember { mutableStateOf(false) }
    var roleMenuExpanded by remember { mutableStateOf(false) }
    
    val loginState by viewModel.loginState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.authEvents.collect { event ->
            when (event) {
                is AuthEvent.ShowMessage -> Toast.makeText(context, event.msg, Toast.LENGTH_LONG).show()
                is AuthEvent.RegistrationSuccess -> Toast.makeText(context, event.msg, Toast.LENGTH_LONG).show()
                is AuthEvent.NavigateToPos -> { /* Nav via state */ }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 500.dp).padding(24.dp)) {
            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Text(text = "Welcome to MidTown POS Onboarding. Please provide your details below to create your secure terminal access profile.", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(12.dp)) {
                Column(modifier = Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("STAFF REGISTRATION", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)

                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Full Name (First & Last)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    OutlinedTextField(value = emailInput, onValueChange = { emailInput = it }, label = { Text("Work Email Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))

                    // --- ROLE SELECTION ---
                    ExposedDropdownMenuBox(expanded = roleMenuExpanded, onExpandedChange = { roleMenuExpanded = !roleMenuExpanded }) {
                        OutlinedTextField(
                            value = selectedRole.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Job Role") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleMenuExpanded) },
                            modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        ExposedDropdownMenu(expanded = roleMenuExpanded, onDismissRequest = { roleMenuExpanded = false }) {
                            UserRole.entries.filter { it != UserRole.ADMIN }.forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role.label) },
                                    onClick = { selectedRole = role; roleMenuExpanded = false }
                                )
                            }
                        }
                    }

                    Column {
                        Text("Terminal Access Code", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(64.dp).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).clickable { showPinPad = true }.padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(painterResource(id = R.drawable.ic_3d_lock), null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                if (pinInput.isEmpty()) Text("Tap to Set 6-Digit PIN", color = Color.Gray) else AnimatedPinDots(pinLength = pinInput.length, maxDots = 6)
                            }
                        }
                    }

                    Box(modifier = Modifier.height(32.dp), contentAlignment = Alignment.Center) { if (loginState is LoginState.Loading) CircularProgressIndicator(modifier = Modifier.size(24.dp)) }

                    Button(
                        onClick = { viewModel.registerStaff(nameInput, emailInput, pinInput, selectedRole, selectedRole == UserRole.MANAGER) },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        enabled = nameInput.isNotBlank() && emailInput.contains("@") && pinInput.length == 6 && loginState !is LoginState.Loading
                    ) { Text("CREATE MY ACCOUNT", fontWeight = FontWeight.Bold) }

                    TextButton(onClick = onBackToLogin, modifier = Modifier.fillMaxWidth()) { Text("Already have a profile? Login", color = MaterialTheme.colorScheme.primary) }
                }
            }
        }

        if (loginState is LoginState.Error) {
            Box(Modifier.fillMaxSize().background(Color.Red.copy(alpha = 0.1f)), contentAlignment = Alignment.TopCenter) {
                Surface(color = Color.Red, shape = RoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text((loginState as LoginState.Error).message, color = Color.White, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showPinPad) { PinPadPopup(currentPin = pinInput, onPinChange = { pinInput = it }, onDismiss = { showPinPad = false }) }
    }
}
