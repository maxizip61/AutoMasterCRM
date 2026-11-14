package ru.automaster.app.ui.screens.order_detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import ru.automaster.app.data.local.entity.EmployeeEntity
import ru.automaster.app.ui.components.MoneyFormatter
import ru.automaster.app.ui.components.PrimaryGlassButton
import ru.automaster.app.ui.components.RowLabelValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    onNavigateBack: () -> Unit,
    viewModel: OrderDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showBottomSheet by remember { mutableStateOf(false) }
    var bottomSheetMode by remember { mutableStateOf("POST") }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var employeeToAccrue by remember { mutableStateOf<EmployeeEntity?>(null) }

    LaunchedEffect(message) {
        if (message != null) {
            scope.launch {
                snackbarHostState.showSnackbar(message!!)
                viewModel.clearMessage()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Заказ #${state.order?.id ?: ""}", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (state.order != null) {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "status_card") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Текущий статус", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            AnimatedContent(
                                targetState = state.order!!.status.name,
                                transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
                                label = "status_animation"
                            ) { targetStatus ->
                                Text(targetStatus, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                item(key = "info") {
                    Column {
                        Text("Клиент: ${state.client?.name ?: "—"}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                        Text("Машина: ${state.car?.make ?: ""} ${state.car?.licensePlate ?: ""}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                    }
                }

                item(key = "finance_card") {
                    Text("Финансы", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).animateContentSize(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val totalPaid = state.payments.sumOf { if (it.type.name == "REFUND") -it.amountCents else it.amountCents }
                            val totalSpent = state.expenses.sumOf { it.amountCents }
                            val totalSalary = state.accruals.sumOf { it.amountCents }

                            RowLabelValue("Смета:", MoneyFormatter.format(state.order!!.agreedEstimateCents), MaterialTheme.colorScheme.onSurface)
                            RowLabelValue("Оплачено клиентом:", MoneyFormatter.format(totalPaid), MaterialTheme.colorScheme.primary)
                            RowLabelValue("Расходы:", MoneyFormatter.format(totalSpent), MaterialTheme.colorScheme.error)
                            RowLabelValue("ЗП мастерам:", MoneyFormatter.format(totalSalary), MaterialTheme.colorScheme.tertiary)

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PrimaryGlassButton(text = "+ Оплата", onClick = { showPaymentDialog = true }, modifier = Modifier.weight(1f))
                                OutlinedButton(
                                    onClick = { showExpenseDialog = true },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                                ) {
                                    Text("+ Расход")
                                }
                            }
                        }
                    }
                }

                item(key = "photos_section") {
                    OrderPhotosSection(
                        photos = state.photos,
                        onPrepareCamera = { viewModel.prepareCameraUri(state.order!!.id) },
                        onSavePhotoResult = { success -> viewModel.savePhotoToDb(state.order!!.id, success) }
                    )
                }

                item(key = "salary_card") {
                    Text("Начисления мастерам", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).animateContentSize(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (state.accruals.isEmpty()) {
                                Text("Нет начислений", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            } else {
                                state.accruals.forEach { accrual ->
                                    val empName = state.employees.find { it.id == accrual.employeeId }?.fullName ?: "Сотрудник"
                                    RowLabelValue(empName, MoneyFormatter.format(accrual.amountCents))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { bottomSheetMode = "EMPLOYEE"; showBottomSheet = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                            ) {
                                Text("+ Начислить ЗП")
                            }
                        }
                    }
                }

                item(key = "action_button") {
                    val status = state.order!!.status.name
                    Spacer(modifier = Modifier.height(8.dp))
                    if (status == "ACCEPTED" || status == "WAITING") {
                        PrimaryGlassButton(text = "Назначить на пост", onClick = { bottomSheetMode = "POST"; showBottomSheet = true }, modifier = Modifier.fillMaxWidth())
                    } else if (status == "BODY" || status == "PREP" || status == "PAINT") {
                        PrimaryGlassButton(text = "Завершить этап", onClick = { viewModel.completeCurrentStage(); onNavigateBack() }, modifier = Modifier.fillMaxWidth())
                    } else if (status == "ISSUE") {
                        PrimaryGlassButton(text = "Выдать автомобиль", onClick = { viewModel.closeOrder(); onNavigateBack() }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    // --- ДИАЛОГИ ---
    if (showPaymentDialog) {
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Добавить оплату") },
            text = { OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() } }, label = { Text("Сумма в рублях") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) },
            confirmButton = { TextButton(onClick = { amount.toLongOrNull()?.let { viewModel.addPayment(it) }; showPaymentDialog = false }) { Text("Добавить") } },
            dismissButton = { TextButton(onClick = { showPaymentDialog = false }) { Text("Отмена") } }
        )
    }

    if (showExpenseDialog) {
        var amount by remember { mutableStateOf("") }
        var comment by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showExpenseDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Добавить расход") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() } }, label = { Text("Сумма в рублях") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Комментарий") })
            }},
            confirmButton = { TextButton(onClick = { amount.toLongOrNull()?.let { viewModel.addExpense(it, comment) }; showExpenseDialog = false }) { Text("Добавить") } },
            dismissButton = { TextButton(onClick = { showExpenseDialog = false }) { Text("Отмена") } }
        )
    }

    if (employeeToAccrue != null) {
        var panelsCount by remember { mutableStateOf("1") }
        val emp = employeeToAccrue!!
        val calculatedAmount = if (emp.compensationType == "PERCENT") (state.order!!.agreedEstimateCents * emp.rateValue) / 100 else (emp.rateValue * (panelsCount.toIntOrNull() ?: 1)) * 100

        AlertDialog(
            onDismissRequest = { employeeToAccrue = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Начислить ЗП: ${emp.fullName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (emp.compensationType == "PERCENT") {
                        Text("Ставка: ${emp.rateValue}% от сметы")
                    } else {
                        Text("Ставка: ${emp.rateValue} ₽ за деталь")
                        OutlinedTextField(value = panelsCount, onValueChange = { panelsCount = it.filter { c -> c.isDigit() } }, label = { Text("Кол-во деталей") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    Text("К начислению: ${MoneyFormatter.format(calculatedAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.addAccrual(emp, panelsCount.toIntOrNull() ?: 1); employeeToAccrue = null }) { Text("Начислить") } },
            dismissButton = { TextButton(onClick = { employeeToAccrue = null }) { Text("Отмена") } }
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (bottomSheetMode == "EMPLOYEE") {
                    Text("Выберите сотрудника", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 16.dp))
                    if (state.employees.isEmpty()) {
                        Text("Нет сотрудников. Добавьте их на вкладке 'Зарплата'.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    state.employees.forEach { emp ->
                        ListItem(
                            headlineContent = { Text(emp.fullName, color = MaterialTheme.colorScheme.onSurface) },
                            supportingContent = { Text(emp.position, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingContent = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                            modifier = Modifier.clickable { employeeToAccrue = emp; showBottomSheet = false }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                } else {
                    Text("Выберите пост", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 16.dp))
                    state.posts.forEach { post ->
                        ListItem(
                            headlineContent = { Text(post.name.name.replace("_", " "), color = MaterialTheme.colorScheme.onSurface) },
                            leadingContent = { Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                            modifier = Modifier.clickable { viewModel.assignToPost(post); showBottomSheet = false; onNavigateBack() }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}