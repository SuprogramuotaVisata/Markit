package com.suprogramuotavisata.markit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.ui.draw.clip
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.text.TextRecognition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.suprogramuotavisata.markit.data.DatabaseHelper
import com.suprogramuotavisata.markit.data.LocalAppStrings
import com.suprogramuotavisata.markit.data.ProductGroup
import com.suprogramuotavisata.markit.data.ProductItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyStoreScreen(
    initialGroupFilter: String? = null
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val coroutineScope = rememberCoroutineScope()
    val s = LocalAppStrings.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Query inputs
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    val selectedGroupIds = remember { mutableStateListOf<Long>() }
    var sortBy by remember { mutableStateOf("date") } // "date" or "code"
    var sortDesc by remember { mutableStateOf(true) }

    var reloadTrigger by remember { mutableStateOf(0) }

    // Database states
    var allGroups by remember { mutableStateOf<List<ProductGroup>>(emptyList()) }
    val itemsLiveData = remember { androidx.lifecycle.MutableLiveData<List<ProductItem>>() }
    val itemsList by itemsLiveData.observeAsState(emptyList())
    var isLoading by remember { mutableStateOf(true) }

    // Dialog state
    var selectedViewItem by remember { mutableStateOf<ProductItem?>(null) }

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

    // Camera operational state
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }
    var isScanning by remember { mutableStateOf(false) }

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
                    Log.e("MyStoreScreen", "Camera binding failed", e)
                }
            }, mainExecutor)
        }
    }

    // Capture and Scan logic
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

                            // Decode original bitmap
                            var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (rotationDegrees != 0) {
                                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                            }

                            // Crop the center region representing our visual focus frame (70% width, 35% height)
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
                                    if (barcodes.isNotEmpty()) {
                                        val code = barcodes.first().rawValue ?: ""
                                        searchQuery = TextFieldValue(text = code, selection = TextRange(0, code.length))
                                        Toast.makeText(context, "Aptiktas barkodas: $code", Toast.LENGTH_SHORT).show()
                                        isScanning = false
                                    } else {
                                        // Try text recognition
                                        val recognizer = TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS)
                                        recognizer.process(inputImage)
                                            .addOnSuccessListener { visionText ->
                                                val textLines = visionText.text.trim().lines()
                                                val detectedText = textLines.firstOrNull { it.isNotBlank() }?.trim() ?: ""
                                                if (detectedText.isNotEmpty()) {
                                                    searchQuery = TextFieldValue(text = detectedText, selection = TextRange(0, detectedText.length))
                                                    Toast.makeText(context, "Aptiktas tekstas: $detectedText", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Nepavyko aptikti nei barkodo, nei teksto rėmelyje", Toast.LENGTH_SHORT).show()
                                                }
                                                isScanning = false
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "Teksto atpažinimo klaida: ${e.message}", Toast.LENGTH_SHORT).show()
                                                isScanning = false
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Barkodo skenavimo klaida: ${e.message}", Toast.LENGTH_SHORT).show()
                                    isScanning = false
                                }
                        } catch (e: Exception) {
                            Log.e("MyStoreScreen", "Skenavimo klaida", e)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Klaida apdorojant kadrą: ${e.message}", Toast.LENGTH_SHORT).show()
                                isScanning = false
                            }
                        }
                    }
                }

                override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                    Toast.makeText(context, "Fotografavimo klaida: ${exception.message}", Toast.LENGTH_SHORT).show()
                    isScanning = false
                }
            }
        )
    }



    // Load groups and set initial filter
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            allGroups = dbHelper.getAllGroups()
        }

        // Apply initial filter if redirected from Dashboard card
        if (initialGroupFilter != null) {
            val matchingGroup = allGroups.find { it.name.equals(initialGroupFilter, ignoreCase = true) }
            if (matchingGroup != null) {
                selectedGroupIds.add(matchingGroup.id)
            }
        }
    }

    // Load matching items whenever query, filters, or sorting configurations change
    LaunchedEffect(searchQuery.text, selectedGroupIds.size, sortBy, sortDesc, allGroups, reloadTrigger) {
        isLoading = true
        coroutineScope.launch {
            val list = withContext(Dispatchers.IO) {
                dbHelper.getAllItems(
                    groupIds = if (selectedGroupIds.isEmpty()) null else selectedGroupIds.toList(),
                    searchQuery = searchQuery.text.takeIf { it.isNotBlank() },
                    sortBy = sortBy,
                    sortDesc = sortDesc
                )
            }
            itemsLiveData.postValue(list)
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = s.myStore,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Camera Preview for scanning barcode or text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                // Laser scan animation
                val infiniteTransition = rememberInfiniteTransition(label = "scanLaser")
                val laserY by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "laserPosition"
                )

                // Visual target focusing frame centered
                Box(
                    modifier = Modifier
                        .size(width = 240.dp, height = 90.dp)
                        .align(Alignment.Center)
                        .border(2.dp, Color(0xFF4CAF50), RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    // Animated laser line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .offset(y = 90.dp * laserY)
                            .background(Color.Red)
                    )
                }

                // Overlaid Scan Button
                Button(
                    onClick = { captureAndScan() },
                    enabled = !isScanning,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Skenuoti", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Prašome suteikti kameros leidimą programos nustatymuose.",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // Search text box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(s.searchPlaceholder) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF2F2F2),
                unfocusedContainerColor = Color(0xFFF9F9F9)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        searchQuery = searchQuery.copy(selection = TextRange(0, searchQuery.text.length))
                    }
                }
        )

        // Group filters row
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                Text(s.filterGroups, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = selectedGroupIds.isEmpty(),
                    onClick = { selectedGroupIds.clear() },
                    label = { Text(s.all) }
                )
                allGroups.forEach { group ->
                    val isSelected = selectedGroupIds.contains(group.id)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) {
                                selectedGroupIds.remove(group.id)
                            } else {
                                selectedGroupIds.add(group.id)
                            }
                        },
                        label = { Text(group.name) }
                    )
                }
            }
        }

        // Sorting & Results Summary Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${s.foundCount}: ${itemsList.size}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Toggle Sort Attribute
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        sortBy = if (sortBy == "date") "code" else "date"
                    }
                ) {
                    Icon(Icons.Default.SortByAlpha, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (sortBy == "date") s.sortByDate else s.sortByCode,
                        fontSize = 12.sp
                    )
                }

                // Toggle Sort Order
                Text(
                    text = if (sortDesc) s.sortDesc else s.sortAsc,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { sortDesc = !sortDesc }
                        .padding(vertical = 4.dp)
                )
            }
        }

        // Items List
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (itemsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = s.noItemsFound,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(itemsList, key = { it.id }) { item ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    dbHelper.deleteItem(item.id)
                                    item.localPhotoPath?.let { path ->
                                        try {
                                            val file = File(path)
                                            if (file.exists()) {
                                                file.delete()
                                            }
                                        } catch (e: Exception) {
                                            Log.e("MyStoreScreen", "Klaida šalinant nuotrauką", e)
                                        }
                                    }
                                    withContext(Dispatchers.Main) {
                                        reloadTrigger++
                                        Toast.makeText(context, "Įrašas ir nuotrauka pašalinti!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                Color.Red
                            } else {
                                Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Pašalinti",
                                    tint = Color.White
                                )
                            }
                        },
                        enableDismissFromStartToEnd = false,
                        content = {
                            ItemCard(
                                item = item,
                                onViewImageClick = { selectedViewItem = item }
                            )
                        }
                    )
                }
            }
        }
    }

    // Photo Viewer Dialog Popup
    if (selectedViewItem != null) {
        val item = selectedViewItem!!
        val file = item.localPhotoPath?.let { File(it) }
        val hasLocalFile = file != null && file.exists()

        Dialog(
            onDismissRequest = { selectedViewItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
            ) {
                // Top control bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = item.code,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${s.categoryLabel}: ${item.groupName}",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                    IconButton(
                        onClick = { selectedViewItem = null },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = s.cancel, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Main Image Display
                if (hasLocalFile) {
                    AsyncImage(
                        model = file,
                        contentDescription = "Prekes nuotrauka",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.8f)
                            .align(Alignment.Center)
                    )
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            s.photoNotFound,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        if (!item.driveFileId.isNullOrBlank()) {
                            Text(
                                "${s.uploadedToDrive} (ID: ${item.driveFileId.take(12)}...)",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                // Bottom Metadata Info
                if (!item.comment.isNullOrBlank() || item.date.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (!item.comment.isNullOrBlank()) {
                                Text(
                                    text = "${s.commentLabel}: ${item.comment}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(
                                text = "${s.dateLabel}: ${item.date}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemCard(
    item: ProductItem,
    onViewImageClick: () -> Unit
) {
    val s = LocalAppStrings.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.groupName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Text(
                        text = item.date.split(" ").firstOrNull() ?: "",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${s.barcodeLabel}: ${item.code}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))
                val displayComment = if (item.comment.isNullOrBlank()) "-" else item.comment
                Text(
                    text = "${s.commentLabel}: $displayComment",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onViewImageClick,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(50))
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = s.viewPhoto,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
