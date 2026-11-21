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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class PostState(val post: PostEntity, val activeStage: OrderStageEntity? = null)

enum class ProfitPeriod {
    DAY, WEEK, MONTH, YEAR;

    fun getRange(now: LocalDate, zone: ZoneId): Pair<Instant, Instant> {
        val start = when (this) {
            DAY -> now.atStartOfDay(zone).toInstant()
            WEEK -> now.minusDays(now.dayOfWeek.value.toLong() - 1).atStartOfDay(zone).toInstant()
            MONTH -> now.withDayOfMonth(1).atStartOfDay(zone).toInstant()
            YEAR -> now.withDayOfYear(1).atStartOfDay(zone).toInstant()
        }
        val end = now.plusDays(1).atStartOfDay(zone).toInstant()
        return start to end
    }

    fun displayName(): String = when (this) {
        DAY -> "День"
        WEEK -> "Неделя"
        MONTH -> "Месяц"
        YEAR -> "Год"
    }
}

data class ProfitBreakdown(
    val revenueCents: Long = 0L,
    val materialsCents: Long = 0L,
    val operatingCents: Long = 0L,
    val salaryPaymentsCents: Long = 0L,
    val taxCents: Long = 0L
) {
    val totalExpensesCents: Long
        get() = materialsCents + operatingCents + salaryPaymentsCents + taxCents

    val netProfitCents: Long
        get() = revenueCents - totalExpensesCents
}

data class DashboardUiState(
    val posts: List<PostState> = emptyList(),
    val inWorkOrders: List<OrderEntity> = emptyList(),
    val waitingOrders: List<OrderEntity> = emptyList(),
    val dailyCashInCents: Long = 0L,
    val dailyCashOutCents: Long = 0L,
    val profitPeriod: ProfitPeriod = ProfitPeriod.DAY,
    val profitBreakdown: ProfitBreakdown = ProfitBreakdown(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    postDao: PostDao,
    orderStageDao: OrderStageDao,
    orderDao: OrderDao,
    private val paymentDao: PaymentDao,
    private val expenseDao: ExpenseDao,
    private val salaryPaymentDao: SalaryPaymentDao,
    private val orderEmployeeAccrualDao: OrderEmployeeAccrualDao
) : ViewModel() {

    private val _profitPeriod = MutableStateFlow(ProfitPeriod.DAY)
    val profitPeriod: StateFlow<ProfitPeriod> = _profitPeriod.asStateFlow()

    fun setProfitPeriod(period: ProfitPeriod) {
        _profitPeriod.value = period
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        postDao.getAll(),
        orderStageDao.getActiveStages(),
        orderDao.getAll(),
        getDailyCashFlowFlow(),
        _profitPeriod.flatMapLatest { period -> calculateProfitFlow(period) }
    ) { posts, activeStages, allOrders, cashFlow, profit ->
        val postStates = posts.map { post ->
            PostState(post, activeStages.find { it.postId == post.id })
        }
        val inWork = allOrders.filter { it.status.name in listOf("BODY", "PREP", "PAINT", "ISSUE") }
        val waiting = allOrders.filter { it.status.name in listOf("ACCEPTED", "WAITING") }

        DashboardUiState(
            posts = postStates,
            inWorkOrders = inWork,
            waitingOrders = waiting,
            dailyCashInCents = cashFlow.first,
            dailyCashOutCents = cashFlow.second,
            profitPeriod = _profitPeriod.value,
            profitBreakdown = profit,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

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

    private fun calculateProfitFlow(period: ProfitPeriod): Flow<ProfitBreakdown> {
        return getCurrentDateFlow().flatMapLatest { date ->
            val zone = ZoneId.systemDefault()
            val (start, end) = period.getRange(date, zone)

            val paymentsFlow = paymentDao.getPaymentsInRangeFlow(start, end)
            val expensesFlow = expenseDao.getExpensesInRangeFlow(start, end)
            val salaryFlow = salaryPaymentDao.getAll()

            combine(paymentsFlow, expensesFlow, salaryFlow) { payments, expenses, salaries ->
                val revenue = payments
                    .filter { it.type == PaymentType.PAYMENT }
                    .sumOf { it.amountCents }

                val materials = expenses
                    .filter { it.orderId != null }
                    .sumOf { it.amountCents }

                val operating = expenses
                    .filter { it.orderId == null }
                    .sumOf { it.amountCents }

                val salaryPayments = salaries
                    .filter { it.date >= start && it.date < end }
                    .sumOf { it.amountCents }

                // УСН 6% от выручки (упрощённо, без учёта вычетов взносов)
                val tax = (revenue * 6 / 100)

                ProfitBreakdown(
                    revenueCents = revenue,
                    materialsCents = materials,
                    operatingCents = operating,
                    salaryPaymentsCents = salaryPayments,
                    taxCents = tax
                )
            }
        }
    }
}