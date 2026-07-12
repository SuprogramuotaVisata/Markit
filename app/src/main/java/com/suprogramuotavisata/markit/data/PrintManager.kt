package com.suprogramuotavisata.markit.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
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
                        BrotherPrinterDriver.printBitmap(context, ip, port, barcodeBitmap)
                    }
                    if (result.isSuccess) {
                        Toast.makeText(context, "Sėkmingai išsiųsta į spausdintuvą", Toast.LENGTH_SHORT).show()
                    } else {
                        val ex = result.exceptionOrNull()
                        val errorMsg = when {
                            ex is java.net.ConnectException -> "Nepavyko prisijungti. Patikrinkite ar telefonas prisijungęs prie spausdintuvo Wi-Fi tinklo (IP: $ip)."
                            ex is java.net.SocketTimeoutException -> "Baigėsi laukimo laikas. Spausdintuvas neatsako."
                            else -> ex?.message ?: "Nežinoma ryšio klaida"
                        }
                        Toast.makeText(context, "Spausdinimo klaida: $errorMsg", Toast.LENGTH_LONG).show()
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
        val labelBitmap = BarcodeGenerator.generateLabel(
            group.name,
            group.code,
            group.barcode,
            group.description
        )
        printBarcode(context, group.name, labelBitmap)
    }
}
