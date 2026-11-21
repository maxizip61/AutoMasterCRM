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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Р’СЃС‚СЂРѕРµРЅРЅР°СЏ РєР°РјРµСЂР° РґР»СЏ СЃСЉС‘РјРєРё РЎРўРЎ.
 * Р–РёРІРѕР№ РїСЂРµРІСЊСЋ + РєРЅРѕРїРєР° СЃРїСѓСЃРєР° + РєРЅРѕРїРєР° РіР°Р»РµСЂРµРё РІРЅСѓС‚СЂРё СЌРєСЂР°РЅР°.
 * РџРёС€РµС‚ С„Р°Р№Р» РЅР°РїСЂСЏРјСѓСЋ, Р±РµР· FileProvider вЂ” РїРѕСЌС‚РѕРјСѓ РЅРµ РїР°РґР°РµС‚.
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

    // РџСЂРёРІСЏР·РєР° РєР°РјРµСЂС‹ Рє Р¶РёР·РЅРµРЅРЅРѕРјСѓ С†РёРєР»Сѓ РїРѕСЃР»Рµ РїРѕР»СѓС‡РµРЅРёСЏ СЂР°Р·СЂРµС€РµРЅРёСЏ
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
            cameraError = "РќРµ СѓРґР°Р»РѕСЃСЊ РІРєР»СЋС‡РёС‚СЊ РєР°РјРµСЂСѓ: ${e.message}"
        }
    }

    // РћСЃРІРѕР±РѕР¶РґР°РµРј РєР°РјРµСЂСѓ РїСЂРё РІС‹С…РѕРґРµ СЃ СЌРєСЂР°РЅР°
    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // РџСЂРµРІСЊСЋ
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FIT_CENTER
                    previewView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Р’РµСЂС…РЅСЏСЏ РїР°РЅРµР»СЊ
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
                    contentDescription = "Р—Р°РєСЂС‹С‚СЊ",
                    tint = Color.White
                )
            }
            Text(
                "РЎРЅРёРјРѕРє РЎРўРЎ",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // РџРѕРґСЃРєР°Р·РєР°
        if (hasPermission && cameraError == null) {
            Text(
                "РџРѕР»РѕР¶РёС‚Рµ РЎРўРЎ СЂРѕРІРЅРѕ, РїСЂРё С…РѕСЂРѕС€РµРј СЃРІРµС‚Рµ",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 140.dp)
            )
        }

        // РћС€РёР±РєР° РёР»Рё РЅРµС‚ СЂР°Р·СЂРµС€РµРЅРёСЏ
        if (!hasPermission || cameraError != null) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    cameraError ?: "РќРµС‚ РґРѕСЃС‚СѓРїР° Рє РєР°РјРµСЂРµ",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!hasPermission) {
                        Button(onClick = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }) { Text("Р”Р°С‚СЊ РґРѕСЃС‚СѓРї") }
                    }
                    OutlinedButton(onClick = onGallery) { Text("РР· РіР°Р»РµСЂРµРё") }
                }
            }
        }

        // РќРёР¶РЅСЏСЏ РїР°РЅРµР»СЊ: РіР°Р»РµСЂРµСЏ | СЃРїСѓСЃРє | (РїСѓСЃС‚Рѕ РґР»СЏ Р±Р°Р»Р°РЅСЃР°)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Р“Р°Р»РµСЂРµСЏ РІРЅСѓС‚СЂРё РєР°РјРµСЂС‹
            IconButton(
                onClick = onGallery,
                modifier = Modifier.size(52.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = "Р’С‹Р±СЂР°С‚СЊ РёР· РіР°Р»РµСЂРµРё",
                    modifier = Modifier.size(28.dp)
                )
            }

            // РљРЅРѕРїРєР° СЃРїСѓСЃРєР°
            FloatingActionButton(
                onClick = {
                    if (isCapturing) return@FloatingActionButton
                    isCapturing = true
                    val file = runCatching { createFile() }.getOrNull()
                    if (file == null) {
                        isCapturing = false
                        cameraError = "РќРµ СѓРґР°Р»РѕСЃСЊ СЃРѕР·РґР°С‚СЊ С„Р°Р№Р» СЃРЅРёРјРєР°"
                        return@FloatingActionButton
                    }
                    val opts = ImageCapture.OutputFileOptions.Builder(file).build()
                    imageCapture.takePicture(
                        opts,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(
                                r: ImageCapture.OutputFileResults
                            ) {
                                isCapturing = false
                                onCaptured(file)
                            }

                            override fun onError(e: ImageCaptureException) {
                                isCapturing = false
                                cameraError = "РћС€РёР±РєР° СЃСЉС‘РјРєРё: ${e.message}"
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
                    contentDescription = "РЎРґРµР»Р°С‚СЊ СЃРЅРёРјРѕРє",
                    tint = Color.Black,
                    modifier = Modifier.size(34.dp)
                )
            }

            // РџСѓСЃС‚С‹С€РєР° РґР»СЏ С†РµРЅС‚СЂРёСЂРѕРІР°РЅРёСЏ СЃРїСѓСЃРєР°
            Spacer(modifier = Modifier.size(52.dp))
        }
    }
}
