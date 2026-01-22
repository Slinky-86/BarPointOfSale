/* Copyright 2024 anyone-Hub */
@file:OptIn(ExperimentalTime::class)
@file:Suppress("AssignedValueIsNeverRead", "RemoveRedundantQualifierName")

package com.anyonehub.barpos.ui.features.pos

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anyonehub.barpos.R
import com.anyonehub.barpos.data.MenuItem
import com.anyonehub.barpos.data.UserRole
import com.anyonehub.barpos.ui.MainViewModel
import com.anyonehub.barpos.ui.features.auth.TimeClockDialog
import com.anyonehub.barpos.ui.features.inventory.AddCategoryDialog
import com.anyonehub.barpos.ui.features.inventory.AddEditItemDialog
import com.anyonehub.barpos.ui.features.inventory.AddMenuGroupDialog
import com.anyonehub.barpos.ui.features.inventory.RestockDialog
import com.anyonehub.barpos.ui.shared.TutorialOverlay
import com.anyonehub.barpos.ui.shared.dialogs.SettlementDialog
import com.anyonehub.barpos.ui.shared.dialogs.EditLineItemPriceDialog
import com.anyonehub.barpos.ui.shared.dialogs.LineItemActionDialog
import com.anyonehub.barpos.ui.shared.dialogs.TabSelectionDialog
import com.anyonehub.barpos.util.ReceiptManager
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPosScreen(
    onToggleTheme: () -> Unit,
    viewModel: PosViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
    onLogout: () -> Unit,
    onNavigateToStats: () -> Unit,
    onMenuClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // DETECT ORIENTATION
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    // --- VIEWMODEL STATE ---
    val isEditMode by viewModel.isEditMode.collectAsState()
    val currentTab by viewModel.currentActiveTab.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val shotWallItems by viewModel.shotWallItems.collectAsState()
    val menuGroups by viewModel.menuGroups.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val gridItems by viewModel.currentGridItems.collectAsState()
    val selectedGroup by viewModel.selectedMenuGroup.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val currentTabItems by viewModel.currentTabItems.collectAsState()
    val subtotal by viewModel.currentSubtotal.collectAsState()
    val total by viewModel.currentGrandTotal.collectAsState()
    val openTabs by viewModel.openTabs.collectAsState()
    val currentTabId by viewModel.currentTabId.collectAsState()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val isDark = appSettings?.isDarkMode ?: isSystemInDarkTheme()

    // --- UI STATE ---
    var showShotWallDialog by remember { mutableStateOf(false) }
    var showTimeClockDialog by remember { mutableStateOf(false) }
    var isTabPanelVisible by remember { mutableStateOf(true) }
    var showTabDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showSpecialsDialog by remember { mutableStateOf(false) }
    var selectedLineItemId by remember { mutableStateOf<Long?>(null) }
    var lineItemToEditPrice by remember { mutableStateOf<Long?>(null) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<MenuItem?>(null) }
    var itemToSelectPricing by remember { mutableStateOf<MenuItem?>(null) }
    var itemToBuild by remember { mutableStateOf<MenuItem?>(null) }
    var itemToRestock by remember { mutableStateOf<MenuItem?>(null) }
    var showManualTutorial by remember { mutableStateOf(false) }

    // TRACK SESSION START TIME
    val sessionStartTime: Instant = remember { Clock.System.now() }

    // FLASH ICON STATE
    var flashIcon by remember { mutableStateOf<Int?>(null) }

    // Collect Error Events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(id = R.drawable.ic_midtown_3d), null, Modifier.size(32.dp), tint = Color.Unspecified)
                        if (!isPortrait) {
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    if (isEditMode) "EDIT MODE ACTIVE" else "MidTown POS",
                                    color = if (isEditMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                currentUser?.let {
                                    Text(it.name, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(painterResource(id = R.drawable.ic_menu_3d), null, Modifier.size(24.dp), tint = Color.Unspecified)
                    }
                },
                actions = {
                    CloudStatusIndicator(mainViewModel)
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            painter = if (isDark) painterResource(id = R.drawable.ic_light_mode_3d) else painterResource(id = R.drawable.ic_dark_mode_3d),
                            contentDescription = "Theme",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                    }
                    IconButton(onClick = { showTimeClockDialog = true }) {
                        Icon(painterResource(id = R.drawable.ic_time_3d), null, Modifier.size(24.dp), tint = Color.Unspecified)
                    }
                    IconButton(onClick = onNavigateToStats) {
                        Icon(painterResource(id = R.drawable.ic_user_profile_3d), "Stats", Modifier.size(24.dp), tint = Color.Unspecified)
                    }
                    IconButton(onClick = { showSpecialsDialog = true }) {
                        Icon(painterResource(id = R.drawable.ic_star_on), "Specials", Modifier.size(24.dp), tint = Color.Unspecified)
                    }
                    IconButton(onClick = { showShotWallDialog = true }) {
                        Icon(painterResource(id = R.drawable.ic_shot_3d), "Shot Wall", Modifier.size(24.dp), tint = Color.Unspecified)
                    }
                    IconButton(onClick = { showTabDialog = true }) {
                        Icon(painterResource(id = R.drawable.ic_3d_add), "Tabs", Modifier.size(24.dp), tint = Color.Unspecified)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    if (currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.MANAGER) {
                        viewModel.toggleEditMode() 
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Administrator access required.")
                        }
                    }
                },
                containerColor = if (isEditMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    painter = if (isEditMode) painterResource(id = R.drawable.ic_3d_lock) else painterResource(id = R.drawable.ic_admin_3d),
                    contentDescription = "Edit",
                    modifier = Modifier.size(32.dp),
                    tint = Color.Unspecified
                )
            }
        }
    ) { padding ->
        if (isPortrait) {
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    MenuLayout(
                        isEditMode = isEditMode,
                        showAddGroupDialog = { showAddGroupDialog = true },
                        showAddCategoryDialog = { showAddCategoryDialog = true },
                        menuGroups = menuGroups,
                        selectedGroup = selectedGroup,
                        allCategories = allCategories,
                        selectedCategory = selectedCategory,
                        gridItems = gridItems,
                        openTabs = openTabs,
                        currentTabId = currentTabId,
                        viewModel = viewModel,
                        onItemClick = { item ->
                            if (isEditMode) {
                                itemToEdit = item 
                            } else {
                                val category = allCategories.find { it.id == item.categoryId }
                                if (category?.requiresBuilder == true) {
                                    itemToBuild = item
                                } else {
                                    itemToSelectPricing = item
                                }
                            }
                        },
                        onTabDialog = { showTabDialog = true }
                    )
                }
                
                AnimatedVisibility(visible = isTabPanelVisible) {
                    CurrentTabPanel(
                        activeTab = currentTab,
                        items = currentTabItems,
                        subtotal = subtotal,
                        tax = total - subtotal,
                        total = total,
                        onPayClick = { showPaymentDialog = true },
                        onLineItemClick = { lineItemId -> selectedLineItemId = lineItemId },
                        onCollapse = { isTabPanelVisible = false },
                        modifier = Modifier.fillMaxWidth().height(300.dp)
                    )
                }
                
                if (!isTabPanelVisible) {
                    Button(
                        onClick = { isTabPanelVisible = true },
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        val now = Clock.System.now()
                        val duration: Duration = now - sessionStartTime
                        val mins = duration.inWholeMinutes
                        Text("SHOW ACTIVE TAB ($${String.format(Locale.US, "%.2f", total)}) - Session: ${mins}m")
                    }
                }
            }
        } else {
            Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                Column(modifier = Modifier.weight(1f)) {
                    MenuLayout(
                        isEditMode = isEditMode,
                        showAddGroupDialog = { showAddGroupDialog = true },
                        showAddCategoryDialog = { showAddCategoryDialog = true },
                        menuGroups = menuGroups,
                        selectedGroup = selectedGroup,
                        allCategories = allCategories,
                        selectedCategory = selectedCategory,
                        gridItems = gridItems,
                        openTabs = openTabs,
                        currentTabId = currentTabId,
                        viewModel = viewModel,
                        onItemClick = { item ->
                            if (isEditMode) {
                                itemToEdit = item 
                            } else {
                                val category = allCategories.find { it.id == item.categoryId }
                                if (category?.requiresBuilder == true) {
                                    itemToBuild = item
                                } else {
                                    itemToSelectPricing = item
                                }
                            }
                        },
                        onTabDialog = { showTabDialog = true }
                    )
                }

                AnimatedVisibility(visible = isTabPanelVisible) {
                    CurrentTabPanel(
                        activeTab = currentTab,
                        items = currentTabItems,
                        subtotal = subtotal,
                        tax = total - subtotal,
                        total = total,
                        onPayClick = { showPaymentDialog = true },
                        onLineItemClick = { lineItemId -> selectedLineItemId = lineItemId },
                        onCollapse = { isTabPanelVisible = false },
                        modifier = Modifier.fillMaxHeight().width(320.dp)
                    )
                }

                if (!isTabPanelVisible) {
                    Box(
                        modifier = Modifier.fillMaxHeight().width(40.dp).background(MaterialTheme.colorScheme.primaryContainer).clickable { isTabPanelVisible = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(id = R.drawable.ic_undo), null, Modifier.size(24.dp), tint = Color.Unspecified)
                    }
                }
            }
        }

        flashIcon?.let { iconRes ->
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                Image(painterResource(id = iconRes), null, Modifier.size(200.dp))
            }
        }
    }

    // --- DIALOG LOGIC ---
    if (showTimeClockDialog && currentUser != null) {
        TimeClockDialog(user = currentUser!!, onDismiss = { showTimeClockDialog = false }, onClockOut = { viewModel.logout(); onLogout() })
    }
    if (showShotWallDialog) {
        ShotWallDialog(items = shotWallItems, isEditMode = isEditMode, onDismiss = { showShotWallDialog = false }, onItemClick = { itemToSelectPricing = it }, onRestockClick = { itemToRestock = it })
    }
    itemToSelectPricing?.let { item ->
        ItemPricingDialog(item = item, onDismiss = { itemToSelectPricing = null }, onSelectPrice = { price, label -> viewModel.addToTab(item, overridePrice = price, label = label); itemToSelectPricing = null })
    }
    itemToBuild?.let { item ->
        val allMenuItems by viewModel.allMenuItems.collectAsState()
        val modifiers = allMenuItems.filter { it.categoryId == item.categoryId && it.isModifier }
        ModifierSelectionDialog(
            mainItem = item,
            modifiers = modifiers,
            onDismiss = { itemToBuild = null },
            onComplete = { note ->
                viewModel.addToTab(item, label = note)
                itemToBuild = null
            }
        )
    }
    itemToEdit?.let { item ->
        AddEditItemDialog(
            existingItem = item, 
            onDismiss = { itemToEdit = null }, 
            onDelete = { viewModel.deleteMenuItem(item.id); itemToEdit = null }, 
            onConfirm = { updatedItem -> 
                viewModel.saveMenuItem(updatedItem)
                itemToEdit = null 
            }
        )
    }
    itemToRestock?.let { item ->
        RestockDialog(item = item, onDismiss = { itemToRestock = null }, onConfirm = { count -> viewModel.restockItem(item.id, count); itemToRestock = null })
    }
    
    if (showAddGroupDialog) { AddMenuGroupDialog(onDismiss = { showAddGroupDialog = false }, onConfirm = { name -> viewModel.addMenuGroup(name); showAddGroupDialog = false }) }
    if (showAddCategoryDialog) { 
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false }, 
            onConfirm = { name, icon, requiresBuilder -> 
                selectedGroup?.let { viewModel.addCategory(it.id.toLong(), name, icon, requiresBuilder) } 
                showAddCategoryDialog = false 
            }
        ) 
    }
    if (showTabDialog) { TabSelectionDialog(openTabs = openTabs, currentTabId = currentTabId, onDismiss = { showTabDialog = false }, onTabSelected = { id -> viewModel.switchTab(id); showTabDialog = false }, onCreateNew = { name -> viewModel.createNewTab(name); showTabDialog = false }, onRenameTab = { id, name -> viewModel.renameTab(id, name) }, posViewModel = viewModel) }
    
    currentTab?.let { activeTab ->
        if (showPaymentDialog) {
            SettlementDialog(
                customerName = activeTab.customerName, 
                totalDue = total, 
                onDismiss = { showPaymentDialog = false }, 
                onConfirmPay = { type ->
                    scope.launch { 
                        ReceiptManager.generateAndShareReceipt(
                            context = context, 
                            tab = activeTab, 
                            items = currentTabItems, 
                            total = total, 
                            supabaseStorage = viewModel.supabaseStorage
                        ) 
                        viewModel.closeTab(paymentType = type)
                        showPaymentDialog = false 
                    } 
                }
            )
        }
    }
    selectedLineItemId?.let { lineId ->
        val entry = currentTabItems.entries.find { it.key.id == lineId }
        entry?.let { itemEntry ->
            LineItemActionDialog(itemName = itemEntry.value.name, currentNote = itemEntry.key.note, isAdmin = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.MANAGER, onDismiss = { selectedLineItemId = null }, onVoid = { viewModel.voidItem(lineId); selectedLineItemId = null }, onSaveNote = { note -> viewModel.updateItemNote(lineId, note); selectedLineItemId = null }, onEditPrice = { lineItemToEditPrice = lineId; selectedLineItemId = null })
        }
    }
    lineItemToEditPrice?.let { lineId ->
        val entry = currentTabItems.entries.find { it.key.id == lineId }
        entry?.let { itemEntry ->
            EditLineItemPriceDialog(itemName = itemEntry.value.name, currentPrice = itemEntry.key.priceAtTimeOfSale, onDismiss = { lineItemToEditPrice = null }, onConfirm = { newPrice -> viewModel.updateLineItemPrice(lineId, newPrice); lineItemToEditPrice = null })
        }
    }
    if (showSpecialsDialog) {
        val currentSettings by viewModel.appSettings.collectAsState()
        SpecialsDialog(allMenuItems = viewModel.allMenuItems.collectAsState().value, specialsJson = currentSettings?.specialsJson ?: "{}", onDismiss = { showSpecialsDialog = false }, onUpdateSpecial = { itemId, price -> viewModel.updateItemSpecial(itemId, price) })
    }
    val isFirstTime = currentUser?.hasSeenTutorial == false
    if (showManualTutorial || isFirstTime) {
        TutorialOverlay(onComplete = { if (isFirstTime) viewModel.markTutorialSeen(); showManualTutorial = false })
    }
}

