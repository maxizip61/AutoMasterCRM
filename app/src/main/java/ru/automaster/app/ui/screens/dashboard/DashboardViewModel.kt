package ru.automaster.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import ru.automaster.app.data.local.dao.*
import ru.automaster.app.data.local.entity.*
import ru.automaster.app.domain.model.PaymentType
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

@OptIn(ExperimentalCoroutinesApi::class)
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
        getDailyCashFlowFlow()
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

    // Тикающий поток текущей даты (обновляется каждую минуту)
    private fun getCurrentDateFlow(): Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now())
            delay(60_000L)
        }
    }

    private fun getDailyCashFlowFlow(): Flow<Pair<Long, Long>> {
        return getCurrentDateFlow().flatMapLatest { date ->
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant()

            val paymentsFlow = paymentDao.getPaymentsInRangeFlow(start, end)
            val expensesFlow = expenseDao.getExpensesInRangeFlow(start, end)

            combine(paymentsFlow, expensesFlow) { payments, expenses ->
                val income = payments.sumOf { if (it.type == PaymentType.REFUND) -it.amountCents else it.amountCents }
                val outcome = expenses.sumOf { it.amountCents }
                income to outcome
            }
        }
    }
}