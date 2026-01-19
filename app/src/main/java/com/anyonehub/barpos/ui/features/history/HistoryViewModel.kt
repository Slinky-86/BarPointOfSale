// Copyright 2024 anyone-Hub
@file:OptIn(ExperimentalTime::class)

package com.anyonehub.barpos.ui.features.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anyonehub.barpos.data.ActiveTab
import com.anyonehub.barpos.data.PosDao
import com.anyonehub.barpos.data.TipLog
import com.anyonehub.barpos.data.repository.PosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val posDao: PosDao,
    private val repository: PosRepository
) : ViewModel() {

    // --- DATA STREAMS ---
    val closedTabs: StateFlow<List<ActiveTab>> = posDao.getClosedTabs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalRevenue: StateFlow<Double> = posDao.getTotalRevenue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val allTips: StateFlow<Double> = posDao.getAllTipsTotal()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // --- EVENTS ---
    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    // --- ACTIONS ---

    /**
     * Clears historical sales data. Use with caution (End of Day/Shift).
     */
    fun clearSalesHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            posDao.clearSalesHistory()
            _events.emit("Shift Closed. Database Ready for Next Day.")
        }
    }

    /**
     * Logs manual cash tips using high-precision Instant.
     * Essential for shift reconciliation in high-volume environments.
     */
    fun logManualTip(amount: Double, note: String, userId: Int) {
        if (amount <= 0) return

        viewModelScope.launch(Dispatchers.IO) {
            // Explicitly typed Instant for high-precision tip tracking
            val now: Instant = Clock.System.now()
            
            val tip = TipLog(
                userId = userId,
                amount = amount,
                note = note,
                timestamp = now
            )
            repository.logTip(tip)
            _events.emit("Cash Tip Logged: $$amount")
        }
    }
}
