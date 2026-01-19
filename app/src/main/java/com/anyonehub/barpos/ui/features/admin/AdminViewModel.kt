// Copyright 2024 anyone-Hub
@file:OptIn(ExperimentalTime::class)
@file:Suppress("RemoveExplicitTypeArguments")

package com.anyonehub.barpos.ui.features.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anyonehub.barpos.data.*
import com.anyonehub.barpos.di.SupabaseConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresListDataFlow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient,
    @Named("admin") private val adminClient: SupabaseClient,
    posDao: PosDao
) : ViewModel() {

    // --- CLOUD SYNC TRACKING ---
    private val _lastSyncTime = MutableStateFlow<Instant>(Clock.System.now())
    val lastSyncTime: StateFlow<Instant> = _lastSyncTime.asStateFlow()

    // 1. Live Staff Monitor (From Supabase 'users' table)
    val staffProfiles: StateFlow<List<User>> = adminClient.realtime.channel("staff_monitor")
        .postgresListDataFlow(
            schema = "public",
            table = SupabaseConstants.TABLE_USERS,
            primaryKey = User::id
        )
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Real-time Live Sales Feed (High-Precision Online-First)
    val liveSales: StateFlow<List<Sale>> = adminClient.realtime.channel("sales_feed")
        .postgresListDataFlow(
            schema = "public",
            table = SupabaseConstants.TABLE_SALES_RECORDS,
            primaryKey = Sale::id
        )
        .map { 
            _lastSyncTime.value = Clock.System.now()
            it.take(20) 
        } 
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Inventory Alerts (Low Stock - Cached from local for speed)
    val lowStockItems: StateFlow<List<MenuItem>> = posDao.getAllMenuItems()
        .map { all -> all.filter { it.inventoryCount < 10 } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 4. Aggregate Earnings (Online-First: Polled from Supabase via Postgrest)
    private val _cloudRevenue = MutableStateFlow(0.0)
    val totalRevenue: StateFlow<Double> = _cloudRevenue.asStateFlow()

    init {
        viewModelScope.launch {
            // Conjunction: connect both clients for high-availability realtime monitoring
            adminClient.realtime.connect()
            try { supabaseClient.realtime.connect() } catch (_: Exception) {}
            refreshCloudStats()
        }
    }

    /**
     * Actively uses Postgrest to fetch authoritative cloud totals.
     * Uses Clock and Instant to mark the precision of the sync.
     */
    fun refreshCloudStats() {
        viewModelScope.launch {
            try {
                // Use Admin and Normal clients in conjunction for authoritative revenue sync
                val sales = try {
                    adminClient.postgrest[SupabaseConstants.TABLE_SALES_RECORDS]
                        .select()
                        .decodeAs<List<Sale>>()
                } catch (_: Exception) {
                    supabaseClient.postgrest[SupabaseConstants.TABLE_SALES_RECORDS]
                        .select()
                        .decodeAs<List<Sale>>()
                }
                
                _cloudRevenue.value = sales.sumOf { it.totalAmount }
                _lastSyncTime.value = Clock.System.now()
                Log.d("AdminViewModel", "Authoritative Revenue Synced at: ${_lastSyncTime.value}")
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Cloud Stat Refresh Failed: ${e.message}")
            }
        }
    }
}
