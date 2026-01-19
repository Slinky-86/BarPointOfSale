// Copyright 2024 anyone-Hub
@file:OptIn(ExperimentalTime::class)

package com.anyonehub.barpos.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime

/**
 * MASTER AUDIT ENTITY
 * Stores the finalized financial data for a shift.
 */
@Serializable
@Entity(tableName = "z_reports")
data class ZReportEntity(
    @PrimaryKey(autoGenerate = true)
    @SerialName("id") val id: Long = 0,
    @SerialName("timestamp") val timestamp: Instant = Clock.System.now(),
    @SerialName("total_revenue") val totalRevenue: Double,
    @SerialName("manager_id") val managerId: Int,          // ID of the person who closed the drawer
    @SerialName("report_data_json") val reportDataJson: String,   // Full Python-generated breakdown (item counts, etc.)
    @SerialName("is_verified") val isVerified: Boolean = true
)
