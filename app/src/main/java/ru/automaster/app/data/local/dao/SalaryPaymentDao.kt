package ru.automaster.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.SalaryPaymentEntity
import java.time.Instant

@Dao
interface SalaryPaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: SalaryPaymentEntity): Long

    @Query("SELECT * FROM salary_payments WHERE employeeId = :employeeId ORDER BY date DESC")
    fun getByEmployee(employeeId: Long): Flow<List<SalaryPaymentEntity>>

    @Query("SELECT * FROM salary_payments ORDER BY date DESC")
    fun getAll(): Flow<List<SalaryPaymentEntity>>

    @Query("SELECT SUM(amountCents) FROM salary_payments WHERE date >= :from AND date <= :to")
    suspend fun getTotalInRange(from: java.time.Instant, to: java.time.Instant): Long?

    @Query("SELECT SUM(amountCents) FROM salary_payments WHERE employeeId = :employeeId AND date >= :from AND date <= :to")
    suspend fun getPaidForEmployee(employeeId: Long, from: java.time.Instant, to: java.time.Instant): Long?
}