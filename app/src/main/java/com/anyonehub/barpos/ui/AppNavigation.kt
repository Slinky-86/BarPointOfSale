/* Copyright 2024 anyone-Hub */
package com.anyonehub.barpos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anyonehub.barpos.R
import com.anyonehub.barpos.ui.features.admin.AdminDashboardScreen
import com.anyonehub.barpos.ui.features.auth.LoginScreen
import com.anyonehub.barpos.ui.features.auth.RegisterScreen
import com.anyonehub.barpos.ui.features.history.HistoryScreen
import com.anyonehub.barpos.ui.features.inventory.MenuSummaryScreen
import com.anyonehub.barpos.ui.features.inventory.ZReportScreen
import com.anyonehub.barpos.ui.features.pos.MainPosScreen
import com.anyonehub.barpos.ui.features.pos.PosViewModel
import com.anyonehub.barpos.ui.features.settings.SettingsScreen
import com.anyonehub.barpos.ui.features.settings.StaffStatsScreen
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)
@Composable
fun BarPosNavHost(
    onLogout: () -> Unit,
    onSplashFinished: () -> Unit,
    onToggleTheme: () -> Unit,
    posViewModel: PosViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // UI State for Drawer
    val currentUser by posViewModel.currentUser.collectAsState()
    
    // Determine if drawer should be active (only on POS and Settings screens usually)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isDrawerEnabled = currentRoute == "pos"

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isDrawerEnabled,
        drawerContent = {
            if (isDrawerEnabled) {
                ModalDrawerSheet {
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.padding(16.dp)) {
                        Icon(painterResource(id = R.drawable.ic_midtown_3d), null, Modifier.size(32.dp), tint = Color.Unspecified)
                        Spacer(Modifier.width(12.dp))
                        Text("MidTown POS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider()

                    NavigationDrawerItem(
                        label = { Text("Menu Audit") },
                        icon = { Icon(painterResource(id = R.drawable.ic_audit_3d), null, Modifier.size(24.dp), tint = Color.Unspecified) },
                        selected = false,
                        onClick = { scope.launch { drawerState.close() }; navController.navigate("audit") }
                    )
                    NavigationDrawerItem(
                        label = { Text("Sales History") },
                        icon = { Icon(painterResource(id = R.drawable.ic_history_3d), null, Modifier.size(24.dp), tint = Color.Unspecified) },
                        selected = false,
                        onClick = { scope.launch { drawerState.close() }; navController.navigate("history") }
                    )
                    NavigationDrawerItem(
                        label = { Text("End Shift (Z-Report)") },
                        icon = { Icon(painterResource(id = R.drawable.ic_zreport_3d), null, Modifier.size(24.dp), tint = Color.Unspecified) },
                        selected = false,
                        onClick = { scope.launch { drawerState.close() }; navController.navigate("zreport") }
                    )
                    NavigationDrawerItem(
                        label = { Text("Tip Tracker") },
                        icon = { Icon(painterResource(id = R.drawable.ic_tip_tracker_3d), null, Modifier.size(24.dp), tint = Color.Unspecified) },
                        selected = false,
                        onClick = { scope.launch { drawerState.close() }; navController.navigate("stats") }
                    )
                    
                    if (currentUser?.isManager == true) {
                        NavigationDrawerItem(
                            label = { Text("Admin Insights") },
                            icon = { Icon(painterResource(id = R.drawable.ic_admin_3d), null, Modifier.size(24.dp), tint = Color.Unspecified) },
                            selected = false,
                            onClick = { scope.launch { drawerState.close() }; navController.navigate("admin") }
                        )
                    }

                    NavigationDrawerItem(
                        label = { Text("Settings") },
                        icon = { Icon(painterResource(id = R.drawable.ic_settings_3d), null, Modifier.size(24.dp), tint = Color.Unspecified) },
                        selected = false,
                        onClick = { scope.launch { drawerState.close() }; navController.navigate("settings") }
                    )

                    Spacer(Modifier.weight(1f))
                    
                    NavigationDrawerItem(
                        label = { Text("Logout") },
                        icon = { Icon(painterResource(id = R.drawable.ic_logout_3d), null, Modifier.size(24.dp), tint = Color.Unspecified) },
                        selected = false,
                        onClick = { 
                            scope.launch { 
                                drawerState.close()
                                posViewModel.logout()
                                onLogout() 
                            }
                        }
                    )
                }
            }
        }
    ) {
        NavHost(navController = navController, startDestination = "splash") {
            composable("splash") {
                SplashScreen(onSplashFinished = {
                    onSplashFinished()
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                })
            }
            composable("login") {
                LoginScreen(
                    viewModel = hiltViewModel(),
                    onNavigateToRegister = { navController.navigate("register") },
                    onLoginSuccess = { 
                        navController.navigate("pos") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            composable("register") {
                RegisterScreen(
                    viewModel = hiltViewModel(),
                    onBackToLogin = { navController.popBackStack() }
                )
            }
            composable("pos") {
                MainPosScreen(
                    onToggleTheme = onToggleTheme,
                    viewModel = posViewModel,
                    onLogout = onLogout,
                    onNavigateToStats = { navController.navigate("stats") },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable("settings") { 
                SettingsScreen(
                    onBackClick = { navController.popBackStack() } 
                ) 
            }
            composable("history") { 
                HistoryScreen(
                    onBackClick = { navController.popBackStack() } 
                ) 
            }
            composable("audit") { 
                MenuSummaryScreen(
                    viewModel = hiltViewModel(),
                    onBack = { navController.popBackStack() } 
                ) 
            }
            composable("admin") { 
                AdminDashboardScreen(
                    viewModel = hiltViewModel(),
                    onBack = { navController.popBackStack() } 
                ) 
            }
            composable("zreport") { 
                ZReportScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() } 
                ) 
            }
            composable("stats") { 
                StaffStatsScreen(
                    posViewModel = posViewModel,
                    authViewModel = hiltViewModel(),
                    onBack = { navController.popBackStack() } 
                ) 
            }
        }
    }
}
