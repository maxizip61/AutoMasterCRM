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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.automaster.app.data.local.entity.EmployeeEntity
import ru.automaster.app.data.local.entity.OrderEmployeeAccrualEntity
import ru.automaster.app.domain.model.EmployeeSpecialty
import ru.automaster.app.domain.model.PostType
import ru.automaster.app.domain.model.StageDecision
import ru.automaster.app.domain.model.WorkRole
import ru.automaster.app.domain.model.WorkType
import ru.automaster.app.domain.model.displayName
import ru.automaster.app.ui.components.MoneyFormatter
import ru.automaster.app.ui.components.PrimaryGlassButton
import ru.automaster.app.ui.components.RowLabelValue
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ReportDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    onNavigateBack: () -> Unit,
    viewModel: OrderDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showBottomSheet by remember { mutableStateOf(false) }
    var bottomSheetMode by remember { mutableStateOf("POST") }

    var postPickEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showAddWorkDialog by remember { mutableStateOf(false) }
    var showStageDecisionDialog by remember { mutableStateOf(false) }
    var employeeToAccrue by remember { mutableStateOf<EmployeeEntity?>(null) }

    // Начисление, которое владелец собирается отменить
    var accrualToReverse by remember { mutableStateOf<OrderEmployeeAccrualEntity?>(null) }

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
                                .filter { it.type == WorkType.BODY }.sumOf { it.costCents }
                            val prepTotal = state.works
                                .filter { it.type == WorkType.PREP }.sumOf { it.costCents }
                            val paintTotal = state.works
                                .filter { it.type == WorkType.PAINT }.sumOf { it.costCents }
                            val materialTotal = state.works
                                .filter { it.type == WorkType.MATERIAL }.sumOf { it.costCents }

                            if (bodyTotal > 0) RowLabelValue(
                                "Кузовные:", MoneyFormatter.format(bodyTotal), Color(0xFF1565C0)
                            )
                            if (prepTotal > 0) RowLabelValue(
                                "Подготовка:", MoneyFormatter.format(prepTotal), Color(0xFFEF6C00)
                            )
                            if (paintTotal > 0) RowLabelValue(
                                "Покраска:", MoneyFormatter.format(paintTotal), Color(0xFFC62828)
                            )
                            if (materialTotal > 0) RowLabelValue(
                                "Материалы:", MoneyFormatter.format(materialTotal), Color(0xFF6B7280)
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
                            // Итог ЗП с учётом сторно: так же, как считает SQL в DAO
                            val totalSalary = state.accruals.sumOf {
                                if (it.reversalOfId == null) it.amountCents else -it.amountCents
                            }

                            RowLabelValue(
                                "Смета:",
                                MoneyFormatter.format(state.order!!.agreedEstimateCents),
                                MaterialTheme.colorScheme.onSurface
                            )
                            RowLabelValue(
                                "Оплачено клиентом:", MoneyFormatter.format(totalPaid),
                                MaterialTheme.colorScheme.primary
                            )
                            RowLabelValue(
                                "Расходы:", MoneyFormatter.format(totalSpent),
                                MaterialTheme.colorScheme.error
                            )
                            RowLabelValue(
                                "ЗП мастерам:", MoneyFormatter.format(totalSalary),
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

                item(key = "photos_section") {
                    OrderPhotosSection(
                        photos = state.photos,
                        onPrepareCamera = { viewModel.prepareCameraUri(state.order!!.id) },
                        onSavePhotoResult = { success ->
                            viewModel.savePhotoToDb(state.order!!.id, success)
                        }
                    )
                }

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
                                    val emp = state.employees.find { it.id == assignment.employeeId }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                emp?.fullName ?: "Мастер в архиве",
                                                color = if (emp == null)
                                                    MaterialTheme.colorScheme.error
                                                else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Роль: ${assignment.role.displayName()}",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            if (emp != null &&
                                                EmployeeSpecialty.roleOf(
                                                    emp.position, emp.compensationType
                                                ) != assignment.role
                                            ) {
                                                Text(
                                                    "Специализация изменена — снимите и назначьте заново",
                                                    color = MaterialTheme.colorScheme.error,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { viewModel.removeAssignment(assignment) }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Снять с заказа",
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
                            ) { Text("+ Добавить мастера на заказ") }
                        }
                    }
                }

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
                                        ?.fullName ?: "Мастер"
                                    val isReversalRow = accrual.reversalOfId != null
                                    val wasReversed = accrual.id in state.reversedIds
                                    val dimmed = isReversalRow || wasReversed

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                if (isReversalRow) "Отмена начисления"
                                                else "$empName (${accrual.role.displayName()})",
                                                color = if (dimmed)
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (dimmed)
                                                    FontWeight.Normal else FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (wasReversed && !isReversalRow) {
                                                Text(
                                                    "отменено",
                                                    color = MaterialTheme.colorScheme.outline,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                        Text(
                                            (if (isReversalRow) "−" else "") +
                                                    MoneyFormatter.format(accrual.amountCents),
                                            color = if (isReversalRow)
                                                MaterialTheme.colorScheme.error
                                            else if (wasReversed)
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            textDecoration = if (wasReversed && !isReversalRow)
                                                TextDecoration.LineThrough else null
                                        )
                                        if (!dimmed) {
                                            TextButton(
                                                onClick = { accrualToReverse = accrual },
                                                contentPadding = PaddingValues(
                                                    horizontal = 8.dp, vertical = 0.dp
                                                )
                                            ) { Text("Отменить") }
                                        } else {
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                    }
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    )
                                }

                                val net = state.accruals.sumOf {
                                    if (it.reversalOfId == null) it.amountCents
                                    else -it.amountCents
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                RowLabelValue(
                                    "Итого к выплате:",
                                    MoneyFormatter.format(net),
                                    MaterialTheme.colorScheme.tertiary
                                )
                                if (state.reversedIds.isNotEmpty()) {
                                    Text(
                                        "Отменённые начисления сохранены в истории и не учитываются в итоге.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                Text(
                                    "Дата выдачи авто: " +
                                            ReportDateFormatter.format(state.order!!.actualDeliveryDate!!),
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
                                        .find { it.id == a.employeeId }?.fullName ?: "—"
                                    Text(
                                        "• ${a.role.displayName()}: $empName",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "action_buttons") {
                    val status = state.order!!.status.name
                    Spacer(modifier = Modifier.height(8.dp))
                    when (status) {
                        "ACCEPTED", "WAITING" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                PrimaryGlassButton(
                                    text = "Поставить машину на пост",
                                    onClick = {
                                        postPickEmployee = null
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
                            onClick = { viewModel.closeOrder() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // --- ПОДТВЕРЖДЕНИЕ ОТМЕНЫ НАЧИСЛЕНИЯ ---
    if (accrualToReverse != null) {
        val a = accrualToReverse!!
        val empName = state.employees.find { it.id == a.employeeId }?.fullName ?: "мастеру"
        AlertDialog(
            onDismissRequest = { accrualToReverse = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Отменить начисление?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "$empName · ${a.role.displayName()} · " +
                                MoneyFormatter.format(a.amountCents),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Исходная запись останется в истории, рядом появится запись отмены. " +
                                "Итог к выплате уменьшится на эту сумму.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "После отмены можно удалить работу или начислить заново.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.reverseAccrual(a)
                    accrualToReverse = null
                }) { Text("Отменить начисление", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { accrualToReverse = null }) { Text("Назад") }
            }
        )
    }

    // --- ДИАЛОГ РЕШЕНИЯ ПРИ ЗАВЕРШЕНИИ ЭТАПА ---
    if (showStageDecisionDialog) {
        AlertDialog(
            onDismissRequest = {
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = (amount.toLongOrNull() ?: 0L) > 0L,
                    onClick = {
                        amount.toLongOrNull()?.let { viewModel.addPayment(it) }
                        showPaymentDialog = false
                    }
                ) { Text("Добавить") }
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Комментарий") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = (amount.toLongOrNull() ?: 0L) > 0L,
                    onClick = {
                        amount.toLongOrNull()?.let { viewModel.addExpense(it, comment) }
                        showExpenseDialog = false
                    }
                ) { Text("Добавить") }
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
                        label = { Text("Название (Снять бампер)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = workCost,
                        onValueChange = { workCost = it.filter { c -> c.isDigit() } },
                        label = { Text("Стоимость (₽)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Тип работы:", color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            WorkType.BODY, WorkType.PREP, WorkType.PAINT,
                            WorkType.MATERIAL, WorkType.OTHER
                        ).forEach { t ->
                            FilterChip(
                                selected = workType == t,
                                onClick = { workType = t },
                                label = { Text(t.displayName(), maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = workName.isNotBlank() && (workCost.toLongOrNull() ?: 0L) > 0L,
                    onClick = {
                        viewModel.addWork(workName, workType, workCost.toLong())
                        showAddWorkDialog = false
                    }
                ) { Text("Добавить") }
            },
            dismissButton = {
                TextButton(onClick = { showAddWorkDialog = false }) { Text("Отмена") }
            }
        )
    }

    // --- НАЧИСЛЕНИЕ ЗП: роль из профиля ---
    if (employeeToAccrue != null) {
        val emp = employeeToAccrue!!
        val role = EmployeeSpecialty.roleOf(emp.position, emp.compensationType)
        val isBody = role == WorkRole.BODY

        var panelsCount by remember(emp.id) { mutableStateOf("1") }
        var rateInput by remember(emp.id) {
            mutableStateOf(if (emp.rateValue > 0L) emp.rateValue.toString() else "")
        }

        val rate = rateInput.toLongOrNull() ?: 0L
        val panels = panelsCount.toIntOrNull() ?: 1
        val bodyWorksCents = state.works
            .filter { it.type == WorkType.BODY }.sumOf { it.costCents }
        val preview: Long = if (isBody) bodyWorksCents * rate / 100 else rate * panels * 100
        val canSubmit = role != WorkRole.GENERAL && rate > 0L &&
                (!isBody || bodyWorksCents > 0L) && (isBody || panels >= 1)

        AlertDialog(
            onDismissRequest = { employeeToAccrue = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Начислить ЗП: ${emp.fullName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            role.displayName(),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (role == WorkRole.GENERAL) {
                        Text(
                            "Специализация не задана. Откройте Настройки → Сотрудники цеха.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (isBody) {
                        Text(
                            "Сумма кузовных работ: ${MoneyFormatter.format(bodyWorksCents)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (bodyWorksCents == 0L) {
                            Text(
                                "Сначала внесите кузовные работы в блок «Работы по заказу»",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        OutlinedTextField(
                            value = rateInput,
                            onValueChange = { rateInput = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Процент, %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = rateInput,
                            onValueChange = { rateInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Цена за деталь, ₽") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = panelsCount,
                            onValueChange = { panelsCount = it.filter { c -> c.isDigit() } },
                            label = { Text("Кол-во деталей") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text(
                        "К начислению: ${MoneyFormatter.format(preview)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = canSubmit,
                    onClick = {
                        viewModel.addAccrual(employee = emp, rate = rate, panels = panels)
                        employeeToAccrue = null
                    }
                ) { Text("Начислить") }
            },
            dismissButton = {
                TextButton(onClick = { employeeToAccrue = null }) { Text("Отмена") }
            }
        )
    }

    // --- НИЖНИЙ ЛИСТ ---
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
                postPickEmployee = null
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                when (bottomSheetMode) {

                    "POST" -> {
                        val picked = postPickEmployee
                        if (picked == null) {
                            Text(
                                "Кто работает над машиной?",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Вид работ определится по специализации мастера.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )
                            if (state.employees.isEmpty()) {
                                Text(
                                    "Нет мастеров. Добавьте их в Настройках → Сотрудники цеха.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            state.employees.forEach { emp ->
                                val role = EmployeeSpecialty.roleOf(
                                    emp.position, emp.compensationType
                                )
                                val known = role != WorkRole.GENERAL
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            emp.fullName,
                                            color = if (known) MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.error
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            if (known) role.displayName()
                                            else "Специализация не задана",
                                            color = if (known) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    leadingContent = {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    modifier = Modifier.clickable(enabled = known) {
                                        postPickEmployee = emp
                                    }
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            }
                        } else {
                            val role = EmployeeSpecialty.roleOf(
                                picked.position, picked.compensationType
                            )
                            Text(
                                "Куда ставим машину?",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${picked.fullName} · ${role.displayName()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )
                            val compatible = state.posts.filter {
                                it.isActive && EmployeeSpecialty.canWorkOn(role, it.type)
                            }
                            if (compatible.isEmpty()) {
                                Text(
                                    EmployeeSpecialty.incompatibilityReason(
                                        role,
                                        if (role == WorkRole.PAINT) PostType.PREP else PostType.PAINT
                                    ),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            compatible.forEach { post ->
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
                                            if (post.type == PostType.PAINT) "Покрасочная камера"
                                            else "Универсальный пост",
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
                                        postPickEmployee = null
                                        viewModel.assignMasterToPost(picked, post)
                                    }
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            }
                            TextButton(onClick = { postPickEmployee = null }) {
                                Text("← Выбрать другого мастера")
                            }
                        }
                    }

                    else -> {
                        Text(
                            if (bottomSheetMode == "EMPLOYEE_ACCRUAL") "Начислить ЗП мастеру"
                            else "Добавить мастера на заказ",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            "Специализация подставится из настроек.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        if (state.employees.isEmpty()) {
                            Text(
                                "Нет мастеров. Добавьте их в Настройках → Сотрудники цеха.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        state.employees.forEach { emp ->
                            val role = EmployeeSpecialty.roleOf(
                                emp.position, emp.compensationType
                            )
                            val known = role != WorkRole.GENERAL
                            val already = state.assignments.any { it.employeeId == emp.id }
                            val blocked = !known ||
                                    (bottomSheetMode == "ASSIGN_EMPLOYEE" && already)
                            ListItem(
                                headlineContent = {
                                    Text(
                                        emp.fullName,
                                        color = if (known) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.error
                                    )
                                },
                                supportingContent = {
                                    Column {
                                        Text(
                                            if (known) role.displayName()
                                            else "Специализация не задана",
                                            color = if (known) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (bottomSheetMode == "ASSIGN_EMPLOYEE" && already) {
                                            Text(
                                                "Уже назначен на этот заказ",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                modifier = Modifier.clickable(enabled = !blocked) {
                                    showBottomSheet = false
                                    if (bottomSheetMode == "EMPLOYEE_ACCRUAL") {
                                        employeeToAccrue = emp
                                    } else {
                                        viewModel.assignEmployee(emp)
                                    }
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