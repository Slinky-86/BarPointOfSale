// Copyright 2024 anyone-Hub
@file:OptIn(ExperimentalTime::class)

package com.anyonehub.barpos.ui.features.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anyonehub.barpos.data.ClockEventType
import com.anyonehub.barpos.data.User
import com.anyonehub.barpos.ui.IconUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Instant
import kotlin.time.ExperimentalTime

@Composable
fun TimeClockDialog(
    user: User,
    onDismiss: () -> Unit,
    onClockOut: () -> Unit = {},
    viewModel: TimeClockViewModel = hiltViewModel()
) {
    val lastEvent by viewModel.lastClockEvent.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(user) {
        viewModel.setCurrentUser(user)
    }

    LaunchedEffect(Unit) {
        viewModel.statusMessage.collect {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TimeClockEvent.ClockedOut -> {
                    onClockOut()
                    onDismiss()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = IconUtils.getIconResource("time")),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(8.dp))
                Text("STAFF TIME CLOCK", fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(user.name.uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(user.role.label, color = Color.Gray, fontSize = 14.sp)
                
                Spacer(Modifier.height(24.dp))
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CURRENT STATUS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        val statusText = when (lastEvent?.eventType) {
                            ClockEventType.SHIFT_START -> "ON SHIFT"
                            ClockEventType.BREAK_START -> "ON BREAK"
                            ClockEventType.BREAK_END -> "ON SHIFT (Back from Break)"
                            ClockEventType.SHIFT_END -> "OFF DUTY"
                            null -> "NOT CLOCKED IN"
                        }
                        Text(
                            statusText, 
                            color = if (lastEvent?.eventType == ClockEventType.SHIFT_START || lastEvent?.eventType == ClockEventType.BREAK_END) Color(0xFF4CAF50) else Color.Red,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                        
                        lastEvent?.let {
                            val df = SimpleDateFormat("hh:mm a", Locale.US)
                            // Convert high-precision Instant to epoch millis for legacy SimpleDateFormat compatibility
                            val timestamp: Instant = it.timestamp
                            Text("Last Event: ${df.format(Date(timestamp.toEpochMilliseconds()))}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClockButton(
                        label = "CLOCK IN",
                        color = Color(0xFF4CAF50),
                        enabled = lastEvent?.eventType != ClockEventType.SHIFT_START && lastEvent?.eventType != ClockEventType.BREAK_END && lastEvent?.eventType != ClockEventType.BREAK_START,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.handleClockEvent(ClockEventType.SHIFT_START) }

                    ClockButton(
                        label = "CLOCK OUT",
                        color = Color.Red,
                        enabled = lastEvent?.eventType == ClockEventType.SHIFT_START || lastEvent?.eventType == ClockEventType.BREAK_END,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.handleClockEvent(ClockEventType.SHIFT_END) }
                }

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClockButton(
                        label = "START BREAK",
                        color = Color(0xFFFF9800),
                        enabled = lastEvent?.eventType == ClockEventType.SHIFT_START || lastEvent?.eventType == ClockEventType.BREAK_END,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.handleClockEvent(ClockEventType.BREAK_START) }

                    ClockButton(
                        label = "END BREAK",
                        color = Color(0xFF2196F3),
                        enabled = lastEvent?.eventType == ClockEventType.BREAK_START,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.handleClockEvent(ClockEventType.BREAK_END) }
                }
                
                SnackbarHost(hostState = snackbarHostState)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("DONE") }
        }
    )
}

@Composable
fun ClockButton(
    label: String,
    color: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = modifier.height(60.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
    }
}
