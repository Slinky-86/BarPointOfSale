// Copyright 2024 anyone-Hub
@file:OptIn(ExperimentalCoroutinesApi::class, InternalAPI::class, ExperimentalTime::class)

package com.anyonehub.barpos.ui.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anyonehub.barpos.data.*
import com.anyonehub.barpos.data.repository.PosRepository
import com.anyonehub.barpos.di.SupabaseConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock
import kotlin.time.Instant
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.ExperimentalTime

sealed class TimeClockEvent {
    object ClockedOut : TimeClockEvent()
}

@HiltViewModel
class TimeClockViewModel @Inject constructor(
    private val repository: PosRepository,
    private val supabaseClient: SupabaseClient,
    @Named("admin") private val adminClient: SupabaseClient
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)

    val lastClockEvent: StateFlow<TimeClockEntry?> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getLastClockEvent(user.id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _statusMessage = MutableSharedFlow<String>()
    val statusMessage = _statusMessage.asSharedFlow()

    private val _events = MutableSharedFlow<TimeClockEvent>()
    val events = _events.asSharedFlow()

    fun setCurrentUser(user: User) {
        _currentUser.value = user
    }

    /**
     * Handles shift events (Clock In/Out) using high-precision Instant.
     * Essential for fair and accurate payroll in busy environments.
     */
    fun handleClockEvent(eventType: ClockEventType) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            // Explicitly using the Instant type to satisfy the high-precision requirement
            val now: Instant = Clock.System.now()
            
            val entry = TimeClockEntry(
                userId = user.id,
                supabaseUserId = user.supabaseId,
                eventType = eventType,
                timestamp = now
            )
            
            // 1. Log to Local Database (Hardened with Instant)
            repository.logTimeClock(entry)
            
            // 2. Trigger Cloud Sync with High-Precision Body Data
            // Uses both Admin and Normal clients in conjunction for authoritative logging
            try {
                val bodyData = buildJsonObject {
                    put("staff_name", user.name)
                    put("event_type", eventType.name)
                    put("timestamp", now.toEpochMilliseconds())
                }
                
                try {
                    adminClient.functions.invoke(SupabaseConstants.FUNCTION_SERVER_CHECKOUT) {
                        body = bodyData
                    }
                } catch (_: Exception) {
                    supabaseClient.functions.invoke(SupabaseConstants.FUNCTION_SERVER_CHECKOUT) {
                        body = bodyData
                    }
                }
            } catch (_: Exception) { }

            _statusMessage.emit("Successfully logged: ${eventType.name}")
            
            if (eventType == ClockEventType.SHIFT_END) {
                _events.emit(TimeClockEvent.ClockedOut)
            }
        }
    }
}
