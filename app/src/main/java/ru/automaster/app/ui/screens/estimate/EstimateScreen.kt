package ru.automaster.app.ui.screens.estimate

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.automaster.app.domain.model.CarPanels
import ru.automaster.app.ui.components.CarDamageMap
import ru.automaster.app.ui.components.MoneyFormatter
import ru.automaster.app.ui.theme.IndigoPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstimateScreen(
    onNavigateBack: () -> Unit,
    viewModel: EstimateViewModel = hiltViewModel()
) {
    val carClass by viewModel.carClass.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val lines by viewModel.lines.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) viewModel.addPhoto(uri) }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }
    LaunchedEffect(make) { viewModel.carMake.value = make }
    LaunchedEffect(model) { viewModel.carModel.value = model }

    val totalMin = lines.sumOf { it.totalMinCents }
    val totalMax = lines.sumOf { it.totalMaxCents }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Оценка по фото", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = make, onValueChange = { make = it },
                label = { Text("Марка") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = model, onValueChange = { model = it },
                label = { Text("Модель") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            Text("Класс авто", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CarPanels.carClasses.forEach { c ->
                    FilterChip(
                        selected = carClass == c,
                        onClick = { viewModel.setClass(c) },
                        label = { Text(CarPanels.carClassLabels[c] ?: c) }
                    )
                }
            }

            Text(
                "Отметьте повреждённые зоны (тап)",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
            )
            CarDamageMap(
                selected = selected.keys,
                onToggle = { viewModel.togglePanel(it) },
                modifier = Modifier.fillMaxWidth()
            )

            // Тип повреждения для каждой выбранной зоны
            selected.forEach { (panel, type) ->
                Column {
                    Text(
                        CarPanels.labels[panel] ?: panel,
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CarPanels.damageTypes.forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { viewModel.setDamage(panel, t) },
                                label = { Text(CarPanels.damageLabels[t] ?: t) }
                            )
                        }
                    }
                }
            }

            // Фото
            OutlinedButton(
                onClick = { pickPhoto.launch("image/*") },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Прикрепить фото (${photos.size})")
            }

            // Итог
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Предварительная смета", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${MoneyFormatter.format(totalMin)} — ${MoneyFormatter.format(totalMax)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, color = IndigoPrimary
                    )
                    Text(
                        "Диапазон ±15%. Точнее — после осмотра.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = { viewModel.save() },
                enabled = lines.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) { Text("Сохранить оценку", fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(24.dp))
        }
    }
}