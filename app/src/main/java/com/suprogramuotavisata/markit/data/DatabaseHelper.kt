package com.suprogramuotavisata.markit.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

data class ProductGroup(
    val id: Long = 0,
    val name: String,
    val driveFolderId: String? = null,
    val code: String = "",
    val barcode: String? = null,
    val padetaTen: String? = null,
    val description: String? = null
)

data class ProductItem(
    val id: Long = 0,
    val groupId: Long,
    val code: String,
    val comment: String?,
    val date: String,
    val localPhotoPath: String?,
    val driveFileId: String? = null,
    val groupName: String = "",
    val barcode: String? = null
)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "com.suprogramuotavisata.markit.db"
        private const val DATABASE_VERSION = 2

        // Table Groups
        private const val TABLE_GROUPS = "product_groups"
        private const val KEY_GROUP_ID = "id"
        private const val KEY_GROUP_NAME = "name"
        private const val KEY_GROUP_DRIVE_ID = "drive_folder_id"
        private const val KEY_GROUP_CODE = "code"
        private const val KEY_GROUP_BARCODE = "barcode"
        private const val KEY_GROUP_PADETA_TEN = "padeta_ten"
        private const val KEY_GROUP_DESCRIPTION = "description"

        // Table Items
        private const val TABLE_ITEMS = "items"
        private const val KEY_ITEM_ID = "id"
        private const val KEY_ITEM_GROUP_ID = "group_id"
        private const val KEY_ITEM_CODE = "code"
        private const val KEY_ITEM_COMMENT = "comment"
        private const val KEY_ITEM_DATE = "date"
        private const val KEY_ITEM_LOCAL_PATH = "local_photo_path"
        private const val KEY_ITEM_DRIVE_ID = "drive_file_id"
        private const val KEY_ITEM_BARCODE = "barcode"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createGroupsTable = ("CREATE TABLE " + TABLE_GROUPS + "("
                + KEY_GROUP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_GROUP_NAME + " TEXT NOT NULL UNIQUE,"
                + KEY_GROUP_DRIVE_ID + " TEXT,"
                + KEY_GROUP_CODE + " TEXT NOT NULL DEFAULT '',"
                + KEY_GROUP_BARCODE + " TEXT,"
                + KEY_GROUP_PADETA_TEN + " TEXT,"
                + KEY_GROUP_DESCRIPTION + " TEXT" + ")")

        val createItemsTable = ("CREATE TABLE " + TABLE_ITEMS + "("
                + KEY_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_ITEM_GROUP_ID + " INTEGER NOT NULL,"
                + KEY_ITEM_CODE + " TEXT NOT NULL,"
                + KEY_ITEM_COMMENT + " TEXT,"
                + KEY_ITEM_DATE + " TEXT NOT NULL,"
                + KEY_ITEM_LOCAL_PATH + " TEXT,"
                + KEY_ITEM_DRIVE_ID + " TEXT,"
                + KEY_ITEM_BARCODE + " TEXT,"
                + "FOREIGN KEY(" + KEY_ITEM_GROUP_ID + ") REFERENCES " + TABLE_GROUPS + "(" + KEY_GROUP_ID + ") ON DELETE CASCADE" + ")")

        db.execSQL(createGroupsTable)
        db.execSQL(createItemsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_GROUPS ADD COLUMN $KEY_GROUP_CODE TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE $TABLE_GROUPS ADD COLUMN $KEY_GROUP_BARCODE TEXT")
                db.execSQL("ALTER TABLE $TABLE_GROUPS ADD COLUMN $KEY_GROUP_PADETA_TEN TEXT")
                db.execSQL("ALTER TABLE $TABLE_GROUPS ADD COLUMN $KEY_GROUP_DESCRIPTION TEXT")
                db.execSQL("ALTER TABLE $TABLE_ITEMS ADD COLUMN $KEY_ITEM_BARCODE TEXT")
                
                // Backfill unique codes for existing groups if any
                val cursor = db.rawQuery("SELECT $KEY_GROUP_ID FROM $TABLE_GROUPS", null)
                cursor.use {
                    if (it.moveToFirst()) {
                        val idCol = it.getColumnIndexOrThrow(KEY_GROUP_ID)
                        do {
                            val id = it.getLong(idCol)
                            val uniqueCode = java.util.UUID.randomUUID().toString().take(8).uppercase()
                            db.execSQL("UPDATE $TABLE_GROUPS SET $KEY_GROUP_CODE = ? WHERE $KEY_GROUP_ID = ?", arrayOf<Any>(uniqueCode, id))
                        } while (it.moveToNext())
                    }
                }
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Database migration failed, recreating tables", e)
                db.execSQL("DROP TABLE IF EXISTS $TABLE_ITEMS")
                db.execSQL("DROP TABLE IF EXISTS $TABLE_GROUPS")
                onCreate(db)
            }
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    // --- Groups API ---

    fun createGroup(
        name: String,
        driveFolderId: String? = null,
        code: String = "",
        barcode: String? = null,
        thereIs: String? = null,
        description: String? = null
    ): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_GROUP_NAME, name)
            put(KEY_GROUP_DRIVE_ID, driveFolderId)
            put(KEY_GROUP_CODE, code)
            put(KEY_GROUP_BARCODE, barcode)
            put(KEY_GROUP_PADETA_TEN, thereIs)
            put(KEY_GROUP_DESCRIPTION, description)
        }
        return db.insertWithOnConflict(TABLE_GROUPS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun updateGroupDriveId(groupId: Long, driveFolderId: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_GROUP_DRIVE_ID, driveFolderId)
        }
        return db.update(TABLE_GROUPS, values, "$KEY_GROUP_ID = ?", arrayOf(groupId.toString())) > 0
    }

    fun getAllGroups(): List<ProductGroup> {
        val list = mutableListOf<ProductGroup>()
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_GROUPS ORDER BY $KEY_GROUP_NAME ASC"
        val cursor = db.rawQuery(selectQuery, null)

        cursor.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndexOrThrow(KEY_GROUP_ID)
                val nameIndex = it.getColumnIndexOrThrow(KEY_GROUP_NAME)
                val driveIndex = it.getColumnIndexOrThrow(KEY_GROUP_DRIVE_ID)
                val codeIndex = it.getColumnIndexOrThrow(KEY_GROUP_CODE)
                val barcodeIndex = it.getColumnIndexOrThrow(KEY_GROUP_BARCODE)
                val padetaTenIndex = it.getColumnIndexOrThrow(KEY_GROUP_PADETA_TEN)
                val descIndex = it.getColumnIndexOrThrow(KEY_GROUP_DESCRIPTION)
                do {
                    list.add(
                        ProductGroup(
                            id = it.getLong(idIndex),
                            name = it.getString(nameIndex),
                            driveFolderId = it.getString(driveIndex),
                            code = it.getString(codeIndex) ?: "",
                            barcode = it.getString(barcodeIndex),
                            padetaTen = it.getString(padetaTenIndex),
                            description = it.getString(descIndex)
                        )
                    )
                } while (it.moveToNext())
            }
        }
        return list
    }

    fun getGroupById(id: Long): ProductGroup? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_GROUPS,
            null,
            "$KEY_GROUP_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return ProductGroup(
                    id = it.getLong(it.getColumnIndexOrThrow(KEY_GROUP_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(KEY_GROUP_NAME)),
                    driveFolderId = it.getString(it.getColumnIndexOrThrow(KEY_GROUP_DRIVE_ID)),
                    code = it.getString(it.getColumnIndexOrThrow(KEY_GROUP_CODE)) ?: "",
                    barcode = it.getString(it.getColumnIndexOrThrow(KEY_GROUP_BARCODE)),
                    padetaTen = it.getString(it.getColumnIndexOrThrow(KEY_GROUP_PADETA_TEN)),
                    description = it.getString(it.getColumnIndexOrThrow(KEY_GROUP_DESCRIPTION))
                )
            }
        }
        return null
    }

    fun getGroupByName(name: String): ProductGroup? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_GROUPS,
            null,
            "$KEY_GROUP_NAME = ?",
            arrayOf(name),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return ProductGroup(
                    id = it.getLong(it.getColumnIndexOrThrow(KEY_GROUP_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(KEY_GROUP_NAME)),
                    driveFolderId = it.getString(it.getColumnIndexOrThrow(KEY_GROUP_DRIVE_ID)),
                    code = it.getString(it.getColumnIndexOrThrow(KEY_GROUP_CODE)) ?: "",
                    barcode = it.getString(it.getColumnIndexOrThrow(KEY_GROUP_BARCODE)),
                    padetaTen = it.getString(it.getColumnIndexOrThrow(KEY_GROUP_PADETA_TEN)),
                    description = it.getString(it.getColumnIndexOrThrow(KEY_GROUP_DESCRIPTION))
                )
            }
        }
        return null
    }

    // --- Items API ---

    fun createItem(
        groupId: Long,
        code: String,
        comment: String?,
        date: String,
        localPhotoPath: String?,
        driveFileId: String? = null,
        barcode: String? = null
    ): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_ITEM_GROUP_ID, groupId)
            put(KEY_ITEM_CODE, code)
            put(KEY_ITEM_COMMENT, comment)
            put(KEY_ITEM_DATE, date)
            put(KEY_ITEM_LOCAL_PATH, localPhotoPath)
            put(KEY_ITEM_DRIVE_ID, driveFileId)
            put(KEY_ITEM_BARCODE, barcode)
        }
        return db.insertOrThrow(TABLE_ITEMS, null, values)
    }

    fun updateItemDriveId(itemId: Long, driveFileId: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_ITEM_DRIVE_ID, driveFileId)
        }
        return db.update(TABLE_ITEMS, values, "$KEY_ITEM_ID = ?", arrayOf(itemId.toString())) > 0
    }

    fun updateItemLocalPath(itemId: Long, localPhotoPath: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_ITEM_LOCAL_PATH, localPhotoPath)
        }
        return db.update(TABLE_ITEMS, values, "$KEY_ITEM_ID = ?", arrayOf(itemId.toString())) > 0
    }

    fun updateItemBarcode(itemId: Long, barcode: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_ITEM_BARCODE, barcode)
        }
        return db.update(TABLE_ITEMS, values, "$KEY_ITEM_ID = ?", arrayOf(itemId.toString())) > 0
    }

    fun getAllItems(
        groupIds: List<Long>? = null,
        searchQuery: String? = null,
        sortBy: String = "date",
        sortDesc: Boolean = true
    ): List<ProductItem> {
        val list = mutableListOf<ProductItem>()
        val db = this.readableDatabase

        var query = "SELECT i.*, g.$KEY_GROUP_NAME as group_name FROM $TABLE_ITEMS i " +
                "JOIN $TABLE_GROUPS g ON i.$KEY_ITEM_GROUP_ID = g.$KEY_GROUP_ID"

        val selectionArgs = mutableListOf<String>()
        val conditions = mutableListOf<String>()

        // Group Filters
        if (!groupIds.isNullOrEmpty()) {
            val placeholders = groupIds.joinToString(",") { "?" }
            conditions.add("i.$KEY_ITEM_GROUP_ID IN ($placeholders)")
            groupIds.forEach { selectionArgs.add(it.toString()) }
        }

        // Search Filters
        if (!searchQuery.isNullOrBlank()) {
            conditions.add("(i.$KEY_ITEM_CODE LIKE ? OR i.$KEY_ITEM_COMMENT LIKE ? OR i.$KEY_ITEM_DATE LIKE ? OR i.$KEY_ITEM_BARCODE LIKE ?)")
            val searchPattern = "%$searchQuery%"
            selectionArgs.add(searchPattern)
            selectionArgs.add(searchPattern)
            selectionArgs.add(searchPattern)
            selectionArgs.add(searchPattern)
        }

        if (conditions.isNotEmpty()) {
            query += " WHERE " + conditions.joinToString(" AND ")
        }

        // Sorting
        val orderCol = if (sortBy == "code") "i.$KEY_ITEM_CODE" else "i.$KEY_ITEM_DATE"
        val orderDirection = if (sortDesc) "DESC" else "ASC"
        query += " ORDER BY $orderCol $orderDirection"

        val cursor = db.rawQuery(query, selectionArgs.toTypedArray())
        cursor.use {
            if (it.moveToFirst()) {
                val idIdx = it.getColumnIndexOrThrow(KEY_ITEM_ID)
                val grpIdIdx = it.getColumnIndexOrThrow(KEY_ITEM_GROUP_ID)
                val codeIdx = it.getColumnIndexOrThrow(KEY_ITEM_CODE)
                val commentIdx = it.getColumnIndexOrThrow(KEY_ITEM_COMMENT)
                val dateIdx = it.getColumnIndexOrThrow(KEY_ITEM_DATE)
                val localPathIdx = it.getColumnIndexOrThrow(KEY_ITEM_LOCAL_PATH)
                val driveIdIdx = it.getColumnIndexOrThrow(KEY_ITEM_DRIVE_ID)
                val grpNameIdx = it.getColumnIndexOrThrow("group_name")
                val barcodeIdx = it.getColumnIndexOrThrow(KEY_ITEM_BARCODE)

                do {
                    list.add(
                        ProductItem(
                            id = it.getLong(idIdx),
                            groupId = it.getLong(grpIdIdx),
                            code = it.getString(codeIdx),
                            comment = it.getString(commentIdx),
                            date = it.getString(dateIdx),
                            localPhotoPath = it.getString(localPathIdx),
                            driveFileId = it.getString(driveIdIdx),
                            groupName = it.getString(grpNameIdx),
                            barcode = it.getString(barcodeIdx)
                        )
                    )
                } while (it.moveToNext())
            }
        }
        return list
    }

    fun deleteItem(id: Long): Boolean {
        val db = this.writableDatabase
        return db.delete(TABLE_ITEMS, "$KEY_ITEM_ID = ?", arrayOf(id.toString())) > 0
    }
}
