package com.suprogramuotavisata.markit.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

object BrotherPrinterDriver {
    private const val TAG = "BrotherPrinterDriver"

    /**
     * Prints a bitmap to a Brother PT-P750W printer via RAW TCP (Port 9100).
     */
    fun printBitmap(context: Context, ip: String, port: Int, bitmap: Bitmap): Result<Unit> {
        return try {
            val socket = Socket()
            
            // On Android 10+, if Wi-Fi has no internet, system might route via Mobile Data.
            // We force the socket to use the Wi-Fi network.
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifiNetwork = connectivityManager.allNetworks.find { network ->
                val caps = connectivityManager.getNetworkCapabilities(network)
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }

            if (wifiNetwork != null) {
                Log.d(TAG, "Binding socket to Wi-Fi network")
                wifiNetwork.bindSocket(socket)
            } else {
                Log.w(TAG, "Wi-Fi network not found, using default routing")
            }

            socket.connect(InetSocketAddress(ip, port), 5000)
            val outputStream = socket.getOutputStream()

            // 1. Enter Raster Mode
            outputStream.write(byteArrayOf(0x1B, 0x69, 0x61, 0x01))

            // 2. Initialize
            sendInitialization(outputStream)

            // 3. Set Print Information (24mm tape)
            val printInfo = byteArrayOf(0x1B, 0x69, 0x7A, 0x84.toByte(), 0x00, 0x18, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
            outputStream.write(printInfo)

            // 4. Set Mode (Auto-cut)
            outputStream.write(byteArrayOf(0x1B, 0x69, 0x4D, 0x40))

            // 5. Set Margin (None)
            outputStream.write(byteArrayOf(0x1B, 0x69, 0x64, 0x00, 0x00))

            // 6. Select compression mode (None)
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
        out.write(ByteArray(100))
        out.write(byteArrayOf(0x1B, 0x40))
    }

    private fun sendRasterData(out: OutputStream, bitmap: Bitmap) {
        val headPins = 128
        val bytesPerLine = 16 
        
        val scale = headPins.toFloat() / bitmap.width.toFloat()
        val targetWidth = headPins
        val targetHeight = (bitmap.height * scale).toInt()
        
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)

        for (y in 0 until scaledBitmap.height) {
            val lineData = ByteArray(bytesPerLine)
            for (x in 0 until scaledBitmap.width) {
                val pixel = scaledBitmap.getPixel(x, y)
                if (Color.red(pixel) < 128) {
                    val byteIdx = x / 8
                    val bitIdx = 7 - (x % 8)
                    lineData[byteIdx] = (lineData[byteIdx].toInt() or (1 shl bitIdx)).toByte()
                }
            }
            out.write(byteArrayOf(0x47, 0x10, 0x00))
            out.write(lineData)
        }
    }
}
