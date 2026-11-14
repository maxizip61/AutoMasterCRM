package ru.automaster.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.TaxPaymentEntity
import java.time.Instant

@Dao
interface TaxPaymentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(payment: TaxPaymentEntity): Long

    @Query("SELECT * FROM tax_payments WHERE date >= :from AND date <= :to")
    suspend fun getPaymentsInRange(from: java.time.Instant, to: java.time.Instant): List<TaxPaymentEntity>
    @Query("SELECT * FROM tax_payments ORDER BY date DESC")
    fun getAll(): Flow<List<TaxPaymentEntity>>

    // ВОТ ЭТОТ МЕТОД НУЖЕН ДЛЯ CASHFLOW
    @Query("SELECT SUM(amountCents) FROM tax_payments WHERE date >= :from AND date <= :to")
    suspend fun getTotalInRange(from: Instant, to: Instant): Long?
}