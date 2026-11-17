package ru.automaster.app.ui.screens.order_detail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.automaster.app.data.local.AppDatabase
import ru.automaster.app.data.local.dao.CarDao
import ru.automaster.app.data.local.dao.ClientDao
import ru.automaster.app.data.local.dao.EmployeeDao
import ru.automaster.app.data.local.dao.ExpenseCategoryDao
import ru.automaster.app.data.local.dao.ExpenseDao
import ru.automaster.app.data.local.dao.OrderDao
import ru.automaster.app.data.local.dao.OrderEmployeeAccrualDao
import ru.automaster.app.data.local.dao.OrderEmployeeAssignmentDao
import ru.automaster.app.data.local.dao.OrderStageDao
import ru.automaster.app.data.local.dao.OrderWorkDao
import ru.automaster.app.data.local.dao.PaymentDao
import ru.automaster.app.data.local.dao.PhotoDao
import ru.automaster.app.data.local.dao.PostDao
import ru.automaster.app.data.local.dao.StatusHistoryDao
import ru.automaster.app.data.local.entity.CarEntity
import ru.automaster.app.data.local.entity.ClientEntity
import ru.automaster.app.data.local.entity.EmployeeEntity
import ru.automaster.app.data.local.entity.ExpenseCategoryEntity
import ru.automaster.app.data.local.entity.ExpenseEntity
import ru.automaster.app.data.local.entity.OrderEmployeeAccrualEntity
import ru.automaster.app.data.local.entity.OrderEmployeeAssignmentEntity
import ru.automaster.app.data.local.entity.OrderEntity
import ru.automaster.app.data.local.entity.OrderStageEntity
import ru.automaster.app.data.local.entity.OrderWorkEntity
import ru.automaster.app.data.local.entity.PaymentEntity
import ru.automaster.app.data.local.entity.PhotoEntity
import ru.automaster.app.data.local.entity.PostEntity
import ru.automaster.app.data.local.entity.StatusHistoryEntity
import ru.automaster.app.data.storage.PhotoFileManager
import ru.automaster.app.domain.model.AccrualType
import ru.automaster.app.domain.model.OrderStatus
import ru.automaster.app.domain.model.PaymentMethod
import ru.automaster.app.domain.model.PaymentType
import ru.automaster.app.domain.model.PhotoType
import ru.automaster.app.domain.model.PostType
import ru.automaster.app.domain.model.StageDecision
import ru.automaster.app.domain.model.StagePhase
import ru.automaster.app.domain.model.StageStatus
import ru.automaster.app.domain.model.WorkRole
import ru.automaster.app.domain.model.WorkType
import ru.automaster.app.domain.model.displayName
import java.io.File
import java.time.Instant
import javax.inject.Inject

