// Copyright 2024 anyone-Hub
@file:OptIn(ExperimentalTime::class)

package com.anyonehub.barpos.data

import androidx.room.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime

/**
 * LEVEL 1: MENU GROUPS
 */
@Serializable
@Entity(tableName = "menu_groups")
data class MenuGroup(
    @PrimaryKey(autoGenerate = true) 
    @ColumnInfo(name = "id")
    @SerialName("id") val id: Int = 0,
    @ColumnInfo(name = "name")
    @SerialName("name") val name: String,
    @ColumnInfo(name = "sort_order")
    @SerialName("sort_order") val sortOrder: Int = 0,
    
    @ColumnInfo(name = "is_synced")
    @Transient
    val isSynced: Boolean = false
)

/**
 * LEVEL 2: CATEGORIES
 */
@Serializable
@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = MenuGroup::class,
            parentColumns = ["id"],
            childColumns = ["menu_group_id"],
            onDelete = ForeignKey.CASCADE 
        )
    ],
    indices = [Index("menu_group_id")]
)
data class Category(
    @PrimaryKey(autoGenerate = true) 
    @ColumnInfo(name = "id")
    @SerialName("id") val id: Int = 0,
    @ColumnInfo(name = "menu_group_id")
    @SerialName("menu_group_id") val menuGroupId: Int,
    @ColumnInfo(name = "name")
    @SerialName("name") val name: String,
    @ColumnInfo(name = "icon_name")
    @SerialName("icon_name") val iconName: String = "default",
    @ColumnInfo(name = "display_order")
    @SerialName("display_order") val displayOrder: Int = 0,
    
    @ColumnInfo(name = "is_synced")
    @Transient
    val isSynced: Boolean = false
)

/**
 * LEVEL 3: MENU ITEMS
 */
