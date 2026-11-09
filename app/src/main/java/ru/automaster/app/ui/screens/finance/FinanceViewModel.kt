package ru.automaster.app.ui.screens.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import ru.automaster.app.data.local.dao.ExpenseDao
import ru.automaster.app.data.local.dao.PaymentDao
import ru.automaster.app.domain.model.PaymentType
import java.time.Instant
import javax.inject.Inject

data class TransactionItem(
    val id: String,
    val date: Instant,
    val amountCents: Long,
    val isIncome: Boolean,
    val title: String,
    val subtitle: String
)

@HiltViewModel
class FinanceViewModel @Inject constructor(
    paymentDao: PaymentDao,
    expenseDao: ExpenseDao
) : ViewModel() {

    val transactions: StateFlow<List<TransactionItem>> = combine(
        paymentDao.getAllPayments(),
        expenseDao.getAll()
    ) { payments, expenses ->
        val pList = payments.map {
            TransactionItem(
                id = "p_${it.id}",
                date = it.date,
                amountCents = it.amountCents,
                isIncome = it.type != PaymentType.REFUND,
                title = if (it.type == PaymentType.REFUND) "Возврат клиенту" else "Оплата от клиента",
                subtitle = "Заказ #${it.orderId} · ${it.method.name}"
            )
        }
        val eList = expenses.map {
            TransactionItem(
                id = "e_${it.id}",
                date = it.date,
                amountCents = it.amountCents,
                isIncome = false,
                title = "Расход",
                subtitle = it.comment ?: "Общий расход"
            )
        }
        (pList + eList).sortedByDescending { it.date }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}