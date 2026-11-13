package ru.automaster.app.ui.screens.order_detail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val photos: List<PhotoEntity> = emptyList(),
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
    private val expenseCategoryDao: ExpenseCategoryDao, // <-- ДОБАВЛЕНО
    private val employeeDao: EmployeeDao,
    private val accrualDao: OrderEmployeeAccrualDao,
    private val photoDao: PhotoDao,
    private val photoFileManager: PhotoFileManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: Long = checkNotNull(savedStateHandle["orderId"])

    private var currentPhotoUri: Uri? = null
    private var currentPhotoFile: File? = null

    val uiState: StateFlow<OrderDetailsUiState> = combine(
        orderDao.getByIdFlow(orderId),
        postDao.getAll(),
        paymentDao.getByOrder(orderId),
        expenseDao.getByOrder(orderId),
        employeeDao.getActive(),
        accrualDao.getByOrder(orderId),
        photoDao.getByOrder(orderId)
    ) { args ->
        val order = args[0] as OrderEntity?
        @Suppress("UNCHECKED_CAST")
        val posts = args[1] as List<PostEntity>
        @Suppress("UNCHECKED_CAST")
        val payments = args[2] as List<PaymentEntity>
        @Suppress("UNCHECKED_CAST")
        val expenses = args[3] as List<ExpenseEntity>
        @Suppress("UNCHECKED_CAST")
        val employees = args[4] as List<EmployeeEntity>
        @Suppress("UNCHECKED_CAST")
        val accruals = args[5] as List<OrderEmployeeAccrualEntity>
        @Suppress("UNCHECKED_CAST")
        val photos = args[6] as List<PhotoEntity>

        if (order != null) {
            val client = clientDao.getById(order.clientId)
            val car = carDao.getById(order.carId)
            OrderDetailsUiState(
                order = order, client = client, car = car, posts = posts,
                payments = payments, expenses = expenses, employees = employees,
                accruals = accruals, photos = photos, isLoading = false
            )
        } else {
            OrderDetailsUiState(isLoading = false)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OrderDetailsUiState()
    )

    fun assignToPost(post: PostEntity) {
        viewModelScope.launch {
            val order = uiState.value.order ?: return@launch
            val newPhase = if (post.type == PostType.PAINT) StagePhase.PAINT else StagePhase.BODY
            val newStatus = if (newPhase == StagePhase.PAINT) OrderStatus.PAINT else OrderStatus.BODY

            orderStageDao.insert(
                OrderStageEntity(
                    orderId = order.id, phase = newPhase, postId = post.id, sequence = 1, isCurrent = true,
                    stageStatus = StageStatus.IN_PROGRESS, plannedStart = Instant.now(),
                    plannedEnd = Instant.now().plusSeconds(3600), actualStart = Instant.now()
                )
            )
            orderDao.update(order.copy(status = newStatus))
            statusHistoryDao.insert(StatusHistoryEntity(orderId = order.id, oldStatus = order.status, newStatus = newStatus, changedAt = Instant.now(), comment = "Назначен на пост ${post.name.name}"))
        }
    }

    fun completeCurrentStage() {
        viewModelScope.launch {
            val order = uiState.value.order ?: return@launch
            val activeStage = orderStageDao.getActiveStageForOrder(order.id)
            if (activeStage != null) {
                orderStageDao.update(activeStage.copy(stageStatus = StageStatus.COMPLETED, actualEnd = Instant.now()))
                val newStatus = OrderStatus.ISSUE
                orderDao.update(order.copy(status = newStatus))
                statusHistoryDao.insert(StatusHistoryEntity(orderId = order.id, oldStatus = order.status, newStatus = newStatus, changedAt = Instant.now(), comment = "Завершен этап работы"))
            }
        }
    }

    fun closeOrder() {
        viewModelScope.launch {
            val order = uiState.value.order ?: return@launch
            val now = Instant.now()
            orderDao.update(order.copy(status = OrderStatus.DONE, actualDeliveryDate = now, closedAt = now))
            statusHistoryDao.insert(StatusHistoryEntity(orderId = order.id, oldStatus = order.status, newStatus = OrderStatus.DONE, changedAt = now, comment = "Автомобиль выдан клиенту"))
        }
    }

    fun addPayment(amountRub: Long) {
        viewModelScope.launch {
            val order = uiState.value.order ?: return@launch
            paymentDao.insert(
                PaymentEntity(
                    orderId = order.id, type = PaymentType.PAYMENT, amountCents = amountRub * 100,
                    date = Instant.now(), method = PaymentMethod.CASH
                )
            )
        }
    }

    fun addExpense(amountRub: Long, comment: String) {
        viewModelScope.launch {
            val order = uiState.value.order ?: return@launch
            // БЕЗОПАСНОЕ ПОЛУЧЕНИЕ КАТЕГОРИИ:
            val categoryId = expenseCategoryDao.getFirstCategoryId() ?: 1L

            expenseDao.insert(
                ExpenseEntity(
                    amountCents = amountRub * 100, date = Instant.now(), categoryId = categoryId,
                    paymentMethod = PaymentMethod.CASH, comment = comment, orderId = order.id
                )
            )
        }
    }

    fun addAccrual(employee: EmployeeEntity, panelsCount: Int = 1) {
        viewModelScope.launch {
            val order = uiState.value.order ?: return@launch

            val finalAmountCents = if (employee.compensationType == "PERCENT") {
                (order.agreedEstimateCents * employee.rateValue) / 100
            } else {
                (employee.rateValue * panelsCount) * 100
            }

            accrualDao.insert(
                OrderEmployeeAccrualEntity(
                    orderId = order.id,
                    employeeId = employee.id,
                    type = if (employee.compensationType == "PERCENT") AccrualType.PERCENT else AccrualType.PANEL,
                    amountCents = finalAmountCents,
                    closedAt = Instant.now(),
                    idempotencyKey = UUID.randomUUID().toString()
                )
            )
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
        if (isSuccess && currentPhotoFile != null) {
            viewModelScope.launch {
                val relativePath = currentPhotoFile!!.absolutePath.substringAfter("files/")
                photoDao.insert(
                    PhotoEntity(orderId = orderId, type = type, filePath = relativePath, createdAt = Instant.now())
                )
            }
        }
        currentPhotoFile = null
        currentPhotoUri = null
    }
}