@Composable
fun MenuLayout(
    isEditMode: Boolean,
    showAddGroupDialog: () -> Unit,
    showAddCategoryDialog: () -> Unit,
    menuGroups: List<com.anyonehub.barpos.data.MenuGroup>,
    selectedGroup: com.anyonehub.barpos.data.MenuGroup?,
    allCategories: List<com.anyonehub.barpos.data.Category>,
    selectedCategory: com.anyonehub.barpos.data.Category?,
    gridItems: List<com.anyonehub.barpos.data.MenuItem>,
    openTabs: List<com.anyonehub.barpos.data.ActiveTab>,
    currentTabId: Long,
    viewModel: PosViewModel,
    onItemClick: (com.anyonehub.barpos.data.MenuItem) -> Unit,
    onTabDialog: () -> Unit
) {
    Column {
        AnimatedVisibility(visible = isEditMode) {
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)).padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(onClick = showAddGroupDialog, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("NEW GROUP") }
                Spacer(Modifier.width(16.dp))
                Button(onClick = showAddCategoryDialog, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("NEW CATEGORY") }
            }
        }

        MenuContent(
            menuGroups = menuGroups,
            selectedMenuGroup = selectedGroup,
            allCategories = allCategories,
            selectedCategory = selectedCategory,
            gridItems = gridItems,
            openTabs = openTabs,
            currentTabId = currentTabId,
            isEditMode = isEditMode,
            onGroupSelect = viewModel::selectMenuGroup,
            onCategorySelect = viewModel::selectCategory,
            onItemClick = onItemClick,
            onTabSelect = viewModel::switchTab,
            onCreateTabClick = onTabDialog
        )
    }
}

