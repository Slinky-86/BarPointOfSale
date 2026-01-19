/* Copyright 2024 anyone-Hub */
package com.anyonehub.barpos.ui.features.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anyonehub.barpos.data.ReportDao
import com.anyonehub.barpos.util.ZReportManager
import com.anyonehub.barpos.util.ZReportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ZReportViewModel @Inject constructor(
    reportDao: ReportDao,
    private val zReportManager: ZReportManager
) : ViewModel() {

    // 1. Fetch closed items that haven't been assigned to a report yet
    val unreportedItems = reportDao.getUnreportedClosedItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // 2. Reactive total for the current "live" shift
    val shiftTotal: StateFlow<Double> = unreportedItems.map { map ->
        map.keys.sumOf { it.priceAtTimeOfSale }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    private val _reportResult = MutableSharedFlow<ZReportResult>()
    val reportResult = _reportResult.asSharedFlow()

    /**
     * FINALIZES THE SHIFT
     * Delegates complex financial logic and Python engine calls to ZReportManager.
     */
    fun runZReport(managerId: Int) {
        viewModelScope.launch {
            val result = zReportManager.runFinalZReport(managerId)
            _reportResult.emit(result)
        }
    }
}
