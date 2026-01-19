// Copyright 2024 anyone-Hub
@file:OptIn(ExperimentalTime::class)

package com.anyonehub.barpos

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.anyonehub.barpos.di.SupabaseConstants
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dagger.hilt.android.HiltAndroidApp
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime

@HiltAndroidApp
class BarPosApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Inject Supabase to warm up the connection on startup
    @Inject
    lateinit var supabaseClient: SupabaseClient

    // ADD ADMIN CLIENT FOR WARMUP (DO NOT REMOVE NORMAL CLIENT)
    @Inject
    @Named("admin")
    lateinit var adminClient: SupabaseClient

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Python Bridge in background to prevent startup ANR
        applicationScope.launch {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(this@BarPosApp))
            }
        }

        // 2. Warm up Supabase Cloud Connection (Online-First Strategy)
        applicationScope.launch(Dispatchers.IO) {
            try {
                // Ensure auth is initialized and check session
                supabaseClient.auth.currentSessionOrNull()
                
                // Perform a lightweight "Ping" to establish connection
                supabaseClient.postgrest[SupabaseConstants.TABLE_APP_SETTINGS].select {
                    limit(1)
                }
                
                // ALSO WARM UP ADMIN CLIENT (DO NOT REMOVE NORMAL CLIENT LOGIC)
                adminClient.postgrest[SupabaseConstants.TABLE_APP_SETTINGS].select {
                    limit(1)
                }
                
                // Actively using high-precision time for engine readiness log
                val readinessTime: Instant = Clock.System.now()
                Log.i("BarPosApp", "Supabase Cloud Engine: INITIALIZED at $readinessTime")
            } catch (e: Exception) {
                Log.w("BarPosApp", "Supabase Cloud Engine: WARMUP FAILED - ${e.message}")
            }
        }
    }
}
