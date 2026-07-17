package com.suprogramuotavisata.markit.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suprogramuotavisata.markit.data.AppLanguage
import com.suprogramuotavisata.markit.data.LocalAppStrings
import com.suprogramuotavisata.markit.data.LicensingManager
import kotlinx.coroutines.launch

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivationScreen(
    currentLanguage: AppLanguage,
    onLanguageToggle: (AppLanguage) -> Unit,
    onActivationSuccess: () -> Unit
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var keyInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val deviceId = remember { LicensingManager.getDeviceId(context) }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var localHostInput by remember { mutableStateOf(LicensingManager.getLocalHostUrl(context) ?: "") }

    LaunchedEffect(Unit) {
        LicensingManager.registerDeviceOnRegistryServer(context)
        val result = LicensingManager.checkDeviceAssignedKey(context)
        result.fold(
            onSuccess = { assignedKey ->
                if (assignedKey != null) {
                    isLoading = true
                    val actResult = LicensingManager.activate(context, assignedKey)
                    isLoading = false
                    if (actResult.getOrDefault(false)) {
                        onActivationSuccess()
                    }
                }
            },
            onFailure = {
                // Ignore silent background check failure
            }
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text(s.localServerSettings, color = Color.White) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = localHostInput,
                        onValueChange = { localHostInput = it },
                        label = { Text(s.localHostUrlLabel, color = Color(0xFF64748B)) },
                        placeholder = { Text("192.168.1.100:8082") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF06B6D4),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF06B6D4)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSettingsDialog = false
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            if (localHostInput.isBlank()) {
                                LicensingManager.setLocalHostUrl(context, null)
                                isLoading = false
                            } else {
                                val result = LicensingManager.checkInLocalServer(context, localHostInput.trim())
                                isLoading = false
                                result.fold(
                                    onSuccess = { isValid ->
                                        if (isValid) {
                                            onActivationSuccess()
                                        } else {
                                            errorMessage = s.activationErrorLimit
                                        }
                                    },
                                    onFailure = {
                                        errorMessage = s.activationErrorNetwork
                                    }
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4), contentColor = Color(0xFF0F172A))
                ) {
                    Text(s.save, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSettingsDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8))
                ) {
                    Text(s.cancel)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Sleek slate-900 background
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section: Language switcher & Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { showSettingsDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = s.settingsBtn,
                    tint = Color(0xFF06B6D4)
                )
            }

            TextButton(
                onClick = {
                    val nextLang = if (currentLanguage == AppLanguage.LT) AppLanguage.EN else AppLanguage.LT
                    onLanguageToggle(nextLang)
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF06B6D4)) // Cyan color
            ) {
                Text(
                    text = if (currentLanguage == AppLanguage.LT) "ENGLISH" else "LIETUVIŲ",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Center Content: Activation form
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = s.appName,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF06B6D4), // Accent Cyan
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "BETA TEST",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF59E0B), // Amber color
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), // Slate-800
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = s.activationTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = s.activationSubtitle,
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8), // Slate-400
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    errorMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = Color(0xFFF87171), // Light red
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text(s.activationKeyLabel, color = Color(0xFF64748B)) },
                        placeholder = { Text(s.activationKeyPlaceholder) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF06B6D4),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF06B6D4)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    )

                    Button(
                        onClick = {
                            if (keyInput.isBlank()) return@Button
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                val result = LicensingManager.activate(context, keyInput.trim())
                                isLoading = false
                                result.fold(
                                    onSuccess = { isValid ->
                                        if (isValid) {
                                            onActivationSuccess()
                                        } else {
                                            errorMessage = s.activationErrorInvalid
                                        }
                                    },
                                    onFailure = {
                                        errorMessage = s.activationErrorNetwork
                                    }
                                )
                            }
                        },
                        enabled = !isLoading && keyInput.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF06B6D4),
                            contentColor = Color(0xFF0F172A),
                            disabledContainerColor = Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color(0xFF0F172A),
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(s.activateButton, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // Bottom Info: Device ID & Copy helper
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        ) {
            Text(
                text = "${s.deviceIdLabel}:",
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )
            
            Text(
                text = deviceId,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Device ID", deviceId)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, s.copiedToClipboard, Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF06B6D4))
            ) {
                Text(s.copyDeviceId, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
