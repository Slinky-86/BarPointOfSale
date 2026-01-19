/* Copyright 2024 anyone-Hub */
@file:Suppress("unused")

package com.anyonehub.barpos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {

    @Query("SELECT * FROM menu_items WHERE is_active = 1 ORDER BY name ASC")
    fun getAllActiveItems(): Flow<List<MenuItem>>

    @Query("SELECT * FROM menu_items WHERE category_id = :catId")
    suspend fun getItemsByCategory(catId: Int): List<MenuItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMenuItem(item: MenuItem)

    @Update
    suspend fun updateMenuItem(item: MenuItem)

    @Delete
    suspend fun deleteMenuItem(item: MenuItem)

    // Quick price update for the "User Editable" requirement
    @Query("UPDATE menu_items SET price = :newPrice WHERE id = :itemId")
    suspend fun updateItemPrice(itemId: Int, newPrice: Double)
}