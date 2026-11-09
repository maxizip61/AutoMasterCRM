package ru.automaster.app.data.local.dao

import java.time.Instant
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.PaymentEntity

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentEntity): Long

    @Query("SELECT * FROM payments WHERE orderId = :orderId ORDER BY date DESC")
    fun getByOrder(orderId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT SUM(amountCents) FROM payments WHERE type = 'PAYMENT' AND date >= :from AND date <= :to")
    suspend fun getReceiptsInRange(from: java.time.Instant, to: java.time.Instant): Long?

    @Query("SELECT SUM(amountCents) FROM payments WHERE type = 'REFUND' AND date >= :from AND date <= :to")
    suspend fun getRefundsInRange(from: java.time.Instant, to: java.time.Instant): Long?

    @Query("SELECT * FROM payments WHERE date >= :from AND date <= :to")
    suspend fun getPaymentsInRange(from: java.time.Instant, to: java.time.Instant): List<PaymentEntity>

    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>
}