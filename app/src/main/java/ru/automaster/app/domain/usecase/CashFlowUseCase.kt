package ru.automaster.app.domain.usecase

import ru.automaster.app.data.local.dao.*
import ru.automaster.app.domain.model.CashFlowReport
import java.time.Instant
import javax.inject.Inject

class CashFlowUseCase @Inject constructor(
    private val paymentDao: PaymentDao,
    private val expenseDao: ExpenseDao,
    private val salaryPaymentDao: SalaryPaymentDao,
    private val taxPaymentDao: TaxPaymentDao
) {
    suspend fun execute(from: Instant, to: Instant): CashFlowReport {
        // 1. Приход (оплаты минус возвраты)
        val receipts = paymentDao.getReceiptsInRange(from, to) ?: 0L
        val refunds = paymentDao.getRefundsInRange(from, to) ?: 0L

        // 2. Расходы (общие + зарплаты + налоги)
        val expenses = expenseDao.getTotalExpensesInRange(from, to) ?: 0L
        val salaries = salaryPaymentDao.getTotalInRange(from, to) ?: 0L
        val taxes = taxPaymentDao.getTotalInRange(from, to) ?: 0L

        // 3. Итог
        val netCashFlow = (receipts - refunds) - expenses - salaries - taxes

        return CashFlowReport(
            receiptsCents = receipts,
            refundsCents = refunds,
            expensesCents = expenses,
            salaryPaymentsCents = salaries,
            taxPaymentsCents = taxes,
            netCashFlowCents = netCashFlow
        )
    }
}