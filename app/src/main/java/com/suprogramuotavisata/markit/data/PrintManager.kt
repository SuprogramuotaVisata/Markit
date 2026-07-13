package com.suprogramuotavisata.markit.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.app.AlertDialog
import android.widget.Toast
import androidx.print.PrintHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object PrintManager {
    private const val TAG = "PrintManager"
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Prints the barcode using the configured connection type (System, Wi-Fi/Network, or Bluetooth).
     */
    fun printBarcode(context: Context, code: String, barcodeBitmap: Bitmap) {
        val sharedPrefs = context.getSharedPreferences("MarkItSettings", Context.MODE_PRIVATE)
        val printerType = sharedPrefs.getString("printer_type", "system") ?: "system"
        
        // Strip accents from the barcode string for strict hardware printer compatibility
        val cleanCode = TranslationManager.stripAccents(code)

        when (printerType) {
            "network" -> {
                val ip = sharedPrefs.getString("printer_ip", "") ?: ""
                val portStr = sharedPrefs.getString("printer_port", "9100") ?: "9100"
                val port = portStr.toIntOrNull() ?: 9100
                
                Log.d(TAG, "Network Printing - Target IP: $ip, Port: $port, Code: $cleanCode")
                
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        var finalIp = ip.trim()
                        if (finalIp.isEmpty()) {
                            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                            val lp = cm.getLinkProperties(cm.activeNetwork)
                            
                            // Default to .1 if no gateway found (Brother standard)
                            finalIp = "192.168.118.1"
                            
                            val gateway = lp?.routes?.firstOrNull { it.isDefaultRoute }?.gateway?.hostAddress
                            if (gateway != null && gateway != "0.0.0.0") {
                                finalIp = gateway
                            }
                        }

                        Log.d(TAG, "Naudojamas spausdintuvo IP: $finalIp")
                        BrotherPrinterDriver.printBitmap(context, finalIp, port, barcodeBitmap)
                    }
                    if (result.isSuccess) {
                        val count = result.getOrNull() ?: 0
                        AlertDialog.Builder(context)
                            .setTitle("Spausdinimas sėkmingas")
                            .setMessage("Sėkmingai išsiųsta į tinklo spausdintuvą.\nGeneruoti juodi taškai: $count")
                            .setPositiveButton("Gerai", null)
                            .show()
                    } else {
                        val ex = result.exceptionOrNull()
                        
                        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                        val lp = cm.getLinkProperties(cm.activeNetwork)
                        val phoneIp = lp?.linkAddresses?.firstOrNull { it.address is java.net.Inet4Address }?.address?.hostAddress ?: "Nežinomas"
                        val gatewayIp = lp?.routes?.firstOrNull { it.isDefaultRoute }?.gateway?.hostAddress ?: "Nerastas"

                        val errorMsg = when {
                            ex is java.net.SocketTimeoutException -> 
                                "LAUKIMO LAIKAS BAIGĖSI.\n\n" +
                                "DIAGNOSTIKA:\n" +
                                "- Telefono IP: $phoneIp\n" +
                                "- Spausdintuvo (Gateway) IP: $gatewayIp"
                            ex is java.net.ConnectException -> 
                                "RYŠYS ATMESTAS.\n\n" +
                                "Spausdintuvas rasta, bet jis užimtas arba neįleido programos. Išjunkite ir vėl įjunkite jį."
                            else -> "KLAIDA: ${ex?.message}"
                        }
                        
                        AlertDialog.Builder(context)
                            .setTitle("Spausdinimo problema")
                            .setMessage(errorMsg)
                            .setPositiveButton("Gerai", null)
                            .show()
                    }
                }
            }
            "usb" -> {
                Log.d(TAG, "USB Printing - Code: $cleanCode")
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        BrotherPrinterDriver.printBitmapUsb(context, barcodeBitmap)
                    }
                    if (result.isSuccess) {
                        val count = result.getOrNull() ?: 0
                        AlertDialog.Builder(context)
                            .setTitle("Spausdinimas sėkmingas")
                            .setMessage("Sėkmingai išsiųsta per USB.\nGeneruoti juodi taškai: $count")
                            .setPositiveButton("Gerai", null)
                            .show()
                    } else {
                        val ex = result.exceptionOrNull()
                        AlertDialog.Builder(context)
                            .setTitle("Spausdinimo per USB klaida")
                            .setMessage(ex?.message ?: "Nežinoma USB klaida")
                            .setPositiveButton("Gerai", null)
                            .show()
                    }
                }
            }
            "bluetooth" -> {
                val btDevice = sharedPrefs.getString("printer_bt", "") ?: ""
                Log.d(TAG, "Bluetooth Printing - Device: $btDevice, Code: $cleanCode")
                // Generic Bluetooth printing implementation (simulation)
                Toast.makeText(context, "Bluetooth Print ($btDevice) -> $cleanCode", Toast.LENGTH_LONG).show()
            }
            else -> {
                // Default System Print Spooler (opens Android print UI)
                try {
                    val printHelper = PrintHelper(context)
                    printHelper.scaleMode = PrintHelper.SCALE_MODE_FIT
                    printHelper.printBitmap("Atzymek tai - $cleanCode", barcodeBitmap)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to print barcode via system spooler", e)
                    Toast.makeText(context, "Spausdinimo klaida: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Prints a full group label.
     */
    fun printGroupLabel(context: Context, group: ProductGroup) {
        val sharedPrefs = context.getSharedPreferences("MarkItSettings", Context.MODE_PRIVATE)
        val rotationStr = sharedPrefs.getString("label_rotation", "0") ?: "0"
        val rotation = rotationStr.toIntOrNull() ?: 0

        val labelBitmap = BarcodeGenerator.generateLabel(
            group.name,
            group.code,
            group.barcode,
            group.description,
            rotation
        )
        printBarcode(context, group.name, labelBitmap)
    }

    /**
     * Prints a full item label.
     */
    fun printItemLabel(context: Context, groupName: String, code: String, barcode: String?, comment: String?) {
        val sharedPrefs = context.getSharedPreferences("MarkItSettings", Context.MODE_PRIVATE)
        val rotationStr = sharedPrefs.getString("label_rotation", "0") ?: "0"
        val rotation = rotationStr.toIntOrNull() ?: 0

        val labelBitmap = BarcodeGenerator.generateLabel(
            groupName,
            code,
            barcode,
            comment,
            rotation
        )
        printBarcode(context, code, labelBitmap)
    }
}
