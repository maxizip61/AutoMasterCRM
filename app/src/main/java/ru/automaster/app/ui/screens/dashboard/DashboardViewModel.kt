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

data class PostState(val post: PostEntity, val activeStage: OrderStageEntity? = null)
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
        getDailyCashFlowFlow() // Теперь это Live Flow
    ) { posts, activeStages, allOrders, cashFlow ->
        val postStates = posts.map { post ->
            PostState(post, activeStages.find { it.postId == post.id })
        }
        val inWork = allOrders.filter { it.status.name in listOf("BODY", "PREP", "PAINT", "ISSUE") }
        val waiting = allOrders.filter { it.status.name in listOf("ACCEPTED", "WAITING") }

        DashboardUiState(
            posts = postStates, inWorkOrders = inWork, waitingOrders = waiting,
            dailyCashInCents = cashFlow.first, dailyCashOutCents = cashFlow.second, isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    // ФИКС: Теперь использует Flow из DAO вместо одноразового flow {}
    private fun getDailyCashFlowFlow(): Flow<Pair<Long, Long>> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val end = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant()

        val paymentsFlow = paymentDao.getPaymentsInRangeFlow(start, end)
        val expensesFlow = expenseDao.getExpensesInRangeFlow(start, end)

        return combine(paymentsFlow, expensesFlow) { payments, expenses ->
            val income = payments.sumOf { if (it.type == PaymentType.REFUND) -it.amountCents else it.amountCents }
            val outcome = expenses.sumOf { it.amountCents }
            income to outcome
        }
    }
}