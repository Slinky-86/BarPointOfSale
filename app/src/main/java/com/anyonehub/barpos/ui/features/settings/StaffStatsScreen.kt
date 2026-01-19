/* Copyright 2024 anyone-Hub */
@file:OptIn(ExperimentalTime::class)
@file:Suppress("AssignedValueIsNeverRead")

package com.anyonehub.barpos.ui.features.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anyonehub.barpos.R
import com.anyonehub.barpos.ui.features.auth.AuthViewModel
import com.anyonehub.barpos.ui.features.history.TipTrackerDialog
import com.anyonehub.barpos.ui.features.pos.PosViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffStatsScreen(
    posViewModel: PosViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by posViewModel.currentUser.collectAsState()
    val totalTips by posViewModel.myTotalTips.collectAsState()
    val tipHistory by posViewModel.myTipHistory.collectAsState()
    val closedCount by posViewModel.myClosedTabsCount.collectAsState()

    var showTipDialog by remember { mutableStateOf(false) }

    // Image Picker Launcher
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = context.createFileFromUri(it)
            authViewModel.uploadStaffPhoto(file)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MY SHIFT STATS", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_undo),
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                    }
                },
                actions = {
                    // Trigger Tip Logging directly from here
                    IconButton(onClick = { showTipDialog = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tip_tracker_3d),
                            contentDescription = "Log Tip",
                            modifier = Modifier.size(28.dp),
                            tint = Color.Unspecified
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTipDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_tip_tracker_3d),
                    contentDescription = "Add Tip",
                    modifier = Modifier.size(32.dp),
                    tint = Color.Unspecified
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. PROFILE & PHOTO SECTION
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AsyncImage(
                                model = currentUser?.avatarUrl ?: R.drawable.ic_user_profile_3d,
                                contentDescription = "Staff Avatar",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.2f))
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { photoLauncher.launch("image/*") },
                                modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_3d_add), 
                                    contentDescription = "Upload", 
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        Spacer(Modifier.width(20.dp))
                        
                        Column {
                            Text(currentUser?.name ?: "Unknown User", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.White)
                            Text(currentUser?.role?.label ?: "Staff", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            if (currentUser?.avatarUrl.isNullOrBlank()) {
                                Text("Secure Profile Incomplete", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. STATS CARDS
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("SHIFT TIPS", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = String.format(Locale.US, "$%.2f", totalTips),
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("TABS CLOSED", color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "$closedCount",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            item {
                Text("RECENT TIP HISTORY", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            if (tipHistory.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No tips logged for this shift.", color = Color.Gray)
                    }
                }
            }

            items(tipHistory) { tip ->
                ListItem(
                    headlineContent = { Text(String.format(Locale.US, "$%.2f", tip.amount), fontWeight = FontWeight.Bold) },
                    supportingContent = { 
                        // Actively using the high-precision Instant field for visual auditing
                        val timestamp: Instant = tip.timestamp
                        Text("${tip.note.ifBlank { "Cash Tip Entry" }} • $timestamp") 
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tip_tracker_3d),
                            null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                    }
                )
                HorizontalDivider()
            }
        }
    }

    if (showTipDialog) {
        TipTrackerDialog(
            currentTotalTips = totalTips,
            onDismiss = { showTipDialog = false },
            onSaveTip = { amount, note ->
                posViewModel.logTip(amount, note)
            }
        )
    }
}

/**
 * Extension to handle Uri to File conversion for Supabase upload
 */
private fun Context.createFileFromUri(uri: Uri): File {
    // Actively applying Clock.System.now() and explicitly typing as Instant
    val now: Instant = Clock.System.now()
    val file = File(cacheDir, "staff_avatar_${now.toEpochMilliseconds()}.jpg")
    contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }
    return file
}
