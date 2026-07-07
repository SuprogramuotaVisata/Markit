package com.suprogramuotavisata.markit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.suprogramuotavisata.markit.data.AppLanguage
import com.suprogramuotavisata.markit.data.EnStrings
import com.suprogramuotavisata.markit.data.LocalAppStrings
import com.suprogramuotavisata.markit.data.LtStrings
import com.suprogramuotavisata.markit.data.TranslationManager
import com.suprogramuotavisata.markit.theme.MarkItTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      var language by remember { mutableStateOf(TranslationManager.getLanguage(this)) }
      val strings = if (language == AppLanguage.LT) LtStrings else EnStrings

      CompositionLocalProvider(LocalAppStrings provides strings) {
        MarkItTheme {
          Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            MainNavigation(
              currentLanguage = language,
              onLanguageToggle = { newLang ->
                TranslationManager.setLanguage(this, newLang)
                language = newLang
              }
            )
          }
        }
      }
    }
  }
}
