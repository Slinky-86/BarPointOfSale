// Copyright 2024 anyone-Hub
package com.anyonehub.barpos.util

import android.util.Log
import com.anyonehub.barpos.data.AppSetting
import com.anyonehub.barpos.data.MenuItem
import com.chaquo.python.Python
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

object PricingEngine {
    private const val TAG = "PricingEngine"

    data class TransactionResult(
        val subtotal: Double,
        val tax: Double,
        val total: Double,
        val formattedTotal: String
    )

    /**
     * Bridges to the Python finance_engine to calculate the correct price
     * based on Specials, Happy Hour, and Inventory status.
     * Returns -1.0 if the item is out of stock.
     */
    fun calculatePriceAtSale(item: MenuItem, settings: AppSetting?): Double {
        return try {
            val py = Python.getInstance()
            val module = py.getModule("finance_engine")

            // 1. Prepare Item Data
            val itemJson = JSONObject().apply {
                put("id", item.id)
                put("price", item.price)
                put("inventory_count", item.inventoryCount)
                put("is_shot_wall", item.isShotWallItem)
            }

            // 2. Prepare Config
            val configJson = JSONObject().apply {
                put("taxRate", settings?.taxRate ?: 0.08)
                put("happyHourStart", settings?.happyHourStart ?: 16)
                put("happyHourEnd", settings?.happyHourEnd ?: 18)
                put("happyHourDiscount", settings?.happyHourDiscount ?: 0.50)
                put("specials", JSONObject(settings?.specialsJson ?: "{}"))
            }

            // 3. Call Python calculate_sale
            val result = module.callAttr("calculate_sale", itemJson.toString(), configJson.toString())
            val response = JSONObject(result.toString())

            // 4. Handle Python response logic
            if (response.optString("status") == "out_of_stock") {
                return -1.0
            }

            // Standardize return value to double
            response.optDouble("unitPrice", item.price)

        } catch (e: Exception) {
            Log.e(TAG, "Python Bridge Failed: ${e.message}")
            // Fallback to base price if bridge or python fails
            item.price
        }
    }

    /**
     * Calculates the final totals for a transaction using the Python finance engine
     * for high-precision Money-based math.
     */
    fun calculateFullTransaction(itemPricesAndQtys: List<Pair<Double, Int>>, settings: AppSetting?): TransactionResult {
        return try {
            val py = Python.getInstance()
            val module = py.getModule("finance_engine")

            val itemsArray = JSONArray()
            itemPricesAndQtys.forEach { (price, qty) ->
                itemsArray.put(JSONObject().apply {
                    put("price", price)
                    put("qty", qty)
                })
            }

            val configJson = JSONObject().apply {
                put("taxRate", settings?.taxRate ?: 0.08)
            }

            val result = module.callAttr("calculate_transaction", itemsArray.toString(), configJson.toString())
            val response = JSONObject(result.toString())

            TransactionResult(
                subtotal = response.getDouble("subtotal"),
                tax = response.getDouble("tax"),
                total = response.getDouble("total"),
                formattedTotal = response.getString("formattedTotal")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Transaction Calculation Failed: ${e.message}")
            // Local fallback if Python fails
            val subtotal = itemPricesAndQtys.sumOf { it.first * it.second }
            val tax = calculateTax(subtotal, settings?.taxRate ?: 0.08)
            TransactionResult(subtotal, tax, subtotal + tax, String.format(Locale.US, "$%.2f", subtotal + tax))
        }
    }

    /**
     * Calculates tax using BigDecimal for precision.
     */
    fun calculateTax(subtotal: Double, taxRate: Double): Double =
        BigDecimal.valueOf(subtotal)
            .multiply(BigDecimal.valueOf(taxRate))
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
}
