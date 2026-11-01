package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ui.theme.HudAmber
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudDeepNavy
import com.example.ui.theme.HudEmerald
import com.example.ui.theme.HudIceWhite
import com.example.ui.theme.HudMutedText
import com.example.ui.theme.HudObsidian
import com.example.ui.theme.HudPanelElevated
import com.example.ui.theme.HudPanelSurface
import com.example.ui.theme.HudSkyBlue
import com.example.viewmodel.AiTaskState

@Composable
fun VisionScreen(
    visionTaskState: AiTaskState,
    onAnalyzeBitmap: (Bitmap, String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var useFrontCamera by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf("SCENE_HUD") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Zero-permission Android Photo Picker fallback
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        capturedBitmap = bmp
                        onAnalyzeBitmap(bmp, selectedMode)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(hasCameraPermission, useFrontCamera) {
        if (hasCameraPermission) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val selector = if (useFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imageCapture
                    )
                } catch (_: Exception) {
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Vision Header & Mode Selector
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = HudPanelSurface),
            border = BorderStroke(1.dp, HudCyan.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = HudCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OPTICAL VISION & SCREEN ANALYZER",
                            style = MaterialTheme.typography.titleLarge,
                            color = HudCyan
                        )
                    }

                    if (hasCameraPermission) {
                        IconButton(
                            onClick = { useFrontCamera = !useFrontCamera },
                            modifier = Modifier.testTag("switch_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch front/back camera",
                                tint = HudSkyBlue
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val modes = listOf(
                        "SCENE_HUD" to "Scene HUD",
                        "OCR_SCREEN" to "Screen Read",
                        "EMOTION" to "Emotion AI",
                        "HARDWARE" to "Hardware"
                    )
                    modes.forEach { (id, label) ->
                        FilterChip(
                            selected = selectedMode == id,
                            onClick = { selectedMode = id },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HudCyan.copy(alpha = 0.25f),
                                selectedLabelColor = HudCyan
                            )
                        )
                    }
                }
            }
        }

        // CameraX Live Viewfinder or Permission Request / Captured Frame
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = HudDeepNavy),
            border = BorderStroke(1.5.dp, HudSkyBlue.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .testTag("vision_viewfinder_card")
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (hasCameraPermission) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (capturedBitmap != null) {
                    Image(
                        bitmap = capturedBitmap!!.asImageBitmap(),
                        contentDescription = "Selected frame for optical analysis",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "OPTICAL SENSOR PERMISSION REQUIRED",
                            style = MaterialTheme.typography.labelLarge,
                            color = HudAmber
                        )
                        Text(
                            text = "Grant Camera access for live HUD optical scanning, or use Load Visual Media to analyze any image/screenshot.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HudMutedText
                        )
                        Button(
                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HudCyan,
                                contentColor = HudObsidian
                            ),
                            modifier = Modifier.testTag("grant_camera_permission_button")
                        ) {
                            Text("Enable CameraX Sensor", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                // HUD Corner Crosshair Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(HudObsidian.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "MODE: $selectedMode // GEMINI-3.8-FLASH",
                        style = MaterialTheme.typography.labelSmall,
                        color = HudCyan
                    )
                }
            }
        }

        // Capture & Photo Picker Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (!hasCameraPermission) {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    } else {
                        imageCapture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val bmp = imageProxyToBitmap(image)
                                    image.close()
                                    if (bmp != null) {
                                        capturedBitmap = bmp
                                        onAnalyzeBitmap(bmp, selectedMode)
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    // Fallback to PreviewView bitmap if emulator camera capture fails
                                    val fallbackBmp = previewView.bitmap
                                    if (fallbackBmp != null) {
                                        capturedBitmap = fallbackBmp
                                        onAnalyzeBitmap(fallbackBmp, selectedMode)
                                    }
                                }
                            }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HudCyan,
                    contentColor = HudObsidian
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("vision_capture_button")
            ) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.vision_capture_analyze), style = MaterialTheme.typography.labelLarge)
            }

            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                border = BorderStroke(1.dp, HudSkyBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("vision_pick_media_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = stringResource(R.string.vision_pick_photo),
                    tint = HudSkyBlue
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Gallery", style = MaterialTheme.typography.labelLarge, color = HudSkyBlue)
            }
        }

        // Multimodal Analysis Output Panel
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = HudPanelSurface),
            border = BorderStroke(1.dp, HudCyan.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vision_analysis_output_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "OPTICAL TELEMETRY READOUT",
                    style = MaterialTheme.typography.labelLarge,
                    color = HudEmerald
                )

                when (visionTaskState) {
                    is AiTaskState.Idle -> {
                        Text(
                            text = "Capture a live CameraX frame or load an image/screenshot to run Gemini 3.8 Flash optical analysis, OCR screen reading, or facial emotion calibration.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HudMutedText
                        )
                    }
                    is AiTaskState.Loading -> {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = HudCyan,
                            trackColor = HudPanelElevated
                        )
                        Text(
                            text = "Running neural optical scan…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HudCyan
                        )
                    }
                    is AiTaskState.Streaming -> {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = HudEmerald,
                            trackColor = HudPanelElevated
                        )
                        Text(
                            text = visionTaskState.partialText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = HudIceWhite
                        )
                    }
                    is AiTaskState.Success -> {
                        Text(
                            text = visionTaskState.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = HudIceWhite
                        )
                    }
                    is AiTaskState.Error -> {
                        Text(
                            text = "OPTICAL ERROR: ${visionTaskState.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HudCrimson
                        )
                    }
                }
            }
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    return try {
        val bmp = image.toBitmap()
        val rotation = image.imageInfo.rotationDegrees
        if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        } else {
            bmp
        }
    } catch (_: Exception) {
        null
    }
}
