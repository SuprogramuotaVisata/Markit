package com.suprogramuotavisata.markit.data.sync

import android.util.Log
import com.suprogramuotavisata.markit.data.ProductGroup
import com.suprogramuotavisata.markit.data.ProductItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL

class SuprogramuotaVisataSyncProvider : SyncProvider {
    override val id: String = "suprogramuota_visata"
    override val name: String = "Suprogramuota Visata"

    override suspend fun syncGroup(group: ProductGroup, apiUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (apiUrl.isBlank()) return@withContext Result.failure(Exception("API URL is blank"))
            
            val endpoint = if (apiUrl.endsWith("/")) "${apiUrl}sync/group" else "$apiUrl/sync/group"
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val json = JSONObject().apply {
                put("id", group.id)
                put("name", group.name)
            }

            connection.outputStream.use { os ->
                os.write(json.toString().toByteArray())
            }

            val responseCode = connection.responseCode
            connection.disconnect()

            if (responseCode in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP Server returned error code: $responseCode"))
            }
        } catch (e: Exception) {
            Log.e("SVSync", "Error syncing group: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun syncItem(item: ProductItem, groupName: String, apiUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (apiUrl.isBlank()) return@withContext Result.failure(Exception("API URL is blank"))

            val endpoint = if (apiUrl.endsWith("/")) "${apiUrl}sync/product" else "$apiUrl/sync/product"
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            
            val boundary = "Boundary-${System.currentTimeMillis()}"
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            connection.outputStream.use { outStream ->
                val writer = PrintWriter(OutputStreamWriter(outStream, "UTF-8"), true)

                // Helper to write form field
                fun writeFormField(name: String, value: String) {
                    writer.append("--$boundary").append("\r\n")
                    writer.append("Content-Disposition: form-data; name=\"$name\"").append("\r\n")
                    writer.append("Content-Type: text/plain; charset=UTF-8").append("\r\n\r\n")
                    writer.append(value).append("\r\n")
                    writer.flush()
                }

                writeFormField("code", item.code)
                writeFormField("barcode", item.barcode ?: "")
                writeFormField("comment", item.comment ?: "")
                writeFormField("date_captured", item.date)
                writeFormField("group_name", groupName)

                // Write photo file if available
                val photoPath = item.localPhotoPath
                if (!photoPath.isNullOrBlank()) {
                    val file = File(photoPath)
                    if (file.exists()) {
                        writer.append("--$boundary").append("\r\n")
                        writer.append("Content-Disposition: form-data; name=\"photo\"; filename=\"${file.name}\"").append("\r\n")
                        writer.append("Content-Type: image/jpeg").append("\r\n\r\n")
                        writer.flush()

                        FileInputStream(file).use { input ->
                            input.copyTo(outStream)
                        }
                        outStream.flush()
                        writer.append("\r\n")
                        writer.flush()
                    }
                }

                // Close boundary
                writer.append("--$boundary--").append("\r\n")
                writer.flush()
            }

            val responseCode = connection.responseCode
            connection.disconnect()

            if (responseCode in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP Server returned error code: $responseCode"))
            }
        } catch (e: Exception) {
            Log.e("SVSync", "Error syncing item: ${e.message}", e)
            Result.failure(e)
        }
    }
}
