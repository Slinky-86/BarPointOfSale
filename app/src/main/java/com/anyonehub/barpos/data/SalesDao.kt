/* Copyright 2024 anyone-Hub */
@file:Suppress("unused")

package com.anyonehub.barpos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.anyonehub.barpos.data.models.SaleWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItem>)

    /**
     * Executes the sale and item insertion as a single atomic unit.
     */
    @Transaction
    suspend fun finalizeSale(sale: Sale, items: List<SaleItem>) {
        val saleId = insertSale(sale)
        val itemsWithId = items.map { it.copy(saleId = saleId) }
        insertSaleItems(itemsWithId)
    }

    @Transaction
    @Query("SELECT * FROM sales_records ORDER BY timestamp DESC")
    fun getAllSalesWithItems(): Flow<List<SaleWithItems>>

    @Query("SELECT SUM(total_amount) FROM sales_records WHERE timestamp >= :startOfDay")
    fun getDailyTotal(startOfDay: Long): Flow<Double>

    @Query("SELECT * FROM sales_records WHERE is_synced = 0")
    fun getUnsyncedSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sale_items WHERE sale_id = :saleId")
    fun getSaleItemsBySaleId(saleId: Long): Flow<List<SaleItem>>

    @Query("UPDATE sales_records SET is_synced = :status WHERE id = :saleId")
    suspend fun updateSaleSyncStatus(saleId: Long, status: Boolean)
    
    @Query("UPDATE sale_items SET is_synced = :status WHERE sale_id = :saleId")
    suspend fun updateSaleItemsSyncStatus(saleId: Long, status: Boolean)
}
