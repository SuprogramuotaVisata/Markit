package com.suprogramuotavisata.markit.data.sync

object SyncManager {
    private val providers = mutableMapOf<String, SyncProvider>()

    init {
        registerProvider(SuprogramuotaVisataSyncProvider())
    }

    fun registerProvider(provider: SyncProvider) {
        providers[provider.id] = provider
    }

    fun getProvider(id: String): SyncProvider? = providers[id]

    fun getAllProviders(): List<SyncProvider> = providers.values.toList()
}
