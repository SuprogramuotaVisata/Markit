package com.suprogramuotavisata.markit.ui

import android.view.WindowManager
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.barcode.BarcodeScanning
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanItScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val dbHelper = remember { DatabaseHelper(context) }
    val coroutineScope = rememberCoroutineScope()
    val s = LocalAppStrings.current
    val focusManager = LocalFocusManager.current
    
    val codeFocus = remember { FocusRequester() }
    val barcodeFocus = remember { FocusRequester() }
    val commentFocus = remember { FocusRequester() }

    // Form inputs
    var itemCode by remember { mutableStateOf(TextFieldValue("")) }
    var itemBarcode by remember { mutableStateOf(TextFieldValue("")) }
    var comment by remember { mutableStateOf(TextFieldValue("")) }

    // Toggle configuration states
    var addCode by remember { mutableStateOf(true) }
    var addComment by remember { mutableStateOf(true) }
    var addDate by remember { mutableStateOf(true) }
    var printBarcode by remember { mutableStateOf(false) }
    var printQrOnly by remember { mutableStateOf(false) }
    var isCameraExpanded by remember { mutableStateOf(false) }

    // Database state
    var groups by remember { mutableStateOf<List<ProductGroup>>(emptyList()) }
    var selectedGroup by remember { mutableStateOf<ProductGroup?>(null) }
    var isGroupsLoading by remember { mutableStateOf(true) }

    // Camera runtime permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { hasCameraPermission = it }

    // Automatically request camera permission and auto-generate code on launch
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        
        val sharedPrefs = context.getSharedPreferences("MarkItSettings", Context.MODE_PRIVATE)
        addCode = sharedPrefs.getBoolean("default_add_code", true)
        addComment = sharedPrefs.getBoolean("default_add_comments", true)
        addDate = sharedPrefs.getBoolean("default_add_date", true)
        printBarcode = sharedPrefs.getBoolean("default_print_barcode", false)
        printQrOnly = sharedPrefs.getBoolean("print_qr_only", false)

        withContext(Dispatchers.IO) {
            groups = dbHelper.getAllGroups()
        }
        if (groups.isNotEmpty()) {
            selectedGroup = groups.first()
        }
        isGroupsLoading = false

        // 3. Auto-generate code and focus/select
        val uniqueCode = UUID.randomUUID().toString().take(8).uppercase()
        itemCode = TextFieldValue(text = uniqueCode, selection = TextRange(0, uniqueCode.length))
        codeFocus.requestFocus()
    }

    // Prevent screen dimming
    DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Camera operational state
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }
    var isCapturing by remember { mutableStateOf(false) }

    // Dropdown state
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Bind camera lifecycle only when camera is expanded
    LaunchedEffect(hasCameraPermission, isCameraExpanded) {
        if (hasCameraPermission && isCameraExpanded) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                } catch (e: Exception) {
                    Log.e("ScanItScreen", "Camera binding failed", e)
                }
            }, mainExecutor)
        } else {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {}
        }
    }

    // Barcode scanner trigger
    fun triggerBarcodeScanner(targetField: String) {
        val scanner = GmsBarcodeScanning.getClient(context)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                barcode.rawValue?.let {
                    val newVal = TextFieldValue(text = it, selection = TextRange(0, it.length))
                    if (targetField == "code") {
                        itemCode = newVal
                    } else {
                        itemBarcode = newVal
                    }
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
                            val buffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            val rotationDegrees = image.imageInfo.rotationDegrees
                            image.close()

                            var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (rotationDegrees != 0) {
                                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                            }

                            // Load camera brightness and contrast settings from SharedPreferences
                            val cameraPrefs = context.getSharedPreferences("MarkItSettings", Context.MODE_PRIVATE)
                            val brightness = cameraPrefs.getFloat("camera_brightness", 0f)
                            val contrast = cameraPrefs.getFloat("camera_contrast", 1f)

                            if (brightness != 0f || contrast != 1f) {
                                bitmap = adjustBitmap(bitmap, brightness, contrast)
                            }

                            // Auto-detect barcode on capture region
                            val detectedBarcode = suspendCancellableCoroutine<String?> { cont ->
                                try {
                                    val bmpWidth = bitmap.width
                                    val bmpHeight = bitmap.height
                                    val cropWidth = (bmpWidth * 0.70f).toInt()
                                    val cropHeight = (bmpHeight * 0.35f).toInt()
                                    val startX = (bmpWidth - cropWidth) / 2
                                    val startY = (bmpHeight - cropHeight) / 2
                                    val croppedBitmap = Bitmap.createBitmap(bitmap, startX, startY, cropWidth, cropHeight)
                                    val inputImage = InputImage.fromBitmap(croppedBitmap, 0)
                                    BarcodeScanning.getClient().process(inputImage)
                                        .addOnSuccessListener { barcodes -> cont.resume(barcodes.firstOrNull()?.rawValue?.trim()) }
                                        .addOnFailureListener { cont.resume(null) }
                                } catch (e: Exception) { cont.resume(null) }
                            }

                            val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                            val currentCode = itemCode.text.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString().take(8).uppercase()
                            
                            val finalBarcode = if (itemBarcode.text.isNotBlank()) {
                                itemBarcode.text
                            } else if (!detectedBarcode.isNullOrBlank() && detectedBarcode != currentCode) {
                                detectedBarcode
                            } else {
                                null
                            }

                            val finalBitmap = BarcodeGenerator.createOverlayPhoto(
                                original = bitmap,
                                code = currentCode,
                                comment = comment.text.takeIf { it.isNotBlank() },
                                date = currentDate,
                                addCode = addCode,
                                addComment = addComment,
                                addDate = addDate,
                                qrOnly = printQrOnly
                            )

                            val photosDir = File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }
                            val fileName = "IMG_${System.currentTimeMillis()}_CODE_${currentCode}.jpg"
                            val localFile = File(photosDir, fileName)
                            
                            val localOutStream = ByteArrayOutputStream()
                            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, localOutStream)
                            val finalBytes = localOutStream.toByteArray()
                            localFile.writeBytes(finalBytes)

                            val itemId = dbHelper.createItem(
                                groupId = selectedGroup!!.id,
                                code = currentCode,
                                comment = comment.text.trim().takeIf { it.isNotBlank() },
                                date = currentDate,
                                localPhotoPath = localFile.absolutePath,
                                driveFileId = null,
                                barcode = finalBarcode
                            )

                            if (finalBarcode != null && finalBarcode != itemBarcode.text) {
                                withContext(Dispatchers.Main) { Toast.makeText(context, "Aptiktas barkodas: $finalBarcode", Toast.LENGTH_LONG).show() }
                            }

                            if (printBarcode) {
                                withContext(Dispatchers.Main) {
                                    PrintManager.printItemLabel(
                                        context = context,
                                        groupName = selectedGroup!!.name,
                                        code = currentCode,
                                        barcode = finalBarcode,
                                        comment = comment.text.trim().takeIf { it.isNotBlank() }
                                    )
                                }
                            }

                            val activeStorageMode = context.getSharedPreferences("MarkItSettings", Context.MODE_PRIVATE).getString("active_storage_mode", "local")
                            if (activeStorageMode == "google_drive") {
                                val driveFileId = GoogleDriveService.uploadPhoto(context, selectedGroup!!.name, fileName, finalBytes)
                                if (driveFileId != null && itemId > 0) {
                                    dbHelper.updateItemDriveId(itemId, driveFileId)
                                    withContext(Dispatchers.Main) { Toast.makeText(context, s.saveSyncSuccess, Toast.LENGTH_SHORT).show() }
                                }
                            } else {
                                if (itemId > 0) withContext(Dispatchers.Main) { Toast.makeText(context, "Išsaugota!", Toast.LENGTH_SHORT).show() }
                            }

                            withContext(Dispatchers.Main) {
                                itemCode = TextFieldValue(UUID.randomUUID().toString().take(8).uppercase())
                                itemBarcode = TextFieldValue("")
                                comment = TextFieldValue("")
                                isCapturing = false
                                isCameraExpanded = false // Collapse camera after saving
                                codeFocus.requestFocus()
                            }
                        } catch (e: Exception) {
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

    fun saveWithoutPhoto() {
        if (selectedGroup == null) {
            Toast.makeText(context, s.selectGroupFirst, Toast.LENGTH_SHORT).show()
            return
        }
        if (isCapturing) return
        isCapturing = true

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val currentCode = itemCode.text.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString().take(8).uppercase()
                
                val finalBarcode = if (itemBarcode.text.isNotBlank()) {
                    itemBarcode.text
                } else {
                    null
                }

                val itemId = dbHelper.createItem(
                    groupId = selectedGroup!!.id,
                    code = currentCode,
                    comment = comment.text.trim().takeIf { it.isNotBlank() },
                    date = currentDate,
                    localPhotoPath = null,
                    driveFileId = null,
                    barcode = finalBarcode
                )

                if (printBarcode) {
                    withContext(Dispatchers.Main) {
                        PrintManager.printItemLabel(
                            context = context,
                            groupName = selectedGroup!!.name,
                            code = currentCode,
                            barcode = finalBarcode,
                            comment = comment.text.trim().takeIf { it.isNotBlank() }
                        )
                    }
                }

                if (itemId > 0) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Išsaugota (be nuotraukos)!", Toast.LENGTH_SHORT).show()
                    }
                }

                withContext(Dispatchers.Main) {
                    itemCode = TextFieldValue(UUID.randomUUID().toString().take(8).uppercase())
                    itemBarcode = TextFieldValue("")
                    comment = TextFieldValue("")
                    isCapturing = false
                    codeFocus.requestFocus()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "${s.dbSaveError}: ${e.message}", Toast.LENGTH_LONG).show()
                    isCapturing = false
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (isCameraExpanded) {
            // Full Screen Camera Mode
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                if (hasCameraPermission) {
                    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                    
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Užfiksuoti (Capture) Button
                        Button(
                            onClick = { captureAndSaveImage() },
                            enabled = !isCapturing && selectedGroup != null,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            if (isCapturing) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PhotoCamera, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(s.captureBtn, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }

                        // Grįžti (Cancel) Button
                        Button(
                            onClick = { isCameraExpanded = false },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(s.cancel, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                            Text("Kamerai reikalingas leidimas", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Suteikti leidima") }
                        }
                    }
                }
            }
        } else {
            // Standard Form Mode (Camera collapsed)
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (isGroupsLoading) CircularProgressIndicator()
                            else if (groups.isEmpty()) Text(text = s.noGroupsTitle, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            else {
                                ExposedDropdownMenuBox(expanded = isDropdownExpanded, onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }) {
                                    OutlinedTextField(
                                        readOnly = true, value = selectedGroup?.name ?: s.selectGroup, onValueChange = {}, label = { Text(s.groupLabel) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(focusedContainerColor = Color(0xFFF2F2F2), unfocusedContainerColor = Color(0xFFF9F9F9)),
                                        modifier = Modifier.fillMaxWidth().menuAnchor()
                                    )
                                    ExposedDropdownMenu(expanded = isDropdownExpanded, onDismissRequest = { isDropdownExpanded = false }) {
                                        groups.forEach { group -> DropdownMenuItem(text = { Text(group.name) }, onClick = { selectedGroup = group; isDropdownExpanded = false }) }
                                    }
                                }
                            }

                            fun fieldModifier(myFocus: FocusRequester, nextFocus: FocusRequester?, fieldName: String): Modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(myFocus)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        when(fieldName) {
                                            "code" -> itemCode = itemCode.copy(selection = TextRange(0, itemCode.text.length))
                                            "barcode" -> itemBarcode = itemBarcode.copy(selection = TextRange(0, itemBarcode.text.length))
                                            "comment" -> comment = comment.copy(selection = TextRange(0, comment.text.length))
                                        }
                                    }
                                }
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.key == Key.Tab || keyEvent.key == Key.Enter) {
                                        if (nextFocus != null) nextFocus.requestFocus() else focusManager.clearFocus()
                                        true
                                    } else false
                                }

                            // 1. Kodas field
                            OutlinedTextField(
                                value = itemCode, onValueChange = { itemCode = it }, label = { Text(s.groupFieldCode) }, singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { barcodeFocus.requestFocus() }),
                                trailingIcon = { IconButton(onClick = { triggerBarcodeScanner("code") }) { Icon(Icons.Default.QrCode, s.scanBarcode) } },
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFF2F2F2), unfocusedContainerColor = Color(0xFFF9F9F9)),
                                modifier = fieldModifier(codeFocus, barcodeFocus, "code")
                            )

                            // 2. Barkodas field
                            OutlinedTextField(
                                value = itemBarcode, onValueChange = { itemBarcode = it }, label = { Text(s.groupFieldBarcode) }, singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { commentFocus.requestFocus() }),
                                trailingIcon = { IconButton(onClick = { triggerBarcodeScanner("barcode") }) { Icon(Icons.Default.QrCode, s.scanBarcode) } },
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFF2F2F2), unfocusedContainerColor = Color(0xFFF9F9F9)),
                                modifier = fieldModifier(barcodeFocus, commentFocus, "barcode")
                            )

                            // 3. Comment field
                            OutlinedTextField(
                                value = comment, onValueChange = { comment = it }, label = { Text(s.commentLabel) }, singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFF2F2F2), unfocusedContainerColor = Color(0xFFF9F9F9)),
                                modifier = fieldModifier(commentFocus, null, "comment")
                            )
                        }
                    }

                    // Two Action Buttons positioned directly under the Comment field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Button 1: Save (crossed-out camera icon) - DB only, optional Auto Print
                        Button(
                            onClick = { saveWithoutPhoto() },
                            enabled = !isCapturing && selectedGroup != null,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Icon(Icons.Default.NoPhotography, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Išsaugoti", fontWeight = FontWeight.Bold)
                        }

                        // Button 2: Save with photo (camera icon) - Opens camera
                        Button(
                            onClick = { isCameraExpanded = true },
                            enabled = selectedGroup != null,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Išsaugoti", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun adjustBitmap(bitmap: Bitmap, brightness: Float, contrast: Float): Bitmap {
    val adjustedBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.height, false)
    val canvas = android.graphics.Canvas(adjustedBitmap)
    val paint = android.graphics.Paint()
    val cm = android.graphics.ColorMatrix(floatArrayOf(
        contrast, 0f, 0f, 0f, brightness,
        0f, contrast, 0f, 0f, brightness,
        0f, 0f, contrast, 0f, brightness,
        0f, 0f, 0f, 1f, 0f
    ))
    paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return adjustedBitmap
}
