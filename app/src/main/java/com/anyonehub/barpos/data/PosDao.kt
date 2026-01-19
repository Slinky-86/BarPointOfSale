/* Copyright 2024 anyone-Hub */
package com.anyonehub.barpos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PosDao {

    // =================================================================================
    // SECTION 1: USER AUTHENTICATION & TIPS
    // =================================================================================

    @Query("SELECT * FROM users WHERE name = :name AND pin_code = :pin AND is_active = 1 LIMIT 1")
    suspend fun loginUser(name: String, pin: String): User?

    @Query("SELECT * FROM users WHERE pin_code = :pin LIMIT 1")
    suspend fun getUserByPin(pin: String): User?

    @Query("SELECT * FROM users WHERE supabase_id = :supabaseId LIMIT 1")
    suspend fun getUserBySupabaseId(supabaseId: String): User?

    @Query("SELECT * FROM users WHERE is_active = 1 ORDER BY name ASC")
    fun getAllUsers(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Insert
    suspend fun logTip(tip: TipLog)

    @Query("SELECT * FROM tip_logs WHERE user_id = :userId ORDER BY timestamp DESC")
    fun getTipsForServer(userId: Int): Flow<List<TipLog>>

    @Query("SELECT IFNULL(SUM(amount), 0.0) FROM tip_logs WHERE user_id = :userId")
    fun getTotalTipsForServer(userId: Int): Flow<Double>

    @Query("SELECT IFNULL(SUM(amount), 0.0) FROM tip_logs")
    fun getAllTipsTotal(): Flow<Double>

    @Query("UPDATE users SET has_seen_tutorial = 1 WHERE id_local = :userId")
    suspend fun markTutorialSeen(userId: Int)

    // =================================================================================
    // SECTION 2: MENU DATA & AUDIT (User Configurable)
    // =================================================================================

    @Query("SELECT * FROM menu_groups ORDER BY sort_order ASC")
    fun getAllMenuGroups(): Flow<List<MenuGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuGroup(group: MenuGroup)

    @Query("DELETE FROM menu_groups WHERE id = :groupId")
    suspend fun deleteMenuGroup(groupId: Int)

    @Query("SELECT * FROM categories WHERE menu_group_id = :groupId ORDER BY display_order ASC")
    fun getCategoriesByGroup(groupId: Int): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategoriesRaw(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: Int)

    @Query("SELECT * FROM menu_items WHERE is_active = 1 ORDER BY category_id ASC, name ASC")
    fun getAllMenuItems(): Flow<List<MenuItem>>

    @Query("SELECT * FROM menu_items WHERE category_id = :categoryId AND is_active = 1 ORDER BY name ASC")
    fun getItemsByCategory(categoryId: Int): Flow<List<MenuItem>>

    @Query("SELECT * FROM menu_items WHERE is_shot_wall_item = 1 AND is_active = 1 ORDER BY name ASC")
    fun getShotWallItems(): Flow<List<MenuItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItem(item: MenuItem)

    @Query("""
        UPDATE menu_items 
        SET name = :name, price = :price, is_shot_wall_item = :isShot, 
            inventory_count = :stock, description = :description,
            hh_price = :hhPrice, bucket_price = :bucketPrice, hh_bucket_price = :hhBucketPrice
        WHERE id = :id
    """)
    suspend fun updateMenuItem(
        id: Int,
        name: String,
        price: Double,
        isShot: Boolean,
        stock: Int,
        description: String,
        hhPrice: Double?,
        bucketPrice: Double?,
        hhBucketPrice: Double?
    )

    @Query("UPDATE menu_items SET inventory_count = :count WHERE id = :itemId")
    suspend fun restockItem(itemId: Int, count: Int)

    @Query("UPDATE menu_items SET is_active = 0 WHERE id = :itemId")
    suspend fun deleteMenuItem(itemId: Int)

    // =================================================================================
    // SECTION 3: INVENTORY & ATOMIC TRANSACTIONS
    // =================================================================================

    @Transaction
    suspend fun addItemToTabAndReduceStock(tabItem: TabItem, menuItemId: Int) {
        insertTabItem(tabItem)
        decrementMenuItemStock(menuItemId)
    }

    @Query("UPDATE menu_items SET inventory_count = inventory_count - 1 WHERE id = :itemId AND inventory_count > 0")
    suspend fun decrementMenuItemStock(itemId: Int)

    // =================================================================================
    // SECTION 4: TABS & ORDERS
    // =================================================================================

    @Query("SELECT * FROM active_tabs WHERE is_open = 1 ORDER BY created_at DESC")
    fun getOpenTabs(): Flow<List<ActiveTab>>

    @Query("SELECT * FROM active_tabs WHERE is_open = 1 AND server_id = :serverId ORDER BY created_at DESC")
    fun getOpenTabsForServer(serverId: Int): Flow<List<ActiveTab>>

    @Query("SELECT * FROM active_tabs WHERE is_open = 0 ORDER BY created_at DESC")
    fun getClosedTabs(): Flow<List<ActiveTab>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createTab(tab: ActiveTab): Long

    @Query("UPDATE active_tabs SET is_open = :isOpen WHERE id = :tabId")
    suspend fun updateTabStatus(tabId: Long, isOpen: Boolean)

    @Query("UPDATE active_tabs SET customer_name = :newName WHERE id = :tabId")
    suspend fun renameTab(tabId: Long, newName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTabItem(tabItem: TabItem)

    @Query("DELETE FROM tab_items WHERE id = :tabItemId")
    suspend fun deleteTabItem(tabItemId: Long)

    @Query("UPDATE tab_items SET note = :note WHERE id = :tabItemId")
    suspend fun updateTabItemNote(tabItemId: Long, note: String)

    @Query("UPDATE tab_items SET price_at_time_of_sale = :price WHERE id = :tabItemId")
    suspend fun updateTabItemPrice(tabItemId: Long, price: Double)

    // =================================================================================
    // SECTION 5: JOINS & SETTINGS
    // =================================================================================

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT tab_items.*, menu_items.* FROM tab_items 
        JOIN menu_items ON tab_items.menu_item_id = menu_items.id 
        WHERE tab_items.tab_id = :tabId
    """)
    fun getTabDetails(tabId: Long): Flow<Map<TabItem, MenuItem>>

    @Query("SELECT IFNULL(SUM(price_at_time_of_sale), 0.0) FROM tab_items WHERE tab_id = :tabId")
    fun getTabTotal(tabId: Long): Flow<Double>

    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT IFNULL(SUM(price_at_time_of_sale), 0.0) 
        FROM tab_items 
        JOIN active_tabs ON tab_items.tab_id = active_tabs.id 
        WHERE active_tabs.is_open = 0
    """)
    fun getTotalRevenue(): Flow<Double>

    @Query("SELECT * FROM app_settings LIMIT 1")
    fun getSettings(): Flow<AppSetting?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSetting)

    @Query("DELETE FROM active_tabs WHERE is_open = 0")
    suspend fun clearSalesHistory()
}
