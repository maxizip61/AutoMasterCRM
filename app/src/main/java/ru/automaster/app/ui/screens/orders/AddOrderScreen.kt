package ru.automaster.app.ui.screens.orders

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.automaster.app.ui.components.StsCameraScreen
import ru.automaster.app.ui.theme.IndigoPrimary
import ru.automaster.app.ui.theme.InputGrey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrderScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddOrderViewModel = hiltViewModel()
) {
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var carMake by remember { mutableStateOf("") }
    var carModel by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var vin by remember { mutableStateOf("") }
    var estimate by remember { mutableStateOf("") }
    var scannedYear by remember { mutableStateOf<Int?>(null) }
    var showRawText by remember { mutableStateOf(false) }
    var rawText by remember { mutableStateOf("") }

    // Показывать встроенную камеру поверх формы
    var showStsCamera by remember { mutableStateOf(false) }

    val message by viewModel.message.collectAsState()
    val resetSignal by viewModel.resetForm.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val draft by viewModel.draft.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Галерея
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.scanDocument(uri)
    }

    LaunchedEffect(draft) {
        draft?.let { d ->
            d.licensePlate?.let { licensePlate = it }
            d.vin?.let { vin = it }
            d.make?.let { carMake = it }
            d.model?.let { carModel = it }
            d.year?.let { scannedYear = it }
            rawText = d.rawText
            viewModel.clearDraft()
        }
    }

    LaunchedEffect(resetSignal) {
        if (resetSignal > 0) {
            clientName = ""
            clientPhone = ""
            carMake = ""
            carModel = ""
            licensePlate = ""
            vin = ""
            estimate = ""
            scannedYear = null
            rawText = ""
            showRawText = false
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Новый заказ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (showStsCamera) {
                // ---------- ВСТРОЕННАЯ КАМЕРА ----------
                StsCameraScreen(
                    createFile = { viewModel.newStsFile() },
                    onCaptured = { file ->
                        showStsCamera = false
                        viewModel.scanStsFile(file)
                    },
                    onGallery = {
                        showStsCamera = false
                        pickImage.launch("image/*")
                    },
                    onClose = { showStsCamera = false }
                )
            } else {
                // ---------- ФОРМА ----------
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Кнопка открытия встроенной камеры
                    OutlinedButton(
                        onClick = { showStsCamera = true },
                        enabled = !isScanning,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Распознаю документ…")
                        } else {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Сканировать СТС (камера / галерея)")
                        }
                    }

                    if (scannedYear != null || rawText.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "Распознано из документа",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                if (scannedYear != null) {
                                    Text(
                                        "Год выпуска: $scannedYear",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                TextButton(
                                    onClick = { showRawText = !showRawText },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        if (showRawText) "Скрыть текст"
                                        else "Показать распознанный текст"
                                    )
                                }
                                if (showRawText) {
                                    Text(
                                        rawText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "Проверьте поля ниже — система учится на ваших правках.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Text(
                        "Данные клиента",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    CustomTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = "ФИО клиента",
                        keyboardType = KeyboardType.Text
                    )
                    CustomTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = "Телефон",
                        keyboardType = KeyboardType.Phone
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Данные автомобиля",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    CustomTextField(
                        value = carMake,
                        onValueChange = { carMake = it },
                        label = "Марка (напр. Lada)",
                        keyboardType = KeyboardType.Text
                    )
                    CustomTextField(
                        value = carModel,
                        onValueChange = { carModel = it },
                        label = "Модель (напр. Vesta)",
                        keyboardType = KeyboardType.Text
                    )
                    CustomTextField(
                        value = licensePlate,
                        onValueChange = { licensePlate = it },
                        label = "Госномер (А123ВС 77)",
                        keyboardType = KeyboardType.Text
                    )
                    CustomTextField(
                        value = vin,
                        onValueChange = { vin = it },
                        label = "VIN",
                        keyboardType = KeyboardType.Text
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Смета",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    CustomTextField(
                        value = estimate,
                        onValueChange = { estimate = it.filter { c -> c.isDigit() } },
                        label = "Сумма в рублях",
                        keyboardType = KeyboardType.Number
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val estimateCents = (estimate.toLongOrNull() ?: 0L) * 100
                            viewModel.createOrder(
                                clientName, clientPhone, carMake, carModel,
                                licensePlate, vin, estimateCents, scannedYear,
                                ocrDraft = draft
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = clientName.isNotBlank() &&
                                licensePlate.isNotBlank() && !isScanning,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                    ) {
                        Text(
                            "Создать заказ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = InputGrey,
            unfocusedContainerColor = InputGrey,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedLabelColor = IndigoPrimary,
            cursorColor = IndigoPrimary,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}