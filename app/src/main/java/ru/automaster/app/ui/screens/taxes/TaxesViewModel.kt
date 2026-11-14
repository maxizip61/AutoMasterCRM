package ru.automaster.app.ui.screens.taxes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.automaster.app.data.local.dao.EmployeeDao
import ru.automaster.app.data.local.dao.PaymentDao
import ru.automaster.app.data.local.dao.TaxPaymentDao
import ru.automaster.app.data.local.entity.TaxPaymentEntity
import ru.automaster.app.data.settings.SettingsRepository
import ru.automaster.app.domain.model.AppSettings
import ru.automaster.app.domain.model.PaymentMethod
import ru.automaster.app.domain.model.PaymentType
import ru.automaster.app.domain.model.TaxPaymentKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class TaxReportUiState(
    val incomeCents: Long = 0L,
    val accruedCents: Long = 0L,
    val deductionCents: Long = 0L,
    val advancesCents: Long = 0L,
    val payableCents: Long = 0L,
    val hasEmployees: Boolean = false,
    val taxRatePercent: Int = 6
)

@HiltViewModel
class TaxesViewModel @Inject constructor(
    private val taxPaymentDao: TaxPaymentDao,
    private val paymentDao: PaymentDao,
    private val employeeDao: EmployeeDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // История всех платежей для отображения в списке
    val taxPayments: StateFlow<List<TaxPaymentEntity>> = taxPaymentDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Текущие настройки (ставка налога)
    val config: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    // Состояние с расчетом налогов
    private val _reportState = MutableStateFlow(TaxReportUiState())
    val reportState: StateFlow<TaxReportUiState> = _reportState.asStateFlow()

    init {
        // Пересчитываем налоги при старте
        calculateTaxes()

        // Пересчитываем при добавлении новых налоговых платежей
        viewModelScope.launch {
            taxPaymentDao.getAll().collect {
                calculateTaxes()
            }
        }
    }

    private fun calculateTaxes() {
        viewModelScope.launch {
            val now = Instant.now()
            val zone = ZoneId.systemDefault()
            // Считаем с 1 января текущего года до сегодняшнего дня
            val startOfYear = LocalDate.now(zone).withDayOfYear(1).atStartOfDay(zone).toInstant()
            val endOfPeriod = now

            // 1. Доход (оплаты минус возвраты)
            val receipts = paymentDao.getPaymentsInRange(startOfYear, endOfPeriod)
                .filter { it.type == PaymentType.PAYMENT }
                .sumOf { it.amountCents }
            val refunds = paymentDao.getPaymentsInRange(startOfYear, endOfPeriod)
                .filter { it.type == PaymentType.REFUND }
                .sumOf { it.amountCents }

            val income = (receipts - refunds).coerceAtLeast(0L)

            // 2. Начисленный налог
            val taxRate = config.value.taxRatePercent
            val accrued = income * taxRate / 100

            // 3. Вычет (взносы ИП)
            val hasEmployees = employeeDao.getActiveSync().isNotEmpty()
            val maxDeduction = if (hasEmployees) accrued / 2 else accrued

            val allPaymentsInPeriod = taxPaymentDao.getPaymentsInRange(startOfYear, endOfPeriod)

            val paidInsurance = allPaymentsInPeriod
                .filter { it.kind != TaxPaymentKind.USN_TAX }
                .sumOf { it.amountCents }

            val deduction = minOf(paidInsurance, maxDeduction)

            // 4. Авансы (уплаченный УСН налог)
            val advances = allPaymentsInPeriod
                .filter { it.kind == TaxPaymentKind.USN_TAX }
                .sumOf { it.amountCents }

            // 5. К уплате
            val payable = maxOf(0L, accrued - deduction - advances)

            _reportState.value = TaxReportUiState(
                incomeCents = income,
                accruedCents = accrued,
                deductionCents = deduction,
                advancesCents = advances,
                payableCents = payable,
                hasEmployees = hasEmployees,
                taxRatePercent = taxRate
            )
        }
    }

    fun addTaxPayment(amountRub: Long, kind: TaxPaymentKind, comment: String) {
        viewModelScope.launch {
            val now = Instant.now()
            taxPaymentDao.insert(
                TaxPaymentEntity(
                    kind = kind,
                    amountCents = amountRub * 100,
                    date = now,
                    method = PaymentMethod.CASH,
                    comment = comment.ifBlank { null },
                    createdAt = now
                )
            )
        }
    }
}