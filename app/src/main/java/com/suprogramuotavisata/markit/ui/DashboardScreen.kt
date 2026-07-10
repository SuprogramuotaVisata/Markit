package com.suprogramuotavisata.markit.ui

import android.widget.Toast
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import com.google.mlkit.vision.text.Text
import com.suprogramuotavisata.markit.data.BarcodeGenerator
import com.suprogramuotavisata.markit.theme.MarkItTheme
import com.suprogramuotavisata.markit.data.EnStrings
import com.suprogramuotavisata.markit.data.DatabaseHelper
import com.suprogramuotavisata.markit.data.GoogleDriveService
import com.suprogramuotavisata.markit.data.LocalAppStrings
import com.suprogramuotavisata.markit.data.ProductGroup
import com.suprogramuotavisata.markit.data.PrintManager
import com.suprogramuotavisata.markit.utils.BarcodeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BeeLogo(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Wings
        drawOval(
            color = Color(0xAA80DEEA),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.1f),
            size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.4f)
        )
        drawOval(
            color = Color(0xAA80DEEA),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.45f, h * 0.1f),
            size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.4f)
        )
        
        // Body
        drawOval(
            color = Color(0xFFFFD54F),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.35f),
            size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.45f)
        )
        
        // Stripes
        drawRect(
            color = Color(0xFF212121),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.37f, h * 0.35f),
            size = androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.45f)
        )
        drawRect(
            color = Color(0xFF212121),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.55f, h * 0.35f),
            size = androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.45f)
        )
        
        // Head
        drawCircle(
            color = Color(0xFF212121),
            radius = w * 0.12f,
            center = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.58f)
        )
        
        // Eye
        drawCircle(
            color = Color.White,
            radius = w * 0.03f,
            center = androidx.compose.ui.geometry.Offset(w * 0.21f, h * 0.55f)
        )
        
        // Stinger
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.75f, h * 0.5f)
            lineTo(w * 0.88f, h * 0.58f)
            lineTo(w * 0.75f, h * 0.65f)
            close()
        }
        drawPath(path = path, color = Color(0xFF212121))
    }
}

