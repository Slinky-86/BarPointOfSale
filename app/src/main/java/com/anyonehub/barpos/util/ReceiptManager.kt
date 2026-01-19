/* Copyright 2024 anyone-Hub */
@file:OptIn(ExperimentalTime::class)

package com.anyonehub.barpos.util

import android.content.Context
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.anyonehub.barpos.data.ActiveTab
import com.anyonehub.barpos.data.MenuItem
import com.anyonehub.barpos.data.TabItem
import com.anyonehub.barpos.di.SupabaseConstants
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime

object ReceiptManager {

    /**
     * Generates a PDF receipt optimized for 80mm thermal printers.
     * Hardened with high-precision kotlin.time for financial audit trails.
     */
    suspend fun generateAndShareReceipt(
        context: Context,
        tab: ActiveTab,
        items: Map<TabItem, MenuItem>,
        total: Double,
        taxRate: Double = 0.0,
        supabaseStorage: Storage? = null
    ) = withContext(Dispatchers.IO) {

        val subtotal = items.keys.sumOf { it.priceAtTimeOfSale }
        val taxAmount = subtotal * (taxRate / 100.0)

        val headerHeight = 140
        val footerHeight = 140
        val lineItemsHeight = items.size * 25
        val notesHeight = items.count { it.key.note.isNotEmpty() } * 20
        val calculatedHeight = headerHeight + lineItemsHeight + notesHeight + footerHeight

        val pdfDocument = PdfDocument()
        val pageInfo =
            PdfDocument.PageInfo.Builder(300, calculatedHeight.coerceAtLeast(450), 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        paint.color = Color.BLACK
        var y = 40f

        paint.textSize = 20f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("MIDTOWN POS", 150f, y, paint)

        y += 25f
        paint.textSize = 10f
        paint.isFakeBoldText = false
        val sdf = SimpleDateFormat("MMM dd, yyyy - h:mm a", Locale.US)
        
        // High-Precision Audit: Capture exact time of generation
        val now: Instant = Clock.System.now()
        
        canvas.drawText("ORDER: ${tab.id} | ${sdf.format(Date(now.toEpochMilliseconds()))}", 150f, y, paint)

        y += 20f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("CUSTOMER: ${tab.customerName.uppercase()}", 20f, y, paint)

        y += 15f
        paint.pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
        canvas.drawLine(20f, y, 280f, y, paint)
        paint.pathEffect = null

        y += 25f

        paint.textSize = 12f
        items.forEach { (tabItem, menuItem) ->
            paint.isFakeBoldText = true
            canvas.drawText(menuItem.name, 20f, y, paint)

            paint.isFakeBoldText = false
            val priceStr = String.format(Locale.US, "$%.2f", tabItem.priceAtTimeOfSale)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(priceStr, 280f, y, paint)

            paint.textAlign = Paint.Align.LEFT
            y += 20f

            if (tabItem.note.isNotEmpty()) {
                paint.textSize = 10f
                paint.color = Color.DKGRAY
                canvas.drawText("  ↳ ${tabItem.note}", 25f, y, paint)
                paint.color = Color.BLACK
                paint.textSize = 12f
                y += 18f
            }
        }

        y += 10f
        canvas.drawLine(20f, y, 280f, y, paint)
        y += 30f

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Subtotal", 20f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(String.format(Locale.US, "$%.2f", subtotal), 280f, y, paint)

        y += 20f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Tax (${String.format(Locale.US, "%.1f", taxRate)}%)", 20f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(String.format(Locale.US, "$%.2f", taxAmount), 280f, y, paint)

        y += 30f
        paint.textSize = 18f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("TOTAL", 20f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(String.format(Locale.US, "$%.2f", total), 280f, y, paint)

        pdfDocument.finishPage(page)

        val receiptsDir = File(context.cacheDir, "exports")
        if (!receiptsDir.exists()) receiptsDir.mkdirs()

        val cleanName = tab.customerName.replace(Regex("[^a-zA-Z0-9]"), "_")
        val fileTimestamp = SimpleDateFormat("yyyyMMdd_HHmm_ss", Locale.US).format(Date(now.toEpochMilliseconds()))
        val fileName = "Receipt_${cleanName}_$fileTimestamp.pdf"
        val file = File(receiptsDir, fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            
            // --- CLOUD BACKUP TO SUPABASE ---
            supabaseStorage?.let { storage ->
                val bucket = storage[SupabaseConstants.BUCKET_RECEIPT_BACKUPS]
                val fileData = file.readBytes()
                bucket.upload(path = fileName, data = fileData) {
                    upsert = true
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }

        withContext(Dispatchers.Main) {
            // Function shareFile must be defined or accessible here
        }
    }
}
