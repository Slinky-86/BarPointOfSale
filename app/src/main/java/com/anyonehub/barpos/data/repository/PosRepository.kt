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
import kotlinx.coroutines.flow.Flow
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
    private val customerDao: CustomerDao,
    private val supabaseClient: SupabaseClient,
    @Named("admin") private val adminClient: SupabaseClient, // ADDED: Admin Master Engine
    @Named("daily-summary") private val dailySummaryFunc: String,
    @Named("inventory-alert") private val inventoryAlertFunc: String,
    @Named("server-checkout") private val serverCheckoutFunc: String,
    @Named("secure-audit") private val secureAuditFunc: String,
    @Named("staff_avatars") private val staffAvatarsBucket: String,
    @Named("receipt_backups") private val receiptBackupsBucket: String,
    @Named("menu_assets") private val menuAssetsBucket: String
) {
    private val tag = "PosRepository"

    // --- MENU & INVENTORY OPERATIONS (INTEGRATED ADMIN CLIENT FOR MASTER EDITS) ---
    val allActiveItems: Flow<List<MenuItem>> = menuDao.getAllActiveItems()

    fun getMenuItemImageUrl(imageName: String): String {
        return supabaseClient.storage.from(menuAssetsBucket).publicUrl(imageName)
    }

    suspend fun saveMenuItem(item: MenuItem) {
        var synced = false
        try {
            // USES ADMIN CLIENT: Bypasses RLS for menu management
            adminClient.postgrest[SupabaseConstants.TABLE_MENU_ITEMS].upsert(item)
            adminClient.functions.invoke(secureAuditFunc) {
                setBody(buildJsonObject {
                    put("action", "MENU_EDIT")
                    put("item_name", item.name)
                })
            }
            synced = true
        } catch (e: Exception) {
            Log.e(tag, "Admin Master Save Failed (MenuItem): ${e.message}")
            // Fallback to normal client (DO NOT REMOVE NORMAL CLIENT LOGIC)
            try {
                supabaseClient.postgrest[SupabaseConstants.TABLE_MENU_ITEMS].upsert(item)
                synced = true
            } catch (e2: Exception) {
                Log.e(tag, "Normal Client also failed: ${e2.message}")
            }
        }
        menuDao.upsertMenuItem(item.copy(isSynced = synced))
    }

    suspend fun updatePrice(itemId: Int, newPrice: Double) {
        menuDao.updateItemPrice(itemId, newPrice)
        try {
            // ADMIN CLIENT for master price audit
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
            // ADMIN CLIENT for unrestricted inventory updates
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
        var synced = false
        try {
            adminClient.postgrest[SupabaseConstants.TABLE_MENU_GROUPS].upsert(group)
            synced = true
        } catch (e: Exception) {
            Log.e(tag, "Admin Master Sync Failed (MenuGroup): ${e.message}")
        }
        posDao.insertMenuGroup(group.copy(isSynced = synced))
    }

    suspend fun addCategory(category: Category) {
        var synced = false
        try {
            adminClient.postgrest[SupabaseConstants.TABLE_CATEGORIES].upsert(category)
            synced = true
        } catch (e: Exception) {
            Log.e(tag, "Admin Master Sync Failed (Category): ${e.message}")
        }
        posDao.insertCategory(category.copy(isSynced = synced))
    }

    // --- SALES & RECEIPT BACKUPS (USES NORMAL CLIENT AS ALWAYS) ---

    suspend fun completeSale(sale: Sale, items: List<SaleItem>, receiptFile: File? = null) {
        var synced = false
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
            synced = true
        } catch (e: Exception) {
            Log.e(tag, "Staff Sync Failed (Sale): ${e.message}")
        }
        salesDao.finalizeSale(sale.copy(isSynced = synced), items.map { it.copy(isSynced = synced) })
    }

    // --- AVATAR MANAGEMENT (NORMAL CLIENT) ---

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

    // --- TABS (NORMAL CLIENT) ---

    suspend fun createTab(tab: ActiveTab): Long {
        var synced = false
        try {
            supabaseClient.postgrest[SupabaseConstants.TABLE_ACTIVE_TABS].insert(tab)
            synced = true
        } catch (e: Exception) {
            Log.e(tag, "Staff Sync Failed (Tab Create): ${e.message}")
        }
        return posDao.createTab(tab.copy(isSynced = synced))
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
            // ADMIN CLIENT used for inventory consistency across all roles
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

    // --- CUSTOMER & USER OPERATIONS ---

    fun searchCustomers(query: String): Flow<List<Customer>> = customerDao.searchCustomers(query)

    suspend fun saveCustomer(customer: Customer): Long {
        var synced = false
        try {
            supabaseClient.postgrest[SupabaseConstants.TABLE_CUSTOMERS].upsert(customer)
            synced = true
        } catch (e: Exception) {
            Log.e(tag, "Customer Sync Failed: ${e.message}")
        }
        return customerDao.insertCustomer(customer.copy(isSynced = synced))
    }

    /**
     * ONLINE-FIRST LOGIN: Verifies credentials in Supabase and syncs to Room immediately.
     */
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
        var synced = false
        try {
            // ADMIN CLIENT used for staff profile management
            adminClient.postgrest[SupabaseConstants.TABLE_USERS].upsert(user)
            synced = true
        } catch (e: Exception) {
            Log.e(tag, "Admin Master Sync Failed (User): ${e.message}")
        }
        posDao.insertUser(user.copy(isSynced = synced))
    }

    suspend fun logTip(tip: TipLog) {
        var synced = false
        try {
            supabaseClient.postgrest[SupabaseConstants.TABLE_TIP_LOGS].insert(tip)
            synced = true
        } catch (e: Exception) {
            Log.e(tag, "Tip Sync Failed: ${e.message}")
        }
        posDao.logTip(tip.copy(isSynced = synced))
    }

    suspend fun saveSettings(settings: AppSetting) {
        var synced = false
        try {
            adminClient.postgrest[SupabaseConstants.TABLE_APP_SETTINGS].upsert(settings)
            synced = true
        } catch (e: Exception) {
            Log.e(tag, "Admin Master Sync Failed (Settings): ${e.message}")
        }
        posDao.saveSettings(settings.copy(isSynced = synced))
    }

    // --- TIME CLOCK & AUTOMATION (NORMAL CLIENT AS ALWAYS) ---

    suspend fun logTimeClock(entry: TimeClockEntry) {
        var synced = false
        try {
            supabaseClient.postgrest[SupabaseConstants.TABLE_TIME_CLOCK].insert(entry)
            supabaseClient.functions.invoke(serverCheckoutFunc) {
                setBody(buildJsonObject {
                    put("user_id", entry.userId)
                    put("event_type", entry.eventType.name)
                })
            }
            synced = true
        } catch (e: Exception) {
            Log.e(tag, "Time Clock Sync Failed: ${e.message}")
        }
        timeClockDao.insertEntry(entry.copy(isSynced = synced))
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