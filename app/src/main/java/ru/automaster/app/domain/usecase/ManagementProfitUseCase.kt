package ru.automaster.app.domain.usecase

import ru.automaster.app.data.local.dao.*
import ru.automaster.app.domain.model.ManagementProfitReport
import java.time.Instant
import javax.inject.Inject

class ManagementProfitUseCase @Inject constructor(
    private val orderDao: OrderDao,
    private val materialUsageDao: MaterialUsageDao,
    private val expenseDao: ExpenseDao,
    private val orderEmployeeAccrualDao: OrderEmployeeAccrualDao
) {
    suspend fun execute(from: Instant, to: Instant): ManagementProfitReport {
        val revenue = orderDao.getRevenueInRange(from, to) ?: 0L
        val materials = materialUsageDao.getMaterialsForClosedOrders(from, to) ?: 0L
        val orderExpenses = expenseDao.getOrderExpensesForClosedOrders(from, to) ?: 0L
        val accruals = orderEmployeeAccrualDao.getAccruedInRange(from, to) ?: 0L
        val indirectExpenses = expenseDao.getIndirectExpensesInRange(from, to) ?: 0L

        val profit = revenue - materials - orderExpenses - accruals - indirectExpenses

        return ManagementProfitReport(
            revenueCents = revenue, materialsCents = materials, orderExpensesCents = orderExpenses,
            accrualsCents = accruals, indirectExpensesCents = indirectExpenses, profitCents = profit
        )
    }
}