data class OrderDetailsUiState(
    val order: OrderEntity? = null,
    val client: ClientEntity? = null,
    val car: CarEntity? = null,
    val posts: List<PostEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val employees: List<EmployeeEntity> = emptyList(),
    val accruals: List<OrderEmployeeAccrualEntity> = emptyList(),
    val assignments: List<OrderEmployeeAssignmentEntity> = emptyList(),
    val photos: List<PhotoEntity> = emptyList(),
    val works: List<OrderWorkEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class OrderDetailsViewModel @Inject constructor(
    private val orderDao: OrderDao,
    private val clientDao: ClientDao,
    private val carDao: CarDao,
    private val postDao: PostDao,
    private val orderStageDao: OrderStageDao,
    private val statusHistoryDao: StatusHistoryDao,
    private val paymentDao: PaymentDao,
    private val expenseDao: ExpenseDao,
    private val expenseCategoryDao: ExpenseCategoryDao,
    private val employeeDao: EmployeeDao,
    private val accrualDao: OrderEmployeeAccrualDao,
    private val assignmentDao: OrderEmployeeAssignmentDao,
    private val photoDao: PhotoDao,
    private val orderWorkDao: OrderWorkDao,
    private val photoFileManager: PhotoFileManager,
    private val db: AppDatabase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: Long = checkNotNull(savedStateHandle["orderId"])
    private var currentPhotoUri: Uri? = null
    private var currentPhotoFile: File? = null

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    val uiState: StateFlow<OrderDetailsUiState> = combine(
        orderDao.getByIdFlow(orderId),
        postDao.observeActive(),
        paymentDao.getByOrder(orderId),
        expenseDao.getByOrder(orderId),
        employeeDao.getActive(),
        accrualDao.getByOrder(orderId),
        photoDao.getByOrder(orderId),
        assignmentDao.getByOrder(orderId),
        orderWorkDao.getByOrder(orderId)
    ) { args ->
        val order = args[0] as OrderEntity?
        @Suppress("UNCHECKED_CAST") val posts = args[1] as List<PostEntity>
        @Suppress("UNCHECKED_CAST") val payments = args[2] as List<PaymentEntity>
        @Suppress("UNCHECKED_CAST") val expenses = args[3] as List<ExpenseEntity>
        @Suppress("UNCHECKED_CAST") val employees = args[4] as List<EmployeeEntity>
        @Suppress("UNCHECKED_CAST") val accruals = args[5] as List<OrderEmployeeAccrualEntity>
        @Suppress("UNCHECKED_CAST") val photos = args[6] as List<PhotoEntity>
        @Suppress("UNCHECKED_CAST") val assignments = args[7] as List<OrderEmployeeAssignmentEntity>
        @Suppress("UNCHECKED_CAST") val works = args[8] as List<OrderWorkEntity>

        if (order != null) {
            val client = clientDao.getById(order.clientId)
            val car = carDao.getById(order.carId)
            OrderDetailsUiState(
                order = order, client = client, car = car, posts = posts,
                payments = payments, expenses = expenses, employees = employees,
                accruals = accruals, assignments = assignments, photos = photos,
                works = works, isLoading = false
            )
        } else OrderDetailsUiState(isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OrderDetailsUiState()
    )

    fun assignToPost(post: PostEntity, phase: StagePhase) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                runCatching {
                    when (post.type) {
                        PostType.PAINT -> if (phase != StagePhase.PAINT) {
                            _message.value = "На покрасочную камеру — только покраска"
                            return@runCatching
                        }
                        PostType.PREP -> if (phase != StagePhase.BODY &&
                            phase != StagePhase.PREP
                        ) {
                            _message.value = "На подготовительный пост — только кузов/подготовка"
                            return@runCatching
                        }
                    }

                    val order = uiState.value.order ?: return@runCatching
                    val now = Instant.now()
                    val newStatus = when (phase) {
                        StagePhase.BODY -> OrderStatus.BODY
                        StagePhase.PREP -> OrderStatus.PREP
                        StagePhase.PAINT -> OrderStatus.PAINT
                        StagePhase.ISSUE -> return@runCatching
                    }

                    val newStage = OrderStageEntity(
                        orderId = order.id,
                        phase = phase,
                        postId = post.id,
                        sequence = 1,
                        isCurrent = true,
                        stageStatus = StageStatus.IN_PROGRESS,
                        plannedStart = now,
                        plannedEnd = now.plusSeconds(3600),
                        actualStart = now
                    )

                    db.withTransaction {
                        orderStageDao.reassign(order.id, now, newStage)
                        orderDao.update(order.copy(status = newStatus))
                        statusHistoryDao.insert(
                            StatusHistoryEntity(
                                orderId = order.id,
                                oldStatus = order.status,
                                newStatus = newStatus,
                                changedAt = now,
                                comment = "Назначен на пост " +
                                        "${post.displayName.ifEmpty { post.name.name }} " +
                                        "(${phase.displayName()})"
                            )
                        )
                    }
                }.onFailure { e ->
                    _message.value = "Ошибка назначения: ${e.message}"
                }
            }
        }
    }

    fun completeCurrentStage(decision: StageDecision) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                runCatching {
                    db.withTransaction {
                        val order = uiState.value.order ?: return@withTransaction
                        val activeStage = orderStageDao.getActiveStageForOrder(order.id)
                            ?: throw IllegalStateException("Нет активного этапа")
                        val now = Instant.now()

                        orderStageDao.update(
                            activeStage.copy(
                                stageStatus = StageStatus.COMPLETED,
                                isCurrent = false,
                                actualEnd = now
                            )
                        )
                        orderDao.update(order.copy(status = OrderStatus.WAITING))
                        statusHistoryDao.insert(
                            StatusHistoryEntity(
                                orderId = order.id,
                                oldStatus = order.status,
                                newStatus = OrderStatus.WAITING,
                                changedAt = now,
                                comment = "Этап завершён (${activeStage.phase.displayName()})"
                            )
                        )

                        if (decision == StageDecision.READY_FOR_ISSUE) {
                            orderDao.update(order.copy(status = OrderStatus.ISSUE))
                            statusHistoryDao.insert(
                                StatusHistoryEntity(
                                    orderId = order.id,
                                    oldStatus = OrderStatus.WAITING,
                                    newStatus = OrderStatus.ISSUE,
                                    changedAt = now,
                                    comment = "Владелец подтвердил готовность к выдаче"
                                )
                            )
                        }
                    }
                }.onFailure { e ->
                    _message.value = "Ошибка завершения этапа: ${e.message}"
                }
            }
        }
    }

    fun markReadyForIssue() {
        viewModelScope.launch {
            withContext(NonCancellable) {
                runCatching {
                    db.withTransaction {
                        val order = uiState.value.order ?: return@withTransaction
                        if (order.status != OrderStatus.WAITING) {
                            throw IllegalStateException("Доступно только из статуса «Ожидает»")
                        }
                        val now = Instant.now()
                        orderDao.update(order.copy(status = OrderStatus.ISSUE))
                        statusHistoryDao.insert(
                            StatusHistoryEntity(
                                orderId = order.id,
                                oldStatus = OrderStatus.WAITING,
                                newStatus = OrderStatus.ISSUE,
                                changedAt = now,
                                comment = "Владелец подтвердил готовность к выдаче"
                            )
                        )
                    }
                }.onFailure { e -> _message.value = "Ошибка: ${e.message}" }
            }
        }
    }

    fun closeOrder() {
        viewModelScope.launch {
            withContext(NonCancellable) {
                runCatching {
                    db.withTransaction {
                        val order = uiState.value.order ?: return@withTransaction
                        val now = Instant.now()
                        orderDao.update(
                            order.copy(
                                status = OrderStatus.DONE,
                                actualDeliveryDate = now,
                                closedAt = now
                            )
                        )
                        statusHistoryDao.insert(
                            StatusHistoryEntity(
                                orderId = order.id,
                                oldStatus = order.status,
                                newStatus = OrderStatus.DONE,
                                changedAt = now,
                                comment = "Автомобиль выдан клиенту"
                            )
                        )
                    }
                }.onFailure { e -> _message.value = "Ошибка выдачи: ${e.message}" }
            }
        }
    }

    fun addPayment(amountRub: Long) {
        viewModelScope.launch {
            val order = uiState.value.order ?: return@launch
            runCatching {
                paymentDao.insert(
                    PaymentEntity(
                        orderId = order.id, type = PaymentType.PAYMENT,
                        amountCents = amountRub * 100, date = Instant.now(),
                        method = PaymentMethod.CASH
                    )
                )
            }.onFailure { e -> _message.value = "Ошибка оплаты: ${e.message}" }
        }
    }

    fun addExpense(amountRub: Long, comment: String) {
        viewModelScope.launch {
            val order = uiState.value.order ?: return@launch
            runCatching {
                val categoryId: Long = expenseCategoryDao.getFirstCategoryId()
                    ?: expenseCategoryDao.insert(
                        ExpenseCategoryEntity(name = "Прочее", defaultInclude = true)
                    )
                expenseDao.insert(
                    ExpenseEntity(
                        amountCents = amountRub * 100, date = Instant.now(),
                        categoryId = categoryId,
                        paymentMethod = PaymentMethod.CASH,
                        comment = comment.ifBlank { null },
                        orderId = order.id
                    )
                )
            }.onFailure { e -> _message.value = "Ошибка расхода: ${e.message}" }
        }
    }

    fun assignEmployee(employee: EmployeeEntity, role: WorkRole) {
        viewModelScope.launch {
            runCatching {
                val order = uiState.value.order ?: return@runCatching
                val existing = assignmentDao.getByOrder(order.id).first()
                if (existing.any { it.employeeId == employee.id && it.role == role }) {
                    _message.value =
                        "${employee.fullName} уже назначен на роль ${role.displayName()}"
                    return@runCatching
                }
                assignmentDao.insert(
                    OrderEmployeeAssignmentEntity(
                        orderId = order.id,
                        employeeId = employee.id,
                        role = role,
                        createdAt = Instant.now()
                    )
                )
            }.onFailure { e -> _message.value = "Ошибка назначения: ${e.message}" }
        }
    }

    fun removeAssignment(assignment: OrderEmployeeAssignmentEntity) {
        viewModelScope.launch {
            runCatching { assignmentDao.delete(assignment) }
                .onFailure { e -> _message.value = "Ошибка удаления: ${e.message}" }
        }
    }

    /**
     * Начисление ЗП по BR-1 / BR-2.
     * @param rate BODY — процент (30); PREP/PAINT — рубли за деталь (1500).
     * @param panels количество деталей, для BODY игнорируется.
     */
    fun addAccrual(
        employee: EmployeeEntity,
        role: WorkRole,
        rate: Long,
        panels: Int = 1
    ) {
        viewModelScope.launch {
            val order = uiState.value.order ?: return@launch
            runCatching {
                if (rate <= 0L) {
                    throw IllegalArgumentException("Ставка должна быть больше нуля")
                }
                if (role != WorkRole.BODY && panels < 1) {
                    throw IllegalArgumentException("Количество деталей должно быть ≥ 1")
                }

                db.withTransaction {
                    val activeStage = orderStageDao.getActiveStageForOrder(order.id)
                        ?: throw IllegalStateException(
                            "Нет активного этапа. Назначьте заказ на пост перед начислением."
                        )
                    val stageId = activeStage.id

                    val tuple = when (role) {
                        WorkRole.BODY -> {
                            val base = orderWorkDao
                                .getTotalByType(order.id, WorkType.BODY) ?: 0L
                            if (base <= 0L) {
                                throw IllegalStateException(
                                    "Сначала внесите кузовные работы в блок «Работы по заказу»"
                                )
                            }
                            AccrualTuple(base * rate / 100, (rate * 100).toInt(), null, null)
                        }
                        WorkRole.PREP, WorkRole.PAINT, WorkRole.GENERAL -> {
                            AccrualTuple(rate * panels * 100, null, rate * 100, panels)
                        }
                    }

                    val key = "$orderId:${employee.id}:$stageId:${role.name}"

                    val insertId = accrualDao.insert(
                        OrderEmployeeAccrualEntity(
                            orderId = order.id,
                            employeeId = employee.id,
                            role = role,
                            type = if (role == WorkRole.BODY)
                                AccrualType.PERCENT else AccrualType.PANEL,
                            percentBps = tuple.percentBps,
                            panelCents = tuple.panelCents,
                            panelsCount = tuple.panelsCount,
                            amountCents = tuple.amountCents,
                            closedAt = Instant.now(),
                            idempotencyKey = key
                        )
                    )

                    if (insertId == -1L) {
                        throw IllegalStateException(
                            "По этому этапу уже начислено (${role.displayName()})"
                        )
                    }
                }
            }.onFailure { e ->
                _message.value = e.message ?: "Ошибка начисления"
            }
        }
    }

    private data class AccrualTuple(
        val amountCents: Long,
        val percentBps: Int?,
        val panelCents: Long?,
        val panelsCount: Int?
    )

    fun addWork(name: String, type: WorkType, costRub: Long) {
        viewModelScope.launch {
            val order = uiState.value.order ?: return@launch
            runCatching {
                orderWorkDao.insert(
                    OrderWorkEntity(
                        orderId = order.id,
                        name = name,
                        type = type,
                        costCents = costRub * 100,
                        createdAt = Instant.now()
                    )
                )
            }.onFailure { e ->
                _message.value = "Ошибка добавления работы: ${e.message}"
            }
        }
    }

    fun deleteWork(work: OrderWorkEntity) {
        viewModelScope.launch {
            runCatching {
                val order = uiState.value.order ?: return@runCatching
                val relevantRole = when (work.type) {
                    WorkType.BODY -> WorkRole.BODY
                    WorkType.PREP -> WorkRole.PREP
                    WorkType.PAINT -> WorkRole.PAINT
                    WorkType.MATERIAL, WorkType.OTHER -> null
                }
                if (relevantRole != null) {
                    val accruals = accrualDao.getByOrder(order.id).first()
                    if (accruals.any { it.role == relevantRole }) {
                        _message.value =
                            "Нельзя удалить работу: по роли ${relevantRole.displayName()} " +
                                    "уже есть начисления. Сначала оформите сторно."
                        return@runCatching
                    }
                }
                orderWorkDao.delete(work)
            }.onFailure { e ->
                _message.value = "Ошибка удаления: ${e.message}"
            }
        }
    }

    fun prepareCameraUri(orderId: Long): Uri {
        val file = photoFileManager.createImageFile(orderId)
        currentPhotoFile = file
        val uri = photoFileManager.getUriForFile(file)
        currentPhotoUri = uri
        return uri
    }

    fun savePhotoToDb(orderId: Long, isSuccess: Boolean, type: PhotoType = PhotoType.BEFORE) {
        val file = currentPhotoFile
        if (isSuccess && file != null) {
            viewModelScope.launch {
                runCatching {
                    val relativePath = file.absolutePath.substringAfter("files/")
                    photoDao.insert(
                        PhotoEntity(
                            orderId = orderId, type = type,
                            filePath = relativePath, createdAt = Instant.now()
                        )
                    )
                }.onFailure { e ->
                    _message.value = "Ошибка фото: ${e.message}"
                }
            }
        }
        currentPhotoFile = null
        currentPhotoUri = null
    }
}