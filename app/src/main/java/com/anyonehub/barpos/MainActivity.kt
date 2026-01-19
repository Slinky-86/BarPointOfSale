/* Copyright 2024 anyone-Hub */
@file:Suppress("AssignedValueIsNeverRead")

package com.anyonehub.barpos

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.anyonehub.barpos.ui.BarPosNavHost
import com.anyonehub.barpos.ui.theme.BarPosTheme
import com.anyonehub.barpos.worker.SyncWorker
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var supabaseClient: SupabaseClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startupTime: Instant = Clock.System.now()
        Log.i("MainActivity", "POS Session Initiated at: $startupTime")
        supabaseClient.handleDeeplinks(intent)

        setContent {
            // 1. Create the state here so it can be passed down
            var isDarkMode by remember { mutableStateOf(false) }

            // 2. Pass that state into your Theme
            BarPosTheme(darkTheme = isDarkMode) {
                // 3. Use a Surface to ensure the background color fills the screen
                // (including under the transparent status bar)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var shouldRequestPermissions by remember { mutableStateOf(false) }

                    if (shouldRequestPermissions) {
                        PermissionHandler()
                    }

                    // 4. Pass the state and the toggle function to your NavHost
                    BarPosNavHost(
                        onToggleTheme = { isDarkMode = !isDarkMode },
                        onLogout = { finish() },
                        onSplashFinished = { }
                    )
                }
            }
        }
        // 5. Background Sync
        scheduleSyncWorker()
    }

    @Composable
    private fun PermissionHandler() {
        val permissionsToRequest = mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            }
        }

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissions.entries.forEach {
                Log.d("MainActivity", "Permission ${it.key}: ${it.value}")
            }
        }

        LaunchedEffect(Unit) {
            if (permissionsToRequest.isNotEmpty()) {
                launcher.launch(permissionsToRequest.toTypedArray())
            }
        }
    }

    private fun scheduleSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SupabaseSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
