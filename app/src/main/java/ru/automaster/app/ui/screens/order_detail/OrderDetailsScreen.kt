package ru.automaster.app.ui.screens.order_detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.automaster.app.data.local.entity.EmployeeEntity
import ru.automaster.app.ui.components.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    onNavigateBack: () -> Unit,
    viewModel: OrderDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showAccrualDialog by remember { mutableStateOf(false) }
    var employeeToAccrue by remember { mutableStateOf<EmployeeEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Заказ #${state.order?.id ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (state.order != null) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Текущий статус", style = MaterialTheme.typography.labelMedium)
                        Text(state.order!!.status.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }

                Text("Клиент: ${state.client?.name ?: "—"}", style = MaterialTheme.typography.bodyLarge)
                Text("Машина: ${state.car?.make ?: ""} ${state.car?.licensePlate ?: ""}", style = MaterialTheme.typography.bodyLarge)

                // ФИНАНСЫ
                Text("Финансы", style = MaterialTheme.typography.titleMedium)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val totalPaid = state.payments.sumOf { if (it.type.name == "REFUND") -it.amountCents else it.amountCents }
                        val totalSpent = state.expenses.sumOf { it.amountCents }
                        val totalSalary = state.accruals.sumOf { it.amountCents }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Смета:"); Text(MoneyFormatter.format(state.order!!.agreedEstimateCents), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Оплачено клиентом:", color = MaterialTheme.colorScheme.primary); Text(MoneyFormatter.format(totalPaid), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Расходы:", color = MaterialTheme.colorScheme.error); Text(MoneyFormatter.format(totalSpent), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ЗП мастерам:", color = MaterialTheme.colorScheme.tertiary); Text(MoneyFormatter.format(totalSalary), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showPaymentDialog = true }, modifier = Modifier.weight(1f)) { Text("+ Оплата", fontSize = MaterialTheme.typography.labelMedium.fontSize) }
                            OutlinedButton(onClick = { showExpenseDialog = true }, modifier = Modifier.weight(1f)) { Text("+ Расход", fontSize = MaterialTheme.typography.labelMedium.fontSize) }
                        }
                    }
                }

                // СОТРУДНИКИ И ЗП
                Text("Начисления мастерам", style = MaterialTheme.typography.titleMedium)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (state.accruals.isEmpty()) {
                            Text("Нет начислений", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            state.accruals.forEach { accrual ->
                                val empName = state.employees.find { it.id == accrual.employeeId }?.fullName ?: "Сотрудник"
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(empName)
                                    Text(MoneyFormatter.format(accrual.amountCents), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { showBottomSheet = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("+ Начислить ЗП")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // КНОПКИ СТАТУСОВ
                val status = state.order!!.status.name
                if (status == "ACCEPTED" || status == "WAITING") {
                    Button(onClick = { showBottomSheet = true }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Назначить на пост") } // Исправлено: showBottomSheet для постов
                } else if (status == "BODY" || status == "PREP" || status == "PAINT") {
                    Button(onClick = { viewModel.completeCurrentStage(); onNavigateBack() }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Завершить этап") }
                } else if (status == "ISSUE") {
                    Button(onClick = { viewModel.closeOrder(); onNavigateBack() }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) { Text("Выдать автомобиль") }
                }
            }
        }
    }

    // ДИАЛОГИ
    if (showPaymentDialog) {
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
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
        var panelsCount by remember { mutableStateOf("1") } // По умолчанию 1 деталь
        val emp = employeeToAccrue!!

        // Вычисляем сумму для предпросмотра
        val calculatedAmount = if (emp.compensationType == "PERCENT") {
            (state.order!!.agreedEstimateCents * emp.rateValue) / 100
        } else {
            (emp.rateValue * (panelsCount.toIntOrNull() ?: 1)) * 100
        }

        AlertDialog(
            onDismissRequest = { employeeToAccrue = null },
            title = { Text("Начислить ЗП: ${emp.fullName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (emp.compensationType == "PERCENT") {
                        Text("Ставка: ${emp.rateValue}% от сметы")
                        Text("Смета заказа: ${MoneyFormatter.format(state.order!!.agreedEstimateCents)}")
                    } else {
                        Text("Ставка: ${emp.rateValue} ₽ за деталь")
                        OutlinedTextField(
                            value = panelsCount,
                            onValueChange = { panelsCount = it.filter { c -> c.isDigit() } },
                            label = { Text("Кол-во деталей") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Text("К начислению: ${MoneyFormatter.format(calculatedAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val count = panelsCount.toIntOrNull() ?: 1
                    viewModel.addAccrual(emp, count)
                    employeeToAccrue = null
                }) { Text("Начислить") }
            },
            dismissButton = { TextButton(onClick = { employeeToAccrue = null }) { Text("Отмена") } }
        )
    }
    // НИЖНЕЕ МЕНЮ: Выбор поста или сотрудника
    var isSelectingEmployee by remember { mutableStateOf(false) }

    if (showBottomSheet) {
        ModalBottomSheet(onDismissRequest = { showBottomSheet = false; isSelectingEmployee = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (isSelectingEmployee) {
                    Text("Выберите сотрудника", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                    state.employees.forEach { emp ->
                        ListItem(
                            headlineContent = { Text(emp.fullName) },
                            supportingContent = { Text(emp.position) },
                            leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.clickable {
                                employeeToAccrue = emp
                                showBottomSheet = false
                                isSelectingEmployee = false
                            }
                        )
                        HorizontalDivider()
                    }
                } else {
                    Text("Выберите пост", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                    state.posts.forEach { post ->
                        ListItem(
                            headlineContent = { Text(post.name.name.replace("_", " ")) },
                            leadingContent = { Icon(Icons.Default.Build, contentDescription = null) },
                            modifier = Modifier.clickable {
                                viewModel.assignToPost(post); showBottomSheet = false; onNavigateBack()
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}