// Copyright 2024 anyone-Hub
package com.anyonehub.barpos.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {

    // CRITICAL FIX: Must match the string used in Room.databaseBuilder() inside AppDatabase.kt
    private val dbName = "midtown_pos_db"

    suspend fun backupDatabase(destUri: Uri): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // 1. Force SQLite to write all transactions from the WAL file to the main DB file
            database.openHelper.writableDatabase.query(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))

            val dbPath = context.getDatabasePath(dbName)

            if (!dbPath.exists()) {
                Log.e("BackupRepo", "Database file does not exist at: ${dbPath.absolutePath}")
                return@withContext Result.failure(Exception("DB file not found"))
            }

            context.contentResolver.openOutputStream(destUri)?.use { output ->
                FileInputStream(dbPath).use { input ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Output stream failed"))

            Result.success(true)
        } catch (e: Exception) {
            Log.e("BackupRepo", "Backup failed", e)
            Result.failure(e)
        }
    }

    suspend fun restoreDatabase(sourceUri: Uri): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val dbPath = context.getDatabasePath(dbName)

            // 2. Close the connection to prevent file locks
            if (database.isOpen) database.close()

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(dbPath).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Input stream failed"))

            // 3. Nuke the temporary WAL/SHM files to prevent data corruption on reload
            File(dbPath.parent, "$dbName-wal").delete()
            File(dbPath.parent, "$dbName-shm").delete()

            Result.success(true)
        } catch (e: Exception) {
            Log.e("BackupRepo", "Restore failed", e)
            Result.failure(e)
        }
    }
}