package ru.automaster.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.OrderEmployeeAccrualEntity
import java.time.Instant

@Dao
interface OrderEmployeeAccrualDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(accrual: OrderEmployeeAccrualEntity): Long

    /** Оригинал для сторно. */
    @Query("SELECT * FROM order_employee_accruals WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): OrderEmployeeAccrualEntity?

    /** Есть ли уже сторно для этого начисления. */
    @Query("SELECT COUNT(*) FROM order_employee_accruals WHERE reversalOfId = :accrualId")
    suspend fun countReversals(accrualId: Long): Int

    @Query("SELECT * FROM order_employee_accruals WHERE orderId = :orderId")
    fun getByOrder(orderId: Long): Flow<List<OrderEmployeeAccrualEntity>>

    @Query("SELECT SUM(CASE WHEN reversalOfId IS NULL THEN amountCents ELSE -amountCents END) FROM order_employee_accruals WHERE closedAt >= :from AND closedAt <= :to")
    suspend fun getAccruedInRange(from: Instant, to: Instant): Long?

    @Query("SELECT SUM(CASE WHEN reversalOfId IS NULL THEN amountCents ELSE -amountCents END) FROM order_employee_accruals WHERE employeeId = :employeeId AND closedAt >= :from AND closedAt <= :to")
    suspend fun getAccruedForEmployee(employeeId: Long, from: Instant, to: Instant): Long?

    @Query("SELECT SUM(CASE WHEN reversalOfId IS NULL THEN amountCents ELSE -amountCents END) FROM order_employee_accruals WHERE employeeId = :employeeId")
    suspend fun getTotalAccruedForEmployee(employeeId: Long): Long?

    @Query("SELECT * FROM order_employee_accruals WHERE employeeId = :employeeId AND closedAt >= :from AND closedAt <= :to")
    fun getByEmployeePeriod(employeeId: Long, from: Long, to: Long): Flow<List<OrderEmployeeAccrualEntity>>
}