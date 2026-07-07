package com.suprogramuotavisata.markit

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Dashboard : NavKey
@Serializable data object ScanIt : NavKey
@Serializable data class MyStore(val initialGroupFilter: String? = null) : NavKey
@Serializable data object Settings : NavKey
