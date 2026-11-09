package ru.automaster.app.domain.model

data class TaxReport(
    val receiptsCents: Long,
    val refundsCents: Long,
    val revenueCents: Long,
    val taxGrossCents: Long,
    val contributionsCents: Long,
    val offsetCents: Long,
    val taxToPayCents: Long
)

data class CashFlowReport(
    val receiptsCents: Long,
    val refundsCents: Long,
    val expensesCents: Long,
    val salaryPaymentsCents: Long,
    val taxPaymentsCents: Long,
    val netCashFlowCents: Long
)

data class ManagementProfitReport(
    val revenueCents: Long,
    val materialsCents: Long,
    val orderExpensesCents: Long,
    val accrualsCents: Long,
    val indirectExpensesCents: Long,
    val profitCents: Long
)

data class PayrollEmployeeReport(
    val employeeId: Long,
    val employeeName: String,
    val accruedCents: Long,
    val paidCents: Long,
    val debtCents: Long
)

data class PayrollReport(
    val employees: List<PayrollEmployeeReport>,
    val totalAccruedCents: Long,
    val totalPaidCents: Long,
    val totalDebtCents: Long
)