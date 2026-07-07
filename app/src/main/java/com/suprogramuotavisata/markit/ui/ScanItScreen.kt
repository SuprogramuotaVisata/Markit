package com.suprogramuotavisata.markit.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.suprogramuotavisata.markit.data.BarcodeGenerator
import com.suprogramuotavisata.markit.data.DatabaseHelper
import com.suprogramuotavisata.markit.data.GoogleDriveService
import com.suprogramuotavisata.markit.data.LocalAppStrings
import com.suprogramuotavisata.markit.data.PrintManager
import com.suprogramuotavisata.markit.data.ProductGroup
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanItScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val dbHelper = remember { DatabaseHelper(context) }
    val coroutineScope = rememberCoroutineScope()
    val s = LocalAppStrings.current

    // Camera runtime permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Automatically request camera permission on screen launch
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Database state
    var groups by remember { mutableStateOf<List<ProductGroup>>(emptyList()) }
    var selectedGroup by remember { mutableStateOf<ProductGroup?>(null) }
    var isGroupsLoading by remember { mutableStateOf(true) }

    // Form inputs
    var barcodeCode by remember { mutableStateOf(TextFieldValue("")) }
    var comment by remember { mutableStateOf(TextFieldValue("")) }

    // Toggle configuration states (initially loaded from Settings defaults)
    var addCode by remember { mutableStateOf(true) }
    var addComment by remember { mutableStateOf(true) }
    var addDate by remember { mutableStateOf(true) }
    var printBarcode by remember { mutableStateOf(false) }

    // Camera operational state
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }
    var isCapturing by remember { mutableStateOf(false) }

    // Dropdown state
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Load defaults from SharedPreferences and load groups
    LaunchedEffect(Unit) {
        val sharedPrefs = context.getSharedPreferences("MarkItSettings", Context.MODE_PRIVATE)
        addCode = sharedPrefs.getBoolean("default_add_code", true)
        addComment = sharedPrefs.getBoolean("default_add_comments", true)
        addDate = sharedPrefs.getBoolean("default_add_date", true)
        printBarcode = sharedPrefs.getBoolean("default_print_barcode", false)

        withContext(Dispatchers.IO) {
            groups = dbHelper.getAllGroups()
        }
        if (groups.isNotEmpty()) {
            selectedGroup = groups.first()
        }
        isGroupsLoading = false
    }

    // Bind camera lifecycle reactively when permission is granted
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                } catch (e: Exception) {
                    Log.e("ScanItScreen", "Camera binding failed", e)
                }
            }, mainExecutor)
        }
    }

    // Barcode scanner trigger
    fun triggerBarcodeScanner() {
        val scanner = GmsBarcodeScanning.getClient(context)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                barcode.rawValue?.let {
                    barcodeCode = TextFieldValue(text = it, selection = TextRange(0, it.length))
                    Toast.makeText(context, "${s.scannedCode}: $it", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "${s.scanFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Image capture and process flow
    fun captureAndSaveImage() {
        if (selectedGroup == null) {
            Toast.makeText(context, s.selectGroupFirst, Toast.LENGTH_SHORT).show()
            return
        }
        if (isCapturing) return
        isCapturing = true

        imageCapture.takePicture(
            mainExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            // Extract bytes and rotation from ImageProxy
                            val buffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            val rotationDegrees = image.imageInfo.rotationDegrees
                            image.close()

                            // Decode and rotate bitmap
                            var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (rotationDegrees != 0) {
                                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                            }

                            // Generate formatted details
                            val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                            val currentCode = if (barcodeCode.text.isBlank()) {
                                // Auto-generate barcode if blank
                                UUID.randomUUID().toString().take(8).uppercase()
                            } else {
                                barcodeCode.text
                            }

                            // Generate and overlay information on photo
                            val finalBitmap = BarcodeGenerator.createOverlayPhoto(
                                original = bitmap,
                                code = currentCode,
                                comment = comment.text.takeIf { it.isNotBlank() },
                                date = currentDate,
                                addCode = addCode,
                                addComment = addComment,
                                addDate = addDate
                            )

                            // Save photo locally
                            val photosDir = File(context.filesDir, "photos")
                            if (!photosDir.exists()) photosDir.mkdirs()
                            
                            val fileName = "IMG_${System.currentTimeMillis()}_CODE_${currentCode}.jpg"
                            val localFile = File(photosDir, fileName)
                            
                            val localOutStream = ByteArrayOutputStream()
                            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, localOutStream)
                            val finalBytes = localOutStream.toByteArray()
                            localFile.writeBytes(finalBytes)

                            // Insert product record into SQLite
                            val itemId = dbHelper.createItem(
                                groupId = selectedGroup!!.id,
                                code = currentCode,
                                comment = comment.text.trim().takeIf { it.isNotBlank() },
                                date = currentDate,
                                localPhotoPath = localFile.absolutePath,
                                driveFileId = null
                            )

                            // Send print order if enabled
                            if (printBarcode) {
                                withContext(Dispatchers.Main) {
                                    val barcodeBmp = BarcodeGenerator.generateBarcode(currentCode, 300, 100)
                                    if (barcodeBmp != null) {
                                        PrintManager.printBarcode(context, currentCode, barcodeBmp)
                                    } else {
                                        Toast.makeText(context, s.printBarcodeError, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            val sharedPrefs = context.getSharedPreferences("MarkItSettings", Context.MODE_PRIVATE)
                            val activeStorageMode = sharedPrefs.getString("active_storage_mode", "local") ?: "local"

                            if (activeStorageMode == "google_drive") {
                                // Upload to Google Drive in the background
                                val groupName = selectedGroup!!.name
                                val driveFileId = GoogleDriveService.uploadPhoto(
                                    context = context,
                                    categoryName = groupName,
                                    fileName = fileName,
                                    fileBytes = finalBytes
                                )

                                if (driveFileId != null && itemId > 0) {
                                    dbHelper.updateItemDriveId(itemId, driveFileId)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, s.saveSyncSuccess, Toast.LENGTH_SHORT).show()
                                    }
                                } else if (itemId > 0) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, s.saveLocalOnly, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                // Local storage mode only
                                if (itemId > 0) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Išsaugota vietinėje saugykloje!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            // Reset inputs on success
                            withContext(Dispatchers.Main) {
                                barcodeCode = TextFieldValue("")
                                comment = TextFieldValue("")
                                isCapturing = false
                            }

                        } catch (e: Exception) {
                            Log.e("ScanItScreen", "Error saving/processing image", e)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "${s.dbSaveError}: ${e.message}", Toast.LENGTH_LONG).show()
                                isCapturing = false
                            }
                        }
                    }
                }

                override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                    Toast.makeText(context, "${s.scanFailed}: ${exception.message}", Toast.LENGTH_SHORT).show()
                    isCapturing = false
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Part: Camera Preview card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                // Overlaid Capture Button
                if (groups.isNotEmpty()) {
                    Button(
                        onClick = { captureAndSaveImage() },
                        enabled = !isCapturing && selectedGroup != null,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(s.captureBtn, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                s.createGroupFirstPrompt,
                                color = Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // If permission is not yet granted, show placeholder with grant request action button
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Kamerai reikalingas leidimas",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                        ) {
                            Text("Suteikti leidima")
                        }
                    }
                }
            }
        }

        // Bottom Part: Scrollable form controls grouped into Cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: Product info input fields
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
                    Text(
                        text = s.appName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (isGroupsLoading) {
                        CircularProgressIndicator()
                    } else if (groups.isEmpty()) {
                        Text(
                            text = s.noGroupsTitle,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    } else {
                        // Category/Group Dropdown
                        ExposedDropdownMenuBox(
                            expanded = isDropdownExpanded,
                            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                readOnly = true,
                                value = selectedGroup?.name ?: s.selectGroup,
                                onValueChange = {},
                                label = { Text(s.groupLabel) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                    focusedContainerColor = Color(0xFFF2F2F2),
                                    unfocusedContainerColor = Color(0xFFF9F9F9)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false }
                            ) {
                                groups.forEach { group ->
                                    DropdownMenuItem(
                                        text = { Text(group.name) },
                                        onClick = {
                                            selectedGroup = group
                                            isDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Code input with scanning button
                    OutlinedTextField(
                        value = barcodeCode,
                        onValueChange = { barcodeCode = it },
                        label = { Text(s.barcodeLabel) },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { triggerBarcodeScanner() }) {
                                Icon(Icons.Default.QrCode, contentDescription = s.scanBarcode)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF2F2F2),
                            unfocusedContainerColor = Color(0xFFF9F9F9)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    barcodeCode = barcodeCode.copy(selection = TextRange(0, barcodeCode.text.length))
                                }
                            }
                    )

                    // Comments input
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text(s.commentLabel) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF2F2F2),
                            unfocusedContainerColor = Color(0xFFF9F9F9)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    comment = comment.copy(selection = TextRange(0, comment.text.length))
                                }
                            }
                    )
                }
            }

            // Card 2: Switches and Configuration options
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
                    Text(
                        text = s.defaultStates,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(s.addCodeToPic, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(checked = addCode, onCheckedChange = { addCode = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(s.addCommentToPic, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(checked = addComment, onCheckedChange = { addComment = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(s.addDateToPic, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(checked = addDate, onCheckedChange = { addDate = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(s.printBarcodeOpt, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(checked = printBarcode, onCheckedChange = { printBarcode = it })
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp)) // Padding to scroll above navigation bar
        }
    }
}
