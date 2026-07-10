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
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag

object NfcDispatcher {
    private var listener: ((Tag?) -> Unit)? = null
    
    fun setListener(l: ((Tag?) -> Unit)?) {
        listener = l
    }
    
    fun dispatch(tag: Tag?) {
        listener?.invoke(tag)
    }
}

class MainActivity : ComponentActivity() {
  private var nfcAdapter: NfcAdapter? = null
  private var pendingIntent: PendingIntent? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    nfcAdapter = NfcAdapter.getDefaultAdapter(this)
    val intent = Intent(this, javaClass).apply {
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    pendingIntent = PendingIntent.getActivity(
        this, 0, intent,
        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

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

  override fun onResume() {
    super.onResume()
    nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
  }

  override fun onPause() {
    super.onPause()
    nfcAdapter?.disableForegroundDispatch(this)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
        NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
        NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        NfcDispatcher.dispatch(tag)
    }
  }
}
