package ru.automaster.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.automaster.app.data.local.entity.PostEntity
import ru.automaster.app.ui.components.PrimaryGlassButton
import ru.automaster.app.ui.components.glassCard
import ru.automaster.app.ui.theme.GraphiteDark
import ru.automaster.app.ui.theme.IndigoPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToBackup: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var postToRename by remember { mutableStateOf<PostEntity?>(null) }

    // Стейты для финансов
    var taxRate by remember(settings) { mutableStateOf(settings.taxRatePercent.toString()) }
    var insurance by remember(settings) { mutableStateOf((settings.insuranceContributionsCents / 100).toString()) }
    var fixedCosts by remember(settings) { mutableStateOf((settings.monthlyFixedCostsCents / 100).toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- СЕКЦИЯ: ЦЕХ ---
            item { Text("Посты цеха", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White) }
            items(posts, key = { it.id }) { post ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(post.displayName.ifBlank { post.name.name }, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(post.type.name, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }

                    IconButton(onClick = { postToRename = post }) {
                        Icon(Icons.Default.Edit, contentDescription = "Переименовать", tint = Color.White)
                    }

                    Switch(
                        checked = post.isActive,
                        onCheckedChange = { viewModel.togglePostActive(post.id, it) }
                    )
                }
            }

            // --- СЕКЦИЯ: ФИНАНСЫ ---
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { Text("Финансы", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White) }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = taxRate,
                        onValueChange = { taxRate = it.filter { c -> c.isDigit() } },
                        label = { Text("Ставка УСН (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = insurance,
                        onValueChange = { insurance = it.filter { c -> c.isDigit() } },
                        label = { Text("Взносы ИП в год (₽)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = fixedCosts,
                        onValueChange = { fixedCosts = it.filter { c -> c.isDigit() } },
                        label = { Text("Постоянные расходы/мес (₽)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    PrimaryGlassButton(
                        text = "Сохранить финансы",
                        onClick = {
                            viewModel.updateFinancials(
                                taxRate.toIntOrNull() ?: 6,
                                (insurance.toLongOrNull() ?: 0L) * 100,
                                (fixedCosts.toLongOrNull() ?: 0L) * 100
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- СЕКЦИЯ: БЕЗОПАСНОСТЬ И БЭКАПЫ ---
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { Text("Безопасность и Данные", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White) }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().glassCard().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Блокировка приложения", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Защита данных PIN-кодом", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

                    OutlinedButton(
                        onClick = { showPinDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (viewModel.appLockManager.isPinSet()) "Сменить PIN-код" else "Установить PIN-код", color = Color.White)
                    }

                    OutlinedButton(
                        onClick = onNavigateToBackup,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Резервные копии", color = Color.White)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    // --- ДИАЛОГ ПЕРЕИМЕНОВАНИЯ ПОСТА ---
    if (postToRename != null) {
        var newName by remember { mutableStateOf(postToRename!!.displayName) }
        AlertDialog(
            onDismissRequest = { postToRename = null },
            title = { Text("Переименовать пост") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Название") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.renamePost(postToRename!!.id, newName)
                    }
                    postToRename = null
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { postToRename = null }) { Text("Отмена") } }
        )
    }

    // --- ДИАЛОГ УСТАНОВКИ PIN ---
    if (showPinDialog) {
        var pin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Установка PIN-кода") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pin, onValueChange = { if (it.length <= 4) pin = it },
                        label = { Text("Новый PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    OutlinedTextField(
                        value = confirmPin, onValueChange = { if (it.length <= 4) confirmPin = it },
                        label = { Text("Повторите PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pin.length < 4) error = "PIN должен быть 4 цифры"
                    else if (pin != confirmPin) error = "PIN-коды не совпадают"
                    else {
                        viewModel.appLockManager.setPin(pin)
                        showPinDialog = false
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("Отмена") } }
        )
    }
}