package com.suprogramuotavisata.markit.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object BarcodeGenerator {

    /**
     * Generates a 1D Code 128 Barcode Bitmap from the given string.
     */
    fun generateBarcode(text: String, width: Int, height: Int): Bitmap? {
        if (text.isBlank()) return null
        try {
            // Force code to be ASCII for ZXing Code 128 safety
            val safeText = TranslationManager.stripAccents(text)
            
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                safeText,
                BarcodeFormat.CODE_128,
                width,
                height
            )
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            return bmp
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Overlays barcode, date, and comments onto the captured photo in the bottom-right corner.
     */
    fun createOverlayPhoto(
        original: Bitmap,
        code: String,
        comment: String?,
        date: String,
        addCode: Boolean,
        addComment: Boolean,
        addDate: Boolean
    ): Bitmap {
        if (!addCode && (!addComment || comment.isNullOrBlank()) && !addDate) {
            return original
        }

        // Create a copy that we can draw on
        val resultBitmap = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)

        val photoWidth = original.width
        val photoHeight = original.height

        // Configure typography/size based on image resolution
        // Set basic size ratios relative to image height to look good on both high-res and low-res photos
        val scaleFactor = (photoHeight / 1000f).coerceAtLeast(0.5f)

        val padding = (16 * scaleFactor).toInt()
        val textSpacing = (8 * scaleFactor).toInt()

        // Content measurements
        var overlayWidth = (photoWidth * 0.45f).toInt().coerceAtLeast(300)
        if (overlayWidth > photoWidth - padding * 2) {
            overlayWidth = photoWidth - padding * 2
        }

        val textPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 14f * scaleFactor
            isAntiAlias = true
        }

        val labelPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 12f * scaleFactor
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // 1. Measure Date
        var dateHeight = 0
        var dateLayout: StaticLayout? = null
        if (addDate) {
            val dateText = "Data: $date"
            dateLayout = StaticLayout.Builder.obtain(dateText, 0, dateText.length, textPaint, overlayWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            dateHeight = dateLayout.height
        }

        // 2. Measure Comment (Supports Lithuanian characters during standard canvas rendering)
        var commentHeight = 0
        var commentLayout: StaticLayout? = null
        if (addComment && !comment.isNullOrBlank()) {
            val commentText = "Aprasymas: $comment"
            commentLayout = StaticLayout.Builder.obtain(commentText, 0, commentText.length, textPaint, overlayWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            commentHeight = commentLayout.height
        }

        // 3. Measure Barcode (Strict ASCII-only representation)
        var barcodeHeight = 0
        var barcodeBitmap: Bitmap? = null
        val barcodeWidth = overlayWidth - padding * 2
        val barcodeTargetHeight = (50f * scaleFactor).toInt().coerceAtLeast(30)
        
        val cleanCode = TranslationManager.stripAccents(code)

        if (addCode && cleanCode.isNotBlank()) {
            barcodeBitmap = generateBarcode(cleanCode, barcodeWidth, barcodeTargetHeight)
            if (barcodeBitmap != null) {
                // Barcode image height + spacing + code label height
                val textBounds = Rect()
                labelPaint.getTextBounds(cleanCode, 0, cleanCode.length, textBounds)
                barcodeHeight = barcodeTargetHeight + textSpacing + textBounds.height()
            }
        }

        // Calculate total content height
        var totalContentHeight = 0
        if (dateHeight > 0) totalContentHeight += dateHeight + textSpacing
        if (commentHeight > 0) totalContentHeight += commentHeight + textSpacing
        if (barcodeHeight > 0) totalContentHeight += barcodeHeight

        if (totalContentHeight == 0) return original

        val boxWidth = overlayWidth + padding * 2
        val boxHeight = totalContentHeight + padding * 2

        // Determine position of overlay box (bottom-right corner)
        val boxLeft = photoWidth - boxWidth - padding
        val boxTop = photoHeight - boxHeight - padding

        // Draw translucent background rectangle
        val bgPaint = Paint().apply {
            color = Color.argb(190, 255, 255, 255) // White with opacity
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = Color.argb(80, 0, 0, 0)
            style = Paint.Style.STROKE
            strokeWidth = 2f * scaleFactor
            isAntiAlias = true
        }

        val overlayRect = Rect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight)
        canvas.drawRect(overlayRect, bgPaint)
        canvas.drawRect(overlayRect, borderPaint)

        // Draw content inside the box
        var currentY = boxTop + padding

        // Draw Date
        if (dateLayout != null) {
            canvas.save()
            canvas.translate((boxLeft + padding).toFloat(), currentY.toFloat())
            dateLayout.draw(canvas)
            canvas.restore()
            currentY += dateHeight + textSpacing
        }

        // Draw Comment
        if (commentLayout != null) {
            canvas.save()
            canvas.translate((boxLeft + padding).toFloat(), currentY.toFloat())
            commentLayout.draw(canvas)
            canvas.restore()
            currentY += commentHeight + textSpacing
        }

        // Draw Barcode
        if (barcodeBitmap != null) {
            canvas.drawBitmap(barcodeBitmap, (boxLeft + padding).toFloat(), currentY.toFloat(), null)
            currentY += barcodeTargetHeight + textSpacing
            
            // Draw code text centered under barcode
            val centerX = boxLeft + padding + (barcodeWidth / 2)
            canvas.drawText(cleanCode, centerX.toFloat(), currentY.toFloat(), labelPaint)
        }

        return resultBitmap
    }
}
