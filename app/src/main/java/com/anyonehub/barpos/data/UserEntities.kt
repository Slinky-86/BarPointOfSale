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

// --- ENUM FOR ROLES ---
@Serializable
enum class UserRole(val label: String) {
    SERVER("Server"),
    BARTENDER("Bartender"),
    MANAGER("Manager"),
    ASSISTANT_MANAGER("Assistant Manager"),
    ADMIN("Admin")
}

// --- USER MANAGEMENT (PROFILES) ---
@Serializable
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) 
    @ColumnInfo(name = "id_local")
    @SerialName("id_local") val id: Int = 0,
    
    @ColumnInfo(name = "supabase_id")
    @SerialName("supabase_id") val supabaseId: String? = null,
    
    @ColumnInfo(name = "name")
    @SerialName("name") val name: String,

    @ColumnInfo(name = "email")
    @SerialName("email") val email: String = "",
    
    @ColumnInfo(name = "pin_code")
    @SerialName("pin_code") val pinCode: String, 
    
    @ColumnInfo(name = "role")
    @SerialName("role") val role: UserRole = UserRole.SERVER, 
    
    @ColumnInfo(name = "is_manager")
    @SerialName("is_manager") val isManager: Boolean = false, 
    
    @ColumnInfo(name = "is_active")
    @SerialName("is_active") val isActive: Boolean = true,
    
    @ColumnInfo(name = "has_seen_tutorial")
    @SerialName("has_seen_tutorial") val hasSeenTutorial: Boolean = false,

    @ColumnInfo(name = "avatar_url")
    @SerialName("avatar_url") val avatarUrl: String? = null,

    @ColumnInfo(name = "last_reminder_date")
    @SerialName("last_reminder_date") val lastReminderDate: String? = null,

    @ColumnInfo(name = "is_synced")
    @Transient
    val isSynced: Boolean = false,

    @ColumnInfo(name = "created_at")
    @SerialName("created_at")
    val createdAt: Instant = Clock.System.now()
)

// --- TIP TRACKING ---
@Serializable
@Entity(
    tableName = "tip_logs",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id_local"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_id"])]
)
data class TipLog(
    @PrimaryKey(autoGenerate = true) 
    @ColumnInfo(name = "id")
    @SerialName("id") val id: Long = 0,
    
    @ColumnInfo(name = "user_id")
    @SerialName("user_id") val userId: Int,
    
    @ColumnInfo(name = "staff_supabase_id")
    @SerialName("staff_supabase_id") val supabaseUserId: String? = null,
    
    @ColumnInfo(name = "amount")
    @SerialName("amount") val amount: Double,

    @ColumnInfo(name = "timestamp")
    @SerialName("timestamp") val timestamp: Instant = Clock.System.now(),

    @ColumnInfo(name = "note")
    @SerialName("note") val note: String = "",

    @ColumnInfo(name = "is_synced")
    @Transient
    val isSynced: Boolean = false
)
