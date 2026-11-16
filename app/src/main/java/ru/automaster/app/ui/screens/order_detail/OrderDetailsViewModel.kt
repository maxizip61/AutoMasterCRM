package ru.automaster.app.ui.screens.order_detail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.automaster.app.data.local.AppDatabase
import ru.automaster.app.data.local.dao.*
import ru.automaster.app.data.local.entity.*
import ru.automaster.app.data.storage.PhotoFileManager
import ru.automaster.app.domain.model.*
import java.io.File
import java.time.Instant
import java.util.UUID
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
        postDao.getAll(),
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

    fun assignToPost(post: PostEntity) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                runCatching {
                    db.withTransaction {
                        val order = uiState.value.order ?: return@withTransaction
                        val now = Instant.now()
                        val newPhase = if (post.type == PostType.PAINT) StagePhase.PAINT else StagePhase.BODY
                        val newStatus = if (newPhase == StagePhase.PAINT) OrderStatus.PAINT else OrderStatus.BODY

                        val activeStage = orderStageDao.getActiveStageForOrder(order.id)
                        if (activeStage != null) {
                            orderStageDao.update(activeStage.copy(stageStatus = StageStatus.COMPLETED, isCurrent = false, actualEnd = now))
                        }
                        orderStageDao.clearCurrentStages(order.id)

                        orderStageDao.insert(
                            OrderStageEntity(
                                orderId = order.id, phase = newPhase, postId = post.id,
                                sequence = 1, isCurrent = true, stageStatus = StageStatus.IN_PROGRESS,
                                plannedStart = now, plannedEnd = now.plusSeconds(3600), actualStart = now
                            )
                        )
                        orderDao.update(order.copy(status = newStatus))
                        statusHistoryDao.insert(
                            StatusHistoryEntity(
                                orderId = order.id, oldStatus = order.status, newStatus = newStatus,
                                changedAt = now, comment = "Назначен на пост ${post.displayName.ifEmpty { post.name.name }}"
                            )
                        )
                    }
                }.onFailure { e -> _message.value = "Ошибка назначения: ${e.message}" }
            }
        }
    }

    fun completeCurrentStage() {
        viewModelScope.launch {
            withContext(NonCancellable) {
                runCatching {
                    db.withTransaction {
                        val order = uiState.value.order ?: return@withTransaction
                        val activeStage = orderStageDao.getActiveStageForOrder(order.id)
                        if (activeStage != null) {
                            val now = Instant.now()
                            orderStageDao.update(activeStage.copy(stageStatus = StageStatus.COMPLETED, isCurrent = false, actualEnd = now))

                            val newStatus = if (activeStage.phase == StagePhase.PAINT) OrderStatus.ISSUE else OrderStatus.WAITING

                            orderDao.update(order.copy(status = newStatus))
                            statusHistoryDao.insert(
                                StatusHistoryEntity(
                                    orderId = order.id, oldStatus = order.status, newStatus = newStatus,
                                    changedAt = now,
                                    comment = if (newStatus == OrderStatus.ISSUE) "Завершен этап покраски. Готов к выдаче" else "Завершен этап. Ожидает следующего"
                                )
                            )
                        }
                    }
                }.onFailure { e -> _message.value = "Ошибка завершения: ${e.message}" }
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
                        orderDao.update(order.copy(status = OrderStatus.DONE, actualDeliveryDate = now, closedAt = now))
                        statusHistoryDao.insert(
                            StatusHistoryEntity(
                                orderId = order.id, oldStatus = order.status, newStatus = OrderStatus.DONE,
                                changedAt = now, comment = "Автомобиль выдан клиенту"
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
                        amountCents = amountRub * 100, date = Instant.now(), method = PaymentMethod.CASH
                    )
                )
            }.onFailure { e -> _message.value = "Ошибка оплаты: ${e.message}" }
        }
    }

    fun addExpense(amountRub: Long, comment: String) {
        viewModelScope.launch {
            val order = uiState.value.order ?: return@launch
            runCatching {
                var categoryId = expenseCategoryDao.getFirstCategoryId()
                if (categoryId == null) {
                    categoryId = expenseCategoryDao.insert(ExpenseCategoryEntity(name = "Прочее", defaultInclude = true))
                }
                expenseDao.insert(
                    ExpenseEntity(
                        amountCents = amountRub * 100, date = Instant.now(), categoryId = categoryId,
                        paymentMethod = PaymentMethod.CASH, comment = comment.ifBlank { null }, orderId = order.id
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
                    _message.value = "${employee.fullName} уже назначен на роль ${role.displayName()}"
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
     * Начисление ЗП:
     * - BODY (кузовщик): процент (rateValue) от суммы BODY-работ
     * - PREP (подготовщик): фикс (rateValue) × количество деталей
     * - PAINT (маляр): фикс (rateValue) × количество деталей
     */
    fun addAccrual(employee: EmployeeEntity, role: WorkRole, panelsCount: Int = 1) {
        viewModelScope.launch {
            val order = uiState.value.order ?: return@launch
            runCatching {
                db.withTransaction { // <-- ТРАНЗАКЦИЯ защищает от race condition
                    val finalAmountCents: Long = when (role) {
                        WorkRole.BODY -> {
                            val bodyWorksCents = orderWorkDao.getTotalByType(order.id, WorkType.BODY) ?: 0L
                            (bodyWorksCents * employee.rateValue) / 100
                        }
                        WorkRole.PREP -> employee.rateValue * 100 * panelsCount
                        WorkRole.PAINT -> employee.rateValue * 100 * panelsCount
                        WorkRole.GENERAL -> employee.rateValue * 100 * panelsCount
                    }

                    if (finalAmountCents <= 0L) {
                        throw IllegalStateException("Сумма начисления 0. Проверьте работы и ставку мастера.")
                    }

                    val accrualType = if (role == WorkRole.BODY) AccrualType.PERCENT else AccrualType.PANEL

                    accrualDao.insert(
                        OrderEmployeeAccrualEntity(
                            orderId = order.id, employeeId = employee.id,
                            role = role,
                            type = accrualType,
                            percentBps = if (role == WorkRole.BODY) employee.rateValue.toInt() else null,
                            panelCents = if (role != WorkRole.BODY) employee.rateValue else null,
                            panelsCount = if (role != WorkRole.BODY) panelsCount else null,
                            amountCents = finalAmountCents, closedAt = Instant.now(),
                            idempotencyKey = UUID.randomUUID().toString()
                        )
                    )
                }
            }.onFailure { e -> _message.value = "Ошибка начисления: ${e.message}" }
        }
    }
    fun addWork(name: String, type: WorkType, costRub: Long) {
        viewModelScope.launch {
            val order = uiState.value.order ?: return@launch
            runCatching {
                orderWorkDao.insert(
                    OrderWorkEntity(
                        orderId = order.id, name = name, type = type,
                        costCents = costRub * 100, createdAt = Instant.now()
                    )
                )
            }.onFailure { e -> _message.value = "Ошибка добавления работы: ${e.message}" }
        }
    }

    fun deleteWork(work: OrderWorkEntity) {
        viewModelScope.launch {
            runCatching {
                val order = uiState.value.order ?: return@runCatching
                // Проверяем, есть ли уже начисления по заказу
                val existingAccruals = accrualDao.getByOrder(order.id).first()
                if (existingAccruals.isNotEmpty()) {
                    _message.value = "Нельзя удалить работу: по заказу уже есть начисления ЗП. Сначала отмените начисления."
                    return@runCatching
                }
                orderWorkDao.delete(work)
            }.onFailure { e -> _message.value = "Ошибка удаления: ${e.message}" }
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
                    photoDao.insert(PhotoEntity(orderId = orderId, type = type, filePath = relativePath, createdAt = Instant.now()))
                }.onFailure { e -> _message.value = "Ошибка фото: ${e.message}" }
            }
        }
        currentPhotoFile = null
        currentPhotoUri = null
    }
}