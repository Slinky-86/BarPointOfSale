// Copyright 2024 anyone-Hub
package com.anyonehub.barpos.util

/**
 * Common result sealed class for shift reporting.
 * Placing this here ensures it is visible to both ZReportManager and PosViewModel.
 */
sealed class ZReportResult {
    data class Success(
        val reportId: Long,
        val totalFormatted: String,
        val itemCounts: Map<String, Int>
    ) : ZReportResult()

    data class Error(val message: String) : ZReportResult()
}