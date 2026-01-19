// Copyright 2024 anyone-Hub
@file:OptIn(ExperimentalTime::class)

package com.anyonehub.barpos.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime

@Serializable
@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true)
    @SerialName("id") val id: Long = 0,
    @SerialName("name") val name: String,
    @SerialName("phone") val phone: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("created_at") val createdAt: Instant = Clock.System.now(),
    
    @Transient
    val isSynced: Boolean = false
)
