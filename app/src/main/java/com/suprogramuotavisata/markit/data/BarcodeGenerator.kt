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
     * Generates a 2D QR Code Bitmap from the given string.
     */
    fun generateQrCode(text: String, width: Int, height: Int): Bitmap? {
        if (text.isBlank()) return null
        try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
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

    /**
     * Generates a printable label bitmap containing group information.
     * Optimized for narrow tape (e.g., 24mm / 128 pins).
     */
    fun generateLabel(
        name: String,
        code: String,
        barcode: String?,
        description: String?,
        rotation: Int = 0,
        qrOnly: Boolean = false
    ): Bitmap {
        if (qrOnly) {
            return generateQrOnlyLabel(name, code, barcode, description, rotation)
        }
        if (rotation == 90 || rotation == 270) {
            return generateHorizontalLabel(name, code, barcode, description, rotation)
        }

        val width = 128 // Target pins for 24mm tape at 180dpi
        val padding = 2 // Decreased padding to maximize barcode width for scan-ability
        val textSpacing = 10
        
        val titlePaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 24f
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        val contentPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 18f
            isAntiAlias = true
        }

        // Measure layouts
        val nameLayout = StaticLayout.Builder.obtain(name, 0, name.length, titlePaint, width - 2 * padding).build()
        val codeText = "Kodas: $code"
        val codeLayout = StaticLayout.Builder.obtain(codeText, 0, codeText.length, contentPaint, width - 2 * padding).build()
        
        var descLayout: StaticLayout? = null
        if (!description.isNullOrBlank()) {
            val descText = "Aprasymas: $description"
            descLayout = StaticLayout.Builder.obtain(descText, 0, descText.length, contentPaint, width - 2 * padding).build()
        }

        var barcodeBitmap: Bitmap? = null
        var barcodeHeight = 0
        val effectiveBarcode = barcode ?: code
        if (effectiveBarcode.isNotBlank()) {
            // Generate barcode to fit the 124px width
            barcodeBitmap = generateBarcode(TranslationManager.stripAccents(effectiveBarcode), width - 2 * padding, 60)
            if (barcodeBitmap != null) {
                barcodeHeight = 60 + textSpacing + 25 // barcode + spacing + code label height with spacing
            }
        }

        val totalHeight = padding * 2 + nameLayout.height + textSpacing + codeLayout.height + 
                         (if (descLayout != null) textSpacing + descLayout.height else 0) +
                         (if (barcodeBitmap != null) textSpacing + barcodeHeight else 0)

        val result = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        var currentY = padding.toFloat()
        
        // Draw Name
        canvas.save()
        canvas.translate(padding.toFloat(), currentY)
        nameLayout.draw(canvas)
        canvas.restore()
        currentY += nameLayout.height + textSpacing

        // Draw Code
        canvas.save()
        canvas.translate(padding.toFloat(), currentY)
        codeLayout.draw(canvas)
        canvas.restore()
        currentY += codeLayout.height + textSpacing

        // Draw Description
        if (descLayout != null) {
            canvas.save()
            canvas.translate(padding.toFloat(), currentY)
            descLayout.draw(canvas)
            canvas.restore()
            currentY += descLayout.height + textSpacing
        }

        // Draw Barcode
        if (barcodeBitmap != null) {
            canvas.drawBitmap(barcodeBitmap, padding.toFloat(), currentY, null)
            currentY += 60 // Barcode height is 60
            
            val labelPaint = Paint().apply {
                color = Color.BLACK
                textSize = 14f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            // Use FontMetrics ascent to position the text correctly below the barcode without overlapping
            val fontMetrics = labelPaint.fontMetrics
            val labelY = currentY + textSpacing - fontMetrics.ascent
            canvas.drawText(effectiveBarcode, (width / 2).toFloat(), labelY, labelPaint)
        }

        if (rotation == 180) {
            val matrix = android.graphics.Matrix().apply { postRotate(180f) }
            return Bitmap.createBitmap(result, 0, 0, result.width, result.height, matrix, true)
        }

        return result
    }

    private fun generateHorizontalLabel(
        name: String,
        code: String,
        barcode: String?,
        description: String?,
        rotation: Int
    ): Bitmap {
        val height = 128
        val padding = 8
        val spacing = 12

        val titlePaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 20f
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        val contentPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 15f
            isAntiAlias = true
        }

        // Measure text column width
        val nameText = name
        val codeText = "Kodas: $code"
        val descText = if (!description.isNullOrBlank()) "Aprasymas: $description" else ""

        val maxTextWidth = maxOf(
            titlePaint.measureText(nameText).toInt(),
            contentPaint.measureText(codeText).toInt(),
            if (descText.isNotEmpty()) contentPaint.measureText(descText).toInt() else 0
        )
        val textWidth = maxTextWidth.coerceIn(150, 300)

        // Measure barcode - Enlarge to 300px for highly reliable scan-ability in horizontal view
        val barcodeWidth = 300
        val barcodeTargetHeight = 60
        val effectiveBarcode = barcode ?: code
        var barcodeBitmap: Bitmap? = null
        if (effectiveBarcode.isNotBlank()) {
            barcodeBitmap = generateBarcode(TranslationManager.stripAccents(effectiveBarcode), barcodeWidth, barcodeTargetHeight)
        }

        // Calculate total canvas width
        val totalWidth = padding + textWidth + spacing + barcodeWidth + padding
        
        val result = Bitmap.createBitmap(totalWidth, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        // Draw Text Column (left side)
        val nameLayout = StaticLayout.Builder.obtain(nameText, 0, nameText.length, titlePaint, textWidth).build()
        val codeLayout = StaticLayout.Builder.obtain(codeText, 0, codeText.length, contentPaint, textWidth).build()
        val descLayout = if (descText.isNotEmpty()) {
            StaticLayout.Builder.obtain(descText, 0, descText.length, contentPaint, textWidth).build()
        } else null

        // Center the text block vertically
        val textBlockHeight = nameLayout.height + 4 + codeLayout.height + (if (descLayout != null) 4 + descLayout.height else 0)
        var textY = ((height - textBlockHeight) / 2).toFloat().coerceAtLeast(padding.toFloat())

        canvas.save()
        canvas.translate(padding.toFloat(), textY)
        nameLayout.draw(canvas)
        canvas.restore()
        textY += nameLayout.height + 4

        canvas.save()
        canvas.translate(padding.toFloat(), textY)
        codeLayout.draw(canvas)
        canvas.restore()
        textY += codeLayout.height + 4

        if (descLayout != null) {
            canvas.save()
            canvas.translate(padding.toFloat(), textY)
            descLayout.draw(canvas)
            canvas.restore()
        }

        // Draw Barcode Column (right side)
        if (barcodeBitmap != null) {
            val barcodeX = padding + textWidth + spacing
            val barcodeBlockHeight = barcodeTargetHeight + spacing + 14 // barcode + label under it
            val barcodeY = ((height - barcodeBlockHeight) / 2).toFloat().coerceAtLeast(padding.toFloat())

            canvas.drawBitmap(barcodeBitmap, barcodeX.toFloat(), barcodeY, null)

            val labelPaint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val labelX = barcodeX + (barcodeWidth / 2)
            // Use FontMetrics to place text cleanly under horizontal barcode
            val fontMetrics = labelPaint.fontMetrics
            val labelY = barcodeY + barcodeTargetHeight + spacing - fontMetrics.ascent
            canvas.drawText(effectiveBarcode, labelX.toFloat(), labelY, labelPaint)
        }

        // Rotate final result
        val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(result, 0, 0, result.width, result.height, matrix, true)
    }

    private fun generateQrOnlyLabel(
        name: String,
        code: String,
        barcode: String?,
        description: String?,
        rotation: Int
    ): Bitmap {
        val size = 128
        val sb = java.lang.StringBuilder()
        sb.append("Pavadinimas: ").append(name)
        sb.append("\nKodas: ").append(code)
        if (!barcode.isNullOrBlank()) {
            sb.append("\nBarkodas: ").append(barcode)
        }
        if (!description.isNullOrBlank()) {
            sb.append("\nAprasymas: ").append(description)
        }

        val qrText = sb.toString()
        val qrBitmap = generateQrCode(qrText, size, size) ?: Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            canvas.drawColor(Color.WHITE)
        }

        if (rotation != 0) {
            val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
            return Bitmap.createBitmap(qrBitmap, 0, 0, qrBitmap.width, qrBitmap.height, matrix, true)
        }
        return qrBitmap
    }
}