@Composable
fun CloudStatusIndicator(viewModel: MainViewModel) {
    val isConnected by viewModel.isCloudConnected.collectAsStateWithLifecycle()
    val isChecking by viewModel.isCheckingConnection.collectAsStateWithLifecycle()
    val lastHeartbeat by viewModel.lastHeartbeat.collectAsStateWithLifecycle()
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val targetColor = when {
        isChecking -> Color(0xFFFF9800)
        isConnected -> Color(0xFF4CAF50)
        else -> Color(0xFFF44336)
    }
    val animatedColor by animateColorAsState(targetValue = targetColor, animationSpec = tween(500), label = "color")
    val pulseProgress by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(if (isChecking) 800 else 2000, easing = LinearEasing), RepeatMode.Restart), label = "pulse")

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isConnected && lastHeartbeat != null) {
            val localTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(lastHeartbeat!!.toEpochMilliseconds()), ZoneId.systemDefault())
            Text(text = String.format(Locale.US, "%02d:%02d", localTime.hour, localTime.minute), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(end = 4.dp))
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp).clickable { viewModel.checkConnection() }) {
            if (isConnected || isChecking) {
                Box(modifier = Modifier.size(20.dp).graphicsLayer { val scaleVal = 0.8f + (pulseProgress * 1.0f); scaleX = scaleVal; scaleY = scaleVal; alpha = 1f - pulseProgress }.background(animatedColor, shape = CircleShape))
            }
            Icon(painter = painterResource(id = R.drawable.ic_cloud_upload), contentDescription = null, tint = animatedColor, modifier = Modifier.size(20.dp))
        }
    }
}
