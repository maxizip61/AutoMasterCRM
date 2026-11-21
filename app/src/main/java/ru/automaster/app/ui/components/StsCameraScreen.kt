package ru.automaster.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Встроенная камера для съёмки СТС.
 * Живой превью + кнопка спуска + кнопка галереи внутри экрана.
 * Пишет файл напрямую, без FileProvider — поэтому не падает.
 */
@Composable
fun StsCameraScreen(
    createFile: () -> File,
    onCaptured: (File) -> Unit,
    onGallery: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = context as LifecycleOwner

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    LaunchedEffect(hasPermission, previewView) {
        if (!hasPermission) return@LaunchedEffect
        val view = previewView ?: return@LaunchedEffect
        try {
            val provider = withContext(Dispatchers.IO) {
                ProcessCameraProvider.getInstance(context).get()
            }
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(view.surfaceProvider)
            }
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
            cameraError = null
        } catch (e: Exception) {
            Log.e("StsCamera", "bind failed", e)
            cameraError = "Не удалось включить камеру: ${e.message}"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FIT_CENTER
                    previewView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Закрыть",
                    tint = Color.White
                )
            }
            Text(
                "Снимок СТС",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (hasPermission && cameraError == null) {
            Text(
                "Положите СТС ровно, при хорошем свете",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 140.dp)
            )
        }

        if (!hasPermission || cameraError != null) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    cameraError ?: "Нет доступа к камере",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!hasPermission) {
                        Button(onClick = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }) { Text("Дать доступ") }
                    }
                    OutlinedButton(onClick = onGallery) { Text("Из галереи") }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onGallery,
                modifier = Modifier.size(52.dp),
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = "Выбрать из галереи",
                    modifier = Modifier.size(28.dp)
                )
            }

            FloatingActionButton(
                onClick = {
                    if (isCapturing) return@FloatingActionButton
                    isCapturing = true
                    val file = runCatching { createFile() }.getOrNull()
                    if (file == null) {
                        isCapturing = false
                        cameraError = "Не удалось создать файл снимка"
                        return@FloatingActionButton
                    }
                    val opts = ImageCapture.OutputFileOptions.Builder(file).build()
                    imageCapture.takePicture(
                        opts,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(r: ImageCapture.OutputFileResults) {
                                isCapturing = false
                                onCaptured(file)
                            }

                            override fun onError(e: ImageCaptureException) {
                                isCapturing = false
                                cameraError = "Ошибка съёмки: ${e.message}"
                                file.delete()
                            }
                        }
                    )
                },
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                containerColor = Color.White
            ) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = "Сделать снимок",
                    tint = Color.Black,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.size(52.dp))
        }
    }
}