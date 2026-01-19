/* Copyright 2024 anyone-Hub */
package com.anyonehub.barpos.ui.features.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anyonehub.barpos.data.AppSetting
import com.anyonehub.barpos.data.Category
import com.anyonehub.barpos.data.MenuItem
import com.anyonehub.barpos.data.PosDao
import com.anyonehub.barpos.data.User
import com.anyonehub.barpos.data.BackupRepository
import com.anyonehub.barpos.util.MenuExportManager
import com.anyonehub.barpos.util.shareFile // Correctly importing centralized utility
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val posDao: PosDao,
    private val backupRepository: BackupRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // --- DATA STREAMS ---
    val appSettings: StateFlow<AppSetting?> = posDao.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allUsers: StateFlow<List<User>> = posDao.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = posDao.getAllCategoriesRaw()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val menuItems: StateFlow<List<MenuItem>> = posDao.getAllMenuItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- EVENTS ---
    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    // --- ACTIONS ---

    fun updateSettings(newSettings: AppSetting) {
        viewModelScope.launch(Dispatchers.IO) {
            posDao.saveSettings(newSettings)
            _events.emit("System Configuration Updated")
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            // PRODUCTION GUARD: Protect the hardcoded admin
            if (user.pinCode == "9999") {
                _events.emit("CRITICAL: Root Admin cannot be deleted.")
                return@launch
            }

            val managers = allUsers.value.count { it.isManager && it.isActive }
            if (user.isManager && managers <= 1) {
                _events.emit("ERROR: At least one manager account is required.")
                return@launch
            }

            posDao.deleteUser(user)
            _events.emit("${user.name} has been removed.")
        }
    }

    // --- BACKUP & RESTORE ---

    fun performBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = backupRepository.backupDatabase(uri)
            if (result.isSuccess) {
                _events.emit("Database Backup Successful")
            } else {
                _events.emit("Backup Failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun performRestore(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = backupRepository.restoreDatabase(uri)
            if (result.isSuccess) {
                _events.emit("Restore Complete. RESTART APP TO APPLY CHANGES.")
            } else {
                _events.emit("Restore Failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    // --- EXPORTS ---

    fun exportMenuCsv() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = MenuExportManager.exportToCsv(context, categories.value, menuItems.value)
            if (file != null) {
                shareFile(context, file, "text/csv") // Using standardized utility
            } else {
                _events.emit("Failed to generate CSV")
            }
        }
    }

    fun exportMenuPdf() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = MenuExportManager.exportToPdf(context, categories.value, menuItems.value)
            if (file != null) {
                shareFile(context, file, "application/pdf") // FIXED: Using standardized utility
            } else {
                _events.emit("Failed to generate PDF")
            }
        }
    }
}