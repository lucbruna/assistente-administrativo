package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.utils.OcrProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

private val AccentNeonTeal = Color(0xFF80D8CC)
private val BorderColor = Color(0xFF49454F)

@Composable
fun RealOcrCameraScreen(
    onTextExtracted: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val cameraPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1C1B1F))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Fechar", tint = Color.White)
            }
            Text(
                "SCANNER DE DOCUMENTO",
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White
            )
            Box(modifier = Modifier.size(48.dp))
        }

        if (!cameraPermissionGranted) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NoPhotography, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Permissão de câmera necessária", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (!showPreview) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).also { view ->
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val provider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(view.surfaceProvider)
                                    }
                                    val capture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                        .build()
                                    imageCapture = capture
                                    val cameraSelector = CameraSelector.Builder()
                                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                        .build()
                                    provider.unbindAll()
                                    provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, capture)
                                }, ContextCompat.getMainExecutor(ctx))
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .border(2.dp, AccentNeonTeal.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    )

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val capture = imageCapture ?: return@IconButton
                                    isCapturing = true
                                    val photoFile = File(context.cacheDir, "ocr_${System.currentTimeMillis()}.jpg")
                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                    val executor = Executors.newSingleThreadExecutor()
                                    capture.takePicture(
                                        outputOptions, executor,
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                                try {
                                                    capturedBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                                                    showPreview = true
                                                } catch (_: Exception) {}
                                                isCapturing = false
                                                executor.shutdown()
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                isCapturing = false
                                                executor.shutdown()
                                            }
                                        }
                                    )
                                },
                                enabled = !isCapturing,
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(Color.White, CircleShape)
                                    .border(4.dp, AccentNeonTeal, CircleShape)
                            ) {
                                if (isCapturing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = AccentNeonTeal, strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.CameraAlt, "Capturar", tint = Color.Black, modifier = Modifier.size(28.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Fotografar documento", fontSize = 11.sp, color = Color.LightGray)
                        }
                    }
                } else {
                    capturedBitmap?.let { bmp ->
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .border(2.dp, AccentNeonTeal, RoundedCornerShape(16.dp))
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Documento capturado",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        showPreview = false
                                        capturedBitmap = null
                                    },
                                    border = BorderStroke(1.dp, Color.Gray)
                                ) {
                                    Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tirar Novamente", color = Color.White)
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isCapturing = true
                                            val bmp = capturedBitmap ?: return@launch
                                            val maxSize = 2048
                                            val scaled = if (bmp.width > maxSize || bmp.height > maxSize) {
                                                val s = maxSize.toFloat() / maxOf(bmp.width, bmp.height)
                                                bmp.scale((bmp.width * s).toInt(), (bmp.height * s).toInt())
                                            } else bmp
                                            val text = withContext(Dispatchers.IO) {
                                                OcrProcessor.recognizeText(scaled)
                                            }
                                            isCapturing = false
                                            if (text.isNotBlank()) onTextExtracted(text)
                                        }
                                    },
                                    enabled = !isCapturing,
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentNeonTeal)
                                ) {
                                    if (isCapturing) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text("Processar OCR", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
