package ru.automaster.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.PaymentEntity
import java.time.Instant

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentEntity): Long

    @Query("SELECT * FROM payments WHERE orderId = :orderId ORDER BY date DESC")
    fun getByOrder(orderId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT SUM(amountCents) FROM payments WHERE type = 'PAYMENT' AND date >= :from AND date <= :to")
    suspend fun getReceiptsInRange(from: Instant, to: Instant): Long?

    @Query("SELECT SUM(amountCents) FROM payments WHERE type = 'REFUND' AND date >= :from AND date <= :to")
    suspend fun getRefundsInRange(from: Instant, to: Instant): Long?

    @Query("SELECT * FROM payments WHERE date >= :from AND date <= :to")
    suspend fun getPaymentsInRange(from: Instant, to: Instant): List<PaymentEntity>

    // Flow-версия для Live Dashboard
    @Query("SELECT * FROM payments WHERE date >= :from AND date <= :to")
    fun getPaymentsInRangeFlow(from: Instant, to: Instant): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>
}