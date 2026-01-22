// Copyright 2024 anyone-Hub
@file:OptIn(ExperimentalTime::class)

package com.anyonehub.barpos.ui.features.inventory

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anyonehub.barpos.data.Category
import com.anyonehub.barpos.data.MenuGroup
import com.anyonehub.barpos.data.MenuItem
import com.anyonehub.barpos.data.PosDao
import com.anyonehub.barpos.data.repository.PosRepository
import com.anyonehub.barpos.di.SupabaseConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
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
import javax.inject.Named

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val posDao: PosDao,
    private val posRepository: PosRepository,
    private val supabaseClient: SupabaseClient,
    @Named("admin") private val adminClient: SupabaseClient // ADDED ADMIN CLIENT
) : ViewModel() {

    // --- DATA STREAMS ---
    val menuGroups: StateFlow<List<MenuGroup>> = posDao.getAllMenuGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = posDao.getAllCategoriesRaw()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allItems: StateFlow<List<MenuItem>> = posDao.getAllMenuItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    // --- CRUD ACTIONS (Online-First) ---

    /**
     * Handles both creating and updating menu items with Cloud Sync.
     * Updated: Aligned with Dynamic Pricing JSON
     */
    fun saveMenuItem(item: MenuItem) {
        viewModelScope.launch(Dispatchers.IO) {
            // High-precision audit of the save/update event
            val actionTime: Instant = Clock.System.now()
            Log.d("InventoryViewModel", "Saving menu item '${item.name}' with icon '${item.iconName}' at $actionTime")

            posRepository.saveMenuItem(item)
            _events.emit(if (item.id != 0) "Item Updated: ${item.name}" else "Item Created: ${item.name}")
        }
    }

    /**
     * Deactivates an item by setting isActive to 0 in the DB.
     */
    fun deleteMenuItem(itemId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val deleteTime: Instant = Clock.System.now()
            Log.d("InventoryViewModel", "Deactivating item ID $itemId at $deleteTime")
            
            posDao.deleteMenuItem(itemId)
            _events.emit("Item Removed")
        }
    }

    /**
     * Direct stock adjustment used by the Audit screen with Cloud Sync.
     */
    fun adjustStock(itemId: Int, newCount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val adjustTime: Instant = Clock.System.now()
            Log.d("InventoryViewModel", "Adjusting stock for ID $itemId to $newCount at $adjustTime")
            
            posRepository.updateStock(itemId, newCount)
            _events.emit("Stock Adjusted")
        }
    }

    /**
     * Triggers the Cloud Z-Report Edge Function (Authoritative Sync)
     */
    fun triggerCloudSync() {
        viewModelScope.launch {
            try {
                val syncTime: Instant = Clock.System.now()
                // Online-First Pulse: Push to Supabase immediately
                // Use Admin client for authoritative sync trigger, falling back to normal client
                try {
                    adminClient.functions.invoke(SupabaseConstants.FUNCTION_DAILY_SUMMARY)
                } catch (_: Exception) {
                    supabaseClient.functions.invoke(SupabaseConstants.FUNCTION_DAILY_SUMMARY)
                }

                Log.i("InventoryViewModel", "Cloud Z-Report Pulse Successful at $syncTime")
                _events.emit("Cloud Z-Report Sync Successful")
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Cloud Sync Pulse Failed: ${e.message}")
                _events.emit("Cloud Sync Failed: ${e.message}")
            }
        }
    }
}
