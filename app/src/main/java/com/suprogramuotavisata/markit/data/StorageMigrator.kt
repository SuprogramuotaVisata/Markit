package com.suprogramuotavisata.markit.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object StorageMigrator {
    private const val TAG = "StorageMigrator"

    /**
     * Migrates all local data (groups and photo files) to Google Drive.
     */
    suspend fun migrateLocalToDrive(context: Context, onProgress: (String) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dbHelper = DatabaseHelper(context)
            val groups = dbHelper.getAllGroups()
            
            if (groups.isEmpty()) {
                return@withContext Result.success(Unit)
            }

            Log.d(TAG, "Starting migration: Local to Google Drive. Total groups: ${groups.size}")
            
            groups.forEachIndexed { groupIndex, group ->
                onProgress("Grupė (${groupIndex + 1}/${groups.size}): ${group.name}")
                
                // 1. Ensure folder exists in Drive
                val folderId = GoogleDriveService.findOrCreateCategoryFolder(context, group.name)
                if (folderId == null) {
                    throw Exception("Nepavyko sukurti Google Drive katalogo grupei: ${group.name}")
                }
                
                // Update SQLite drive folder reference
                dbHelper.updateGroupDriveId(group.id, folderId)

                // 2. Fetch all local items in this group
                val items = dbHelper.getAllItems(listOf(group.id))
                items.forEachIndexed { itemIndex, item ->
                    if (item.driveFileId.isNullOrBlank() && !item.localPhotoPath.isNullOrBlank()) {
                        val localFile = File(item.localPhotoPath)
                        if (localFile.exists()) {
                            onProgress("Užkraunama: ${group.name} (${itemIndex + 1}/${items.size})")
                            
                            val bytes = localFile.readBytes()
                            val fileName = localFile.name
                            
                            // Make filename contain the barcode code for later download parsing:
                            // Format: IMG_[timestamp]_CODE_[barcode].jpg
                            val cleanFileName = if (!fileName.contains("_CODE_")) {
                                val baseName = fileName.substringBeforeLast(".")
                                "${baseName}_CODE_${item.code}.jpg"
                            } else {
                                fileName
                            }

                            val driveFileId = GoogleDriveService.uploadPhoto(
                                context = context,
                                categoryName = group.name,
                                fileName = cleanFileName,
                                fileBytes = bytes
                            )
                            
                            if (driveFileId != null) {
                                dbHelper.updateItemDriveId(item.id, driveFileId)
                                // If the local file name has changed to match the CODE suffix format, update it
                                if (cleanFileName != fileName) {
                                    val newLocalFile = File(localFile.parentFile, cleanFileName)
                                    if (localFile.renameTo(newLocalFile)) {
                                        dbHelper.updateItemLocalPath(item.id, newLocalFile.absolutePath)
                                    }
                                }
                            } else {
                                Log.e(TAG, "Failed to upload photo for item: ${item.code}")
                            }
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "migrateLocalToDrive failed", e)
            Result.failure(e)
        }
    }

    /**
     * Migrates all category folders and photos from Google Drive back to local storage and SQLite.
     */
    suspend fun migrateDriveToLocal(context: Context, onProgress: (String) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dbHelper = DatabaseHelper(context)
            
            onProgress("Nuskaitomi Google Drive katalogai...")
            val folders = GoogleDriveService.listCategoryFolders(context)
            if (folders.isEmpty()) {
                return@withContext Result.success(Unit)
            }

            Log.d(TAG, "Starting migration: Google Drive to Local. Total folders: ${folders.size}")

            folders.forEachIndexed { folderIndex, (folderName, driveFolderId) ->
                onProgress("Katalogas (${folderIndex + 1}/${folders.size}): $folderName")

                // 1. Find or create group in local SQLite database
                var group = dbHelper.getGroupByName(folderName)
                val groupId = if (group == null) {
                    val newId = dbHelper.createGroup(folderName, driveFolderId)
                    group = dbHelper.getGroupById(newId)
                    newId
                } else {
                    if (group.driveFolderId.isNullOrBlank()) {
                        dbHelper.updateGroupDriveId(group.id, driveFolderId)
                    }
                    group.id
                }

                if (groupId <= 0) {
                    throw Exception("Nepavyko sukurti vietinės grupės duomenų bazėje: $folderName")
                }

                // 2. List all files inside the category folder in Google Drive
                val driveFiles = GoogleDriveService.listFilesInFolder(context, driveFolderId, folderName)
                
                driveFiles.forEachIndexed { fileIndex, (fileName, fileId, createdTime) ->
                    onProgress("Atsisiunčiama: $folderName (${fileIndex + 1}/${driveFiles.size})")

                    // Parse the barcode code from the filename suffix if present, otherwise fallback
                    // Format: IMG_[timestamp]_CODE_[barcode].jpg
                    val barcodeCode = if (fileName.contains("_CODE_")) {
                        fileName.substringAfter("_CODE_").substringBeforeLast(".")
                    } else {
                        // Fallback to filename prefix or timestamp code
                        fileName.substringBeforeLast(".").takeLast(8).uppercase()
                    }

                    // Check if this item is already registered in SQLite
                    val existingItems = dbHelper.getAllItems(listOf(groupId))
                    val alreadyExists = existingItems.any { it.driveFileId == fileId || it.localPhotoPath?.contains(fileName) == true }

                    if (!alreadyExists) {
                        // Download file bytes from Google Drive
                        val bytes = GoogleDriveService.downloadFile(context, fileId, folderName, fileName)
                        if (bytes != null) {
                            // Save photo locally to context.filesDir/photos/
                            val photosDir = File(context.filesDir, "photos")
                            if (!photosDir.exists()) photosDir.mkdirs()
                            
                            val localFile = File(photosDir, fileName)
                            localFile.writeBytes(bytes)

                            // Insert item record into local SQLite database
                            dbHelper.createItem(
                                groupId = groupId,
                                code = barcodeCode,
                                comment = "-", // Fallback comment placeholder
                                date = createdTime,
                                localPhotoPath = localFile.absolutePath,
                                driveFileId = fileId
                            )
                        } else {
                            Log.e(TAG, "Failed to download file from Drive: $fileId ($fileName)")
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "migrateDriveToLocal failed", e)
            Result.failure(e)
        }
    }
}
