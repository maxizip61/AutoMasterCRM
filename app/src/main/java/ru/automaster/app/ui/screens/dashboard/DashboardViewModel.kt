package ru.automaster.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import ru.automaster.app.data.local.dao.*
import ru.automaster.app.data.local.entity.*
import ru.automaster.app.domain.model.PaymentType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class PostState(
    val post: PostEntity,
    val activeStage: OrderStageEntity? = null
)

data class DashboardUiState(
    val posts: List<PostState> = emptyList(),
    val inWorkOrders: List<OrderEntity> = emptyList(),
    val waitingOrders: List<OrderEntity> = emptyList(),
    val dailyCashInCents: Long = 0L,
    val dailyCashOutCents: Long = 0L,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    postDao: PostDao,
    orderStageDao: OrderStageDao,
    orderDao: OrderDao,
    private val paymentDao: PaymentDao,
    private val expenseDao: ExpenseDao
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        postDao.getAll(),
        orderStageDao.getActiveStages(),
        orderDao.getAll(),
        getDailyCashFlowFlow() // Наш новый Flow для кассы
    ) { posts, activeStages, allOrders, cashFlow ->

        val postStates = posts.map { post ->
            val stage = activeStages.find { it.postId == post.id }
            PostState(post = post, activeStage = stage)
        }

        val inWork = allOrders.filter { it.status.name in listOf("BODY", "PREP", "PAINT", "ISSUE") }
        val waiting = allOrders.filter { it.status.name in listOf("ACCEPTED", "WAITING") }

        DashboardUiState(
            posts = postStates,
            inWorkOrders = inWork,
            waitingOrders = waiting,
            dailyCashInCents = cashFlow.first,
            dailyCashOutCents = cashFlow.second,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    private fun getDailyCashFlowFlow(): Flow<Pair<Long, Long>> = flow {
        val zone = ZoneId.systemDefault()
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val endOfDay = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant()

        // Получаем оплаты и расходы за сегодня
        val todayPayments = paymentDao.getPaymentsInRange(startOfDay, endOfDay)
        val todayExpenses = expenseDao.getExpensesInRange(startOfDay, endOfDay)

        // Считаем приход (Оплаты минус возвраты)
        val income = todayPayments.sumOf { if (it.type == PaymentType.REFUND) -it.amountCents else it.amountCents }
        // Считаем расход
        val outcome = todayExpenses.sumOf { it.amountCents }

        emit(Pair(income, outcome))
    }
}