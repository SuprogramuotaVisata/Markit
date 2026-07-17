package com.suprogramuotavisata.markit.data

import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object LicensingManager {
    private const val PREFS_NAME = "MarkItLicensePrefs"
    private const val KEY_ACTIVATED = "is_activated"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_VALID_UNTIL = "valid_until"

    private const val VALIDATION_URL = "https://ktor-regapi-1046068691869.europe-west3.run.app/api/keys/validate"
    private const val REGISTRY_BASE_URL = "https://ktor-regapi-1046068691869.europe-west3.run.app"

    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device_id"
    }

    fun getLocalHostUrl(context: Context): String? {
        val sharedPrefs = context.getSharedPreferences("MarkItSettings", Context.MODE_PRIVATE)
        val host = sharedPrefs.getString("local_host_url", "")
        return if (host.isNullOrBlank()) null else host
    }

    fun setLocalHostUrl(context: Context, hostUrl: String?) {
        val sharedPrefs = context.getSharedPreferences("MarkItSettings", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("local_host_url", hostUrl?.trim()).apply()
        if (hostUrl.isNullOrBlank()) {
            // Also reset api sync url to default / empty if disabled
            val settingsPrefs = context.getSharedPreferences("MarkItSettings", Context.MODE_PRIVATE)
            settingsPrefs.edit().putString("api_sync_type", "disabled").putString("api_sync_url", "").apply()
        } else {
            // Automatically set api sync type and url to local server terminalas sync endpoint
            val cleanHost = if (!hostUrl.startsWith("http://") && !hostUrl.startsWith("https://")) {
                "http://$hostUrl"
            } else {
                hostUrl
            }
            val syncUrl = if (cleanHost.endsWith("/")) "${cleanHost}api/terminalas" else "$cleanHost/api/terminalas"
            val settingsPrefs = context.getSharedPreferences("MarkItSettings", Context.MODE_PRIVATE)
            settingsPrefs.edit().putString("api_sync_type", "suprogramuota_visata").putString("api_sync_url", syncUrl).apply()
        }
    }

    fun isActivated(context: Context): Boolean {
        val host = getLocalHostUrl(context)
        if (host != null) {
            return true
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val activated = prefs.getBoolean(KEY_ACTIVATED, false)
        val validUntil = prefs.getLong(KEY_VALID_UNTIL, 0L)
        
        if (activated && validUntil > 0L && System.currentTimeMillis() > validUntil) {
            prefs.edit().putBoolean(KEY_ACTIVATED, false).apply()
            return false
        }
        return activated
    }

    fun getSavedKey(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_API_KEY, null)
    }

    fun getValidUntil(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(KEY_VALID_UNTIL, 0L)
    }

    suspend fun registerDeviceOnRegistryServer(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val deviceId = getDeviceId(context)
            val deviceName = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL
            val url = URL("$REGISTRY_BASE_URL/api/keys/register-device")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val json = JSONObject().apply {
                put("deviceId", deviceId)
                put("deviceName", deviceName)
            }

            connection.outputStream.use { os ->
                OutputStreamWriter(os, "UTF-8").use { writer ->
                    writer.write(json.toString())
                    writer.flush()
                }
            }

            val responseCode = connection.responseCode
            connection.disconnect()
            if (responseCode in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP error registering device: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkDeviceAssignedKey(context: Context): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val deviceId = getDeviceId(context)
            val url = URL("$REGISTRY_BASE_URL/api/keys/check-device?deviceId=$deviceId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val apiKey = json.optString("apiKey", "")
                Result.success(if (apiKey.isBlank() || apiKey == "null") null else apiKey)
            } else {
                Result.failure(Exception("HTTP error checking assigned key: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkInLocalServer(context: Context, hostUrl: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanHost = if (!hostUrl.startsWith("http://") && !hostUrl.startsWith("https://")) {
                "http://$hostUrl"
            } else {
                hostUrl
            }
            val endpoint = if (cleanHost.endsWith("/")) "${cleanHost}api/terminalas/check-in" else "$cleanHost/api/terminalas/check-in"
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 6000
            connection.readTimeout = 6000

            val deviceId = getDeviceId(context)
            val deviceName = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL
            val json = JSONObject().apply {
                put("deviceId", deviceId)
                put("deviceName", deviceName)
            }

            connection.outputStream.use { os ->
                OutputStreamWriter(os, "UTF-8").use { writer ->
                    writer.write(json.toString())
                    writer.flush()
                }
            }

            val responseCode = connection.responseCode
            connection.disconnect()
            if (responseCode in 200..299) {
                setLocalHostUrl(context, hostUrl)
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                    putBoolean(KEY_ACTIVATED, true)
                    putString(KEY_API_KEY, "local_mode")
                    putLong(KEY_VALID_UNTIL, System.currentTimeMillis() + 24L * 60 * 60 * 1000)
                    apply()
                }
                Result.success(true)
            } else if (responseCode == 403) {
                Result.success(false)
            } else {
                Result.failure(Exception("HTTP error $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun activate(context: Context, key: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val deviceId = getDeviceId(context)
            val url = URL(VALIDATION_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            val json = JSONObject().apply {
                put("apiKey", key)
                put("instanceId", deviceId)
            }

            connection.outputStream.use { os ->
                OutputStreamWriter(os, "UTF-8").use { writer ->
                    writer.write(json.toString())
                    writer.flush()
                }
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(responseText)
                val isValid = responseJson.optBoolean("valid", false)
                
                if (isValid) {
                    val validUntil = responseJson.optLong("validUntil", 0L)
                    
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                        putBoolean(KEY_ACTIVATED, true)
                        putString(KEY_API_KEY, key)
                        putLong(KEY_VALID_UNTIL, validUntil)
                        apply()
                    }
                    Result.success(true)
                } else {
                    Result.success(false)
                }
            } else {
                Result.failure(Exception("HTTP error: $responseCode"))
            }
        } catch (e: Exception) {
            Log.e("LicensingManager", "Error during activation: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun verifyActiveLicense(context: Context): Boolean = withContext(Dispatchers.IO) {
        val host = getLocalHostUrl(context)
        if (host != null) {
            val result = checkInLocalServer(context, host)
            return@withContext result.getOrDefault(true)
        }

        val savedKey = getSavedKey(context) ?: return@withContext false
        val deviceId = getDeviceId(context)
        Log.d("LicensingManager", "Programos paleidimas – tikrinamas licencijos raktas: $savedKey")

        try {
            val url = URL(VALIDATION_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val json = JSONObject().apply {
                put("apiKey", savedKey)
                put("instanceId", deviceId)
            }

            connection.outputStream.use { os ->
                OutputStreamWriter(os, "UTF-8").use { writer ->
                    writer.write(json.toString())
                    writer.flush()
                }
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(responseText)
                val isValid = responseJson.optBoolean("valid", false)
                val validUntil = responseJson.optLong("validUntil", 0L)

                if (isValid) {
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                        putBoolean(KEY_ACTIVATED, true)
                        putLong(KEY_VALID_UNTIL, validUntil)
                        apply()
                    }
                    true
                } else {
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                        putBoolean(KEY_ACTIVATED, false)
                        apply()
                    }
                    false
                }
            } else {
                isActivated(context)
            }
        } catch (e: Exception) {
            isActivated(context)
        }
    }
    
    fun clearLicense(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        setLocalHostUrl(context, null)
    }
}

