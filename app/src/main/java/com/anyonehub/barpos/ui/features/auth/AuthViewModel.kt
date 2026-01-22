/* Copyright 2024 anyone-Hub */
@file:OptIn(ExperimentalTime::class)

package com.anyonehub.barpos.ui.features.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anyonehub.barpos.data.PosDao
import com.anyonehub.barpos.data.SessionManager
import com.anyonehub.barpos.data.User
import com.anyonehub.barpos.data.UserRole
import com.anyonehub.barpos.data.repository.PosRepository
import com.anyonehub.barpos.di.SupabaseConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime
import javax.inject.Inject
import javax.inject.Named

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}

sealed class AuthEvent {
    object NavigateToPos : AuthEvent()
    data class ShowMessage(val msg: String) : AuthEvent()
    data class RegistrationSuccess(val msg: String) : AuthEvent()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val posDao: PosDao,
    private val posRepository: PosRepository,
    private val supabaseClient: SupabaseClient,
    @Named("admin") private val adminClient: SupabaseClient,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _authEvents = MutableSharedFlow<AuthEvent>()
    val authEvents = _authEvents.asSharedFlow()

    private val adminEmails = listOf("slinkiesfam@gmail.com", "kimascott81@gmail.com")

    init {
        // Recover session if already authenticated (e.g. App Restart)
        viewModelScope.launch {
            supabaseClient.auth.sessionStatus
                .collect { status ->
                    if (status is SessionStatus.Authenticated) {
                        val supabaseUserId = status.session.user?.id
                        if (supabaseUserId != null) {
                            var user = withContext(Dispatchers.IO) {
                                posDao.getUserBySupabaseId(supabaseUserId)
                            }
                            
                            if (user == null) {
                                try {
                                    user = try {
                                        adminClient.postgrest[SupabaseConstants.TABLE_USERS]
                                            .select { filter { User::supabaseId eq supabaseUserId } }
                                            .decodeSingleOrNull<User>()
                                    } catch (_: Exception) {
                                        supabaseClient.postgrest[SupabaseConstants.TABLE_USERS]
                                            .select { filter { User::supabaseId eq supabaseUserId } }
                                            .decodeSingleOrNull<User>()
                                    }
                                    
                                    user?.let {
                                        withContext(Dispatchers.IO) { posDao.insertUser(it.copy(isSynced = true)) }
                                    }
                                } catch (e: Exception) {
                                    Log.e("AuthViewModel", "Failed to fetch profile for session recovery: ${e.message}")
                                }
                            }

                            user?.let {
                                sessionManager.setCurrentUser(it)
                                checkAvatarReminder(it)
                                _loginState.value = LoginState.Success(it)
                                _authEvents.emit(AuthEvent.NavigateToPos)
                            }
                        }
                    }
                }
        }
    }

    /**
     * SUPABASE-FIRST Login: Checks Supabase cloud identity as the primary source of truth.
     */
    fun login(name: String, pin: String) {
        viewModelScope.launch {
            if (_loginState.value is LoginState.Loading) return@launch
            _loginState.value = LoginState.Loading

            // 1. SUPABASE CLOUD CHECK (Primary Source of Truth)
            // This bypasses the local DB and goes directly to the cloud.
            val cloudUser = try {
                posRepository.verifyAndSyncUser(name, pin)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Supabase cloud check failed: ${e.message}")
                null
            }
            
            // 2. USE CLOUD USER OR FALLBACK TO LOCAL (Local is only a backup if cloud is unreachable)
            val userToAuth = cloudUser ?: withContext(Dispatchers.IO) { posDao.loginUser(name, pin) }

            if (userToAuth != null) {
                try {
                    // 3. COMPLETE CLOUD AUTH
                    supabaseClient.auth.signInWith(Email) {
                        email = userToAuth.email
                        password = pin 
                    }

                    sessionManager.setCurrentUser(userToAuth)
                    checkAvatarReminder(userToAuth)
                    _loginState.value = LoginState.Success(userToAuth)
                    _authEvents.emit(AuthEvent.NavigateToPos)

                } catch (e: Exception) {
                    Log.w("AuthViewModel", "Cloud Auth failed, attempting emergency offline entry: ${e.message}")
                    // If we found them (via local or cloud verify) but sign-in fails, allow access if they are a known user
                    sessionManager.setCurrentUser(userToAuth)
                    _loginState.value = LoginState.Success(userToAuth)
                    _authEvents.emit(AuthEvent.NavigateToPos)
                }
            } else {
                _loginState.value = LoginState.Error("Login failed. Profile not found on Supabase or Local DB. Please verify Name and PIN.")
            }
        }
    }

    /**
     * CLOUD-FIRST Registration: Ensures staff data is global before local.
     */
    fun registerStaff(name: String, emailAddr: String, pin: String, role: UserRole, isManager: Boolean) {
        viewModelScope.launch {
            try {
                val finalIsManager = isManager || adminEmails.contains(emailAddr.lowercase())
                val finalRole = if (adminEmails.contains(emailAddr.lowercase())) UserRole.ADMIN else role

                // 1. REGISTER IN SUPABASE AUTH
                val userInfo = supabaseClient.auth.signUpWith(provider = Email) {
                    email = emailAddr
                    password = pin
                }
                
                val supabaseId = userInfo?.id ?: supabaseClient.auth.currentSessionOrNull()?.user?.id

                val now: Instant = Clock.System.now()

                val newUser = User(
                    supabaseId = supabaseId,
                    name = name,
                    email = emailAddr,
                    pinCode = pin,
                    role = finalRole,
                    isManager = finalIsManager,
                    isActive = true,
                    isSynced = true,
                    createdAt = now
                )

                // 2. SAVE TO SUPABASE 'users' TABLE AND ROOM (Write-Through)
                withContext(Dispatchers.IO) {
                    posRepository.saveUser(newUser)
                }
                
                _authEvents.emit(AuthEvent.RegistrationSuccess("Registered $name successfully"))
                sessionManager.setCurrentUser(newUser)
                checkAvatarReminder(newUser)
                _loginState.value = LoginState.Success(newUser)
                _authEvents.emit(AuthEvent.NavigateToPos)

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Registration Failed", e)
                _loginState.value = LoginState.Error("Reg Error: ${e.localizedMessage}")
            }
        }
    }

    private fun checkAvatarReminder(user: User) {
        if (user.avatarUrl.isNullOrBlank()) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            if (user.lastReminderDate != today) {
                viewModelScope.launch {
                    _authEvents.emit(AuthEvent.ShowMessage("Secure Profile incomplete: Please upload your staff photo in Settings."))
                    withContext(Dispatchers.IO) {
                        posDao.insertUser(user.copy(lastReminderDate = today))
                    }
                }
            }
        }
    }

    fun uploadStaffPhoto(photoFile: File) {
        val user = sessionManager.currentUser.value ?: return
        viewModelScope.launch {
            val photoUrl = posRepository.uploadStaffAvatar(user.supabaseId ?: "", photoFile)
            if (photoUrl != null) {
                val updatedUser = user.copy(avatarUrl = photoUrl, isSynced = true)
                withContext(Dispatchers.IO) {
                    posRepository.saveUser(updatedUser)
                }
                sessionManager.setCurrentUser(updatedUser)
                _authEvents.emit(AuthEvent.ShowMessage("Secure Profile Updated Successfully"))
            } else {
                _authEvents.emit(AuthEvent.ShowMessage("Photo upload failed. Please try again."))
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}
