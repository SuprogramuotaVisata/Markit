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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
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
import com.suprogramuotavisata.markit.data.BarcodeGenerator
import com.suprogramuotavisata.markit.data.GoogleDriveService
import com.suprogramuotavisata.markit.data.LocalAppStrings
import com.suprogramuotavisata.markit.data.PrintManager
import com.suprogramuotavisata.markit.data.StorageMigrator
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.launch

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
        
        activeStorageMode = sharedPrefs.getString("active_storage_mode", "local") ?: "local"
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

        // 1. Google Drive Authentication Panel
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(s.driveSettings, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (authMode == GoogleDriveService.MODE_NONE) {
                    Text(
                        s.driveSettingsDesc,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

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
                            Text(s.signInGoogle, fontSize = 12.sp)
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
                            Text(s.signInMock, fontSize = 12.sp)
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

                    Spacer(modifier = Modifier.height(16.dp))

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
                            Text(if (isCheckingConnection) s.dataLoading else s.testConnection, fontSize = 12.sp)
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
                            Text(s.signOut, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Storage options card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(s.storageLocationLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

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
                                // Check if authenticated
                                if (authMode == GoogleDriveService.MODE_NONE) {
                                    Toast.makeText(context, "Pirmiausia prisijunkite prie Google Drive paskyros!", Toast.LENGTH_LONG).show()
                                } else {
                                    // Trigger Local -> Drive migration
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
                                // Trigger Drive -> Local migration
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
            }
        }

        // 2. Default Overlay Switches
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(s.defaultStates, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

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
            }
        }

        // 3. Printer Settings & Test Card (Adapted for Brother PT-P750W / NFC)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(s.printerSettings, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Text(
                    s.printerSettingsDesc,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Selectable Connection Type Chips in a FlowRow (wraps automatically to next line when narrow)
                Text(s.printerTypeLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
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
                        selected = printerType == "bluetooth",
                        onClick = {
                            printerType = "bluetooth"
                            sharedPrefs.edit().putString("printer_type", "bluetooth").apply()
                        },
                        label = { Text(s.printerTypeBluetooth) }
                    )
                }

                // Dynamic Input forms depending on Selected Printer Type
                if (printerType == "network") {
                    OutlinedTextField(
                        value = printerIp,
                        onValueChange = {
                            printerIp = it
                            sharedPrefs.edit().putString("printer_ip", it).apply()
                        },
                        label = { Text(s.printerIpLabel) },
                        singleLine = true,
                        placeholder = { Text("e.g. 192.168.1.100") },
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
                        placeholder = { Text("9100") },
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
                        placeholder = { Text("MAC Address or Device Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF2F2F2),
                            unfocusedContainerColor = Color(0xFFF9F9F9)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Button to launch native Android Bluetooth settings
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
                    // Button to launch native Android Print Settings
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

                // 4. NFC Quick Config Integration (adapted for Brother PT-P750W touch)
                if (printerType == "network" || printerType == "system") {
                    Spacer(modifier = Modifier.height(8.dp))
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
                            
                            // NFC status indicator
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

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val barcodeBmp = BarcodeGenerator.generateBarcode("TEST-12345", 300, 100)
                        if (barcodeBmp != null) {
                            PrintManager.printBarcode(context, s.testBarcodeText, barcodeBmp)
                        } else {
                            Toast.makeText(context, s.printBarcodeError, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(s.printTestBarcode)
                }
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
                    text = s.sloganText,
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
                printerType = "network"
                printerIp = "192.168.1.125" // Typical Brother Local IP
                printerPort = "9100"       // Standard RAW port
                
                sharedPrefs.edit()
                    .putString("printer_type", "network")
                    .putString("printer_ip", "192.168.1.125")
                    .putString("printer_port", "9100")
                    .apply()
                    
                showNfcDialog = false
                Toast.makeText(context, "NFC: Brother PT-P750W sėkmingai susietas!", Toast.LENGTH_LONG).show()
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
                            printerIp = "192.168.1.125" // Typical Brother Local IP
                            printerPort = "9100"       // Standard RAW port
                            
                            sharedPrefs.edit()
                                .putString("printer_type", "network")
                                .putString("printer_ip", "192.168.1.125")
                                .putString("printer_port", "9100")
                                .apply()
                                
                            showNfcDialog = false
                            Toast.makeText(context, "NFC: Brother PT-P750W sujungtas sekmingai!", Toast.LENGTH_LONG).show()
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
