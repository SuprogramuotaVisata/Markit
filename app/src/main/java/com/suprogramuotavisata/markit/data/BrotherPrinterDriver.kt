package com.suprogramuotavisata.markit.data

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

object BrotherPrinterDriver {
    private const val TAG = "BrotherPrinterDriver"

    /**
     * Prints a bitmap to a Brother PT-P750W printer via RAW TCP (Port 9100).
     */
    fun printBitmap(ip: String, port: Int, bitmap: Bitmap): Result<Unit> {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), 5000)
            val outputStream = socket.getOutputStream()

            // 1. Enter Raster Mode (Send twice for stability)
            outputStream.write(byteArrayOf(0x1B, 0x69, 0x61, 0x01))
            outputStream.write(byteArrayOf(0x1B, 0x69, 0x61, 0x01))

            // 2. Initialize
            sendInitialization(outputStream)

            // 3. Set Print Information (24mm tape)
            // ESC i z {PI} {Kind} {Width} {Length} {LengthHigh} 00 00
            val printInfo = byteArrayOf(0x1B, 0x69, 0x7A, 0x84.toByte(), 0x00, 0x18, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
            outputStream.write(printInfo)

            // 4. Set Mode (Auto-cut)
            // ESC i M {n} -> 0x40 is auto-cut
            outputStream.write(byteArrayOf(0x1B, 0x69, 0x4D, 0x40))

            // 5. Set Margin (None)
            // ESC i d {n1} {n2} -> 00 00
            outputStream.write(byteArrayOf(0x1B, 0x69, 0x64, 0x00, 0x00))

            // 6. Select compression mode (None)
            // 0x4D 0x00
            outputStream.write(byteArrayOf(0x4D, 0x00))

            // 7. Send Raster Data
            sendRasterData(outputStream, bitmap)

            // 8. Print and Feed
            outputStream.write(byteArrayOf(0x1A)) // Control-Z

            outputStream.flush()
            socket.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Print failed", e)
            Result.failure(e)
        }
    }

    private fun sendInitialization(out: OutputStream) {
        // Invalidate: 200 bytes of NULL
        out.write(ByteArray(200))
        // Initialize: ESC @
        out.write(byteArrayOf(0x1B, 0x40))
    }

    private fun sendRasterData(out: OutputStream, bitmap: Bitmap) {
        val headPins = 128
        val bytesPerLine = 16 // 128 / 8
        
        // Ensure bitmap width is 128 pins
        val scale = headPins.toFloat() / bitmap.width.toFloat()
        val targetWidth = headPins
        val targetHeight = (bitmap.height * scale).toInt()
        
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        Log.d(TAG, "Sending raster data: ${scaledBitmap.width}x${scaledBitmap.height}")

        // Each row of the bitmap becomes one raster line across the tape width
        for (y in 0 until scaledBitmap.height) {
            val lineData = ByteArray(bytesPerLine)
            for (x in 0 until scaledBitmap.width) {
                val pixel = scaledBitmap.getPixel(x, y)
                // Black threshold (1 = Black in Brother Raster)
                if (Color.red(pixel) < 128) {
                    val byteIdx = x / 8
                    val bitIdx = 7 - (x % 8)
                    lineData[byteIdx] = (lineData[byteIdx].toInt() or (1 shl bitIdx)).toByte()
                }
            }
            
            // Raster command 'G' (Transfer raster data)
            // 0x47 {n1} {n2} {data}
            out.write(byteArrayOf(0x47, 0x10, 0x00))
            out.write(lineData)
        }
    }
}
