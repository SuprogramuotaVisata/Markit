package com.suprogramuotavisata.markit.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

object BrotherPrinterDriver {
    private const val TAG = "BrotherPrinterDriver"

    fun printBitmap(context: Context, ip: String, port: Int, bitmap: Bitmap): Result<Unit> {
        var socket: Socket? = null
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifiNetwork = connectivityManager.allNetworks.find { network ->
                val caps = connectivityManager.getNetworkCapabilities(network)
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }

            socket = Socket()
            socket.tcpNoDelay = true
            wifiNetwork?.bindSocket(socket)

            socket.connect(InetSocketAddress(ip, port), 5000)
            val out = BufferedOutputStream(socket.getOutputStream())
            
            // 1. WAKE UP & INITIALIZE
            out.write(byteArrayOf(0x1B, 0x69, 0x53)) // ESC i S (Status Request)
            out.write(ByteArray(100) { 0 })         // Invalidate any junk
            out.write(byteArrayOf(0x1B, 0x40))       // ESC @ (Initialize)
            out.flush()
            
            // 2. SWITCH TO RASTER MODE
            out.write(byteArrayOf(0x1B, 0x69, 0x61, 0x01)) // ESC i a 1
            out.flush()
            
            // 3. SET MEDIA INFO (Exactly 13 bytes total after command for 24mm tape)
            // 1B 69 7A {84h: Width Only} {00h: TZe} {18h: 24mm} {00h: Auto} {00 00 00 00: Length} {00 00: Reserve}
            out.write(byteArrayOf(0x1B, 0x69, 0x7A, 0x84.toByte(), 0x00, 0x18, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
            
            // 4. SET PARAMETERS
            out.write(byteArrayOf(0x1B, 0x69, 0x4D, 0x40)) // Auto cut
            out.write(byteArrayOf(0x1B, 0x69, 0x64, 0x00, 0x00)) // No margin
            out.write(byteArrayOf(0x4D, 0x00)) // No compression
            out.flush()

            // 5. DATA TRANSFER (16 bytes per line)
            val emptyRow = ByteArray(16)
            
            // A. Initial Padding (Run-up)
            repeat(50) {
                out.write(byteArrayOf(0x47, 0x10, 0x00))
                out.write(emptyRow)
            }

            // B. Actual Image
            sendStandardRasterLines(out, bitmap)

            // C. Final Padding (CRITICAL: Push text past the cutter)
            // PT-P750W needs about 25mm to clear the head-to-cutter gap
            repeat(250) {
                out.write(byteArrayOf(0x47, 0x10, 0x00))
                out.write(emptyRow)
            }

            // 6. FINISH
            out.write(byteArrayOf(0x1A)) // Control-Z (Execute Print)
            out.flush()
            
            Thread.sleep(2000) // Give time for the long physical feed
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Print Error: ${e.message}")
            Result.failure(e)
        } finally {
            try { socket?.close() } catch (e: Exception) {}
        }
    }

    private fun sendStandardRasterLines(out: OutputStream, bitmap: Bitmap) {
        val targetWidth = 128 // 128 dots for 24mm tape
        val bytesPerLine = 16 

        val scale = targetWidth.toFloat() / bitmap.width.toFloat()
        val scaledWidth = targetWidth
        val scaledHeight = (bitmap.height * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

        for (y in 0 until scaled.height) {
            val line = ByteArray(bytesPerLine)
            for (x in 0 until scaled.width) {
                val pixel = scaled.getPixel(x, y)
                // Threshold: anything darker than mid-gray
                if (Color.red(pixel) < 180) { 
                    val byteIdx = x / 8
                    val bitIdx = 7 - (x % 8)
                    line[byteIdx] = (line[byteIdx].toInt() or (1 shl bitIdx)).toByte()
                }
            }
            // 'G' (0x47) 16 bytes L (0x10) 00 bytes H (0x00)
            out.write(byteArrayOf(0x47, 0x10, 0x00))
            out.write(line)
        }
    }
}
