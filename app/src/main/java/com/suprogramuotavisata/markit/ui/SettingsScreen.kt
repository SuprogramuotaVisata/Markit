package com.suprogramuotavisata.markit.ui

import android.nfc.NfcAdapter
import android.util.Log
import com.suprogramuotavisata.markit.NfcDispatcher
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Slider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suprogramuotavisata.markit.data.GoogleDriveService
import com.suprogramuotavisata.markit.data.LocalAppStrings
import com.suprogramuotavisata.markit.data.PrintManager
import com.suprogramuotavisata.markit.data.StorageMigrator
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val s = LocalAppStrings.current

    // Config default switches
    var defaultAddCode by remember { mutableStateOf(true) }
    var defaultAddComments by remember { mutableStateOf(true) }
    var defaultAddDate by remember { mutableStateOf(true) }
    var defaultPrintBarcode by remember { mutableStateOf(false) }

    // Google Sign-In state
    var authMode by remember { mutableStateOf(GoogleDriveService.MODE_NONE) }
    var userEmail by remember { mutableStateOf<String?>(null) }
    var isCheckingConnection by remember { mutableStateOf(false) }

    // Printer settings state
    var printerType by remember { mutableStateOf("system") }
    var printerIp by remember { mutableStateOf("") }
    var printerPort by remember { mutableStateOf("9100") }
    var printerBt by remember { mutableStateOf("") }
    var labelRotation by remember { mutableStateOf("0") }
    var printQrOnly by remember { mutableStateOf(false) }
    var printCopiesText by remember { mutableStateOf("1") }
    var promptForCopies by remember { mutableStateOf(false) }
    var marginStart by remember { mutableStateOf(30f) }
    var marginEnd by remember { mutableStateOf(200f) }
    var printerAutoCut by remember { mutableStateOf(true) }
    var printerTapeWidth by remember { mutableStateOf(24) }
    var printerBrand by remember { mutableStateOf("brother") }
    var posPaperWidth by remember { mutableStateOf(80) }
    var cameraBrightness by remember { mutableStateOf(0f) }
    var cameraContrast by remember { mutableStateOf(1f) }
    var settingsApiUrl by remember { mutableStateOf("") }

    // Collapsible sections state
    var isCloudExpanded by remember { mutableStateOf(false) }
    var isCameraExpanded by remember { mutableStateOf(false) }
    var isPrinterExpanded by remember { mutableStateOf(true) }

    // Active storage configuration & migration state
    var activeStorageMode by remember { mutableStateOf("local") }
    var isMigrating by remember { mutableStateOf(false) }
    var migrationProgressText by remember { mutableStateOf("") }

    // NFC Pairing Dialog state
    var showNfcDialog by remember { mutableStateOf(false) }

    val sharedPrefs = remember { context.getSharedPreferences("MarkItSettings", android.content.Context.MODE_PRIVATE) }

    // Load configurations on launch
    LaunchedEffect(Unit) {
        defaultAddCode = sharedPrefs.getBoolean("default_add_code", true)
        defaultAddComments = sharedPrefs.getBoolean("default_add_comments", true)
        defaultAddDate = sharedPrefs.getBoolean("default_add_date", true)
        defaultPrintBarcode = sharedPrefs.getBoolean("default_print_barcode", false)

        authMode = GoogleDriveService.getAuthMode(context)
        userEmail = GoogleDriveService.getUserEmail(context)

        printerType = sharedPrefs.getString("printer_type", "system") ?: "system"
        printerIp = sharedPrefs.getString("printer_ip", "") ?: ""
        printerPort = sharedPrefs.getString("printer_port", "9100") ?: "9100"
        printerBt = sharedPrefs.getString("printer_bt", "") ?: ""
        labelRotation = sharedPrefs.getString("label_rotation", "0") ?: "0"
        printQrOnly = sharedPrefs.getBoolean("print_qr_only", false)
        printCopiesText = sharedPrefs.getInt("print_copies", 1).toString()
        promptForCopies = sharedPrefs.getBoolean("prompt_for_copies", false)
        marginStart = sharedPrefs.getInt("margin_start", 50).coerceIn(35, 150).toFloat()
        marginEnd = sharedPrefs.getInt("margin_end", 250).coerceIn(180, 300).toFloat()
        printerAutoCut = sharedPrefs.getBoolean("printer_auto_cut", true)
        printerTapeWidth = sharedPrefs.getInt("printer_tape_width", 24)
        printerBrand = sharedPrefs.getString("printer_brand", "brother") ?: "brother"
        posPaperWidth = sharedPrefs.getInt("pos_paper_width", 80)
        
        activeStorageMode = sharedPrefs.getString("active_storage_mode", "local") ?: "local"
        cameraBrightness = sharedPrefs.getFloat("camera_brightness", 0f)
        cameraContrast = sharedPrefs.getFloat("camera_contrast", 1f)
        settingsApiUrl = sharedPrefs.getString("settings_api_url", "") ?: ""
    }

    // NFC Hardware checks
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    val nfcStatusText = remember(nfcAdapter) {
        if (nfcAdapter == null) "not_supported"
        else if (!nfcAdapter.isEnabled) "disabled"
        else "supported"
    }

    // SharedPreferences save helpers
    fun savePref(key: String, value: Boolean) {
        sharedPrefs.edit().putBoolean(key, value).apply()
    }

    // Google Sign-In configuration
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                GoogleDriveService.setAuthMode(context, GoogleDriveService.MODE_GOOGLE)
                GoogleDriveService.setUserEmail(context, account.email)
                GoogleDriveService.setGoogleAccountName(context, account.email)

                authMode = GoogleDriveService.MODE_GOOGLE
                userEmail = account.email

                Toast.makeText(context, "${s.connectionLoggedIn}: ${account.email}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Toast.makeText(context, "${s.printError}: ${e.message} (${e.statusCode})", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = s.settings,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Group 1: Debesies saugykla & Sinchronizacija (Cloud Storage & Backups)
        CollapsibleGroupCard(
            title = s.groupCloudSettings,
            icon = Icons.Default.Cloud,
            isExpanded = isCloudExpanded,
            onToggleExpand = { isCloudExpanded = !isCloudExpanded }
        ) {
            Text(
                s.driveSettings,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
            if (authMode == GoogleDriveService.MODE_NONE) {
                Text(
                    s.driveSettingsDesc,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(s.signInGoogle, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                    OutlinedButton(
                        onClick = {
                            GoogleDriveService.setAuthMode(context, GoogleDriveService.MODE_MOCK)
                            GoogleDriveService.setUserEmail(context, "vietinis-vartotojas@vietinis.lt")
                            authMode = GoogleDriveService.MODE_MOCK
                            userEmail = "vietinis-vartotojas@vietinis.lt"
                            Toast.makeText(context, s.mockModeActive, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(s.signInMock, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                Text(
                    text = "${s.connectionStatus}: ${s.connectionLoggedIn}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${s.connectionUser}: $userEmail",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${s.connectionMode}: ${if (authMode == GoogleDriveService.MODE_GOOGLE) "Google Drive" else s.connectionLocalMode}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isCheckingConnection = true
                            coroutineScope.launch {
                                val id = GoogleDriveService.findOrCreateAppFolder(context)
                                isCheckingConnection = false
                                if (id != null) {
                                    Toast.makeText(context, s.connectionOk, Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, s.connectionFail, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isCheckingConnection,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isCheckingConnection) s.dataLoading else s.testConnection, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                    Button(
                        onClick = {
                            googleSignInClient.signOut().addOnCompleteListener {
                                GoogleDriveService.logout(context)
                                authMode = GoogleDriveService.MODE_NONE
                                userEmail = null
                                Toast.makeText(context, s.logOutSuccess, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(s.signOut, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
            Spacer(modifier = Modifier.height(8.dp))

            // Storage options section
            Text(
                s.storageLocationLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                s.storageLocationDesc,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (activeStorageMode == "google_drive") s.storageDrive else s.storageLocal,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = activeStorageMode == "google_drive",
                    onCheckedChange = { isDrive ->
                        if (isDrive) {
                            if (authMode == GoogleDriveService.MODE_NONE) {
                                Toast.makeText(context, "Pirmiausia prisijunkite prie Google Drive paskyros!", Toast.LENGTH_LONG).show()
                            } else {
                                isMigrating = true
                                migrationProgressText = "Ruošiamas perkėlimas..."
                                coroutineScope.launch {
                                    val result = StorageMigrator.migrateLocalToDrive(context) { progress ->
                                        migrationProgressText = progress
                                    }
                                    isMigrating = false
                                    if (result.isSuccess) {
                                        activeStorageMode = "google_drive"
                                        sharedPrefs.edit().putString("active_storage_mode", "google_drive").apply()
                                        Toast.makeText(context, s.migrationSuccess, Toast.LENGTH_LONG).show()
                                    } else {
                                        val err = result.exceptionOrNull()?.message ?: ""
                                        Toast.makeText(context, "${s.migrationError}$err", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        } else {
                            isMigrating = true
                            migrationProgressText = "Ruošiamas atsisiuntimas..."
                            coroutineScope.launch {
                                val result = StorageMigrator.migrateDriveToLocal(context) { progress ->
                                    migrationProgressText = progress
                                }
                                isMigrating = false
                                if (result.isSuccess) {
                                    activeStorageMode = "local"
                                    sharedPrefs.edit().putString("active_storage_mode", "local").apply()
                                    Toast.makeText(context, s.migrationSuccess, Toast.LENGTH_LONG).show()
                                } else {
                                    val err = result.exceptionOrNull()?.message ?: ""
                                    Toast.makeText(context, "${s.migrationError}$err", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                s.settingsApiUrlLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
            
            OutlinedTextField(
                value = settingsApiUrl,
                onValueChange = {
                    settingsApiUrl = it
                    sharedPrefs.edit().putString("settings_api_url", it).apply()
                },
                label = { Text("API URL") },
                singleLine = true,
                placeholder = { Text("https://example.com/api/settings") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF2F2F2),
                    unfocusedContainerColor = Color(0xFFF9F9F9)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Send Settings Button
                Button(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                if (settingsApiUrl.isBlank()) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Įveskite API adresą!", Toast.LENGTH_SHORT).show()
                                    }
                                    return@launch
                                }
                                
                                val json = org.json.JSONObject().apply {
                                    put("default_add_code", sharedPrefs.getBoolean("default_add_code", true))
                                    put("default_add_comments", sharedPrefs.getBoolean("default_add_comments", true))
                                    put("default_add_date", sharedPrefs.getBoolean("default_add_date", true))
                                    put("default_print_barcode", sharedPrefs.getBoolean("default_print_barcode", false))
                                    put("print_copies", sharedPrefs.getInt("print_copies", 1))
                                    put("prompt_for_copies", sharedPrefs.getBoolean("prompt_for_copies", false))
                                    put("margin_start", sharedPrefs.getInt("margin_start", 50))
                                    put("margin_end", sharedPrefs.getInt("margin_end", 250))
                                    put("printer_type", sharedPrefs.getString("printer_type", "system"))
                                    put("printer_brand", sharedPrefs.getString("printer_brand", "brother"))
                                    put("printer_tape_width", sharedPrefs.getInt("printer_tape_width", 24))
                                    put("pos_paper_width", sharedPrefs.getInt("pos_paper_width", 80))
                                    put("printer_auto_cut", sharedPrefs.getBoolean("printer_auto_cut", true))
                                    put("printer_ip", sharedPrefs.getString("printer_ip", ""))
                                    put("printer_port", sharedPrefs.getString("printer_port", "9100"))
                                    put("printer_bt", sharedPrefs.getString("printer_bt", ""))
                                    put("label_rotation", sharedPrefs.getString("label_rotation", "0"))
                                    put("camera_brightness", sharedPrefs.getFloat("camera_brightness", 0f))
                                    put("camera_contrast", sharedPrefs.getFloat("camera_contrast", 1f))
                                }

                                val connection = java.net.URL(settingsApiUrl).openConnection() as java.net.HttpURLConnection
                                connection.requestMethod = "POST"
                                connection.setRequestProperty("Content-Type", "application/json")
                                connection.doOutput = true
                                connection.connectTimeout = 5000
                                connection.readTimeout = 5000
                                
                                connection.outputStream.use { os ->
                                    os.write(json.toString().toByteArray())
                                }

                                val responseCode = connection.responseCode
                                if (responseCode in 200..299) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, s.settingsApiSendSuccess, Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "${s.settingsApiError} HTTP $responseCode", Toast.LENGTH_LONG).show()
                                    }
                                }
                                connection.disconnect()
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "${s.settingsApiError} ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(s.settingsApiSendBtn, fontSize = 12.sp, textAlign = TextAlign.Center)
                }

                // Get Settings Button
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                if (settingsApiUrl.isBlank()) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Įveskite API adresą!", Toast.LENGTH_SHORT).show()
                                    }
                                    return@launch
                                }

                                val connection = java.net.URL(settingsApiUrl).openConnection() as java.net.HttpURLConnection
                                connection.requestMethod = "GET"
                                connection.connectTimeout = 5000
                                connection.readTimeout = 5000

                                val responseCode = connection.responseCode
                                if (responseCode in 200..299) {
                                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                                    val json = org.json.JSONObject(body)
                                    
                                    val edit = sharedPrefs.edit()
                                    if (json.has("default_add_code")) edit.putBoolean("default_add_code", json.getBoolean("default_add_code"))
                                    if (json.has("default_add_comments")) edit.putBoolean("default_add_comments", json.getBoolean("default_add_comments"))
                                    if (json.has("default_add_date")) edit.putBoolean("default_add_date", json.getBoolean("default_add_date"))
                                    if (json.has("default_print_barcode")) edit.putBoolean("default_print_barcode", json.getBoolean("default_print_barcode"))
                                    if (json.has("print_copies")) edit.putInt("print_copies", json.getInt("print_copies"))
                                    if (json.has("prompt_for_copies")) edit.putBoolean("prompt_for_copies", json.getBoolean("prompt_for_copies"))
                                    if (json.has("margin_start")) edit.putInt("margin_start", json.getInt("margin_start"))
                                    if (json.has("margin_end")) edit.putInt("margin_end", json.getInt("margin_end"))
                                    if (json.has("printer_type")) edit.putString("printer_type", json.getString("printer_type"))
                                    if (json.has("printer_brand")) edit.putString("printer_brand", json.getString("printer_brand"))
                                    if (json.has("printer_tape_width")) edit.putInt("printer_tape_width", json.getInt("printer_tape_width"))
                                    if (json.has("pos_paper_width")) edit.putInt("pos_paper_width", json.getInt("pos_paper_width"))
                                    if (json.has("printer_auto_cut")) edit.putBoolean("printer_auto_cut", json.getBoolean("printer_auto_cut"))
                                    if (json.has("printer_ip")) edit.putString("printer_ip", json.getString("printer_ip"))
                                    if (json.has("printer_port")) edit.putString("printer_port", json.getString("printer_port"))
                                    if (json.has("printer_bt")) edit.putString("printer_bt", json.getString("printer_bt"))
                                    if (json.has("label_rotation")) edit.putString("label_rotation", json.getString("label_rotation"))
                                    if (json.has("camera_brightness")) edit.putFloat("camera_brightness", json.optDouble("camera_brightness", 0.0).toFloat())
                                    if (json.has("camera_contrast")) edit.putFloat("camera_contrast", json.optDouble("camera_contrast", 1.0).toFloat())
                                    edit.apply()

                                    withContext(Dispatchers.Main) {
                                        defaultAddCode = sharedPrefs.getBoolean("default_add_code", true)
                                        defaultAddComments = sharedPrefs.getBoolean("default_add_comments", true)
                                        defaultAddDate = sharedPrefs.getBoolean("default_add_date", true)
                                        defaultPrintBarcode = sharedPrefs.getBoolean("default_print_barcode", false)
                                        printCopiesText = sharedPrefs.getInt("print_copies", 1).toString()
                                        promptForCopies = sharedPrefs.getBoolean("prompt_for_copies", false)
                                        marginStart = sharedPrefs.getInt("margin_start", 50).toFloat()
                                        marginEnd = sharedPrefs.getInt("margin_end", 250).toFloat()
                                        printerType = sharedPrefs.getString("printer_type", "system") ?: "system"
                                        printerBrand = sharedPrefs.getString("printer_brand", "brother") ?: "brother"
                                        printerTapeWidth = sharedPrefs.getInt("printer_tape_width", 24)
                                        posPaperWidth = sharedPrefs.getInt("pos_paper_width", 80)
                                        printerAutoCut = sharedPrefs.getBoolean("printer_auto_cut", true)
                                        printerIp = sharedPrefs.getString("printer_ip", "") ?: ""
                                        printerPort = sharedPrefs.getString("printer_port", "9100") ?: "9100"
                                        printerBt = sharedPrefs.getString("printer_bt", "") ?: ""
                                        labelRotation = sharedPrefs.getString("label_rotation", "0") ?: "0"
                                        cameraBrightness = sharedPrefs.getFloat("camera_brightness", 0f)
                                        cameraContrast = sharedPrefs.getFloat("camera_contrast", 1f)

                                        Toast.makeText(context, s.settingsApiSuccess, Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "${s.settingsApiError} HTTP $responseCode", Toast.LENGTH_LONG).show()
                                    }
                                }
                                connection.disconnect()
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "${s.settingsApiError} ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(s.settingsApiGetBtn, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }

        // Group 2: Fotografavimo nustatymai (Camera Configuration)
        CollapsibleGroupCard(
            title = s.groupCameraSettings,
            icon = Icons.Default.PhotoCamera,
            isExpanded = isCameraExpanded,
            onToggleExpand = { isCameraExpanded = !isCameraExpanded }
        ) {
            Text(
                s.defaultStatesDesc,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(s.addCodeToPic, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = defaultAddCode,
                    onCheckedChange = {
                        defaultAddCode = it
                        savePref("default_add_code", it)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(s.addCommentToPic, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = defaultAddComments,
                    onCheckedChange = {
                        defaultAddComments = it
                        savePref("default_add_comments", it)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(s.addDateToPic, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = defaultAddDate,
                    onCheckedChange = {
                        defaultAddDate = it
                        savePref("default_add_date", it)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(s.printBarcodeOpt, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = defaultPrintBarcode,
                    onCheckedChange = {
                        defaultPrintBarcode = it
                        savePref("default_print_barcode", it)
                    }
                )
            }

            if (defaultPrintBarcode) {
                OutlinedTextField(
                    value = printCopiesText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.toIntOrNull() != null)) {
                            printCopiesText = newValue
                            val copiesVal = newValue.toIntOrNull() ?: 1
                            sharedPrefs.edit().putInt("print_copies", copiesVal).apply()
                        }
                    },
                    label = { Text(s.printCopiesLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF2F2F2),
                        unfocusedContainerColor = Color(0xFFF9F9F9)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.promptForCopiesLabel, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(s.promptForCopiesDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = promptForCopies,
                        onCheckedChange = {
                            promptForCopies = it
                            sharedPrefs.edit().putBoolean("prompt_for_copies", it).apply()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
            Spacer(modifier = Modifier.height(8.dp))

            Text("Nuotraukos ryškumas ir kontrastas", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Text("Naudokite jei fotografuojate tamsioje arba prastai apšviestoje aplinkoje.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Ryškumas: ${if (cameraBrightness > 0) "+" else ""}${cameraBrightness.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Slider(
                    value = cameraBrightness,
                    onValueChange = {
                        cameraBrightness = it
                        sharedPrefs.edit().putFloat("camera_brightness", it).apply()
                    },
                    valueRange = -100f..100f,
                    steps = 20
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Kontrastas: ${((cameraContrast * 10).toInt() / 10.0)}x", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Slider(
                    value = cameraContrast,
                    onValueChange = {
                        cameraContrast = it
                        sharedPrefs.edit().putFloat("camera_contrast", it).apply()
                    },
                    valueRange = 0.5f..2.5f,
                    steps = 20
                )
            }
        }

        // Group 3: Spausdintuvo nustatymai (Printer Configuration)
        CollapsibleGroupCard(
            title = s.groupPrinterSettings,
            icon = Icons.Default.Print,
            isExpanded = isPrinterExpanded,
            onToggleExpand = { isPrinterExpanded = !isPrinterExpanded }
        ) {
            // Manufacturer selection
            Text(s.printerBrandLabel, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = printerBrand == "brother",
                    onClick = {
                        printerBrand = "brother"
                        sharedPrefs.edit().putString("printer_brand", "brother").apply()
                    },
                    label = { Text("Brother") }
                )
                FilterChip(
                    selected = printerBrand == "xprinter",
                    onClick = {
                        printerBrand = "xprinter"
                        sharedPrefs.edit().putString("printer_brand", "xprinter").apply()
                    },
                    label = { Text("Xprinter / POS") }
                )
            }

            // Model selection
            Text(s.printerModelLabel, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (printerBrand == "brother") {
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = { Text("PT-P750W") }
                    )
                } else {
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = { Text("XP-80 / XP-58 / POS") }
                    )
                }
            }

            // Connection type selector
            Text(s.printerTypeLabel, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = printerType == "system",
                    onClick = {
                        printerType = "system"
                        sharedPrefs.edit().putString("printer_type", "system").apply()
                    },
                    label = { Text(s.printerTypeSystem) }
                )
                FilterChip(
                    selected = printerType == "network",
                    onClick = {
                        printerType = "network"
                        sharedPrefs.edit().putString("printer_type", "network").apply()
                    },
                    label = { Text(s.printerTypeNetwork) }
                )
                FilterChip(
                    selected = printerType == "usb",
                    onClick = {
                        printerType = "usb"
                        sharedPrefs.edit().putString("printer_type", "usb").apply()
                    },
                    label = { Text(s.printerTypeUsb) }
                )
                FilterChip(
                    selected = printerType == "bluetooth",
                    onClick = {
                        printerType = "bluetooth"
                        sharedPrefs.edit().putString("printer_type", "bluetooth").apply()
                    },
                    label = { Text(s.printerTypeBluetooth) }
                )
            }

            // Connection Configuration inputs
            if (printerType == "network") {
                OutlinedTextField(
                    value = printerIp,
                    onValueChange = {
                        printerIp = it
                        sharedPrefs.edit().putString("printer_ip", it).apply()
                    },
                    label = { Text(s.printerIpLabel) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF2F2F2),
                        unfocusedContainerColor = Color(0xFFF9F9F9)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = printerPort,
                    onValueChange = {
                        printerPort = it
                        sharedPrefs.edit().putString("printer_port", it).apply()
                    },
                    label = { Text(s.printerPortLabel) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF2F2F2),
                        unfocusedContainerColor = Color(0xFFF9F9F9)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (printerType == "bluetooth") {
                OutlinedTextField(
                    value = printerBt,
                    onValueChange = {
                        printerBt = it
                        sharedPrefs.edit().putString("printer_bt", it).apply()
                    },
                    label = { Text(s.printerBtLabel) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF2F2F2),
                        unfocusedContainerColor = Color(0xFFF9F9F9)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Klaida atidarant Bluetooth nustatymus: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(s.openBluetoothSettings)
                }
            } else if (printerType == "system") {
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_PRINT_SETTINGS).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Klaida atidarant spausdinimo nustatymus: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(s.openSystemPrintSettings)
                }
            }

            // NFC Configuration Trigger
            if (printerType == "network") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Nfc, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(s.printerNfcLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        
                        Text(s.printerNfcDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${s.printerNfcStatus}:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            val (statusColor, statusText) = when (nfcStatusText) {
                                "supported" -> Color(0xFF4CAF50) to s.printerNfcSupported
                                "disabled" -> Color(0xFFFF9800) to s.printerNfcDisabled
                                else -> MaterialTheme.colorScheme.error to s.printerNfcNotSupported
                            }
                            Text(statusText, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        
                        if (nfcStatusText == "supported") {
                            Button(
                                onClick = { showNfcDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(s.printerNfcPairBtn, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Brand specific layout (Brother Tape Width & Margins OR Xprinter Paper Width)
            if (printerBrand == "brother") {
                Text(s.printerTapeWidthLabel, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(s.printerTapeWidthDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(6, 9, 12, 18, 24).forEach { width ->
                        FilterChip(
                            selected = printerTapeWidth == width,
                            onClick = {
                                printerTapeWidth = width
                                sharedPrefs.edit().putInt("printer_tape_width", width).apply()
                            },
                            label = { Text("${width}mm", maxLines = 1) }
                        )
                    }
                }

                Text("Paraščių nustatymai", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Pakoreguokite tuščios juostos tarpus spaudinio pradžioje ir pabaigoje.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${s.marginStartLabel}: ${marginStart.toInt()} taškų", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = marginStart,
                        onValueChange = {
                            marginStart = it
                            sharedPrefs.edit().putInt("margin_start", it.toInt()).apply()
                        },
                        valueRange = 35f..150f,
                        steps = 23
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${s.marginEndLabel}: ${marginEnd.toInt()} taškų", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = marginEnd,
                        onValueChange = {
                            marginEnd = it
                            sharedPrefs.edit().putInt("margin_end", it.toInt()).apply()
                        },
                        valueRange = 180f..300f,
                        steps = 24
                    )
                }
            } else {
                // Xprinter Paper Width Selection
                Text(s.posPaperWidthLabel, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(s.posPaperWidthDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(80, 160).forEach { width ->
                        FilterChip(
                            selected = posPaperWidth == width,
                            onClick = {
                                posPaperWidth = width
                                sharedPrefs.edit().putInt("pos_paper_width", width).apply()
                            },
                            label = { Text("${width}mm") }
                        )
                    }
                }
            }

            // Auto-Cutter option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.printerAutoCutLabel, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(s.printerAutoCutDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = printerAutoCut,
                    onCheckedChange = {
                        printerAutoCut = it
                        sharedPrefs.edit().putBoolean("printer_auto_cut", it).apply()
                    }
                )
            }

            // Rotation options
            Text(s.labelRotationLabel, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = labelRotation == "0",
                    onClick = {
                        labelRotation = "0"
                        sharedPrefs.edit().putString("label_rotation", "0").apply()
                    },
                    label = { Text(s.labelRotation0) }
                )
                FilterChip(
                    selected = labelRotation == "90",
                    onClick = {
                        labelRotation = "90"
                        sharedPrefs.edit().putString("label_rotation", "90").apply()
                    },
                    label = { Text(s.labelRotation90) }
                )
                FilterChip(
                    selected = labelRotation == "180",
                    onClick = {
                        labelRotation = "180"
                        sharedPrefs.edit().putString("label_rotation", "180").apply()
                    },
                    label = { Text(s.labelRotation180) }
                )
                FilterChip(
                    selected = labelRotation == "270",
                    onClick = {
                        labelRotation = "270"
                        sharedPrefs.edit().putString("label_rotation", "270").apply()
                    },
                    label = { Text(s.labelRotation270) }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    val testGroup = com.suprogramuotavisata.markit.data.ProductGroup(
                        name = "TESTINIS ELEMENTAS",
                        code = "ABC-123",
                        barcode = "12345678",
                        description = "Tai yra bandomasis lipdukas patikrinimui."
                    )
                    PrintManager.printGroupLabel(context, testGroup)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(s.printTestBarcode)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth().align(alignment = Alignment.CenterHorizontally)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${s.appName} - ${s.sloganText}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                val versionName = remember {
                    try {
                        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                        pInfo.versionName
                    } catch (e: Exception) {
                        "1.0.14"
                    }
                }
                
                Text(
                    text = "Versija: $versionName",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Autorius: Dimitrijus Maslovas",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "info.suprogramuotavisata@gmail.com",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    text = "© 2026 Suprogramuota visata",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        
        Spacer(modifier = Modifier.height(60.dp)) // Extra space to scroll above navigation bar
    }

    // NFC Pairing Simulation Dialog
    DisposableEffect(showNfcDialog) {
        if (showNfcDialog) {
            NfcDispatcher.setListener { tag ->
                Log.d("SettingsScreen", "NFC Tag detected: $tag")
                
                // Automatically fill connection credentials from Brother NFC Payload
                // Note: PT-P750W typical Wi-Fi Direct IP is 192.168.118.1
                printerType = "network"
                printerIp = "192.168.118.1"
                printerPort = "9100"       // Standard RAW port
                
                sharedPrefs.edit()
                    .putString("printer_type", "network")
                    .putString("printer_ip", "192.168.118.1")
                    .putString("printer_port", "9100")
                    .apply()
                    
                showNfcDialog = false
                Toast.makeText(context, "NFC: Nustatymai gauti! SVARBU: Dabar rankiniu būdu prisijunkite prie spausdintuvo Wi-Fi tinklo savo telefono nustatymuose.", Toast.LENGTH_LONG).show()
            }
        }
        onDispose {
            NfcDispatcher.setListener(null)
        }
    }

    if (showNfcDialog) {
        AlertDialog(
            onDismissRequest = { showNfcDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Nfc, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("NFC Susiejimas")
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    
                    Text(
                        s.printerNfcPairingPrompt,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    
                    Text(
                        "(Priglauskite telefona prie Brother PT-P750W spausdintuvo virsuje esancios NFC zymos)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                    
                    // Simulate Touch button for testing/mock purposes
                    OutlinedButton(
                        onClick = {
                            // Automatically fill connection credentials from Simulated Brother NFC Payload
                            printerType = "network"
                            printerIp = "192.168.118.1" // Wi-Fi Direct Default
                            printerPort = "9100"       // Standard RAW port
                            
                            sharedPrefs.edit()
                                .putString("printer_type", "network")
                                .putString("printer_ip", "192.168.118.1")
                                .putString("printer_port", "9100")
                                .apply()
                                
                            showNfcDialog = false
                            Toast.makeText(context, "NFC: Brother PT-P750W sukonfigūruotas! Prisijunkite prie jo Wi-Fi.", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Simuliuoti prilietima (Mock Touch)")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showNfcDialog = false }) {
                    Text(s.cancel)
                }
            }
        )
    }

    if (isMigrating) {
        AlertDialog(
            onDismissRequest = {}, // Make it non-dismissable during migration
            title = { Text(s.migratingTitle) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = migrationProgressText,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}

@Composable
private fun CollapsibleGroupCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
            if (isExpanded) {
                Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