@Serializable
@Entity(
    tableName = "menu_items",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("category_id")]
)
data class MenuItem(
    @PrimaryKey(autoGenerate = true) 
    @ColumnInfo(name = "id")
    @SerialName("id") val id: Int = 0,
    @ColumnInfo(name = "category_id")
    @SerialName("category_id") val categoryId: Int,
    @ColumnInfo(name = "name")
    @SerialName("name") val name: String,
    @ColumnInfo(name = "price")
    @SerialName("price") val price: Double, 
    @ColumnInfo(name = "description")
    @SerialName("description") val description: String = "",

    @ColumnInfo(name = "icon_name")
    @SerialName("icon_name")
    val iconName: String = "beer",

    @ColumnInfo(name = "is_shot_wall_item")
    @SerialName("is_shot_wall_item")
    val isShotWallItem: Boolean = false,

    @ColumnInfo(name = "is_active")
    @SerialName("is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "inventory_count")
    @SerialName("inventory_count")
    val inventoryCount: Int = 999,

    @ColumnInfo(name = "hh_price")
    @SerialName("hh_price")
    val hhPrice: Double? = null,
    
    @ColumnInfo(name = "bucket_price")
    @SerialName("bucket_price")
    val bucketPrice: Double? = null,
    
    @ColumnInfo(name = "hh_bucket_price")
    @SerialName("hh_bucket_price")
    val hhBucketPrice: Double? = null,
    
    @ColumnInfo(name = "is_synced")
    @Transient
    val isSynced: Boolean = false
)

/**
 * TABS & SALES RECORDS
 */
@Serializable
@Entity(tableName = "active_tabs")
data class ActiveTab(
    @PrimaryKey(autoGenerate = true) 
    @ColumnInfo(name = "id")
    @SerialName("id") val id: Long = 0,
    @ColumnInfo(name = "customer_name")
    @SerialName("customer_name") val customerName: String,
    @ColumnInfo(name = "created_at")
    @SerialName("created_at") val createdAt: Instant = Clock.System.now(),
    @ColumnInfo(name = "is_open")
    @SerialName("is_open") val isOpen: Boolean = true,
    @ColumnInfo(name = "server_id")
    @SerialName("server_id") val serverId: Int = 0,
    
    @ColumnInfo(name = "is_synced")
    @Transient
    val isSynced: Boolean = false
)

@Serializable
@Entity(
    tableName = "tab_items",
    foreignKeys = [
        ForeignKey(
            entity = ActiveTab::class,
            parentColumns = ["id"],
            childColumns = ["tab_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MenuItem::class,
            parentColumns = ["id"],
            childColumns = ["menu_item_id"],
            onDelete = ForeignKey.NO_ACTION 
        )
    ],
    indices = [Index("tab_id"), Index("menu_item_id")]
)
data class TabItem(
    @PrimaryKey(autoGenerate = true) 
    @ColumnInfo(name = "id")
    @SerialName("id") val id: Long = 0,
    @ColumnInfo(name = "tab_id")
    @SerialName("tab_id") val tabId: Long,
    @ColumnInfo(name = "menu_item_id")
    @SerialName("menu_item_id") val menuItemId: Int,
    @ColumnInfo(name = "price_at_time_of_sale")
    @SerialName("price_at_time_of_sale") val priceAtTimeOfSale: Double,
    @ColumnInfo(name = "quantity")
    @SerialName("quantity") val quantity: Int = 1,
    @ColumnInfo(name = "note")
    @SerialName("note") val note: String = "",
    @ColumnInfo(name = "report_id")
    @SerialName("report_id") val reportId: Long? = null,
    
    @ColumnInfo(name = "is_synced")
    @Transient
    val isSynced: Boolean = false
)

@Serializable
@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey 
    @ColumnInfo(name = "id")
    @SerialName("id") val id: Int = 1,
    @ColumnInfo(name = "tax_rate")
    @SerialName("tax_rate") val taxRate: Double = 0.08,
    @ColumnInfo(name = "bar_name")
    @SerialName("bar_name") val barName: String = "MidTown POS",
    @ColumnInfo(name = "is_dark_mode")
    @SerialName("is_dark_mode") val isDarkMode: Boolean = true,
    @ColumnInfo(name = "happy_hour_start")
    @SerialName("happy_hour_start") val happyHourStart: Int = 16,
    @ColumnInfo(name = "happy_hour_end")
    @SerialName("happy_hour_end") val happyHourEnd: Int = 18,
    @ColumnInfo(name = "happy_hour_discount")
    @SerialName("happy_hour_discount") val happyHourDiscount: Double = 0.50,
    @ColumnInfo(name = "specials_json")
    @SerialName("specials_json") val specialsJson: String = "{}",
    
    @ColumnInfo(name = "is_synced")
    @Transient
    val isSynced: Boolean = false
)

@Serializable
@Entity(tableName = "sales_records")
data class Sale(
    @PrimaryKey(autoGenerate = true) 
    @ColumnInfo(name = "id")
    @SerialName("id") val id: Long = 0,
    @ColumnInfo(name = "timestamp")
    @SerialName("timestamp") val timestamp: Instant = Clock.System.now(),
    @ColumnInfo(name = "total_amount")
    @SerialName("total_amount") val totalAmount: Double,
    @ColumnInfo(name = "tax_amount")
    @SerialName("tax_amount") val taxAmount: Double,
    @ColumnInfo(name = "payment_type")
    @SerialName("payment_type") val paymentType: String,
    @ColumnInfo(name = "customer_name")
    @SerialName("customer_name") val customerName: String? = null,
    @ColumnInfo(name = "server_id")
    @SerialName("server_id") val serverId: Int = 0,
    
    @ColumnInfo(name = "is_synced")
    @Transient
    val isSynced: Boolean = false
)

@Serializable
@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = Sale::class,
            parentColumns = ["id"],
            childColumns = ["sale_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sale_id")]
)
data class SaleItem(
    @PrimaryKey(autoGenerate = true) 
    @ColumnInfo(name = "id")
    @SerialName("id") val id: Long = 0,
    @ColumnInfo(name = "sale_id")
    @SerialName("sale_id") val saleId: Long,
    @ColumnInfo(name = "item_name")
    @SerialName("item_name") val itemName: String,
    @ColumnInfo(name = "price_paid")
    @SerialName("price_paid") val pricePaid: Double,
    @ColumnInfo(name = "quantity")
    @SerialName("quantity") val quantity: Int = 1,
    
    @ColumnInfo(name = "is_synced")
    @Transient
    val isSynced: Boolean = false
)
