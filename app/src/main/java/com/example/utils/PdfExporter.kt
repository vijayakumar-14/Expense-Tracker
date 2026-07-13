package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.data.Expense
import java.io.OutputStream
import java.time.LocalDate

object PdfExporter {
    fun exportExpensesToPdf(
        context: Context,
        title: String,
        dateRange: String,
        expenses: List<Expense>,
        totalAmount: Double
    ) {
        var pdfDocument: PdfDocument? = null
        try {
            pdfDocument = PdfDocument()
            
            // A4 size in points: 595 x 842
            val pageWidth = 595
            val pageHeight = 842
            
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            
            // Paints
            val titlePaint = Paint().apply {
                color = Color.rgb(0, 92, 185) // BluePrimary #005CB9
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            
            val subTitlePaint = Paint().apply {
                color = Color.rgb(26, 28, 30) // LightOnBackground
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            
            val dateRangePaint = Paint().apply {
                color = Color.rgb(116, 119, 127) // LightTextTertiary
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }
            
            val headerPaint = Paint().apply {
                color = Color.rgb(0, 92, 185)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            
            val borderPaint = Paint().apply {
                color = Color.rgb(225, 226, 236) // LightOutline #E1E2EC
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            
            val textPaint = Paint().apply {
                color = Color.rgb(26, 28, 30)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }
            
            val totalLabelPaint = Paint().apply {
                color = Color.rgb(26, 28, 30)
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            
            val totalValuePaint = Paint().apply {
                color = Color.rgb(0, 92, 185)
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            
            // Draw Header
            var yPosition = 50f
            canvas.drawText("Monthly Expense Tracker", 40f, yPosition, titlePaint)
            yPosition += 24f
            canvas.drawText(title, 40f, yPosition, subTitlePaint)
            yPosition += 16f
            canvas.drawText("Date Range: $dateRange", 40f, yPosition, dateRangePaint)
            yPosition += 14f
            canvas.drawText("Generated on: ${LocalDate.now()}", 40f, yPosition, dateRangePaint)
            
            yPosition += 30f
            
            // Draw Table Headers
            canvas.drawText("Date", 40f, yPosition, headerPaint)
            canvas.drawText("Expense Category / Type", 150f, yPosition, headerPaint)
            canvas.drawText("Amount", 480f, yPosition, headerPaint)
            
            yPosition += 8f
            canvas.drawLine(40f, yPosition, 555f, yPosition, borderPaint)
            yPosition += 18f
            
            var currentPageNumber = 1
            
            if (expenses.isEmpty()) {
                canvas.drawText("No expenses recorded for this period.", 40f, yPosition, textPaint)
                yPosition += 18f
            } else {
                // Draw Table Rows
                for (expense in expenses) {
                    // Check if we need a new page
                    if (yPosition > 780f) {
                        pdfDocument.finishPage(page)
                        currentPageNumber++
                        val newPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                        page = pdfDocument.startPage(newPageInfo)
                        canvas = page.canvas
                        
                        // Redraw Table Headers on new page
                        yPosition = 50f
                        canvas.drawText("Date", 40f, yPosition, headerPaint)
                        canvas.drawText("Expense Category / Type", 150f, yPosition, headerPaint)
                        canvas.drawText("Amount", 480f, yPosition, headerPaint)
                        yPosition += 8f
                        canvas.drawLine(40f, yPosition, 555f, yPosition, borderPaint)
                        yPosition += 18f
                    }
                    
                    canvas.drawText(expense.date, 40f, yPosition, textPaint)
                    canvas.drawText(expense.name, 150f, yPosition, textPaint)
                    canvas.drawText(CurrencyUtils.format(expense.amount), 480f, yPosition, textPaint)
                    
                    yPosition += 8f
                    canvas.drawLine(40f, yPosition, 555f, yPosition, borderPaint)
                    yPosition += 18f
                }
            }
            
            // Draw Total Row (check if we need new page for total block)
            if (yPosition > 750f) {
                pdfDocument.finishPage(page)
                currentPageNumber++
                val newPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                page = pdfDocument.startPage(newPageInfo)
                canvas = page.canvas
                yPosition = 50f
            }
            
            yPosition += 10f
            canvas.drawText("TOTAL AMOUNT EXPENDED:", 300f, yPosition, totalLabelPaint)
            canvas.drawText(CurrencyUtils.format(totalAmount), 480f, yPosition, totalValuePaint)
            
            pdfDocument.finishPage(page)
            
            // Save to MediaStore (Downloads Folder)
            val cleanTitle = title.replace("\\s+".toRegex(), "_")
            val fileName = "Expense_Report_${cleanTitle}_${System.currentTimeMillis()}.pdf"
            val resolver = context.contentResolver
            
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }
            
            val downloadUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Files.getContentUri("external")
            }
            
            val uri = resolver.insert(downloadUri, contentValues)
            if (uri != null) {
                val outputStream: OutputStream? = resolver.openOutputStream(uri)
                if (outputStream != null) {
                    pdfDocument.writeTo(outputStream)
                    outputStream.close()
                    Toast.makeText(context, "PDF saved to Downloads folder!", Toast.LENGTH_LONG).show()
                    
                    // Share/View Intent
                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    
                    val shareIntent = Intent.createChooser(viewIntent, "Open PDF Report").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    
                    context.startActivity(shareIntent)
                }
            } else {
                Toast.makeText(context, "Failed to create PDF file in MediaStore", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error exporting PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            pdfDocument?.close()
        }
    }
}
