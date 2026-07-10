package com.suprogramuotavisata.markit.utils

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap

object BarcodeUtils {
    @Composable
    fun BarcodeBitmap(androidBitmap: android.graphics.Bitmap, barcode: String, modifier: Modifier = Modifier) {
        // Konvertuojame teksta i barkoda
        Image(
            bitmap = androidBitmap.asImageBitmap(),
            contentDescription = barcode,
            modifier = modifier
        )
    }
}