/* Copyright 2024 anyone-Hub */
package com.anyonehub.barpos.data.repository

import android.util.Log
import com.anyonehub.barpos.data.*
import com.anyonehub.barpos.di.SupabaseConstants
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PosRepository @Inject constructor(
    private val menuDao: MenuDao,
    private val salesDao: SalesDao,
    private val posDao: PosDao,
    private val timeClockDao: TimeClockDao,
    private val supabaseClient: SupabaseClient,
    @Named("admin") private val adminClient: SupabaseClient,
    @Named("daily-summary") private val dailySummaryFunc: String,
    @Named("inventory-alert") private val inventoryAlertFunc: String,
    @Named("server-checkout") private val serverCheckoutFunc: String,
    @Named("secure-audit") private val secureAuditFunc: String,
    @Named("staff_avatars") private val staffAvatarsBucket: String,
    @Named("receipt_backups") private val receiptBackupsBucket: String,
    @Named("menu_assets") private val menuAssetsBucket: String
) {
    private val tag = "PosRepository"
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        // --- PRODUCTION SEEDER: Automatic Menu Generation ---
        repositoryScope.launch {
            seedNewProductionMenus()
        }
    }

    private suspend fun seedNewProductionMenus() {
        try {
            val currentGroups = posDao.getAllMenuGroups().first()
            
            // 1. Ensure Appetizers
            if (currentGroups.none { it.name.equals("Appetizers", ignoreCase = true) }) {
                posDao.insertMenuGroup(MenuGroup(name = "Appetizers", sortOrder = 1, isSynced = true))
            }

            // 2. Ensure Specialty Drinks + Sub-Menus (Neat/Rocks, Fruits, etc.)
            if (currentGroups.none { it.name.equals("Specialty Drinks", ignoreCase = true) }) {
                val groupId = posDao.insertMenuGroup(MenuGroup(name = "Specialty Drinks", sortOrder = 2, isSynced = true))
                posDao.insertCategory(Category(menuGroupId = groupId.toInt(), name = "Liquor", iconName = "shot", isSynced = true))
                posDao.insertCategory(Category(menuGroupId = groupId.toInt(), name = "Soda", iconName = "soda", isSynced = true))
                posDao.insertCategory(Category(menuGroupId = groupId.toInt(), name = "Juice", iconName = "soda", isSynced = true))
                posDao.insertCategory(Category(menuGroupId = groupId.toInt(), name = "Neat/Rocks", iconName = "rotate", isSynced = true))
                posDao.insertCategory(Category(menuGroupId = groupId.toInt(), name = "Fruits", iconName = "star", isSynced = true))
            }

            // 3. Ensure Mixed Drinks + Sub-Menus
            if (currentGroups.none { it.name.equals("Mixed Drinks", ignoreCase = true) }) {
                val groupId = posDao.insertMenuGroup(MenuGroup(name = "Mixed Drinks", sortOrder = 3, isSynced = true))
                posDao.insertCategory(Category(menuGroupId = groupId.toInt(), name = "Drink Mix", iconName = "cocktail", isSynced = true))
                posDao.insertCategory(Category(menuGroupId = groupId.toInt(), name = "Juice", iconName = "soda", isSynced = true))
                posDao.insertCategory(Category(menuGroupId = groupId.toInt(), name = "Garnish", iconName = "star", isSynced = true))
            }
            Log.i(tag, "Production Menus Seeded Successfully.")
        } catch (e: Exception) {
            Log.e(tag, "Menu Seeding Failed: ${e.message}")
        }
    }

    // --- MENU & INVENTORY OPERATIONS ---
    val allActiveItems: Flow<List<MenuItem>> = menuDao.getAllActiveItems()

    fun getMenuItemImageUrl(imageName: String): String {
        return supabaseClient.storage.from(menuAssetsBucket).publicUrl(imageName)
    }

    suspend fun saveMenuItem(item: MenuItem) {
        try {
            adminClient.postgrest[SupabaseConstants.TABLE_MENU_ITEMS].upsert(item)
            adminClient.functions.invoke(secureAuditFunc) {
                setBody(buildJsonObject {
                    put("action", "MENU_EDIT")
                    put("item_name", item.name)
                })
            }
            menuDao.upsertMenuItem(item.copy(isSynced = true))
        } catch (e: Exception) {
            Log.e(tag, "Admin Master Save Failed (MenuItem): ${e.message}")
            try {
                supabaseClient.postgrest[SupabaseConstants.TABLE_MENU_ITEMS].upsert(item)
                menuDao.upsertMenuItem(item.copy(isSynced = true))
            } catch (e2: Exception) {
                Log.e(tag, "Normal Client also failed: ${e2.message}")
                menuDao.upsertMenuItem(item.copy(isSynced = false))
            }
        }
    }

    suspend fun updatePrice(itemId: Int, newPrice: Double) {
        menuDao.updateItemPrice(itemId, newPrice)
        try {
            adminClient.functions.invoke(secureAuditFunc) {
                setBody(buildJsonObject {
                    put("action", "PRICE_OVERRIDE")
                    put("new_price", newPrice)
                })
            }
        } catch (e: Exception) {
            Log.e(tag, "Admin Master Sync Failed (Price): ${e.message}")
        }
    }

    suspend fun updateStock(itemId: Int, newCount: Int) {
        posDao.restockItem(itemId, newCount)
        try {
            adminClient.postgrest[SupabaseConstants.TABLE_MENU_ITEMS].update({ MenuItem::inventoryCount setTo newCount }) {
                filter { MenuItem::id eq itemId }
            }
            if (newCount < 10) {
                adminClient.functions.invoke(inventoryAlertFunc) {
                    setBody(buildJsonObject {
                        put("item_id", itemId)
                        put("new_count", newCount)
                        put("status", "LOW_STOCK")
                    })
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Admin Master Sync Failed (Inventory): ${e.message}")
        }
    }

    suspend fun addMenuGroup(group: MenuGroup) {
        try {
            adminClient.postgrest[SupabaseConstants.TABLE_MENU_GROUPS].upsert(group)
            posDao.insertMenuGroup(group.copy(isSynced = true))
        } catch (e: Exception) {
            Log.e(tag, "Admin Master Sync Failed (MenuGroup): ${e.message}")
            posDao.insertMenuGroup(group.copy(isSynced = false))
        }
    }

    suspend fun addCategory(category: Category) {
        try {
            adminClient.postgrest[SupabaseConstants.TABLE_CATEGORIES].upsert(category)
            posDao.insertCategory(category.copy(isSynced = true))
        } catch (e: Exception) {
            Log.e(tag, "Admin Master Sync Failed (Category): ${e.message}")
            posDao.insertCategory(category.copy(isSynced = false))
        }
    }

    // --- SALES & RECEIPT BACKUPS ---

    suspend fun completeSale(sale: Sale, items: List<SaleItem>, receiptFile: File? = null) {
        try {
            val cloudSale = supabaseClient.postgrest[SupabaseConstants.TABLE_SALES_RECORDS].insert(sale) { select() }.decodeSingle<Sale>()
            val linkedItems = items.map { it.copy(saleId = cloudSale.id) }
            supabaseClient.postgrest[SupabaseConstants.TABLE_SALE_ITEMS].insert(linkedItems)

            receiptFile?.let { file ->
                val path = "receipts/${cloudSale.id}.pdf"
                supabaseClient.storage.from(receiptBackupsBucket).upload(path, file.readBytes()) {
                    upsert = true
                }
            }
            salesDao.finalizeSale(sale.copy(isSynced = true), items.map { it.copy(isSynced = true) })
        } catch (e: Exception) {
            Log.e(tag, "Staff Sync Failed (Sale): ${e.message}")
            salesDao.finalizeSale(sale.copy(isSynced = false), items.map { it.copy(isSynced = false) })
        }
    }

    // --- AVATAR MANAGEMENT ---

    suspend fun uploadStaffAvatar(userId: String, photoFile: File): String? {
        return try {
            val path = "avatars/$userId.jpg"
            supabaseClient.storage.from(staffAvatarsBucket).upload(path, photoFile.readBytes()) {
                upsert = true
            }
            supabaseClient.storage.from(staffAvatarsBucket).publicUrl(path)
        } catch (e: Exception) {
            Log.e(tag, "Avatar Upload Failed: ${e.message}")
            null
        }
    }

    // --- TABS ---

    suspend fun createTab(tab: ActiveTab): Long {
        return try {
            supabaseClient.postgrest[SupabaseConstants.TABLE_ACTIVE_TABS].insert(tab)
            posDao.createTab(tab.copy(isSynced = true))
        } catch (e: Exception) {
            Log.e(tag, "Staff Sync Failed (Tab Create): ${e.message}")
            posDao.createTab(tab.copy(isSynced = false))
        }
    }

    suspend fun addTabItem(tabItem: TabItem, menuItem: MenuItem) {
        if (menuItem.isShotWallItem) {
            posDao.addItemToTabAndReduceStock(tabItem, menuItem.id)
            syncInventory(menuItem.id, menuItem.inventoryCount - 1)
        } else {
            posDao.insertTabItem(tabItem)
        }

        try {
            supabaseClient.postgrest[SupabaseConstants.TABLE_TAB_ITEMS].insert(tabItem)
        } catch (e: Exception) {
            Log.e(tag, "Staff Sync Failed (TabItem): ${e.message}")
        }
    }

    private suspend fun syncInventory(itemId: Int, count: Int) {
        try {
            adminClient.postgrest[SupabaseConstants.TABLE_MENU_ITEMS].update({ MenuItem::inventoryCount setTo count }) {
                filter { MenuItem::id eq itemId }
            }
            if (count < 10) {
                adminClient.functions.invoke(inventoryAlertFunc) {
                    setBody(buildJsonObject {
                        put("item_id", itemId)
                        put("new_count", count)
                        put("status", "LOW_STOCK_AUTO")
                    })
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Admin Master Inventory Sync Failed: ${e.message}")
        }
    }

    fun searchCustomers(query: String): Flow<List<Customer>> = posDao.searchCustomers(query)

    suspend fun saveCustomer(customer: Customer): Long {
        try {
            supabaseClient.postgrest[SupabaseConstants.TABLE_CUSTOMERS].upsert(customer)
        } catch (e: Exception) {
            Log.e(tag, "Customer Sync Failed: ${e.message}")
        }
        return 0L
    }

    suspend fun verifyAndSyncUser(name: String, pin: String): User? {
        return try {
            val cloudUser = supabaseClient.postgrest[SupabaseConstants.TABLE_USERS]
                .select {
                    filter {
                        User::name eq name
                        User::pinCode eq pin
                        User::isActive eq true
                    }
                }.decodeSingleOrNull<User>()

            cloudUser?.let {
                posDao.insertUser(it.copy(isSynced = true))
                it
            }
        } catch (e: Exception) {
            Log.e(tag, "Online User Verification Failed: ${e.message}")
            null
        }
    }

    suspend fun saveUser(user: User) {
        try {
            adminClient.postgrest[SupabaseConstants.TABLE_USERS].upsert(user)
            posDao.insertUser(user.copy(isSynced = true))
        } catch (e: Exception) {
            Log.e(tag, "Admin Master Sync Failed (User): ${e.message}")
            posDao.insertUser(user.copy(isSynced = false))
        }
    }

    suspend fun logTip(tip: TipLog) {
        try {
            supabaseClient.postgrest[SupabaseConstants.TABLE_TIP_LOGS].insert(tip)
            posDao.logTip(tip.copy(isSynced = true))
        } catch (e: Exception) {
            Log.e(tag, "Tip Sync Failed: ${e.message}")
            posDao.logTip(tip.copy(isSynced = false))
        }
    }

    suspend fun saveSettings(settings: AppSetting) {
        try {
            adminClient.postgrest[SupabaseConstants.TABLE_APP_SETTINGS].upsert(settings)
            posDao.saveSettings(settings.copy(isSynced = true))
        } catch (e: Exception) {
            Log.e(tag, "Admin Master Sync Failed (Settings): ${e.message}")
            posDao.saveSettings(settings.copy(isSynced = false))
        }
    }

    suspend fun logTimeClock(entry: TimeClockEntry) {
        try {
            supabaseClient.postgrest[SupabaseConstants.TABLE_TIME_CLOCK].insert(entry)
            supabaseClient.functions.invoke(serverCheckoutFunc) {
                setBody(buildJsonObject {
                    put("user_id", entry.userId)
                    put("event_type", entry.eventType.name)
                })
            }
            timeClockDao.insertEntry(entry.copy(isSynced = true))
        } catch (e: Exception) {
            Log.e(tag, "Time Clock Sync Failed: ${e.message}")
            timeClockDao.insertEntry(entry.copy(isSynced = false))
        }
    }

    suspend fun triggerDailySummary() {
        try {
            adminClient.functions.invoke(dailySummaryFunc)
        } catch (e: Exception) {
            Log.e(tag, "Admin Master Daily Summary Pulse Failed: ${e.message}")
        }
    }

    fun getLastClockEvent(userId: Int): Flow<TimeClockEntry?> = timeClockDao.getLastEntryForUser(userId)
}
