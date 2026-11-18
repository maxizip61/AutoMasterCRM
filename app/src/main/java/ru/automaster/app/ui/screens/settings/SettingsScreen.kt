package ru.automaster.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import ru.automaster.app.data.local.entity.EmployeeEntity
import ru.automaster.app.data.local.entity.PostEntity
import ru.automaster.app.domain.model.EmployeeSpecialty
import ru.automaster.app.ui.components.PrimaryGlassButton
import ru.automaster.app.ui.components.glassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToBackup: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val employees by viewModel.employees.collectAsState()
    val archived by viewModel.archivedEmployees.collectAsState()
    val message by viewModel.message.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var showPinDialog by remember { mutableStateOf(false) }
    var postToRename by remember { mutableStateOf<PostEntity?>(null) }

    // null = закрыт; true-режим редактирования определяется employeeToEdit
    var showEmployeeDialog by remember { mutableStateOf(false) }
    var employeeToEdit by remember { mutableStateOf<EmployeeEntity?>(null) }
    var employeeToArchive by remember { mutableStateOf<EmployeeEntity?>(null) }
    var showArchived by remember { mutableStateOf(false) }

    var taxRate by remember(settings) { mutableStateOf(settings.taxRatePercent.toString()) }
    var insurance by remember(settings) {
        mutableStateOf((settings.insuranceContributionsCents / 100).toString())
    }
    var fixedCosts by remember(settings) {
        mutableStateOf((settings.monthlyFixedCostsCents / 100).toString())
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
                title = { Text("Настройки", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ══════ СОТРУДНИКИ ЦЕХА ══════
            item(key = "emp_title") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Сотрудники цеха",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "${employees.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item(key = "emp_hint") {
                Text(
                    "Специализация задаётся здесь один раз. При назначении на заказ " +
                            "роль подставится автоматически.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (employees.isEmpty()) {
                item(key = "emp_empty") {
                    Text(
                        "Мастеров пока нет. Добавьте первого.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(employees, key = { "emp_${it.id}" }) { emp ->
                    val isBody = emp.compensationType == "PERCENT"
                    val rateText = if (isBody) "${emp.rateValue}% от кузовных работ"
                    else "${emp.rateValue} ₽ / деталь"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.padding(10.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                emp.fullName,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                emp.position,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                rateText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            employeeToEdit = emp
                            showEmployeeDialog = true
                        }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Изменить",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { employeeToArchive = emp }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "В архив",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            item(key = "emp_add") {
                OutlinedButton(
                    onClick = {
                        employeeToEdit = null
                        showEmployeeDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Добавить сотрудника")
                }
            }

            if (archived.isNotEmpty()) {
                item(key = "emp_arch_toggle") {
                    TextButton(onClick = { showArchived = !showArchived }) {
                        Text(
                            if (showArchived) "Скрыть архив (${archived.size})"
                            else "Показать архив (${archived.size})"
                        )
                    }
                }
                if (showArchived) {
                    items(archived, key = { "arch_${it.id}" }) { emp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    emp.fullName,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${emp.position} · в архиве",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            TextButton(onClick = { viewModel.restoreEmployee(emp) }) {
                                Text("Вернуть")
                            }
                        }
                    }
                }
            }

            // ══════ ПОСТЫ ══════
            item(key = "posts_spacer") { Spacer(modifier = Modifier.height(8.dp)) }
            item(key = "posts_title") {
                Text(
                    "Посты цеха",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(posts, key = { "post_${it.id}" }) { post ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            post.displayName.ifBlank { post.name.name },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (post.type.name == "PAINT") "Покрасочная камера"
                            else "Универсальный (кузов/подготовка)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = { postToRename = post }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Переименовать",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Switch(
                        checked = post.isActive,
                        onCheckedChange = { viewModel.togglePostActive(post.id, it) }
                    )
                }
            }

            // ══════ ФИНАНСЫ ══════
            item(key = "fin_spacer") { Spacer(modifier = Modifier.height(8.dp)) }
            item(key = "fin_title") {
                Text(
                    "Финансы",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            item(key = "fin_card") {
                Column(
                    modifier = Modifier.fillMaxWidth().glassCard().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = taxRate,
                        onValueChange = { taxRate = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Ставка УСН (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    OutlinedTextField(
                        value = insurance,
                        onValueChange = { insurance = it.filter { c -> c.isDigit() } },
                        label = { Text("Взносы ИП в год (₽)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    OutlinedTextField(
                        value = fixedCosts,
                        onValueChange = { fixedCosts = it.filter { c -> c.isDigit() } },
                        label = { Text("Постоянные расходы/мес (₽)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
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

            // ══════ БЕЗОПАСНОСТЬ И ДАННЫЕ ══════
            item(key = "sec_spacer") { Spacer(modifier = Modifier.height(8.dp)) }
            item(key = "sec_title") {
                Text(
                    "Безопасность и данные",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            item(key = "sec_card") {
                Column(
                    modifier = Modifier.fillMaxWidth().glassCard().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Блокировка приложения",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Защита данных PIN-кодом",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        onClick = { showPinDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (viewModel.appLockManager.isPinSet()) "Сменить PIN-код"
                            else "Установить PIN-код",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    OutlinedButton(
                        onClick = onNavigateToBackup,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Резервные копии", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    // ══════ ДИАЛОГ: ДОБАВИТЬ / ИЗМЕНИТЬ СОТРУДНИКА ══════
    if (showEmployeeDialog) {
        val editing = employeeToEdit
        var name by remember(editing) { mutableStateOf(editing?.fullName ?: "") }
        var specialty by remember(editing) {
            mutableStateOf(
                editing?.position?.takeIf { it in EmployeeSpecialty.all }
                    ?: EmployeeSpecialty.BODY
            )
        }
        var rateText by remember(editing) {
            mutableStateOf(editing?.rateValue?.toString() ?: "")
        }
        var error by remember { mutableStateOf<String?>(null) }

        val rate = rateText.toLongOrNull() ?: 0L
        val valid = name.isNotBlank() && rate > 0L &&
                (specialty != EmployeeSpecialty.BODY || rate <= 100L)

        AlertDialog(
            onDismissRequest = {
                showEmployeeDialog = false
                employeeToEdit = null
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(if (editing == null) "Новый сотрудник" else "Изменить данные") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("ФИО") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "Специализация",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EmployeeSpecialty.all.forEach { s ->
                            FilterChip(
                                selected = specialty == s,
                                onClick = { specialty = s; rateText = "" },
                                label = { Text(s, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = rateText,
                        onValueChange = {
                            rateText = it.filter { c -> c.isDigit() }
                                .take(if (specialty == EmployeeSpecialty.BODY) 3 else 7)
                        },
                        label = { Text(EmployeeSpecialty.rateLabelFor(specialty)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (specialty == EmployeeSpecialty.BODY) {
                        Text(
                            "Процент считается от суммы кузовных работ по заказу. " +
                                    "Материалы и покраска в базу не входят.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (editing != null && editing.position != specialty) {
                        Text(
                            "Специализация меняется: ${editing.position} → $specialty. " +
                                    "Прошлые начисления останутся без изменений.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = valid,
                    onClick = {
                        if (editing == null) {
                            viewModel.addEmployee(name, specialty, rate)
                        } else {
                            viewModel.updateEmployee(editing, name, specialty, rate)
                        }
                        showEmployeeDialog = false
                        employeeToEdit = null
                    }
                ) { Text(if (editing == null) "Добавить" else "Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEmployeeDialog = false
                    employeeToEdit = null
                }) { Text("Отмена") }
            }
        )
    }

    // ══════ ДИАЛОГ: В АРХИВ ══════
    if (employeeToArchive != null) {
        val emp = employeeToArchive!!
        AlertDialog(
            onDismissRequest = { employeeToArchive = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Убрать в архив?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${emp.fullName} (${emp.position}) больше не будет появляться " +
                                "в списках назначения на заказ.",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Все начисления зарплаты сохранятся — история не потеряется. " +
                                "Вернуть мастера можно из архива в любой момент.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.archiveEmployee(emp)
                    employeeToArchive = null
                }) { Text("В архив", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { employeeToArchive = null }) { Text("Отмена") }
            }
        )
    }

    // ══════ ДИАЛОГ: ПЕРЕИМЕНОВАТЬ ПОСТ ══════
    if (postToRename != null) {
        var newName by remember { mutableStateOf(postToRename!!.displayName) }
        AlertDialog(
            onDismissRequest = { postToRename = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Переименовать пост") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        viewModel.renamePost(postToRename!!.id, newName)
                        postToRename = null
                    }
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { postToRename = null }) { Text("Отмена") }
            }
        )
    }

    // ══════ ДИАЛОГ: PIN ══════
    if (showPinDialog) {
        var pin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Установка PIN-кода") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4) pin = it.filter { c -> c.isDigit() } },
                        label = { Text("Новый PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = {
                            if (it.length <= 4) confirmPin = it.filter { c -> c.isDigit() }
                        },
                        label = { Text("Повторите PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        pin.length < 4 -> error = "PIN должен быть 4 цифры"
                        pin != confirmPin -> error = "PIN-коды не совпадают"
                        else -> {
                            viewModel.appLockManager.setPin(pin)
                            showPinDialog = false
                        }
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Отмена") }
            }
        )
    }
}