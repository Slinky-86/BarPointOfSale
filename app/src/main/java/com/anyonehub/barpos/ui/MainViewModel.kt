// Copyright 2024 anyone-Hub
@file:OptIn(ExperimentalTime::class)

package com.anyonehub.barpos.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anyonehub.barpos.di.SupabaseConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant
import javax.inject.Inject
import kotlin.time.ExperimentalTime

@HiltViewModel
class MainViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _isCloudConnected = MutableStateFlow(false)
    val isCloudConnected: StateFlow<Boolean> = _isCloudConnected.asStateFlow()

    private val _isCheckingConnection = MutableStateFlow(false)
    val isCheckingConnection: StateFlow<Boolean> = _isCheckingConnection.asStateFlow()

    // High-Precision Heatbeat Tracker
    private val _lastHeartbeat = MutableStateFlow<Instant?>(null)
    val lastHeartbeat: StateFlow<Instant?> = _lastHeartbeat.asStateFlow()

    init {
        startConnectionMonitor()
    }

    /**
     * Proactive Connection Monitor:
     * Periodically verifies Supabase reachability to ensure "Online-First" reliability.
     */
    private fun startConnectionMonitor() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                checkConnection()
                // Wait 30 seconds between checks if connected, or 5 seconds if offline (to recover faster)
                val delayTime = if (_isCloudConnected.value) 30000L else 5000L
                delay(delayTime)
            }
        }
    }

    /**
     * Performs a lightweight query to Supabase to verify reachability.
     * Essential for high-volume environments where "Closing Out" depends on cloud sync.
     */
    fun checkConnection() {
        if (_isCheckingConnection.value) return
        
        viewModelScope.launch(Dispatchers.IO) {
            _isCheckingConnection.value = true
            try {
                // Heartbeat: Attempt to fetch a single row from app_settings
                supabaseClient.postgrest[SupabaseConstants.TABLE_APP_SETTINGS].select {
                    limit(1)
                }
                
                // Explicitly typed high-precision timestamp for the heartbeat
                val now: Instant = Clock.System.now()
                _lastHeartbeat.value = now
                _isCloudConnected.value = true
                
                Log.d("MainViewModel", "Supabase Cloud Heartbeat: ONLINE at $now")
            } catch (e: Exception) {
                _isCloudConnected.value = false
                Log.e("MainViewModel", "Supabase Cloud Heartbeat: OFFLINE - ${e.message}")
            } finally {
                _isCheckingConnection.value = false
            }
        }
    }
}
