package com.example.ui.qrforge

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.components.ButtonStyle
import com.example.ui.components.AzurePillButton
import com.example.ui.components.AzureTextField
import com.example.ui.theme.*
import com.example.util.ClipboardUtil
import com.example.util.PermissionUtil
import com.example.util.ShareUtil
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ScanTab(viewModel: QrForgeViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(PermissionUtil.isPermissionGranted(context, Manifest.permission.CAMERA))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    var scannedResult by remember { mutableStateOf<String?>(null) }
    var scannedType by remember { mutableStateOf("QR CODE") }
    var isTorchOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    // Custom Mock Input Box for fallback / emulator testing
    var mockInputText by remember { mutableStateOf("") }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera permission is required to scan codes",
                    style = TextStyle(fontFamily = JetBrainsMono, color = TextSecondary, fontSize = 14.sp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                AzurePillButton(
                    text = "GRANT PERMISSION",
                    onClick = { launcher.launch(Manifest.permission.CAMERA) }
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Background)) {
            // Viewfinder and MLKit
            val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
            val executor = remember { Executors.newSingleThreadExecutor() }

            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val scanner = BarcodeScanning.getClient()

                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            @SuppressLint("UnsafeOptInUsageError")
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        if (barcodes.isNotEmpty() && scannedResult == null) {
                                            val barcode = barcodes.first()
                                            val value = barcode.rawValue
                                            if (value != null) {
                                                scannedResult = value
                                                scannedType = when (barcode.valueType) {
                                                    com.google.mlkit.vision.barcode.common.Barcode.TYPE_URL -> "URL"
                                                    com.google.mlkit.vision.barcode.common.Barcode.TYPE_WIFI -> "WIFI"
                                                    com.google.mlkit.vision.barcode.common.Barcode.TYPE_PHONE -> "PHONE"
                                                    com.google.mlkit.vision.barcode.common.Barcode.TYPE_EMAIL -> "EMAIL"
                                                    else -> "QR CODE"
                                                }
                                                // Save to history automatically
                                                viewModel.addToHistory(wasScanned = true, contentOverride = value)
                                                // Vibrate
                                                vibrateOnScan(context)
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        Log.e("ScanTab", "Scan failed", it)
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            cameraControl = camera.cameraControl
                        } catch (e: Exception) {
                            Log.e("ScanTab", "Camera binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Scanning overlays
            ScanningOverlay()

            // Torch Toggle top right
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = {
                        isTorchOn = !isTorchOn
                        cameraControl?.enableTorch(isTorchOn)
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Surface.copy(alpha = 0.8f))
                        .border(1.dp, Border, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Torch",
                        tint = Accent
                    )
                }
            }

            // Quick Manual fallback simulator at the bottom center if running on device without functional scanner
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface.copy(alpha = 0.9f))
                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Emulator Scan Emulator Fallback",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AzureTextField(
                        value = mockInputText,
                        onValueChange = { mockInputText = it },
                        placeholder = "Type content to simulate scan...",
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (mockInputText.isNotBlank()) {
                                        scannedResult = mockInputText
                                        scannedType = if (mockInputText.startsWith("http")) "URL" else "QR CODE"
                                        viewModel.addToHistory(wasScanned = true, contentOverride = mockInputText)
                                        vibrateOnScan(context)
                                        mockInputText = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Simulate Scan", tint = Accent)
                            }
                        }
                    )
                }
            }

            // Scanned bottom sheet if result is present
            scannedResult?.let { result ->
                ModalBottomSheet(
                    onDismissRequest = { scannedResult = null },
                    containerColor = Surface2,
                    contentColor = TextPrimary
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AccentDim)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = scannedType,
                                style = TextStyle(
                                    fontFamily = JetBrainsMono,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Accent
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = result,
                            style = TextStyle(
                                fontFamily = JetBrainsMono,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Border, RoundedCornerShape(8.dp))
                                .background(Surface)
                                .padding(16.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AzurePillButton(
                                text = "COPY",
                                onClick = {
                                    ClipboardUtil.copyToClipboard(context, result)
                                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                                },
                                style = ButtonStyle.Secondary,
                                modifier = Modifier.weight(1f)
                            )
                            if (scannedType == "URL" || result.startsWith("http")) {
                                AzurePillButton(
                                    text = "OPEN",
                                    onClick = {
                                        try {
                                            val url = if (result.startsWith("http")) result else "https://$result"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot open URL", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    style = ButtonStyle.Primary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            AzurePillButton(
                                text = "SHARE",
                                onClick = {
                                    ShareUtil.shareText(context, result)
                                },
                                style = ButtonStyle.Secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScanningOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "ReticlePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val rectSize = 250.dp.toPx() * pulseScale

        val left = (width - rectSize) / 2
        val top = (height - rectSize) / 2
        val right = left + rectSize
        val bottom = top + rectSize

        val cornerLength = 24.dp.toPx()
        val strokeWidth = 3.dp.toPx()

        // 1. Top-Left Corner
        drawLine(
            color = Accent,
            start = Offset(left, top),
            end = Offset(left + cornerLength, top),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = Accent,
            start = Offset(left, top),
            end = Offset(left, top + cornerLength),
            strokeWidth = strokeWidth
        )

        // 2. Top-Right Corner
        drawLine(
            color = Accent,
            start = Offset(right, top),
            end = Offset(right - cornerLength, top),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = Accent,
            start = Offset(right, top),
            end = Offset(right, top + cornerLength),
            strokeWidth = strokeWidth
        )

        // 3. Bottom-Left Corner
        drawLine(
            color = Accent,
            start = Offset(left, bottom),
            end = Offset(left + cornerLength, bottom),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = Accent,
            start = Offset(left, bottom),
            end = Offset(left, bottom - cornerLength),
            strokeWidth = strokeWidth
        )

        // 4. Bottom-Right Corner
        drawLine(
            color = Accent,
            start = Offset(right, bottom),
            end = Offset(right - cornerLength, bottom),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = Accent,
            start = Offset(right, bottom),
            end = Offset(right, bottom - cornerLength),
            strokeWidth = strokeWidth
        )
    }
}

@Suppress("DEPRECATION")
fun vibrateOnScan(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(80)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
