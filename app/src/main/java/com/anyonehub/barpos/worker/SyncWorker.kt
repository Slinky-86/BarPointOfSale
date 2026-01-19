/* Copyright 2024 anyone-Hub */
@file:OptIn(ExperimentalTime::class)

package com.anyonehub.barpos.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anyonehub.barpos.data.*
import com.anyonehub.barpos.di.SupabaseConstants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Named
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime

/**
 * MASTER SUPABASE CLOUD SYNC ENGINE
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val posDao: PosDao,
    private val timeClockDao: TimeClockDao,
    private val salesDao: SalesDao,
    private val supabaseClient: SupabaseClient,
    @Named("admin") private val adminClient: SupabaseClient // ADDED ADMIN CLIENT
) : CoroutineWorker(context, workerParams) {

    private val tag = "SupabaseSyncWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val syncStartTime: Instant = Clock.System.now()
        
        try {
            Log.d(tag, "Starting Comprehensive Cloud Sync Sequence at $syncStartTime...")

            // 1. SYNC NEW STAFF PROFILES (Uses Admin Client for unrestricted profile sync)
            val unsyncedUsers = posDao.getAllUsers().first().filter { !it.isSynced }
            if (unsyncedUsers.isNotEmpty()) {
                unsyncedUsers.forEach { user ->
                    adminClient.postgrest[SupabaseConstants.TABLE_USERS].upsert(user)
                    posDao.insertUser(user.copy(isSynced = true))
                }
            }

            // 2. SYNC TIME CLOCK (Standard audit, can use normal client)
            val unsyncedTime = timeClockDao.getUnsyncedEntries()
            if (unsyncedTime.isNotEmpty()) {
                unsyncedTime.forEach { entry ->
                    supabaseClient.postgrest[SupabaseConstants.TABLE_TIME_CLOCK].upsert(entry)
                    timeClockDao.updateEntry(entry.copy(isSynced = true))
                }
            }

            // 3. SYNC SALES RECORDS (Standard audit, can use normal client)
            val unsyncedSales = salesDao.getUnsyncedSales().first()
            if (unsyncedSales.isNotEmpty()) {
                unsyncedSales.forEach { sale ->
                    supabaseClient.postgrest[SupabaseConstants.TABLE_SALES_RECORDS].upsert(sale)
                    val items = salesDao.getSaleItemsBySaleId(sale.id).first()
                    if (items.isNotEmpty()) {
                        supabaseClient.postgrest[SupabaseConstants.TABLE_SALE_ITEMS].upsert(items)
                    }
                    salesDao.updateSaleSyncStatus(sale.id, true)
                }
            }

            // 4. SYNC MENU UPDATES (Uses Admin Client to bypass RLS for menu changes)
            val menuItems = posDao.getAllMenuItems().first().filter { !it.isSynced }
            if (menuItems.isNotEmpty()) {
                menuItems.forEach { item ->
                    adminClient.postgrest[SupabaseConstants.TABLE_MENU_ITEMS].upsert(item)
                    posDao.insertMenuItem(item.copy(isSynced = true))
                }
            }

            val syncEndTime: Instant = Clock.System.now()
            Log.i(tag, "Cloud Sync Sequence: SUCCESS at $syncEndTime")
            Result.success()

        } catch (e: Exception) {
            Log.e(tag, "Cloud Sync Sequence: FAILED - ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
