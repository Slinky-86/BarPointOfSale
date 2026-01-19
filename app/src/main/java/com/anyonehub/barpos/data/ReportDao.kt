/* Copyright 2024 anyone-Hub */
package com.anyonehub.barpos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {

    /**
     * USAGE: Fetches items from closed tabs that haven't been audited yet.
     * FIX: Uses @RewriteQueriesToDropUnusedColumns to strip 'customer_name', 'server_id', etc.
     */
    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT tab_items.*, menu_items.* FROM tab_items 
        INNER JOIN active_tabs ON tab_items.tab_id = active_tabs.id 
        INNER JOIN menu_items ON tab_items.menu_item_id = menu_items.id 
        WHERE active_tabs.is_open = 0 AND tab_items.report_id IS NULL
    """)
    fun getUnreportedClosedItems(): Flow<Map<TabItem, MenuItem>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertZReport(report: ZReportEntity): Long

    @Query("UPDATE tab_items SET report_id = :reportId WHERE id IN (:itemIds)")
    suspend fun lockItemsToReport(reportId: Long, itemIds: List<Long>)

    @Query("SELECT * FROM z_reports ORDER BY timestamp DESC")
    fun getAllHistoricalReports(): Flow<List<ZReportEntity>>

    /**
     * USAGE: Used for the Audit screen to show specific tab breakdowns.
     */
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT tab_items.*, menu_items.* FROM tab_items 
        JOIN menu_items ON tab_items.menu_item_id = menu_items.id 
        WHERE tab_items.tab_id = :tabId
    """)
    fun getTabDetails(tabId: Long): Flow<Map<TabItem, MenuItem>>

    @Query("SELECT * FROM z_reports WHERE id = :reportId LIMIT 1")
    suspend fun getReportById(reportId: Long): ZReportEntity?
}