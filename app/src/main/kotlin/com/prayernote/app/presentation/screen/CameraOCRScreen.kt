package com.prayernote.app.presentation.screen

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import com.prayernote.app.presentation.viewmodel.TextRecognitionState
import com.prayernote.app.presentation.viewmodel.TextRecognitionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraOCRScreen(
    onTextRecognized: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: TextRecognitionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    var showCapturedImage by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    
    val recognitionState by viewModel.recognitionState.collectAsState()

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.recognizeTextFromUri(it) { imageUri ->
                loadBitmapFromUri(context, imageUri)
            }
        }
    }

    LaunchedEffect(recognitionState) {
        when (val state = recognitionState) {
            is TextRecognitionState.Success -> {
                recognizedText = state.text
                showEditDialog = true
                viewModel.resetState()
            }
            is TextRecognitionState.Error -> {
                // Error will be shown in UI
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("카메라로 텍스트 인식") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                },
                actions = {
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = "갤러리")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                !cameraPermissionState.status.isGranted -> {
                    PermissionRequestScreen(
                        onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                    )
                }
                showCapturedImage && capturedBitmap != null -> {
                    CapturedImagePreview(
                        bitmap = capturedBitmap!!,
                        isProcessing = recognitionState is TextRecognitionState.Processing,
                        onRetake = {
                            showCapturedImage = false
                            capturedBitmap = null
                        },
                        onConfirm = {
                            viewModel.recognizeTextFromBitmap(capturedBitmap!!)
                        }
                    )
                }
                else -> {
                    CameraPreview(
                        onImageCaptured = { bitmap ->
                            capturedBitmap = bitmap
                            showCapturedImage = true
                        }
                    )
                }
            }

            // Error message
            if (recognitionState is TextRecognitionState.Error) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.resetState() }) {
                            Text("확인")
                        }
                    }
                ) {
                    Text((recognitionState as TextRecognitionState.Error).message)
                }
            }
        }
    }

    if (showEditDialog) {
        RecognizedTextEditDialog(
            text = recognizedText,
            onDismiss = { 
                showEditDialog = false
                showCapturedImage = false
                capturedBitmap = null
            },
            onConfirm = { editedText ->
                onTextRecognized(editedText)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "카메라 권한이 필요합니다",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "텍스트를 인식하려면 카메라 권한을 허용해주세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRequestPermission) {
                Text("권한 허용")
            }
        }
    }
}

