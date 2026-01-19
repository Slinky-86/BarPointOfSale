// Copyright 2024 anyone-Hub
@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class, ExperimentalTime::class)
@file:Suppress("unused")

package com.anyonehub.barpos.ui.features.pos

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anyonehub.barpos.data.*
import com.anyonehub.barpos.data.repository.PosRepository
import com.anyonehub.barpos.di.SupabaseConstants
import com.anyonehub.barpos.util.PricingEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class PosViewModel @Inject constructor(
    private val posDao: PosDao,
    private val repository: PosRepository,
    private val supabaseClient: SupabaseClient,
    @Named("admin") private val adminClient: SupabaseClient,
    private val sessionManager: SessionManager
) : ViewModel() {

    // Expose storage for receipt generation (Using normal client as default)
    val supabaseStorage = supabaseClient.storage

    // --- DATA STREAMS ---
    val menuItems: StateFlow<List<MenuItem>> = repository.allActiveItems
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val appSettings: StateFlow<AppSetting?> = posDao.getSettings()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Use SessionManager for global user state
    val currentUser: StateFlow<User?> = sessionManager.currentUser

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    // --- CUSTOMER SEARCH ---
    private val _customerSearchQuery = MutableStateFlow("")
    val customerSuggestions: StateFlow<List<Customer>> = _customerSearchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.length < 2) flowOf(emptyList())
            else repository.searchCustomers(query)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateCustomerSearch(query: String) {
        _customerSearchQuery.value = query
    }

    // --- STAFF STATS STREAMS (High-Precision Updates) ---
    
    val myTotalTips: StateFlow<Double> = currentUser.flatMapLatest { user ->
        if (user != null) posDao.getTotalTipsForServer(user.id) else flowOf(0.0)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val myTipHistory: StateFlow<List<TipLog>> = currentUser.flatMapLatest { user ->
        if (user != null) posDao.getTipsForServer(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val myClosedTabsCount: StateFlow<Int> = currentUser.flatMapLatest { user ->
        if (user != null) {
            posDao.getClosedTabs().map { tabs -> 
                tabs.count { it.serverId == user.id } 
            }
        } else flowOf(0)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // --- USER ACTIONS ---
    fun toggleEditMode() { _isEditMode.value = !_isEditMode.value }
    
    fun logout() {
        viewModelScope.launch {
            try {
                supabaseClient.auth.signOut()
            } catch (e: Exception) {
                Log.e("PosViewModel", "Cloud sign out failed: ${e.message}")
            }
            sessionManager.logout()
            _currentTabId.value = -1L
        }
    }

    // --- MENU DATA ---
    val menuGroups: StateFlow<List<MenuGroup>> = posDao.getAllMenuGroups()
        .onEach { groups ->
            if (_selectedMenuGroup.value == null && groups.isNotEmpty()) {
                _selectedMenuGroup.value = groups.first()
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allMenuItems: StateFlow<List<MenuItem>> = posDao.getAllMenuItems()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allCategories: StateFlow<List<Category>> = posDao.getAllCategoriesRaw()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val shotWallItems: StateFlow<List<MenuItem>> = posDao.getShotWallItems()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedMenuGroup = MutableStateFlow<MenuGroup?>(null)
    val selectedMenuGroup: StateFlow<MenuGroup?> = _selectedMenuGroup.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    val currentGridItems: StateFlow<List<MenuItem>> = combine(
        _selectedCategory, 
        _selectedMenuGroup, 
        allMenuItems, 
        allCategories
    ) { cat, group, allItems, allCats ->
        when {
            cat != null -> allItems.filter { it.categoryId == cat.id }
            group != null -> {
                val catIds = allCats.filter { it.menuGroupId == group.id }.map { it.id }
                allItems.filter { it.categoryId in catIds }
            }
            else -> emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun selectMenuGroup(group: MenuGroup?) {
        _selectedMenuGroup.value = group
        _selectedCategory.value = null
    }
    fun selectCategory(category: Category?) { _selectedCategory.value = category }

    /**
     * Resolves the cloud storage URL for a specific product photo.
     */
    fun getItemImageUrl(itemName: String): String {
        val sanitized = itemName.lowercase().replace(" ", "_") + ".jpg"
        return repository.getMenuItemImageUrl(sanitized)
    }

    // --- TABS & PRICING ---
    val openTabs: StateFlow<List<ActiveTab>> = posDao.getOpenTabs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentTabId = MutableStateFlow(-1L)
    val currentTabId: StateFlow<Long> = _currentTabId.asStateFlow()

    val currentActiveTab: StateFlow<ActiveTab?> = combine(openTabs, _currentTabId) { tabs, id ->
        tabs.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val currentTabItems: StateFlow<Map<TabItem, MenuItem>> = _currentTabId.flatMapLatest { id ->
        if (id != -1L) posDao.getTabDetails(id) else flowOf(emptyMap())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val currentSubtotal: StateFlow<Double> = currentTabItems.map { it.keys.sumOf { item -> item.priceAtTimeOfSale } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val currentGrandTotal: StateFlow<Double> = combine(currentTabItems, appSettings) { items, settings ->
        val data = items.keys.map { it.priceAtTimeOfSale to it.quantity }
        val result = PricingEngine.calculateFullTransaction(data, settings)
        result.total
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // --- ACTIONS ---

    /**
     * Triggers a manual cloud synchronization pulse.
     * Uses SupabaseConstants to provide explicit feedback.
     */
    fun triggerCloudSync() {
        viewModelScope.launch {
            try {
                repository.triggerDailySummary()
                _errorMessage.emit("Sync: ${SupabaseConstants.FUNCTION_DAILY_SUMMARY} Complete")
            } catch (e: Exception) {
                _errorMessage.emit("Sync Error: ${e.localizedMessage}")
            }
        }
    }

    fun markTutorialSeen() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value ?: return@launch
            posDao.markTutorialSeen(user.id)
            val updatedUser = user.copy(hasSeenTutorial = true)
            sessionManager.setCurrentUser(updatedUser)
        }
    }

    fun switchTab(id: Long) { _currentTabId.value = id }
    fun renameTab(id: Long, name: String) { viewModelScope.launch { posDao.renameTab(id, name) } }
    
    fun createNewTab(customerName: String) {
        viewModelScope.launch {
            val serverId = currentUser.value?.id ?: 0
            val now: Instant = Clock.System.now()
            repository.saveCustomer(Customer(name = customerName, createdAt = now))
            repository.createTab(ActiveTab(customerName = customerName, isOpen = true, serverId = serverId, createdAt = now))
        }
    }

    fun closeTab() {
        viewModelScope.launch {
            val id = _currentTabId.value
            val activeTab = currentActiveTab.value
            val items = currentTabItems.value
            val total = currentGrandTotal.value
            val subtotal = currentSubtotal.value

            if (id != -1L && activeTab != null) {
                // Explicitly typed high-precision timestamp
                val now: Instant = Clock.System.now()
                
                val sale = Sale(
                    totalAmount = total, 
                    taxAmount = total - subtotal, 
                    paymentType = "TAB_SETTLED", 
                    customerName = activeTab.customerName, 
                    serverId = activeTab.serverId,
                    timestamp = now
                )
                val saleItems = items.map { (tabItem, menuItem) -> 
                    SaleItem(
                        saleId = 0, 
                        itemName = menuItem.name, 
                        pricePaid = tabItem.priceAtTimeOfSale, 
                        quantity = tabItem.quantity 
                    ) 
                }
                repository.completeSale(sale, saleItems)
                posDao.updateTabStatus(id, false)
                _currentTabId.value = -1L
            }
        }
    }

    fun addToTab(item: MenuItem, overridePrice: Double? = null, label: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val tabId = _currentTabId.value
            if (tabId == -1L) {
                _errorMessage.emit("Please Open a Tab First")
                return@launch
            }
            val price = overridePrice ?: PricingEngine.calculatePriceAtSale(item, appSettings.value)
            repository.addTabItem(TabItem(tabId = tabId, menuItemId = item.id, priceAtTimeOfSale = price, note = label ?: ""), item)
        }
    }

    fun logTip(amount: Double, note: String) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            repository.logTip(TipLog(userId = user.id, amount = amount, note = note))
        }
    }

    fun voidItem(tabItemId: Long) { viewModelScope.launch { posDao.deleteTabItem(tabItemId) } }
    fun updateItemNote(tabItemId: Long, note: String) { viewModelScope.launch { posDao.updateTabItemNote(tabItemId, note) } }

    fun updateLineItemPrice(tabItemId: Long, newPrice: Double) { 
        viewModelScope.launch { 
            posDao.updateTabItemPrice(tabItemId, newPrice)
            repository.updatePrice(-1, newPrice) 
        } 
    }
    fun restockItem(itemId: Int, count: Int) { viewModelScope.launch { repository.updateStock(itemId, count) } }
    fun deleteMenuItem(id: Int) { viewModelScope.launch { posDao.deleteMenuItem(id) } }
    fun saveMenuItem(item: MenuItem) { viewModelScope.launch(Dispatchers.IO) { repository.saveMenuItem(item) } }
    fun addMenuGroup(name: String) { viewModelScope.launch { repository.addMenuGroup(MenuGroup(name = name)) } }
    fun addCategory(groupId: Long, name: String, icon: String) { viewModelScope.launch { repository.addCategory(Category(menuGroupId = groupId.toInt(), name = name, iconName = icon)) } }
    fun updateSettings(settings: AppSetting) { viewModelScope.launch { repository.saveSettings(settings) } }
    fun updateItemSpecial(itemId: Int, price: Double?) {
        viewModelScope.launch {
            val settings = appSettings.value ?: return@launch
            val json = try { JSONObject(settings.specialsJson) } catch (_: Exception) { JSONObject("{}") }
            if (price == null) json.remove(itemId.toString()) else json.put(itemId.toString(), price)
            repository.saveSettings(settings.copy(specialsJson = json.toString()))
        }
    }
}