@Composable
fun DashboardScreen(
    onNavigateToStore: (String) -> Unit
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val coroutineScope = rememberCoroutineScope()
    val s = LocalAppStrings.current

    var groups by remember { mutableStateOf<List<ProductGroup>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf(TextFieldValue("")) }
    var newGroupCode by remember { mutableStateOf(TextFieldValue("")) }
    var newGroupBarcode by remember { mutableStateOf(TextFieldValue("")) }
    var newGroupThereIs by remember { mutableStateOf(TextFieldValue("")) }
    var newGroupDescription by remember { mutableStateOf(TextFieldValue("")) }
    var isCreatingGroup by remember { mutableStateOf(false) }

    // Camera states for dashboard scanner
    val lifecycleOwner = LocalLifecycleOwner.current
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

    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }
    var isScanning by remember { mutableStateOf(false) }

    // Bind camera lifecycle reactively
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = CameraPreview.Builder().build()
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
                    Log.e("DashboardScreen", "Camera binding failed", e)
                }
            }, mainExecutor)
        }
    }

    // Automatically request camera permission on screen launch
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Scanner logic
    fun captureAndScan() {
        if (isScanning) return
        isScanning = true

        imageCapture.takePicture(
            mainExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    coroutineScope.launch(Dispatchers.Default) {
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

                            val bmpWidth = bitmap.width
                            val bmpHeight = bitmap.height
                            val cropWidth = (bmpWidth * 0.70f).toInt()
                            val cropHeight = (bmpHeight * 0.35f).toInt()
                            val startX = (bmpWidth - cropWidth) / 2
                            val startY = (bmpHeight - cropHeight) / 2

                            val croppedBitmap = Bitmap.createBitmap(bitmap, startX, startY, cropWidth, cropHeight)
                            val inputImage = InputImage.fromBitmap(croppedBitmap, 0)

                            val barcodeScanner = BarcodeScanning.getClient()
                            barcodeScanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    val firstBarcode = barcodes.firstOrNull()?.rawValue?.trim() ?: ""
                                    if (firstBarcode.isNotEmpty()) {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            val matchingGroup = dbHelper.getAllGroups().find {
                                                it.code.equals(firstBarcode, ignoreCase = true) || it.barcode?.equals(firstBarcode) == true
                                            }
                                            if (matchingGroup != null) {
                                                withContext(Dispatchers.Main) {
                                                    onNavigateToStore(matchingGroup.name)
                                                    Toast.makeText(context, "Rasta grupė: ${matchingGroup.name}", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                val matchingItem = dbHelper.getAllItems().find {
                                                    it.code.equals(firstBarcode, ignoreCase = true) || it.barcode?.equals(firstBarcode) == true
                                                }
                                                if (matchingItem != null) {
                                                    withContext(Dispatchers.Main) {
                                                        onNavigateToStore(matchingItem.groupName)
                                                        Toast.makeText(context, "Rastas daiktas grupėje: ${matchingItem.groupName}", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(context, "Kodas nerastas: $firstBarcode", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }
                                        isScanning = false
                                    } else {
                                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                        recognizer.process(inputImage)
                                            .addOnSuccessListener { visionText ->
                                                val detectedText = visionText.text.trim().lines().firstOrNull { it.isNotBlank() }?.trim() ?: ""
                                                if (detectedText.isNotEmpty()) {
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        val matchingGroup = dbHelper.getAllGroups().find {
                                                            it.name.contains(detectedText, ignoreCase = true) || it.code.contains(detectedText, ignoreCase = true)
                                                        }
                                                        if (matchingGroup != null) {
                                                            withContext(Dispatchers.Main) {
                                                                onNavigateToStore(matchingGroup.name)
                                                                Toast.makeText(context, "Rasta grupė pagal tekstą: ${matchingGroup.name}", Toast.LENGTH_SHORT).show()
                                                            }
                                                        } else {
                                                            val matchingItem = dbHelper.getAllItems().find {
                                                                it.code.contains(detectedText, ignoreCase = true) || it.comment?.contains(detectedText, ignoreCase = true) == true
                                                            }
                                                            if (matchingItem != null) {
                                                                withContext(Dispatchers.Main) {
                                                                    onNavigateToStore(matchingItem.groupName)
                                                                    Toast.makeText(context, "Rastas daiktas pagal tekstą grupėje: ${matchingItem.groupName}", Toast.LENGTH_SHORT).show()
                                                                }
                                                            } else {
                                                                withContext(Dispatchers.Main) {
                                                                    Toast.makeText(context, "Tekstas nerastas: $detectedText", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Nepavyko aptikti nei barkodo, nei teksto rėmelyje", Toast.LENGTH_SHORT).show()
                                                }
                                                isScanning = false
                                            }
                                            .addOnFailureListener {
                                                isScanning = false
                                            }
                                    }
                                }
                                .addOnFailureListener {
                                    isScanning = false
                                }
                        } catch (e: Exception) {
                            Log.e("DashboardScreen", "Scan error", e)
                            isScanning = false
                        }
                    }
                }

                override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                    Toast.makeText(context, "Klaida fotografuojant: ${exception.message}", Toast.LENGTH_SHORT).show()
                    isScanning = false
                }
            }
        )
    }

    // Function to reload groups
    fun loadGroups() {
        coroutineScope.launch {
            isLoading = true
            groups = withContext(Dispatchers.IO) {
                dbHelper.getAllGroups()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadGroups()
    }

    DashboardScreenContent(
        groups = groups,
        isLoading = isLoading,
        onNavigateToStore = onNavigateToStore,
        onAddGroupClick = {
            val suggestedNum = groups.size + 1
            val text = "Grupe $suggestedNum"
            newGroupName = TextFieldValue(text = text, selection = TextRange(0, text.length))
            val uniqueCode = java.util.UUID.randomUUID().toString().take(8).uppercase()
            newGroupCode = TextFieldValue(text = uniqueCode, selection = TextRange(0, uniqueCode.length))
            newGroupBarcode = TextFieldValue("")
            newGroupThereIs = TextFieldValue("")
            newGroupDescription = TextFieldValue("")
            showAddDialog = true
        },
        hasCameraPermission = hasCameraPermission,
        previewView = previewView,
        isScanning = isScanning,
        onScanClick = { captureAndScan() },
        showAddDialog = showAddDialog,
        isCreatingGroup = isCreatingGroup,
        newGroupName = newGroupName,
        newGroupCode = newGroupCode,
        newGroupBarcode = newGroupBarcode,
        newGroupThereIs = newGroupThereIs,
        newGroupDescription = newGroupDescription,
        onGroupNameChange = { newGroupName = it },
        onGroupCodeChange = { newGroupCode = it },
        onGroupBarcodeChange = { newGroupBarcode = it },
        onGroupThereIsChange = { newGroupThereIs = it },
        onGroupDescriptionChange = { newGroupDescription = it },
        onDismissAddDialog = { showAddDialog = false },
        onConfirmAddGroup = {
            isCreatingGroup = true
            coroutineScope.launch {
                val name = newGroupName.text.trim()
                val code = newGroupCode.text.trim()
                val barcode = newGroupBarcode.text.trim().takeIf { it.isNotBlank() }
                val thereIs = newGroupThereIs.text.trim().takeIf { it.isNotBlank() }
                val description = newGroupDescription.text.trim().takeIf { it.isNotBlank() }

                val existing = withContext(Dispatchers.IO) { dbHelper.getGroupByName(name) }
                if (existing != null) {
                    Toast.makeText(context, s.groupAlreadyExists, Toast.LENGTH_SHORT).show()
                    isCreatingGroup = false
                    return@launch
                }

                val sharedPrefs = context.getSharedPreferences("MarkItSettings", Context.MODE_PRIVATE)
                val activeStorageMode = sharedPrefs.getString("active_storage_mode", "local") ?: "local"

                // Create folder on Google Drive in background only if configured
                val driveFolderId = if (activeStorageMode == "google_drive") {
                    withContext(Dispatchers.IO) {
                        GoogleDriveService.findOrCreateCategoryFolder(context, name)
                    }
                } else {
                    null
                }

                // Save in local SQLite
                val success = withContext(Dispatchers.IO) {
                    dbHelper.createGroup(name, driveFolderId, code, barcode, thereIs, description)
                }

                isCreatingGroup = false
                showAddDialog = false
                if (success > -1) {
                    Toast.makeText(context, s.groupCreated, Toast.LENGTH_SHORT).show()
                    loadGroups()
                } else {
                    Toast.makeText(context, s.dbSaveError, Toast.LENGTH_SHORT).show()
                }
            }
        }
    )
}

@Composable
fun DashboardScreenContent(
    groups: List<ProductGroup>,
    isLoading: Boolean,
    onNavigateToStore: (String) -> Unit,
    onAddGroupClick: () -> Unit,
    hasCameraPermission: Boolean,
    previewView: PreviewView?,
    isScanning: Boolean,
    onScanClick: () -> Unit,
    showAddDialog: Boolean,
    isCreatingGroup: Boolean,
    newGroupName: TextFieldValue,
    newGroupCode: TextFieldValue,
    newGroupBarcode: TextFieldValue,
    newGroupThereIs: TextFieldValue,
    newGroupDescription: TextFieldValue,
    onGroupNameChange: (TextFieldValue) -> Unit,
    onGroupCodeChange: (TextFieldValue) -> Unit,
    onGroupBarcodeChange: (TextFieldValue) -> Unit,
    onGroupThereIsChange: (TextFieldValue) -> Unit,
    onGroupDescriptionChange: (TextFieldValue) -> Unit,
    onDismissAddDialog: () -> Unit,
    onConfirmAddGroup: () -> Unit
) {
    val s = LocalAppStrings.current
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddGroupClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = s.createGroup)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            // Styled header with Bee Logo and slogan
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                BeeLogo(modifier = Modifier.size(54.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = s.sloganText,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (groups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = s.noGroupsTitle,
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = s.noGroupsTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = s.noGroupsDesc,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(groups) { group ->
                        GroupCard(
                            group = group,
                            onClick = { onNavigateToStore(group.name) }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
            AlertDialog(
                onDismissRequest = { if (!isCreatingGroup) onDismissAddDialog() },
                title = { Text(s.createGroup) },
                text = {
                    Column {
                        Text(
                            s.enterGroupName,
                            modifier = Modifier.padding(bottom = 8.dp),
                            fontSize = 14.sp
                        )
                        OutlinedTextField(
                            value = newGroupName,
                            onValueChange = onGroupNameChange,
                            label = { Text(s.groupName) },
                            singleLine = true,
                            enabled = !isCreatingGroup,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF2F2F2),
                                unfocusedContainerColor = Color(0xFFF9F9F9)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                        OutlinedTextField(
                            value = newGroupCode,
                            onValueChange = onGroupCodeChange,
                            label = { Text(s.groupFieldCode) },
                            singleLine = true,
                            enabled = !isCreatingGroup,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF2F2F2),
                                unfocusedContainerColor = Color(0xFFF9F9F9)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                        OutlinedTextField(
                            value = newGroupBarcode,
                            onValueChange = onGroupBarcodeChange,
                            label = { Text(s.groupFieldBarcode) },
                            singleLine = true,
                            enabled = !isCreatingGroup,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF2F2F2),
                                unfocusedContainerColor = Color(0xFFF9F9F9)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                        OutlinedTextField(
                            value = newGroupThereIs,
                            onValueChange = onGroupThereIsChange,
                            label = { Text(s.groupFieldThereIsIt) },
                            singleLine = true,
                            enabled = !isCreatingGroup,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF2F2F2),
                                unfocusedContainerColor = Color(0xFFF9F9F9)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                        OutlinedTextField(
                            value = newGroupDescription,
                            onValueChange = onGroupDescriptionChange,
                            label = { Text(s.groupFieldDescription) },
                            singleLine = true,
                            enabled = !isCreatingGroup,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF2F2F2),
                                unfocusedContainerColor = Color(0xFFF9F9F9)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )

                        if (isCreatingGroup) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Text(s.creatingGroupFolder, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = newGroupName.text.isNotBlank() && !isCreatingGroup,
                        onClick = onConfirmAddGroup
                    ) {
                        Text(s.create)
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isCreatingGroup,
                        onClick = onDismissAddDialog
                    ) {
                        Text(s.cancel)
                    }
                }
            )
        }
    }
}

@Composable
fun GroupCard(
    group: ProductGroup,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val s = LocalAppStrings.current
    val sharedPrefs = remember { context.getSharedPreferences("MarkItSettings", Context.MODE_PRIVATE) }
    
    val printerType = sharedPrefs.getString("printer_type", "system") ?: "system"
    val printerIp = sharedPrefs.getString("printer_ip", "") ?: ""
    val printerBt = sharedPrefs.getString("printer_bt", "") ?: ""
    
    val isPrinterConnected = when (printerType) {
        "system" -> true
        "network" -> printerIp.isNotBlank()
        "bluetooth" -> printerBt.isNotBlank()
        else -> false
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
//                Box(
//                    modifier = Modifier
//                        .size(36.dp)
//                        .background(
//                            MaterialTheme.colorScheme.primaryContainer,
//                            RoundedCornerShape(8.dp)
//                        ),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Folder,
//                        contentDescription = null,
//                        tint = MaterialTheme.colorScheme.primary,
//                        modifier = Modifier.size(20.dp)
//                    )
//                }
                
                val hasDrive = !group.driveFolderId.isNullOrBlank()
                Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(
                        text = group.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasDrive) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = if (hasDrive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                    )
                    
                    IconButton(
                        onClick = { 
                            PrintManager.printGroupLabel(context, group)
                        },
                        enabled = isPrinterConnected,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print Label",
                            tint = if (isPrinterConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            
            if (group.code.isNotBlank()) {
                Text(
                    text = "${s.groupFieldCode}: ${group.code}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
            if (!group.barcode.isNullOrBlank()) {
                Text(
                    text = "${s.groupFieldBarcode}: ${group.barcode}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 1.dp)
                )
                var br : Bitmap? = BarcodeGenerator.generateBarcode(group.barcode,
                    width = 500, height = 80)
               
                if(br != null) {
                    BarcodeUtils.BarcodeBitmap(
                        br,
                        group.barcode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(top = 1.dp)
                    )
                }
            }
            if (!group.padetaTen.isNullOrBlank()) {
                Text(
                    text = "${s.groupFieldThereIsIt}: ${group.padetaTen}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
            if (!group.description.isNullOrBlank()) {
                Text(
                    text = group.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    MarkItTheme {
        CompositionLocalProvider(LocalAppStrings provides EnStrings) {
            DashboardScreenContent(
                groups = listOf(
                    ProductGroup(id = 1, name = "Electronics", code = "ELEC",barcode="123456", description = "Gadgets and stuff"),
                    ProductGroup(id = 2, name = "Clothing", code = "CLOT", barcode="123456",padetaTen = "Shelf A1"),
                    ProductGroup(id = 3, name = "Kitchen", code = "KITCH",barcode="123456", description = "Utensils")
                ),
                isLoading = false,
                onNavigateToStore = {},
                onAddGroupClick = {},
                hasCameraPermission = true,
                previewView = null,
                isScanning = false,
                onScanClick = {},
                showAddDialog = false,
                isCreatingGroup = false,
                newGroupName = TextFieldValue(""),
                newGroupCode = TextFieldValue(""),
                newGroupBarcode = TextFieldValue(""),
                newGroupThereIs = TextFieldValue(""),
                newGroupDescription = TextFieldValue(""),
                onGroupNameChange = {},
                onGroupCodeChange = {},
                onGroupBarcodeChange = {},
                onGroupThereIsChange = {},
                onGroupDescriptionChange = {},
                onDismissAddDialog = {},
                onConfirmAddGroup = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenEmptyPreview() {
    MarkItTheme {
        CompositionLocalProvider(LocalAppStrings provides EnStrings) {
            DashboardScreenContent(
                groups = emptyList(),
                isLoading = false,
                onNavigateToStore = {},
                onAddGroupClick = {},
                hasCameraPermission = false,
                previewView = null,
                isScanning = false,
                onScanClick = {},
                showAddDialog = false,
                isCreatingGroup = false,
                newGroupName = TextFieldValue(""),
                newGroupCode = TextFieldValue(""),
                newGroupBarcode = TextFieldValue(""),
                newGroupThereIs = TextFieldValue(""),
                newGroupDescription = TextFieldValue(""),
                onGroupNameChange = {},
                onGroupCodeChange = {},
                onGroupBarcodeChange = {},
                onGroupThereIsChange = {},
                onGroupDescriptionChange = {},
                onDismissAddDialog = {},
                onConfirmAddGroup = {}
            )
        }
    }
}
