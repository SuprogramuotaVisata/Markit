package com.suprogramuotavisata.markit.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

/**
 * A reusable real-time scanning widget that supports Barcode and Text recognition.
 * Features a dynamic green target frame that adjusts to text length.
 */
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun LiveScanner(
    onResult: (String) -> Unit,
    isScanning: Boolean,
    modifier: Modifier = Modifier,
    height: Int = 110
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    
    // Camera Components
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(android.util.Size(640, 480))
            .build()
    }

    // Detection State
    var detectedText by remember { mutableStateOf("") }
    var lastAnalysisTime by remember { mutableStateOf(0L) }
    
    // ML Kit Clients
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    // Permission handling
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { hasCameraPermission = it }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Keep screen on while scanner is visible
    DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Bind CameraX Lifecycle
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                
                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastAnalysisTime < 200) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    lastAnalysisTime = currentTime

                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        
                        barcodeScanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                if (barcodes.isNotEmpty()) {
                                    detectedText = barcodes.first().rawValue ?: ""
                                } else {
                                    textRecognizer.process(inputImage)
                                        .addOnSuccessListener { visionText ->
                                            val blocks = visionText.textBlocks
                                            if (blocks.isNotEmpty()) {
                                                val imgWidth = if (imageProxy.imageInfo.rotationDegrees % 180 == 0) imageProxy.width else imageProxy.height
                                                val imgHeight = if (imageProxy.imageInfo.rotationDegrees % 180 == 0) imageProxy.height else imageProxy.width
                                                
                                                val centerX = imgWidth / 2
                                                val centerY = imgHeight / 2
                                                
                                                // Strictly filter blocks that are within the central area
                                                val filteredBlocks = blocks.filter { block ->
                                                    val b = block.boundingBox ?: return@filter false
                                                    val text = block.text.trim()
                                                    if (text.length !in 5..128) return@filter false
                                                    
                                                    val withinX = b.centerX() > imgWidth * 0.15 && b.centerX() < imgWidth * 0.85
                                                    val withinY = b.centerY() > imgHeight * 0.25 && b.centerY() < imgHeight * 0.75
                                                    withinX && withinY
                                                }

                                                val bestBlock = filteredBlocks.minByOrNull { block ->
                                                    val b = block.boundingBox ?: Rect(0,0,0,0)
                                                    val dx = b.centerX() - centerX
                                                    val dy = b.centerY() - centerY
                                                    dx*dx + dy*dy
                                                }
                                                detectedText = bestBlock?.text?.trim()?.lines()?.firstOrNull() ?: ""
                                            } else {
                                                detectedText = ""
                                            }
                                        }
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                        imageAnalysis
                    )
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                } catch (e: Exception) {
                    Log.e("LiveScanner", "Camera binding failed", e)
                }
            }, mainExecutor)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(2.5.dp, Color(0xFFFFD54F), RoundedCornerShape(12.dp)) // Yellow Outer
            .clickable { 
                if (detectedText.isNotEmpty()) {
                    onResult(detectedText)
                    Toast.makeText(context, "Nuskaityta: $detectedText", Toast.LENGTH_SHORT).show()
                }
            }
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Real-time feedback overlay (Dynamic Green Rectangle)
            if (detectedText.isNotEmpty()) {
                val targetWidth = (detectedText.length * 11 + 50).dp.coerceIn(130.dp, 280.dp)
                val animatedWidth by animateDpAsState(
                    targetValue = targetWidth,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                    label = "dynamicWidth"
                )
                
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(animatedWidth)
                        .height(44.dp)
                        .border(2.5.dp, Color(0xFF4CAF50), RoundedCornerShape(6.dp))
                        .background(Color(0x774CAF50))
                ) {
                    Text(
                        text = detectedText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 8.dp)
                    )
                }
            } else {
                // Static target guide
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(160.dp)
                        .height(40.dp)
                        .border(1.2.dp, Color(0xFF4CAF50).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                )
            }

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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .offset(y = height.dp * laserY)
                    .background(Color.Red.copy(alpha = 0.4f))
            )

            if (isScanning) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text("Reikalingas kameros leidimas", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}
