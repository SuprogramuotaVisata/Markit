package com.suprogramuotavisata.markit.data

import android.accounts.Account
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

object GoogleDriveService {
    private const val TAG = "GoogleDriveService"
    private const val PREFS_NAME = "MarkItAuthPrefs"
    private const val KEY_AUTH_MODE = "auth_mode"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_GOOGLE_ACCOUNT = "google_account_name"

    const val MODE_NONE = "none"
    const val MODE_MOCK = "mock"
    const val MODE_GOOGLE = "google"

    // --- Configuration API ---

    fun getAuthMode(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_AUTH_MODE, MODE_NONE) ?: MODE_NONE
    }

    fun setAuthMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_AUTH_MODE, mode)
            .apply()
    }

    fun getUserEmail(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_EMAIL, null)
    }

    fun setUserEmail(context: Context, email: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_EMAIL, email)
            .apply()
    }

    fun getGoogleAccountName(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_GOOGLE_ACCOUNT, null)
    }

    fun setGoogleAccountName(context: Context, name: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GOOGLE_ACCOUNT, name)
            .apply()
    }

    fun logout(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_AUTH_MODE)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_GOOGLE_ACCOUNT)
            .apply()
    }

    // --- Google Drive REST API Calls ---

    private suspend fun getAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        val accountName = getGoogleAccountName(context) ?: return@withContext null
        try {
            // Scope for files created or opened by the app
            val scope = "oauth2:https://www.googleapis.com/auth/drive.file"
            val account = Account(accountName, "com.google")
            GoogleAuthUtil.getToken(context, account, scope)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get access token", e)
            null
        }
    }

    /**
     * Finds or creates the app folder "MarkIt" on Google Drive.
     */
    suspend fun findOrCreateAppFolder(context: Context): String? = withContext(Dispatchers.IO) {
        val mode = getAuthMode(context)
        if (mode == MODE_MOCK) {
            val mockDir = File(context.filesDir, "mock_drive")
            if (!mockDir.exists()) mockDir.mkdirs()
            return@withContext "mock_root_folder_id"
        }
        if (mode != MODE_GOOGLE) return@withContext null

        val token = getAccessToken(context) ?: return@withContext null
        
        // 1. Search for folder
        val query = URLEncoder.encode("name = 'MarkIt' and mimeType = 'application/vnd.google-apps.folder' and trashed = false", "UTF-8")
        val searchUrl = "https://www.googleapis.com/drive/v3/files?q=$query"
        
        try {
            val response = httpGet(searchUrl, token)
            val json = JSONObject(response)
            val files = json.getJSONArray("files")
            if (files.length() > 0) {
                return@withContext files.getJSONObject(0).getString("id")
            }
            
            // 2. Not found, create it
            val createUrl = "https://www.googleapis.com/drive/v3/files"
            val body = JSONObject().apply {
                put("name", "MarkIt")
                put("mimeType", "application/vnd.google-apps.folder")
            }.toString()
            
            val createResponse = httpPost(createUrl, token, body)
            val createJson = JSONObject(createResponse)
            createJson.getString("id")
        } catch (e: Exception) {
            Log.e(TAG, "findOrCreateAppFolder failed", e)
            null
        }
    }

    /**
     * Finds or creates a category subfolder inside the MarkIt app folder.
     */
    suspend fun findOrCreateCategoryFolder(context: Context, categoryName: String): String? = withContext(Dispatchers.IO) {
        val mode = getAuthMode(context)
        if (mode == MODE_MOCK) {
            val catDir = File(File(context.filesDir, "mock_drive"), categoryName)
            if (!catDir.exists()) catDir.mkdirs()
            return@withContext "mock_category_${categoryName}_id"
        }
        if (mode != MODE_GOOGLE) return@withContext null

        val parentId = findOrCreateAppFolder(context) ?: return@withContext null
        val token = getAccessToken(context) ?: return@withContext null

        val query = URLEncoder.encode("name = '$categoryName' and '$parentId' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false", "UTF-8")
        val searchUrl = "https://www.googleapis.com/drive/v3/files?q=$query"

        try {
            val response = httpGet(searchUrl, token)
            val json = JSONObject(response)
            val files = json.getJSONArray("files")
            if (files.length() > 0) {
                return@withContext files.getJSONObject(0).getString("id")
            }

            // Not found, create category folder
            val createUrl = "https://www.googleapis.com/drive/v3/files"
            val body = JSONObject().apply {
                put("name", categoryName)
                put("mimeType", "application/vnd.google-apps.folder")
                put("parents", org.json.JSONArray().put(parentId))
            }.toString()

            val createResponse = httpPost(createUrl, token, body)
            val createJson = JSONObject(createResponse)
            createJson.getString("id")
        } catch (e: Exception) {
            Log.e(TAG, "findOrCreateCategoryFolder failed", e)
            null
        }
    }

    /**
     * Uploads the photo bitmap bytes to Google Drive inside the category subfolder.
     */
    suspend fun uploadPhoto(
        context: Context,
        categoryName: String,
        fileName: String,
        fileBytes: ByteArray
    ): String? = withContext(Dispatchers.IO) {
        val mode = getAuthMode(context)
        if (mode == MODE_MOCK) {
            // Write bytes locally to mock directory
            val catDir = File(File(context.filesDir, "mock_drive"), categoryName)
            if (!catDir.exists()) catDir.mkdirs()
            val file = File(catDir, fileName)
            file.writeBytes(fileBytes)
            return@withContext "mock_file_${UUID.randomUUID()}"
        }
        if (mode != MODE_GOOGLE) return@withContext null

        val categoryFolderId = findOrCreateCategoryFolder(context, categoryName) ?: return@withContext null
        val token = getAccessToken(context) ?: return@withContext null

        try {
            // 1. Create file metadata in Google Drive (returns File ID)
            val metaUrl = "https://www.googleapis.com/drive/v3/files"
            val metaBody = JSONObject().apply {
                put("name", fileName)
                put("parents", org.json.JSONArray().put(categoryFolderId))
            }.toString()

            val metaResponse = httpPost(metaUrl, token, metaBody)
            val fileId = JSONObject(metaResponse).getString("id")

            // 2. Upload binary content (media) via PATCH request
            val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media"
            val uploadResponse = httpPatchMedia(uploadUrl, token, fileBytes, "image/jpeg")
            
            if (uploadResponse != null) {
                Log.d(TAG, "Successfully uploaded photo: $fileId")
                fileId
            } else {
                Log.e(TAG, "Failed to upload photo content for file ID: $fileId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "uploadPhoto failed", e)
            null
        }
    }

    /**
     * Lists all category folders under the app's MarkIt folder on Google Drive.
     * Returns a list of Pair(folderName, folderId).
     */
    suspend fun listCategoryFolders(context: Context): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val mode = getAuthMode(context)
        if (mode == MODE_MOCK) {
            val mockDir = File(context.filesDir, "mock_drive")
            if (!mockDir.exists()) return@withContext emptyList()
            return@withContext mockDir.listFiles { f -> f.isDirectory }?.map { it.name to "mock_category_${it.name}_id" } ?: emptyList()
        }
        if (mode != MODE_GOOGLE) return@withContext emptyList()

        val parentId = findOrCreateAppFolder(context) ?: return@withContext emptyList()
        val token = getAccessToken(context) ?: return@withContext emptyList()

        val query = URLEncoder.encode("'$parentId' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false", "UTF-8")
        val searchUrl = "https://www.googleapis.com/drive/v3/files?q=$query"

        try {
            val response = httpGet(searchUrl, token)
            val json = JSONObject(response)
            val files = json.getJSONArray("files")
            val result = mutableListOf<Pair<String, String>>()
            for (i in 0 until files.length()) {
                val f = files.getJSONObject(i)
                result.add(f.getString("name") to f.getString("id"))
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "listCategoryFolders failed", e)
            emptyList()
        }
    }

    /**
     * Lists all files inside a specific Google Drive folder.
     * Returns a list of Triple(fileName, fileId, fileCreatedTime).
     */
    suspend fun listFilesInFolder(context: Context, folderId: String, folderName: String): List<Triple<String, String, String>> = withContext(Dispatchers.IO) {
        val mode = getAuthMode(context)
        if (mode == MODE_MOCK) {
            val catDir = File(File(context.filesDir, "mock_drive"), folderName)
            if (!catDir.exists()) return@withContext emptyList()
            return@withContext catDir.listFiles { f -> f.isFile }?.map { 
                val timeStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(it.lastModified()))
                Triple(it.name, "mock_file_${it.name}_id", timeStr)
            } ?: emptyList()
        }
        if (mode != MODE_GOOGLE) return@withContext emptyList()

        val token = getAccessToken(context) ?: return@withContext emptyList()
        val query = URLEncoder.encode("'$folderId' in parents and mimeType != 'application/vnd.google-apps.folder' and trashed = false", "UTF-8")
        val url = "https://www.googleapis.com/drive/v3/files?q=$query&fields=files(id,name,createdTime)"

        try {
            val response = httpGet(url, token)
            val json = JSONObject(response)
            val files = json.getJSONArray("files")
            val result = mutableListOf<Triple<String, String, String>>()
            for (i in 0 until files.length()) {
                val f = files.getJSONObject(i)
                val rawTime = f.optString("createdTime", "")
                val cleanTime = if (rawTime.length >= 19) {
                    rawTime.substring(0, 19).replace("T", " ")
                } else {
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                }
                result.add(Triple(f.getString("name"), f.getString("id"), cleanTime))
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "listFilesInFolder failed", e)
            emptyList()
        }
    }

    /**
     * Downloads file bytes from Google Drive.
     */
    suspend fun downloadFile(context: Context, fileId: String, folderName: String, fileName: String): ByteArray? = withContext(Dispatchers.IO) {
        val mode = getAuthMode(context)
        if (mode == MODE_MOCK) {
            val catDir = File(File(context.filesDir, "mock_drive"), folderName)
            val file = File(catDir, fileName)
            if (file.exists()) {
                return@withContext file.readBytes()
            }
            return@withContext null
        }
        if (mode != MODE_GOOGLE) return@withContext null

        val token = getAccessToken(context) ?: return@withContext null
        val urlString = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"

        try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connect()

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                conn.inputStream.use { it.readBytes() }
            } else {
                val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "Failed downloadFile $fileId: $responseCode - $error")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "downloadFile failed", e)
            null
        }
    }

    // --- HTTP Helper Methods ---

    private fun httpGet(urlString: String, token: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Accept", "application/json")
        conn.connect()

        val responseCode = conn.responseCode
        if (responseCode in 200..299) {
            return conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw Exception("HTTP GET failed with code $responseCode: $error")
        }
    }

    private fun httpPost(urlString: String, token: String, jsonBody: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.setRequestProperty("Accept", "application/json")
        conn.doOutput = true
        conn.outputStream.use { os ->
            os.write(jsonBody.toByteArray(Charsets.UTF_8))
        }

        val responseCode = conn.responseCode
        if (responseCode in 200..299) {
            return conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw Exception("HTTP POST failed with code $responseCode: $error")
        }
    }

    private fun httpPatchMedia(urlString: String, token: String, content: ByteArray, contentType: String): String? {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "PATCH"
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Content-Type", contentType)
        conn.setRequestProperty("Content-Length", content.size.toString())
        conn.doOutput = true
        conn.outputStream.use { os ->
            os.write(content)
        }

        val responseCode = conn.responseCode
        if (responseCode in 200..299) {
            return conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            Log.e(TAG, "HTTP PATCH media failed with code $responseCode: $error")
            return null
        }
    }
}
