package com.suprogramuotavisata.markit.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

object BrotherPrinterDriver {
    private const val TAG = "BrotherPrinterDriver"

    fun printBitmap(context: Context, ip: String, port: Int, bitmap: Bitmap): Result<Int> {
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
            
            // Calculate lines info
            val targetWidth = 128
            val scale = targetWidth.toFloat() / bitmap.width.toFloat()
            val scaledHeight = (bitmap.height * scale).toInt()
            val totalLines = scaledHeight + 300
            
            val r0 = (totalLines and 0xFF).toByte()
            val r1 = ((totalLines shr 8) and 0xFF).toByte()
            val r2 = ((totalLines shr 16) and 0xFF).toByte()
            val r3 = ((totalLines shr 24) and 0xFF).toByte()

            // 1. CLEAR BUFFER & INITIALIZE & SWITCH TO RASTER MODE
            out.write(ByteArray(350) { 0 })                 // 350 null bytes to clear printer command buffer
            out.write(byteArrayOf(0x1B, 0x40))             // ESC @ (Initialize)
            out.write(byteArrayOf(0x1B, 0x69, 0x61, 0x01))   // ESC i a 1 (Switch to Raster Mode)
            out.flush()
            
            // 3. SET MEDIA INFO (TZe tape width 24mm 0x18, auto-detect tape 0x00, total lines)
            out.write(byteArrayOf(
                0x1B, 0x69, 0x7A, 
                0x84.toByte(), // valid_flags (Recover, Width)
                0x00,          // media_type (auto-detect)
                0x18,          // media_width (24mm)
                0x00,          // media_length (0)
                r0, r1, r2, r3, // raster_num (4 bytes little-endian)
                0x00,          // page_type (first/only page)
                0x00           // fixed
            ))
            
            // 4. SET PARAMETERS
            out.write(byteArrayOf(0x1B, 0x69, 0x4D, 0x40)) // Auto cut
            out.write(byteArrayOf(0x4D, 0x02)) // Select RLE/TIFF compression mode (0x02)
            out.flush()

            // 5. DATA TRANSFER
            // A. Initial Padding (Run-up)
            repeat(50) {
                out.write(0x5A) // Z command: empty line
            }

            // B. Actual Image
            val blackPixels = sendStandardRasterLines(out, bitmap)

            // C. Final Padding (CRITICAL: Push text past the cutter)
            repeat(250) {
                out.write(0x5A) // Z command: empty line
            }

            // 6. FINISH
            out.write(byteArrayOf(0x1A)) // Control-Z (Execute Print)
            out.flush()
            
            Thread.sleep(2000) // Give time for physical feed
            Result.success(blackPixels)
        } catch (e: Exception) {
            Log.e(TAG, "Print Error: ${e.message}")
            Result.failure(e)
        } finally {
            try { socket?.close() } catch (e: Exception) {}
        }
    }

    private fun sendStandardRasterLines(out: OutputStream, bitmap: Bitmap): Int {
        val targetWidth = 128 // 128 dots for 24mm tape
        val bytesPerLine = 16 
        var blackPixels = 0

        val scale = targetWidth.toFloat() / bitmap.width.toFloat()
        val scaledWidth = targetWidth
        val scaledHeight = (bitmap.height * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

        for (y in 0 until scaled.height) {
            val line = ByteArray(bytesPerLine)
            var hasBlack = false
            for (x in 0 until scaled.width) {
                val pixel = scaled.getPixel(x, y)
                if (Color.red(pixel) < 180) { 
                    val mirroredX = (scaledWidth - 1) - x
                    val byteIdx = mirroredX / 8
                    val bitIdx = 7 - (mirroredX % 8)
                    line[byteIdx] = (line[byteIdx].toInt() or (1 shl bitIdx)).toByte()
                    blackPixels++
                    hasBlack = true
                }
            }
            if (!hasBlack) {
                out.write(0x5A) // Z command: empty line
            } else {
                out.write(byteArrayOf(0x47, 0x11, 0x00)) // G, 17 bytes count, 0x00
                out.write(byteArrayOf(0x0F))             // RLE header: 16 bytes uncompressed literal run
                out.write(line)
            }
        }
        return blackPixels
    }

    /**
     * Prints the bitmap using native Android USB host connectivity.
     */
    fun printBitmapUsb(context: Context, bitmap: Bitmap): Result<Int> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList = usbManager.deviceList
        // Brother PT-P750W VID is 0x04F9, PID is 0x2074
        val device = deviceList.values.find { it.vendorId == 0x04F9 && it.productId == 0x2074 }
            ?: return Result.failure(Exception("Spausdintuvas Brother PT-P750W nerastas USB jungtyje."))

        // Request USB permission if not granted
        if (!usbManager.hasPermission(device)) {
            val permissionIntent = PendingIntent.getBroadcast(
                context, 0, Intent("com.suprogramuotavisata.markit.USB_PERMISSION"),
                PendingIntent.FLAG_MUTABLE
            )
            usbManager.requestPermission(device, permissionIntent)
            return Result.failure(Exception("Suteikite USB leidimą ir bandykite dar kartą."))
        }

        var connection: UsbDeviceConnection? = null
        return try {
            connection = usbManager.openDevice(device)
                ?: return Result.failure(Exception("Nepavyko atidaryti USB įrenginio."))

            var usbInterface: UsbInterface? = null
            var outEndpoint: UsbEndpoint? = null

            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                for (j in 0 until iface.endpointCount) {
                    val ep = iface.getEndpoint(j)
                    if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                        ep.direction == UsbConstants.USB_DIR_OUT) {
                        usbInterface = iface
                        outEndpoint = ep
                        break
                    }
                }
                if (outEndpoint != null) break
            }

            if (usbInterface == null || outEndpoint == null) {
                return Result.failure(Exception("Nerasta tinkama USB spausdintuvo bulk out jungtis."))
            }

            if (!connection.claimInterface(usbInterface, true)) {
                return Result.failure(Exception("Nepavyko užimti USB sąsajos."))
            }

            val byteStream = java.io.ByteArrayOutputStream()
            
            // Calculate lines info
            val targetWidth = 128
            val scale = targetWidth.toFloat() / bitmap.width.toFloat()
            val scaledHeight = (bitmap.height * scale).toInt()
            val totalLines = scaledHeight + 300
            
            val r0 = (totalLines and 0xFF).toByte()
            val r1 = ((totalLines shr 8) and 0xFF).toByte()
            val r2 = ((totalLines shr 16) and 0xFF).toByte()
            val r3 = ((totalLines shr 24) and 0xFF).toByte()

            // 1. CLEAR BUFFER & INITIALIZE & SWITCH TO RASTER MODE
            byteStream.write(ByteArray(350) { 0 })                 // 350 null bytes to clear printer command buffer
            byteStream.write(byteArrayOf(0x1B, 0x40))             // ESC @ (Initialize)
            byteStream.write(byteArrayOf(0x1B, 0x69, 0x61, 0x01))   // ESC i a 1 (Switch to Raster Mode)
            
            // 3. SET MEDIA INFO (TZe tape width 24mm 0x18, auto-detect tape 0x00, total lines)
            byteStream.write(byteArrayOf(
                0x1B, 0x69, 0x7A, 
                0x84.toByte(), // valid_flags (Recover, Width)
                0x00,          // media_type (auto-detect)
                0x18,          // media_width (24mm)
                0x00,          // media_length (0)
                r0, r1, r2, r3, // raster_num (4 bytes little-endian)
                0x00,          // page_type (first/only page)
                0x00           // fixed
            ))
            
            // 4. SET PARAMETERS
            byteStream.write(byteArrayOf(0x1B, 0x69, 0x4D, 0x40)) // Auto cut
            byteStream.write(byteArrayOf(0x4D, 0x02)) // Select RLE/TIFF compression mode (0x02)

            // 5. DATA TRANSFER
            // A. Initial Padding (Run-up)
            repeat(50) {
                byteStream.write(0x5A) // Z command: empty line
            }

            // B. Actual Image
            val blackPixels = writeRasterLinesToStream(byteStream, bitmap)

            // C. Final Padding (CRITICAL: Push text past the cutter)
            repeat(250) {
                byteStream.write(0x5A) // Z command: empty line
            }

            // 6. FINISH
            byteStream.write(byteArrayOf(0x1A)) // Control-Z (Execute Print)
            
            val payload = byteStream.toByteArray()
            
            var bytesSent = 0
            val chunkSize = 16384
            while (bytesSent < payload.size) {
                val length = minOf(chunkSize, payload.size - bytesSent)
                val chunk = payload.copyOfRange(bytesSent, bytesSent + length)
                val transferred = connection.bulkTransfer(outEndpoint, chunk, length, 5000)
                if (transferred < 0) {
                    return Result.failure(Exception("Klaida siunčiant duomenis per USB."))
                }
                bytesSent += transferred
            }

            connection.releaseInterface(usbInterface)
            Result.success(blackPixels)
        } catch (e: Exception) {
            Log.e(TAG, "USB Print Error: ${e.message}")
            Result.failure(e)
        } finally {
            try { connection?.close() } catch (e: Exception) {}
        }
    }

    private fun writeRasterLinesToStream(stream: java.io.ByteArrayOutputStream, bitmap: Bitmap): Int {
        val targetWidth = 128
        val bytesPerLine = 16 
        var blackPixels = 0

        val scale = targetWidth.toFloat() / bitmap.width.toFloat()
        val scaledWidth = targetWidth
        val scaledHeight = (bitmap.height * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

        for (y in 0 until scaled.height) {
            val line = ByteArray(bytesPerLine)
            var hasBlack = false
            for (x in 0 until scaled.width) {
                val pixel = scaled.getPixel(x, y)
                if (Color.red(pixel) < 180) { 
                    val mirroredX = (scaledWidth - 1) - x
                    val byteIdx = mirroredX / 8
                    val bitIdx = 7 - (mirroredX % 8)
                    line[byteIdx] = (line[byteIdx].toInt() or (1 shl bitIdx)).toByte()
                    blackPixels++
                    hasBlack = true
                }
            }
            if (!hasBlack) {
                stream.write(0x5A) // Z command: empty line
            } else {
                stream.write(byteArrayOf(0x47, 0x11, 0x00)) // G, 17 bytes count, 0x00
                stream.write(byteArrayOf(0x0F))             // RLE header: 16 bytes uncompressed literal run
                stream.write(line)
            }
        }
        return blackPixels
    }
}
