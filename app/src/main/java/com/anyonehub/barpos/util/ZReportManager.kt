// Copyright 2024 anyone-Hub
package com.anyonehub.barpos.util

import android.util.Log
import com.anyonehub.barpos.data.*
import com.anyonehub.barpos.data.repository.PosRepository
import com.chaquo.python.Python
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZReportManager @Inject constructor(
    private val reportDao: ReportDao,
    private val repository: PosRepository // Refactored to use centralized Repository
) {
    private val tag = "ZReportManager"

    /**
     * Executes a hard-close of the current sales session.
     * Integrates with Python finance engine and triggers centralized cloud analytics.
     */
    suspend fun runFinalZReport(managerId: Int): ZReportResult {
        return try {
            val unreportedMap = reportDao.getUnreportedClosedItems().first()

            if (unreportedMap.isEmpty()) {
                return ZReportResult.Error("Drawer is already empty.")
            }

            val salesArray = JSONArray()
            val itemIdsToLock = mutableListOf<Long>()

            unreportedMap.forEach { (tabItem: TabItem, menuItem: MenuItem) ->
                salesArray.put(JSONObject().apply {
                    put("name", menuItem.name)
                    put("price", tabItem.priceAtTimeOfSale)
                })
                itemIdsToLock.add(tabItem.id)
            }

            // 1. Process via Python Finance Engine
            val py = Python.getInstance()
            val module = py.getModule("finance_engine")
            val pythonJson = module.callAttr("generate_z_report", salesArray.toString()).toString()
            val response = JSONObject(pythonJson)

            if (response.optString("status") == "error") {
                return ZReportResult.Error(response.optString("message"))
            }

            val totalRevenue = response.optString("total_raw", "0.00").toDouble()

            // 2. ATOMIC LOCAL TRANSACTION
            val reportId = reportDao.insertZReport(ZReportEntity(
                totalRevenue = totalRevenue,
                managerId = managerId,
                reportDataJson = pythonJson
            ))

            reportDao.lockItemsToReport(reportId, itemIdsToLock)

            // 3. TRIGGER CLOUD ANALYTICS (Auth-Checked via Repository)
            repository.triggerDailySummary()

            ZReportResult.Success(
                reportId = reportId,
                totalFormatted = response.optString("total_formatted", "$0.00"),
                itemCounts = parseItemCounts(response.optJSONObject("item_counts"))
            )

        } catch (e: Exception) {
            Log.e(tag, "Z-Report Transaction Failed", e)
            ZReportResult.Error("Financial Error: ${e.message}")
        }
    }

    private fun parseItemCounts(json: JSONObject?): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        json?.let {
            val keys = it.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = it.getInt(key)
            }
        }
        return map
    }
}
