package com.suprogramuotavisata.markit.data.sync

import com.suprogramuotavisata.markit.data.ProductGroup
import com.suprogramuotavisata.markit.data.ProductItem

interface SyncProvider {
    val id: String
    val name: String

    suspend fun syncGroup(group: ProductGroup, apiUrl: String): Result<Unit>
    suspend fun syncItem(item: ProductItem, groupName: String, apiUrl: String): Result<Unit>
}
