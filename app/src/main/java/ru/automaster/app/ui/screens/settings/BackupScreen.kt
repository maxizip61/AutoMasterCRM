package ru.automaster.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Process
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MIN_PASSWORD_LENGTH = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val backups by viewModel.localBackups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    val restartRequired by viewModel.restartRequired.collectAsState()

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val fileNameFormat = remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()) }

    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var pendingExportPassword by remember { mutableStateOf<CharArray?>(null) }

    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestorePasswordDialog by remember { mutableStateOf(false) }
    var showPlainRestoreConfirm by remember { mutableStateOf(false) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val pwd = pendingExportPassword
        if (uri != null && pwd != null) {
            viewModel.exportEncrypted(uri, pwd)
        }
        pendingExportPassword = null
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                when (viewModel.detectFormat(uri)) {
                    BackupFormat.ENCRYPTED -> {
                        pendingRestoreUri = uri
                        showRestorePasswordDialog = true
                    }
                    BackupFormat.PLAIN_ZIP -> {
                        pendingRestoreUri = uri
                        showPlainRestoreConfirm = true
                    }
                    BackupFormat.UNKNOWN -> {
                        viewModel.notify("Неизвестный формат файла. Выберите .amb или .zip")
                    }
                }
            }
        }
    }

    LaunchedEffect(message) {
        message?.let {
            if (!restartRequired) {
                snackbarHostState.showSnackbar(it)
            }
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(restartRequired) {
        if (restartRequired) {
            delay(1200)
            val intent = context.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            if (intent != null) context.startActivity(intent)
            Process.killProcess(Process.myPid())
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Бэкапы и восстановление", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (restartRequired) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        "Данные восстановлены. Перезапускаю приложение…",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showExportPasswordDialog = true },
                    enabled = !isLoading && !restartRequired,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сохранить")
                }
                OutlinedButton(
                    onClick = { openBackupLauncher.launch(arrayOf("*/*")) },
                    enabled = !isLoading && !restartRequired,
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Восстановить")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Бэкап шифруется паролем",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "AES-256. Без пароля файл бесполезен — запишите пароль " +
                                    "и храните отдельно от телефона. Восстановить без него невозможно.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Локальные копии (автобэкап)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (backups.isEmpty()) {
                Text(
                    "Автобэкапов пока нет",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(backups, key = { it.absolutePath }) { file ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    file.name,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Размер: ${file.length() / 1024} КБ · " +
                                            dateFormat.format(Date(file.lastModified())),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ===== Пароль для экспорта =====
    if (showExportPasswordDialog) {
        var pwd by remember { mutableStateOf("") }
        var pwd2 by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showExportPasswordDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Пароль бэкапа") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Минимум $MIN_PASSWORD_LENGTH символов. Запишите его — " +
                                "без пароля данные не восстановить.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pwd,
                        onValueChange = { pwd = it },
                        label = { Text("Пароль") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pwd2,
                        onValueChange = { pwd2 = it },
                        label = { Text("Повторите пароль") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        pwd.length < MIN_PASSWORD_LENGTH ->
                            error = "Минимум $MIN_PASSWORD_LENGTH символов"
                        pwd != pwd2 ->
                            error = "Пароли не совпадают"
                        else -> {
                            pendingExportPassword = pwd.toCharArray()
                            showExportPasswordDialog = false
                            createBackupLauncher.launch(
                                "automaster_${fileNameFormat.format(Date())}.amb"
                            )
                        }
                    }
                }) { Text("Далее") }
            },
            dismissButton = {
                TextButton(onClick = { showExportPasswordDialog = false }) { Text("Отмена") }
            }
        )
    }

    // ===== Пароль для восстановления .amb =====
    if (showRestorePasswordDialog && pendingRestoreUri != null) {
        var pwd by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                showRestorePasswordDialog = false
                pendingRestoreUri = null
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Восстановление данных") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "ВНИМАНИЕ! Текущие данные будут заменены данными из копии. " +
                                "Приложение перезапустится.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = pwd,
                        onValueChange = { pwd = it },
                        label = { Text("Пароль бэкапа") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pwd.isNotEmpty(),
                    onClick = {
                        val uri = pendingRestoreUri!!
                        showRestorePasswordDialog = false
                        pendingRestoreUri = null
                        viewModel.restore(uri, pwd.toCharArray(), encrypted = true)
                    }
                ) { Text("Восстановить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestorePasswordDialog = false
                    pendingRestoreUri = null
                }) { Text("Отмена") }
            }
        )
    }

    // ===== Подтверждение для старого .zip =====
    if (showPlainRestoreConfirm && pendingRestoreUri != null) {
        AlertDialog(
            onDismissRequest = {
                showPlainRestoreConfirm = false
                pendingRestoreUri = null
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Старый бэкап без шифрования") },
            text = {
                Text(
                    "Этот файл не зашифрован (старый формат .zip). " +
                            "Текущие данные будут заменены, приложение перезапустится. Продолжить?",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pendingRestoreUri!!
                    showPlainRestoreConfirm = false
                    pendingRestoreUri = null
                    viewModel.restore(uri, null, encrypted = false)
                }) { Text("Восстановить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPlainRestoreConfirm = false
                    pendingRestoreUri = null
                }) { Text("Отмена") }
            }
        )
    }
}