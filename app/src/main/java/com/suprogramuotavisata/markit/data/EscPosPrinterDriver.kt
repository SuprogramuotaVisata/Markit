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
import java.net.InetSocketAddress
import java.net.Socket

object EscPosPrinterDriver {
    private const val TAG = "EscPosPrinterDriver"

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

            val sharedPrefs = context.getSharedPreferences("MarkItSettings", Context.MODE_PRIVATE)
            val autoCut = sharedPrefs.getBoolean("printer_auto_cut", true)
            val posPaperWidth = sharedPrefs.getInt("pos_paper_width", 80) // 80mm or 160mm

            // Resolve target width in pixels (80mm has 576 dots printable area, 160mm has 1280 dots)
            val targetWidth = if (posPaperWidth == 160) 1280 else 576
            val widthBytes = targetWidth / 8

            // Scale calculations
            val scale = targetWidth.toFloat() / bitmap.width.toFloat()
            val scaledHeight = (bitmap.height * scale).toInt()
            val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, scaledHeight, true)

            Log.d(TAG, "printBitmap (ESC/POS Network) - width: ${bitmap.width}, height: ${bitmap.height}, paper: ${posPaperWidth}mm, scaledWidth: $targetWidth, scaledHeight: $scaledHeight")

            // 1. INITIALIZE PRINTER
            out.write(byteArrayOf(0x1B, 0x40)) // ESC @

            // 2. PRINT RASTER BIT IMAGE (GS v 0)
            val xL = (widthBytes and 0xFF).toByte()
            val xH = ((widthBytes shr 8) and 0xFF).toByte()
            val yL = (scaledHeight and 0xFF).toByte()
            val yH = ((scaledHeight shr 8) and 0xFF).toByte()

            out.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH))

            var blackPixels = 0
            for (y in 0 until scaledHeight) {
                val row = ByteArray(widthBytes)
                for (x in 0 until targetWidth) {
                    val pixel = scaled.getPixel(x, y)
                    // If pixel is sufficiently dark
                    if (Color.red(pixel) < 180) {
                        val byteIdx = x / 8
                        val bitIdx = 7 - (x % 8)
                        row[byteIdx] = (row[byteIdx].toInt() or (1 shl bitIdx)).toByte()
                        blackPixels++
                    }
                }
                out.write(row)
            }

            // 3. FEED LINES (usually 3 lines)
            out.write(byteArrayOf(0x1B, 0x64, 0x03)) // ESC d 3

            // 4. AUTO CUT (GS V 66 0)
            if (autoCut) {
                out.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00)) // GS V 66 0 (Cut page)
            }
            out.flush()

            Thread.sleep(1500) // Wait for physical print out
            Result.success(blackPixels)
        } catch (e: Exception) {
            Log.e(TAG, "ESC/POS Print Error: ${e.message}")
            Result.failure(e)
        } finally {
            try { socket?.close() } catch (e: Exception) {}
        }
    }

    fun printBitmapUsb(context: Context, bitmap: Bitmap): Result<Int> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList = usbManager.deviceList

        // Scan for printer class (7) devices dynamically
        val device = deviceList.values.find { dev ->
            (0 until dev.interfaceCount).any { i ->
                dev.getInterface(i).interfaceClass == 7
            }
        } ?: deviceList.values.find { dev ->
            dev.deviceClass == 7
        } ?: return Result.failure(Exception("Nerastas joks USB POS termo spausdintuvas."))

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

            val sharedPrefs = context.getSharedPreferences("MarkItSettings", Context.MODE_PRIVATE)
            val autoCut = sharedPrefs.getBoolean("printer_auto_cut", true)
            val posPaperWidth = sharedPrefs.getInt("pos_paper_width", 80) // 80mm or 160mm

            val targetWidth = if (posPaperWidth == 160) 1280 else 576
            val widthBytes = targetWidth / 8

            // Scale calculations
            val scale = targetWidth.toFloat() / bitmap.width.toFloat()
            val scaledHeight = (bitmap.height * scale).toInt()
            val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, scaledHeight, true)

            Log.d(TAG, "printBitmapUsb (ESC/POS USB) - width: ${bitmap.width}, height: ${bitmap.height}, paper: ${posPaperWidth}mm, scaledWidth: $targetWidth, scaledHeight: $scaledHeight")

            val byteStream = java.io.ByteArrayOutputStream()

            // 1. INITIALIZE PRINTER
            byteStream.write(byteArrayOf(0x1B, 0x40)) // ESC @

            // 2. PRINT RASTER BIT IMAGE (GS v 0)
            val xL = (widthBytes and 0xFF).toByte()
            val xH = ((widthBytes shr 8) and 0xFF).toByte()
            val yL = (scaledHeight and 0xFF).toByte()
            val yH = ((scaledHeight shr 8) and 0xFF).toByte()

            byteStream.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH))

            var blackPixels = 0
            for (y in 0 until scaledHeight) {
                val row = ByteArray(widthBytes)
                for (x in 0 until targetWidth) {
                    val pixel = scaled.getPixel(x, y)
                    if (Color.red(pixel) < 180) {
                        val byteIdx = x / 8
                        val bitIdx = 7 - (x % 8)
                        row[byteIdx] = (row[byteIdx].toInt() or (1 shl bitIdx)).toByte()
                        blackPixels++
                    }
                }
                byteStream.write(row)
            }

            // 3. FEED LINES
            byteStream.write(byteArrayOf(0x1B, 0x64, 0x03)) // ESC d 3

            // 4. AUTO CUT
            if (autoCut) {
                byteStream.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00)) // GS V 66 0
            }

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
            Log.e(TAG, "USB ESC/POS Print Error: ${e.message}")
            Result.failure(e)
        } finally {
            try { connection?.close() } catch (e: Exception) {}
        }
    }
}
