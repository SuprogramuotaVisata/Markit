package com.suprogramuotavisata.markit.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.print.PrintHelper

object PrintManager {
    private const val TAG = "PrintManager"

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
                val port = sharedPrefs.getString("printer_port", "9100") ?: "9100"
                Log.d(TAG, "Network Printing - Target IP: $ip, Port: $port, Code: $cleanCode")
                // Generic ESC/POS Wi-Fi printing implementation (simulation)
                Toast.makeText(context, "Network Print (IP: $ip, Port: $port) -> $cleanCode", Toast.LENGTH_LONG).show()
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
}
