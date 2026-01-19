// Copyright 2024 anyone-Hub
@file:OptIn(ExperimentalTime::class)

package com.anyonehub.barpos.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime

@Serializable
enum class ClockEventType {
    @SerialName("SHIFT_START") SHIFT_START,
    @SerialName("SHIFT_END") SHIFT_END,
    @SerialName("BREAK_START") BREAK_START,
    @SerialName("BREAK_END") BREAK_END
}

@Serializable
@Entity(
    tableName = "time_clock_entries",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id_local"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("user_id")]
)
data class TimeClockEntry(
    @PrimaryKey(autoGenerate = true)
    @SerialName("id") val id: Long = 0,

    @ColumnInfo(name = "user_id")
    @SerialName("user_id") val userId: Int,

    @ColumnInfo(name = "staff_supabase_id")
    @SerialName("staff_supabase_id") val supabaseUserId: String? = null,

    @ColumnInfo(name = "event_type")
    @SerialName("event_type") val eventType: ClockEventType,

    @ColumnInfo(name = "timestamp")
    @SerialName("timestamp") val timestamp: Instant = Clock.System.now(),
    
    @ColumnInfo(name = "is_synced")
    @Transient // Don't send this flag to Supabase
    val isSynced: Boolean = false
)
