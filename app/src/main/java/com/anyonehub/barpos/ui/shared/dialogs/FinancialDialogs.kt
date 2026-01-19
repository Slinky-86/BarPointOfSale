// Copyright 2024 anyone-Hub
@file:OptIn(ExperimentalTime::class)

package com.anyonehub.barpos.ui.shared.dialogs

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime

@Composable
fun CashPaymentDialog(
    customerName: String,
    totalDue: Double,
    onDismiss: () -> Unit,
    onConfirmPay: () -> Unit
) {
    var tenderedStr by remember { mutableStateOf("") }
    val tendered = tenderedStr.toDoubleOrNull() ?: 0.0
    val changeDue = tendered - totalDue
    val isSufficient = tendered >= totalDue

    // Quick denomination buttons helper
    val addBill = { amount: Int ->
        val current = tenderedStr.toDoubleOrNull() ?: 0.0
        tenderedStr = (current + amount).toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Checkout: $customerName",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format(Locale.US, "$%.2f", totalDue),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // --- TENDERED INPUT ---
                OutlinedTextField(
                    value = tenderedStr,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) tenderedStr = it },
                    label = { Text("Cash Tendered") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // --- QUICK CASH BUTTONS ---
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val bills = listOf(1, 5, 10, 20)
                    bills.forEach { bill ->
                        Button(
                            onClick = { addBill(bill) },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("$$bill", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val bills = listOf(50, 100)
                    bills.forEach { bill ->
                        Button(
                            onClick = { addBill(bill) },
                            modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("$$bill")
                        }
                    }
                    // Exact Change Button
                    Button(
                        onClick = { tenderedStr = totalDue.toString() },
                        modifier = Modifier.weight(2f).padding(horizontal = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Exact")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // --- CHANGE DISPLAY ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CHANGE DUE:", fontWeight = FontWeight.Bold)
                    Text(
                        text = if (isSufficient) String.format(Locale.US, "$%.2f", changeDue) else "---",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSufficient) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }

                if (!isSufficient && tendered > 0) {
                    Text(
                        text = "Insufficient Funds",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // High-Precision Payment Audit
                    val paymentTime: Instant = Clock.System.now()
                    Log.d("FinancialDialogs", "Cash payment finalized at $paymentTime")
                    onConfirmPay()
                },
                enabled = isSufficient,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("FINALIZE SALE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditLineItemPriceDialog(
    itemName: String,
    currentPrice: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var priceStr by remember { mutableStateOf(currentPrice.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("EDIT PRICE: $itemName", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                Text("Adjust price for this specific entry:", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) priceStr = it },
                    label = { Text("New Price ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    onConfirm(price)
                }
            ) {
                Text("UPDATE PRICE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