@Composable
fun CameraPreview(onImageCaptured: (Bitmap) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { ContextCompat.getMainExecutor(context) }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setTargetRotation(previewView.display.rotation)
                        .build()

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, executor)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Capture button
        FloatingActionButton(
            onClick = {
                imageCapture?.let { capture ->
                    val photoFile = File.createTempFile("photo", ".jpg", context.cacheDir)
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    capture.takePicture(
                        outputOptions,
                        executor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                                // Rotate based on EXIF orientation
                                val rotatedBitmap = rotateImageIfRequired(originalBitmap, photoFile.absolutePath)
                                // Crop to target area (70% width, centered)
                                val croppedBitmap = cropToTargetArea(rotatedBitmap)
                                onImageCaptured(croppedBitmap)
                                photoFile.delete()
                            }

                            override fun onError(exception: ImageCaptureException) {
                                exception.printStackTrace()
                            }
                        }
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = "사진 촬영")
        }

        // OCR Target Area Overlay
        OCRTargetOverlay()

        // Guide text
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        ) {
            Text(
                text = "네모 안에 텍스트를 맞춰주세요",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun OCRTargetOverlay() {
    val density = LocalDensity.current
    
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // Target rectangle size (70% of screen width, centered)
        val rectWidth = canvasWidth * 0.7f
        val rectHeight = rectWidth * 0.5f  // 16:9 비율의 절반 정도
        
        val left = (canvasWidth - rectWidth) / 2
        val top = (canvasHeight - rectHeight) / 2
        
        // Draw semi-transparent overlay (darken area outside target)
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            size = Size(canvasWidth, top)  // Top area
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, top + rectHeight),
            size = Size(canvasWidth, canvasHeight - top - rectHeight)  // Bottom area
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, top),
            size = Size(left, rectHeight)  // Left area
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(left + rectWidth, top),
            size = Size(canvasWidth - left - rectWidth, rectHeight)  // Right area
        )
        
        // Draw target rectangle with animated dashed border
        val dashWidth = 20f
        val dashGap = 15f
        
        // Main border (white)
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(rectWidth, rectHeight),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(
                width = 4f,
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(dashWidth, dashGap),
                    phase = 0f
                )
            )
        )
        
        // Corner indicators (brighter)
        val cornerLength = 40f
        val cornerWidth = 6f
        
        // Top-left corner
        drawLine(
            color = Color(0xFF4CAF50),
            start = Offset(left, top + cornerLength),
            end = Offset(left, top),
            strokeWidth = cornerWidth
        )
        drawLine(
            color = Color(0xFF4CAF50),
            start = Offset(left, top),
            end = Offset(left + cornerLength, top),
            strokeWidth = cornerWidth
        )
        
        // Top-right corner
        drawLine(
            color = Color(0xFF4CAF50),
            start = Offset(left + rectWidth, top + cornerLength),
            end = Offset(left + rectWidth, top),
            strokeWidth = cornerWidth
        )
        drawLine(
            color = Color(0xFF4CAF50),
            start = Offset(left + rectWidth, top),
            end = Offset(left + rectWidth - cornerLength, top),
            strokeWidth = cornerWidth
        )
        
        // Bottom-left corner
        drawLine(
            color = Color(0xFF4CAF50),
            start = Offset(left, top + rectHeight - cornerLength),
            end = Offset(left, top + rectHeight),
            strokeWidth = cornerWidth
        )
        drawLine(
            color = Color(0xFF4CAF50),
            start = Offset(left, top + rectHeight),
            end = Offset(left + cornerLength, top + rectHeight),
            strokeWidth = cornerWidth
        )
        
        // Bottom-right corner
        drawLine(
            color = Color(0xFF4CAF50),
            start = Offset(left + rectWidth, top + rectHeight - cornerLength),
            end = Offset(left + rectWidth, top + rectHeight),
            strokeWidth = cornerWidth
        )
        drawLine(
            color = Color(0xFF4CAF50),
            start = Offset(left + rectWidth, top + rectHeight),
            end = Offset(left + rectWidth - cornerLength, top + rectHeight),
            strokeWidth = cornerWidth
        )
    }
}

@Composable
fun CapturedImagePreview(
    bitmap: Bitmap,
    isProcessing: Boolean,
    onRetake: () -> Unit,
    onConfirm: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "캡처된 이미지",
            modifier = Modifier.fillMaxSize()
        )

        if (isProcessing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("텍스트 인식 중...")
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = onRetake,
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "다시 찍기")
                }
                FloatingActionButton(
                    onClick = onConfirm,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "확인")
                }
            }
        }
    }
}

@Composable
fun RecognizedTextEditDialog(
    text: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var editedText by remember { mutableStateOf(text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("인식된 텍스트") },
        text = {
            Column {
                Text(
                    text = "인식된 텍스트를 확인하고 수정하세요",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5,
                    label = { Text("기도제목") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(editedText.trim()) },
                enabled = editedText.isNotBlank()
            ) {
                Text("추가")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

private fun rotateImageIfRequired(bitmap: Bitmap, imagePath: String): Bitmap {
    return try {
        val exif = ExifInterface(imagePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        
        val rotation = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        
        if (rotation != 0f) {
            val matrix = Matrix()
            matrix.postRotate(rotation)
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    } catch (e: Exception) {
        e.printStackTrace()
        bitmap
    }
}

private fun cropToTargetArea(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    
    // Target area dimensions (same as overlay: 70% width, centered)
    val targetWidth = (width * 0.7f).toInt()
    val targetHeight = (targetWidth * 0.5f).toInt() // 2:1 ratio
    
    // Calculate position (centered)
    val left = ((width - targetWidth) / 2).coerceAtLeast(0)
    val top = ((height - targetHeight) / 2).coerceAtLeast(0)
    
    // Ensure we don't exceed bitmap bounds
    val actualWidth = targetWidth.coerceAtMost(width - left)
    val actualHeight = targetHeight.coerceAtMost(height - top)
    
    return try {
        Bitmap.createBitmap(bitmap, left, top, actualWidth, actualHeight)
    } catch (e: Exception) {
        e.printStackTrace()
        bitmap // Return original if cropping fails
    }
}
