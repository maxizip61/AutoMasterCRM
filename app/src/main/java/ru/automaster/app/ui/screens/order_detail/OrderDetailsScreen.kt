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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import ru.automaster.app.data.local.entity.EmployeeEntity
import ru.automaster.app.data.local.entity.PostEntity
import ru.automaster.app.domain.model.PostType
import ru.automaster.app.domain.model.StageDecision
import ru.automaster.app.domain.model.StagePhase
import ru.automaster.app.domain.model.WorkRole
import ru.automaster.app.domain.model.WorkType
import ru.automaster.app.domain.model.displayName
import ru.automaster.app.ui.components.MoneyFormatter
import ru.automaster.app.ui.components.PrimaryGlassButton
import ru.automaster.app.ui.components.RowLabelValue
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

    // Bottom-sheet режимы: "POST" | "EMPLOYEE_ACCRUAL" | "ASSIGN_EMPLOYEE"
    var showBottomSheet by remember { mutableStateOf(false) }
    var bottomSheetMode by remember { mutableStateOf("POST") }

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showAddWorkDialog by remember { mutableStateOf(false) }

    // Выбор фазы для PREP-поста
    var postPendingPhase by remember { mutableStateOf<PostEntity?>(null) }
    // Диалог решения при завершении этапа
    var showStageDecisionDialog by remember { mutableStateOf(false) }

    var employeeToAccrue by remember { mutableStateOf<EmployeeEntity?>(null) }
    var accrualRole by remember { mutableStateOf(WorkRole.BODY) }

    var employeeToAssign by remember { mutableStateOf<EmployeeEntity?>(null) }
    var showRoleDialog by remember { mutableStateOf(false) }

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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Заказ #${state.order?.id ?: ""}",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
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
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else if (state.order != null) {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Статус
                item(key = "status_card") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy
                                )
                            )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Текущий статус",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            AnimatedContent(
                                targetState = state.order!!.status.displayName(),
                                transitionSpec = {
                                    (slideInVertically { it } + fadeIn()) togetherWith
                                            (slideOutVertically { -it } + fadeOut())
                                },
                                label = "status_animation"
                            ) { targetStatus ->
                                Text(
                                    targetStatus,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // 2. Инфо
                item(key = "info") {
                    Column {
                        Text(
                            "Клиент: ${state.client?.name ?: "—"}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Машина: ${state.car?.make ?: ""} ${state.car?.licensePlate ?: ""}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // 3. РАБОТЫ ПО ЗАКАЗУ
                item(key = "works_card") {
                    Text(
                        "Работы по заказу",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .animateContentSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (state.works.isEmpty()) {
                                Text(
                                    "Нет добавленных работ",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                state.works.forEach { work ->
                                    val typeColor = when (work.type) {
                                        WorkType.BODY -> Color(0xFF1565C0)
                                        WorkType.PREP -> Color(0xFFEF6C00)
                                        WorkType.PAINT -> Color(0xFFC62828)
                                        WorkType.MATERIAL -> Color(0xFF6B7280)
                                        WorkType.OTHER -> MaterialTheme.colorScheme.outline
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                work.name,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                work.type.displayName(),
                                                color = typeColor,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Text(
                                            MoneyFormatter.format(work.costCents),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(onClick = { viewModel.deleteWork(work) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Удалить",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            val bodyTotal = state.works
                                .filter { it.type == WorkType.BODY }
                                .sumOf { it.costCents }
                            val prepTotal = state.works
                                .filter { it.type == WorkType.PREP }
                                .sumOf { it.costCents }
                            val paintTotal = state.works
                                .filter { it.type == WorkType.PAINT }
                                .sumOf { it.costCents }
                            val materialTotal = state.works
                                .filter { it.type == WorkType.MATERIAL }
                                .sumOf { it.costCents }

                            if (bodyTotal > 0) RowLabelValue(
                                "Кузовные:",
                                MoneyFormatter.format(bodyTotal),
                                Color(0xFF1565C0)
                            )
                            if (prepTotal > 0) RowLabelValue(
                                "Подготовка:",
                                MoneyFormatter.format(prepTotal),
                                Color(0xFFEF6C00)
                            )
                            if (paintTotal > 0) RowLabelValue(
                                "Покраска:",
                                MoneyFormatter.format(paintTotal),
                                Color(0xFFC62828)
                            )
                            if (materialTotal > 0) RowLabelValue(
                                "Материалы:",
                                MoneyFormatter.format(materialTotal),
                                Color(0xFF6B7280)
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showAddWorkDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) { Text("+ Добавить работу") }
                        }
                    }
                }

                // 4. Финансы
                item(key = "finance_card") {
                    Text(
                        "Финансы",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .animateContentSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val totalPaid = state.payments.sumOf {
                                if (it.type.name == "REFUND") -it.amountCents else it.amountCents
                            }
                            val totalSpent = state.expenses.sumOf { it.amountCents }
                            val totalSalary = state.accruals.sumOf { it.amountCents }

                            RowLabelValue(
                                "Смета:",
                                MoneyFormatter.format(state.order!!.agreedEstimateCents),
                                MaterialTheme.colorScheme.onSurface
                            )
                            RowLabelValue(
                                "Оплачено клиентом:",
                                MoneyFormatter.format(totalPaid),
                                MaterialTheme.colorScheme.primary
                            )
                            RowLabelValue(
                                "Расходы:",
                                MoneyFormatter.format(totalSpent),
                                MaterialTheme.colorScheme.error
                            )
                            RowLabelValue(
                                "ЗП мастерам:",
                                MoneyFormatter.format(totalSalary),
                                MaterialTheme.colorScheme.tertiary
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PrimaryGlassButton(
                                    text = "+ Оплата",
                                    onClick = { showPaymentDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = { showExpenseDialog = true },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) { Text("+ Расход") }
                            }
                        }
                    }
                }

                // 5. Фото
                item(key = "photos_section") {
                    OrderPhotosSection(
                        photos = state.photos,
                        onPrepareCamera = { viewModel.prepareCameraUri(state.order!!.id) },
                        onSavePhotoResult = { success ->
                            viewModel.savePhotoToDb(state.order!!.id, success)
                        }
                    )
                }

                // 6. Сотрудники
                item(key = "assigned_employees") {
                    Text(
                        "Кто работает над заказом",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .animateContentSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (state.assignments.isEmpty()) {
                                Text(
                                    "Никто не назначен",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                state.assignments.forEach { assignment ->
                                    val empName = state.employees
                                        .find { it.id == assignment.employeeId }
                                        ?.fullName ?: "Сотрудник"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                empName,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Роль: ${assignment.role.displayName()}",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.removeAssignment(assignment) }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Удалить",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    bottomSheetMode = "ASSIGN_EMPLOYEE"
                                    showBottomSheet = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) { Text("+ Назначить сотрудника") }
                        }
                    }
                }

                // 7. Начисления ЗП
                item(key = "accruals_card") {
                    Text(
                        "Начисления зарплаты",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .animateContentSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (state.accruals.isEmpty()) {
                                Text(
                                    "Нет начислений",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                state.accruals.forEach { accrual ->
                                    val empName = state.employees
                                        .find { it.id == accrual.employeeId }
                                        ?.fullName ?: "Сотрудник"
                                    RowLabelValue(
                                        "$empName (${accrual.role.displayName()})",
                                        MoneyFormatter.format(accrual.amountCents)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    bottomSheetMode = "EMPLOYEE_ACCRUAL"
                                    showBottomSheet = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) { Text("+ Начислить ЗП") }
                        }
                    }
                }

                // 8. Итоги по выдаче
                if (state.order!!.status.name == "DONE" &&
                    state.order!!.actualDeliveryDate != null
                ) {
                    item(key = "completion_report") {
                        Text(
                            "Итоги по заказу",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val dateStr = DateTimeFormatter
                                    .ofPattern("dd.MM.yyyy HH:mm")
                                    .withZone(ZoneId.systemDefault())
                                    .format(state.order!!.actualDeliveryDate!!)
                                Text(
                                    "Дата выдачи авто: $dateStr",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Выполненные работы:",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                if (state.works.isEmpty()) {
                                    Text(
                                        "Нет данных",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else {
                                    state.works.forEach { work ->
                                        Text(
                                            "• ${work.name} (${work.type.displayName()}) — " +
                                                    MoneyFormatter.format(work.costCents),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Исполнители:",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                state.assignments.forEach { a ->
                                    val empName = state.employees
                                        .find { it.id == a.employeeId }
                                        ?.fullName ?: "—"
                                    Text(
                                        "• ${a.role.displayName()}: $empName",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // 9. Кнопки действия
                item(key = "action_buttons") {
                    val status = state.order!!.status.name
                    Spacer(modifier = Modifier.height(8.dp))
                    when (status) {
                        "ACCEPTED", "WAITING" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                PrimaryGlassButton(
                                    text = "Назначить на пост",
                                    onClick = {
                                        bottomSheetMode = "POST"
                                        showBottomSheet = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (status == "WAITING") {
                                    OutlinedButton(
                                        onClick = { viewModel.markReadyForIssue() },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) { Text("Готов к выдаче") }
                                }
                            }
                        }
                        "BODY", "PREP", "PAINT" -> PrimaryGlassButton(
                            text = "Завершить этап",
                            onClick = { showStageDecisionDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                        "ISSUE" -> PrimaryGlassButton(
                            text = "Выдать автомобиль",
                            onClick = {
                                viewModel.closeOrder()
                                onNavigateBack()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // --- ДИАЛОГИ ---

    // Диалог выбора фазы для PREP-поста
    if (postPendingPhase != null) {
        val post = postPendingPhase!!
        AlertDialog(
            onDismissRequest = { postPendingPhase = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Вид работ на «${post.displayName.ifEmpty { post.name.name }}»") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.assignToPost(post, StagePhase.BODY)
                            postPendingPhase = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Кузовные работы (рихтовка)") }
                    Button(
                        onClick = {
                            viewModel.assignToPost(post, StagePhase.PREP)
                            postPendingPhase = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Подготовка (шпаклёвка, шлифовка)") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { postPendingPhase = null }) { Text("Отмена") }
            }
        )
    }

    // Диалог решения при завершении этапа
    if (showStageDecisionDialog) {
        AlertDialog(
            onDismissRequest = {
                // Закрытие без выбора = безопасный дефолт: нужен ещё пост.
                viewModel.completeCurrentStage(StageDecision.NEED_MORE_POST)
                showStageDecisionDialog = false
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Этап завершён. Что дальше?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.completeCurrentStage(StageDecision.NEED_MORE_POST)
                            showStageDecisionDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Нужен ещё пост") }
                    Button(
                        onClick = {
                            viewModel.completeCurrentStage(StageDecision.READY_FOR_ISSUE)
                            showStageDecisionDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Готов к выдаче") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    viewModel.completeCurrentStage(StageDecision.NEED_MORE_POST)
                    showStageDecisionDialog = false
                }) { Text("Отмена") }
            }
        )
    }

    if (showPaymentDialog) {
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Добавить оплату") },
            text = {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() } },
                    label = { Text("Сумма (₽)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    amount.toLongOrNull()?.let { viewModel.addPayment(it) }
                    showPaymentDialog = false
                }) { Text("Добавить") }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showExpenseDialog) {
        var amount by remember { mutableStateOf("") }
        var comment by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showExpenseDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Добавить расход") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { c -> c.isDigit() } },
                        label = { Text("Сумма (₽)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Комментарий") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    amount.toLongOrNull()?.let { viewModel.addExpense(it, comment) }
                    showExpenseDialog = false
                }) { Text("Добавить") }
            },
            dismissButton = {
                TextButton(onClick = { showExpenseDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showAddWorkDialog) {
        var workName by remember { mutableStateOf("") }
        var workCost by remember { mutableStateOf("") }
        var workType by remember { mutableStateOf(WorkType.BODY) }

        AlertDialog(
            onDismissRequest = { showAddWorkDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Добавить работу") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = workName,
                        onValueChange = { workName = it },
                        label = { Text("Название (Снять бампер)") }
                    )
                    OutlinedTextField(
                        value = workCost,
                        onValueChange = { workCost = it.filter { c -> c.isDigit() } },
                        label = { Text("Стоимость (₽)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text("Тип работы:", color = MaterialTheme.colorScheme.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = workType == WorkType.BODY,
                            onClick = { workType = WorkType.BODY },
                            label = { Text("Кузов") }
                        )
                        FilterChip(
                            selected = workType == WorkType.PREP,
                            onClick = { workType = WorkType.PREP },
                            label = { Text("Подгот.") }
                        )
                        FilterChip(
                            selected = workType == WorkType.PAINT,
                            onClick = { workType = WorkType.PAINT },
                            label = { Text("Маляр") }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = workType == WorkType.MATERIAL,
                            onClick = { workType = WorkType.MATERIAL },
                            label = { Text("Материал") }
                        )
                        FilterChip(
                            selected = workType == WorkType.OTHER,
                            onClick = { workType = WorkType.OTHER },
                            label = { Text("Прочее") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (workName.isNotBlank() && workCost.isNotBlank()) {
                        viewModel.addWork(workName, workType, workCost.toLong())
                        showAddWorkDialog = false
                    }
                }) { Text("Добавить") }
            },
            dismissButton = {
                TextButton(onClick = { showAddWorkDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (employeeToAccrue != null) {
        var panelsCount by remember { mutableStateOf("1") }
        val emp = employeeToAccrue!!

        val bodyWorksCents = state.works
            .filter { it.type == WorkType.BODY }
            .sumOf { it.costCents }
        val calculatedAmount: Long = when (accrualRole) {
            WorkRole.BODY -> (bodyWorksCents * emp.rateValue) / 100
            else -> emp.rateValue * 100 * (panelsCount.toIntOrNull() ?: 1)
        }

        AlertDialog(
            onDismissRequest = { employeeToAccrue = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Начислить ЗП: ${emp.fullName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Роль на заказе:", color = MaterialTheme.colorScheme.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = accrualRole == WorkRole.BODY,
                            onClick = { accrualRole = WorkRole.BODY },
                            label = { Text("Кузовщик") }
                        )
                        FilterChip(
                            selected = accrualRole == WorkRole.PREP,
                            onClick = { accrualRole = WorkRole.PREP },
                            label = { Text("Подготовщик") }
                        )
                        FilterChip(
                            selected = accrualRole == WorkRole.PAINT,
                            onClick = { accrualRole = WorkRole.PAINT },
                            label = { Text("Маляр") }
                        )
                    }

                    if (accrualRole == WorkRole.BODY) {
                        Text(
                            "Ставка: ${emp.rateValue}% от суммы кузовных работ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Сумма кузовных работ: ${MoneyFormatter.format(bodyWorksCents)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "Ставка: ${emp.rateValue} ₽ за деталь",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = panelsCount,
                            onValueChange = { panelsCount = it.filter { c -> c.isDigit() } },
                            label = { Text("Кол-во деталей") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "К начислению: ${MoneyFormatter.format(calculatedAmount)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addAccrual(emp, accrualRole, panelsCount.toIntOrNull() ?: 1)
                    employeeToAccrue = null
                }) { Text("Начислить") }
            },
            dismissButton = {
                TextButton(onClick = { employeeToAccrue = null }) { Text("Отмена") }
            }
        )
    }

    if (showRoleDialog && employeeToAssign != null) {
        val emp = employeeToAssign!!
        AlertDialog(
            onDismissRequest = {
                showRoleDialog = false
                employeeToAssign = null
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Роль: ${emp.fullName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.assignEmployee(emp, WorkRole.BODY)
                            showRoleDialog = false
                            employeeToAssign = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Кузовщик") }
                    Button(
                        onClick = {
                            viewModel.assignEmployee(emp, WorkRole.PREP)
                            showRoleDialog = false
                            employeeToAssign = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Подготовщик") }
                    Button(
                        onClick = {
                            viewModel.assignEmployee(emp, WorkRole.PAINT)
                            showRoleDialog = false
                            employeeToAssign = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Маляр") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showRoleDialog = false
                    employeeToAssign = null
                }) { Text("Отмена") }
            }
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                when (bottomSheetMode) {
                    "POST" -> {
                        Text(
                            "Выберите пост",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        state.posts.forEach { post ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        post.displayName.ifEmpty {
                                            post.name.name.replace("_", " ")
                                        },
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        if (post.type == PostType.PAINT)
                                            "Покрасочная камера"
                                        else
                                            "Универсальный (кузов/подготовка)",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Build,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                modifier = Modifier.clickable {
                                    showBottomSheet = false
                                    if (post.type == PostType.PAINT) {
                                        viewModel.assignToPost(post, StagePhase.PAINT)
                                    } else {
                                        postPendingPhase = post
                                    }
                                }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                    }
                    "EMPLOYEE_ACCRUAL" -> {
                        Text(
                            "Выберите сотрудника",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        if (state.employees.isEmpty()) Text(
                            "Нет сотрудников",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        state.employees.forEach { emp ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        emp.fullName,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        emp.position,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                modifier = Modifier.clickable {
                                    employeeToAccrue = emp
                                    showBottomSheet = false
                                }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                    }
                    "ASSIGN_EMPLOYEE" -> {
                        Text(
                            "Выберите сотрудника",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        if (state.employees.isEmpty()) Text(
                            "Нет сотрудников",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        state.employees.forEach { emp ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        emp.fullName,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        emp.position,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                modifier = Modifier.clickable {
                                    employeeToAssign = emp
                                    showBottomSheet = false
                                    showRoleDialog = true
                                }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
}