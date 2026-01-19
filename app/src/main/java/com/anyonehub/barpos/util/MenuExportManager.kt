/* Copyright 2024 anyone-Hub */
@file:OptIn(ExperimentalTime::class)

package com.anyonehub.barpos.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.anyonehub.barpos.data.Category
import com.anyonehub.barpos.data.MenuItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime

/**
 * Handles the generation of CSV and PDF menu audit reports.
 * Hardened with high-precision time for accurate inventory versioning.
 */
object MenuExportManager {

    /**
     * Exports the menu to CSV for external auditing.
     */
    fun exportToCsv(context: Context, categories: List<Category>, items: List<MenuItem>): File? {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        // High-Precision Audit Timestamp for export filename
        val now: Instant = Clock.System.now()
        val fileName = "Midtown_Menu_Audit_${getFormattedTimestamp(now)}.csv"
        val file = File(exportDir, fileName)

        return try {
            FileOutputStream(file).use { out ->
                val header = "ID,Item Name,Category,Price,Stock,Shot Wall Mode,Active\n"
                out.write(header.toByteArray())

                items.forEach { item ->
                    val categoryName = categories.find { it.id == item.categoryId }?.name ?: "Uncategorized"
                    val safeName = item.name.replace("\"", "\"\"")

                    val line = StringBuilder()
                        .append("${item.id},")
                        .append("\"$safeName\",")
                        .append("\"$categoryName\",")
                        .append("${String.format(Locale.US, "%.2f", item.price)},")
                        .append("${item.inventoryCount},")
                        .append("${if(item.isShotWallItem) "YES" else "NO"},")
                        .append("${if(item.isActive) "Yes" else "No"}\n")

                    out.write(line.toString().toByteArray())
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a printable PDF "Menu Summary".
     */
    fun exportToPdf(context: Context, categories: List<Category>, items: List<MenuItem>): File? {
        val pdfDocument = PdfDocument()
        val now: Instant = Clock.System.now()

        try {
            val titlePaint = Paint().apply { textSize = 24f; isFakeBoldText = true; color = Color.BLACK }
            val headerPaint = Paint().apply { textSize = 16f; isFakeBoldText = true; color = Color.BLACK }
            val textPaint = Paint().apply { textSize = 12f; color = Color.DKGRAY }
            val pricePaint = Paint().apply { textSize = 12f; textAlign = Paint.Align.RIGHT; color = Color.BLACK }
            val bgPaint = Paint().apply { color = Color.LTGRAY }

            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            var y = 50f

            canvas.drawText("MIDTOWN POS - INVENTORY AUDIT", 40f, y, titlePaint)
            y += 25f
            canvas.drawText("Generated: ${getFormattedTimestamp(now)}", 40f, y, textPaint)
            y += 40f

            categories.forEach { category ->
                if (y > 780f) {
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = 50f
                }

                canvas.drawRect(40f, y - 20f, 555f, y + 10f, bgPaint)
                canvas.drawText(category.name.uppercase(), 50f, y + 5f, headerPaint)
                y += 35f

                val categoryItems = items.filter { it.categoryId == category.id }
                if (categoryItems.isEmpty()) {
                    canvas.drawText("(No items in this category)", 50f, y, textPaint)
                    y += 20f
                }

                categoryItems.forEach { item ->
                    if (y > 800f) {
                        pdfDocument.finishPage(page)
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        y = 50f
                    }

                    canvas.drawText(item.name, 50f, y, textPaint)
                    canvas.drawText("Stock: ${item.inventoryCount}", 400f, y, textPaint)
                    val formattedPrice = String.format(Locale.US, "$%.2f", item.price)
                    canvas.drawText(formattedPrice, 550f, y, pricePaint)
                    y += 20f
                }
                y += 15f
            }

            pdfDocument.finishPage(page)

            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val fileName = "Midtown_Menu_Summary_${getFormattedTimestamp(now)}.pdf"
            val file = File(exportDir, fileName)

            FileOutputStream(file).use { pdfDocument.writeTo(it) }
            return file

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
    }

    private fun getFormattedTimestamp(instant: Instant) = 
        SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(instant.toEpochMilliseconds()))
}
