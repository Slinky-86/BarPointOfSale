/* Copyright 2024 anyone-Hub */
@file:Suppress("AssignedValueIsNeverRead")

package com.anyonehub.barpos.ui.features.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anyonehub.barpos.R
import com.anyonehub.barpos.data.AppSetting
import com.anyonehub.barpos.data.User
import com.anyonehub.barpos.ui.features.auth.AuthViewModel
import com.anyonehub.barpos.ui.features.auth.RegisterUserDialog
import com.anyonehub.barpos.ui.features.pos.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    posViewModel: PosViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val settings by viewModel.appSettings.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val currentUser by posViewModel.currentUser.collectAsState()
    val context = LocalContext.current

    var showRegisterDialog by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri -> if (uri != null) viewModel.performBackup(uri) }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.performRestore(uri) }

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SYSTEM CONFIGURATION", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        // Using custom ic_undo for Back navigation
                        Icon(
                            painter = painterResource(id = R.drawable.ic_undo),
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Business Rules", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Staff & Security", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Data & Exports", fontWeight = FontWeight.Bold) }
                )
            }

            Box(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                if (settings != null) {
                    when (selectedTab) {
                        0 -> BusinessTab(settings!!, viewModel::updateSettings)
                        1 -> StaffTab(
                            users = allUsers,
                            onDelete = viewModel::deleteUser,
                            onAddClick = { 
                                if (currentUser?.isManager == true) {
                                    showRegisterDialog = true 
                                } else {
                                    Toast.makeText(context, "Admin access only. Login as admin to continue.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        2 -> DataTab(
                            onBackup = { backupLauncher.launch("midtown_backup.db") },
                            onRestore = { restoreLauncher.launch(arrayOf("*/*")) },
                            onCsv = viewModel::exportMenuCsv,
                            onPdf = viewModel::exportMenuPdf
                        )
                    }
                } else {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }

    if (showRegisterDialog) {
        RegisterUserDialog(
            onDismiss = { showRegisterDialog = false },
            onRegister = { name, email, pin, role, isManager ->
                authViewModel.registerStaff(name, email, pin, role, isManager)
                showRegisterDialog = false
            }
        )
    }
}

@Composable
fun BusinessTab(settings: AppSetting, onSave: (AppSetting) -> Unit) {
    var barName by remember { mutableStateOf(settings.barName) }
    var taxRate by remember { mutableStateOf((settings.taxRate * 100).toString()) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("General Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = barName,
            onValueChange = { barName = it },
            label = { Text("Business/Bar Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = taxRate,
            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) taxRate = it },
            label = { Text("Tax Rate %") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val newRate = (taxRate.toDoubleOrNull() ?: 0.0) / 100.0
                onSave(settings.copy(barName = barName, taxRate = newRate))
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_save), 
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
            Spacer(Modifier.width(8.dp))
            Text("SAVE SETTINGS", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StaffTab(users: List<User>, onDelete: (User) -> Unit, onAddClick: () -> Unit) {
    Column {
        Button(
            onClick = onAddClick,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_3d_add), 
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
            Spacer(Modifier.width(8.dp))
            Text("REGISTER NEW STAFF MEMBER", fontWeight = FontWeight.Black)
        }

        Text("Active Staff Members", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(users) { user ->
                ListItem(
                    headlineContent = { Text(user.name, fontWeight = FontWeight.Bold) },
                    supportingContent = {
                        Column {
                            Text("ROLE: ${user.role.label}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            Text(
                                text = if (user.isManager) "MANAGER ACCESS" else "STANDARD USER",
                                color = if (user.isManager) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                fontSize = 11.sp
                            )
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = { onDelete(user) }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_3d_delete), 
                                contentDescription = "Remove", 
                                modifier = Modifier.size(24.dp),
                                tint = Color.Unspecified
                            )
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun DataTab(onBackup: () -> Unit, onRestore: () -> Unit, onCsv: () -> Unit, onPdf: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Database Backup & Restore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onBackup, modifier = Modifier.weight(1f)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_3d_lock), 
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(4.dp))
                Text("BACKUP")
            }
            OutlinedButton(onClick = onRestore, modifier = Modifier.weight(1f)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_3d_refill), 
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(4.dp))
                Text("RESTORE")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text("Inventory & Audit Exports", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onCsv,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_tag_3d), 
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(4.dp))
                // Using ic_redo for Exports (Forward Action)
                Icon(
                    painter = painterResource(id = R.drawable.ic_redo),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(4.dp))
                Text("EXPORT CSV")
            }
            Button(
                onClick = onPdf,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_audit_3d), 
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(4.dp))
                // Using ic_redo for Exports (Forward Action)
                Icon(
                    painter = painterResource(id = R.drawable.ic_redo),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(4.dp))
                Text("EXPORT PDF")
            }
        }
    }
}
