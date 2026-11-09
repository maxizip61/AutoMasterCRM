package ru.automaster.app.domain.usecase

import ru.automaster.app.data.local.dao.EmployeeDao
import ru.automaster.app.data.local.dao.OrderEmployeeAccrualDao
import ru.automaster.app.data.local.dao.SalaryPaymentDao
import ru.automaster.app.domain.model.PayrollEmployeeReport
import ru.automaster.app.domain.model.PayrollReport
import java.time.Instant
import javax.inject.Inject

class PayrollCalculationUseCase @Inject constructor(
    private val employeeDao: EmployeeDao,
    private val accrualDao: OrderEmployeeAccrualDao,
    private val salaryPaymentDao: SalaryPaymentDao
) {
    suspend fun execute(from: Instant, to: Instant): PayrollReport {
        val employees = employeeDao.getActiveSync()
        val reports = employees.map { emp ->
            val accrued = accrualDao.getAccruedForEmployee(emp.id, from, to) ?: 0L
            val paid = salaryPaymentDao.getPaidForEmployee(emp.id, from, to) ?: 0L
            PayrollEmployeeReport(
                employeeId = emp.id, employeeName = emp.fullName,
                accruedCents = accrued, paidCents = paid, debtCents = accrued - paid
            )
        }

        return PayrollReport(
            employees = reports,
            totalAccruedCents = reports.sumOf { it.accruedCents },
            totalPaidCents = reports.sumOf { it.paidCents },
            totalDebtCents = reports.sumOf { it.debtCents }
        )
    }
}