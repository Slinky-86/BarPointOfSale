/* Copyright 2024 anyone-Hub */
@file:OptIn(ExperimentalTime::class)

package com.anyonehub.barpos.ui.features.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit // NEW: Success callback
) {
    val loginState by viewModel.loginState.collectAsState()
    var nameInput by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var showPinPad by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Observe Auth Events for feedback and one-time actions
    LaunchedEffect(Unit) {
        viewModel.authEvents.collect { event ->
            when (event) {
                is AuthEvent.NavigateToPos -> onLoginSuccess()
                is AuthEvent.ShowMessage -> Toast.makeText(context, event.msg, Toast.LENGTH_LONG).show()
                is AuthEvent.RegistrationSuccess -> { /* Feedback provided via Toast in RegisterScreen */ }
            }
        }
    }

    LaunchedEffect(pinInput) {
        if (pinInput.length == 6) {
            delay(200)
            viewModel.login(nameInput, pinInput)
            showPinPad = false
        }
    }

    LaunchedEffect(loginState) {
        when (loginState) {
            is LoginState.Success -> {
                // SUCCESS: Proceed to POS
                onLoginSuccess()
            }
            is LoginState.Error -> {
                delay(1500)
                pinInput = ""
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 450.dp).padding(24.dp)
        ) {
            // Header Branding
            Icon(
                painter = painterResource(id = R.drawable.ic_midtown_3d),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = Color.Unspecified
            )
            Text(
                "MidTown POS",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(40.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text(
                        "TERMINAL ACCESS",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )

                    // 1. Name Input
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Enter Your Name (First & Last)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        )
                    )

                    // 2. PIN Input Trigger
                    Column {
                        Text("Access PIN", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable { showPinPad = true }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_3d_lock), 
                                    contentDescription = null, 
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                if (pinInput.isEmpty()) {
                                    Text("Tap to Enter PIN", color = Color.Gray)
                                } else {
                                    AnimatedPinDots(pinLength = pinInput.length, maxDots = 6)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { 
                            val attemptTime: Instant = Clock.System.now()
                            Log.d("LoginScreen", "Login attempt initiated at $attemptTime")
                            viewModel.login(nameInput, pinInput) 
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        enabled = nameInput.isNotBlank() && pinInput.length == 6,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("UNLOCK TERMINAL", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    TextButton(
                        onClick = onNavigateToRegister,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("New Staff? Create Your Profile", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Error Feedback
        if (loginState is LoginState.Error) {
            Box(Modifier.fillMaxSize().background(Color.Red.copy(alpha = 0.1f)), contentAlignment = Alignment.TopCenter) {
                Surface(
                    color = Color.Red,
                    shape = RoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        (loginState as LoginState.Error).message,
                        color = Color.White,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Animated PIN Pad Pop-up
        if (showPinPad) {
            PinPadPopup(
                currentPin = pinInput,
                onPinChange = { pinInput = it },
                onDismiss = { showPinPad = false }
            )
        }
    }
}

@Composable
fun PinPadPopup(
    currentPin: String,
    onPinChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(enabled = false) { },
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = Color(0xFF1E1E1E)
        ) {
            Column(
                modifier = Modifier.padding(top = 16.dp, bottom = 40.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.Gray.copy(alpha = 0.5f), CircleShape)
                )
                
                Spacer(Modifier.height(24.dp))
                Text("SECURE PIN ENTRY", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(24.dp))
                
                AnimatedPinDots(pinLength = currentPin.length, maxDots = 6)
                
                Spacer(Modifier.height(32.dp))

                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("CLR", "0", "OK")
                )

                keys.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                        row.forEach { key ->
                            PinKey(key) {
                                when (key) {
                                    "CLR" -> onPinChange("")
                                    "OK" -> onDismiss()
                                    else -> if (currentPin.length < 6) onPinChange(currentPin + key)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinKey(key: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(80.dp).clickable { onClick() },
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = key,
                color = if (key == "OK") Color(0xFF00E676) else Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
