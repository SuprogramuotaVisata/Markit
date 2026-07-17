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
        val promptForCopies = sharedPrefs.getBoolean("prompt_for_copies", false)
        val printerType = sharedPrefs.getString("printer_type", "system") ?: "system"
        val cleanCode = TranslationManager.stripAccents(code)

        if (promptForCopies && (printerType == "network" || printerType == "usb")) {
            // Show alert dialog to enter copies count
            val input = android.widget.EditText(context).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setText("1")
                setSelection(0, 1)
            }
            
            // Set padding to look nice in the dialog
            val container = android.widget.FrameLayout(context)
            val params = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = (24 * context.resources.displayMetrics.density).toInt()
                rightMargin = (24 * context.resources.displayMetrics.density).toInt()
                topMargin = (8 * context.resources.displayMetrics.density).toInt()
                bottomMargin = (8 * context.resources.displayMetrics.density).toInt()
            }
            input.layoutParams = params
            container.addView(input)

            AlertDialog.Builder(context)
                .setTitle("Pasirinkite kopijų skaičių")
                .setView(container)
                .setPositiveButton("Spausdinti") { _, _ ->
                    val copiesVal = input.text.toString().toIntOrNull() ?: 1
                    executePrint(context, cleanCode, barcodeBitmap, copiesVal.coerceAtLeast(1))
                }
                .setNegativeButton("Atšaukti", null)
                .show()
        } else {
            val copies = sharedPrefs.getInt("print_copies", 1).coerceAtLeast(1)
            executePrint(context, cleanCode, barcodeBitmap, copies)
        }
    }

    private fun executePrint(context: Context, cleanCode: String, barcodeBitmap: Bitmap, copies: Int) {
        val sharedPrefs = context.getSharedPreferences("MarkItSettings", Context.MODE_PRIVATE)
        val printerType = sharedPrefs.getString("printer_type", "system") ?: "system"
        val printerBrand = sharedPrefs.getString("printer_brand", "brother") ?: "brother"

        when (printerType) {
            "network" -> {
                val ip = sharedPrefs.getString("printer_ip", "") ?: ""
                val portStr = sharedPrefs.getString("printer_port", "9100") ?: "9100"
                val port = portStr.toIntOrNull() ?: 9100
                
                Log.d(TAG, "Network Printing - Target IP: $ip, Port: $port, Code: $cleanCode, Copies: $copies")
                
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

                        Log.d(TAG, "Naudojamas spausdintuvo IP: $finalIp, Brand: $printerBrand")
                        var lastResult: Result<Int> = Result.success(0)
                        for (i in 0 until copies) {
                            lastResult = if (printerBrand == "xprinter") {
                                EscPosPrinterDriver.printBitmap(context, finalIp, port, barcodeBitmap)
                            } else {
                                BrotherPrinterDriver.printBitmap(context, finalIp, port, barcodeBitmap)
                            }
                            if (lastResult.isFailure) break
                            if (copies > 1 && i < copies - 1) {
                                try { Thread.sleep(500) } catch (e: Exception) {}
                            }
                        }
                        lastResult
                    }
                    if (result.isSuccess) {
                        Toast.makeText(context, "Sėkmingai įvykdyta", Toast.LENGTH_SHORT).show()
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
                Log.d(TAG, "USB Printing - Code: $cleanCode, Copies: $copies, Brand: $printerBrand")
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        var lastResult: Result<Int> = Result.success(0)
                        for (i in 0 until copies) {
                            lastResult = if (printerBrand == "xprinter") {
                                EscPosPrinterDriver.printBitmapUsb(context, barcodeBitmap)
                            } else {
                                BrotherPrinterDriver.printBitmapUsb(context, barcodeBitmap)
                            }
                            if (lastResult.isFailure) break
                            if (copies > 1 && i < copies - 1) {
                                try { Thread.sleep(500) } catch (e: Exception) {}
                            }
                        }
                        lastResult
                    }
                    if (result.isSuccess) {
                        Toast.makeText(context, "Sėkmingai įvykdyta", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "Bluetooth Print ($btDevice) -> $cleanCode", Toast.LENGTH_LONG).show()
            }
            else -> {
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
        val qrOnly = sharedPrefs.getBoolean("print_qr_only", false)
        val tapeWidth = sharedPrefs.getInt("printer_tape_width", 24)
        val targetWidth = when (tapeWidth) {
            18 -> 112
            12 -> 74
            9  -> 50
            6  -> 32
            else -> 128
        }

        val labelBitmap = BarcodeGenerator.generateLabel(
            group.name,
            group.code,
            group.barcode,
            group.description,
            rotation,
            qrOnly,
            targetWidth
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
        val qrOnly = sharedPrefs.getBoolean("print_qr_only", false)
        val tapeWidth = sharedPrefs.getInt("printer_tape_width", 24)
        val targetWidth = when (tapeWidth) {
            18 -> 112
            12 -> 74
            9  -> 50
            6  -> 32
            else -> 128
        }

        val labelBitmap = BarcodeGenerator.generateLabel(
            groupName,
            code,
            barcode,
            comment,
            rotation,
            qrOnly,
            targetWidth
        )
        printBarcode(context, code, labelBitmap)
    }
}
