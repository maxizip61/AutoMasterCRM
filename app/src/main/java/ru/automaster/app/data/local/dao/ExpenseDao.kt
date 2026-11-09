package ru.automaster.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.ExpenseEntity
import java.time.Instant

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT SUM(amountCents) FROM expenses WHERE includeInManagementResult = 1 AND orderId IN (SELECT id FROM orders WHERE closedAt >= :from AND closedAt <= :to)")
    suspend fun getOrderExpensesForClosedOrders(from: java.time.Instant, to: java.time.Instant): Long?

    @Query("SELECT SUM(amountCents) FROM expenses WHERE includeInManagementResult = 1 AND orderId IS NULL AND date >= :from AND date <= :to")
    suspend fun getIndirectExpensesInRange(from: java.time.Instant, to: java.time.Instant): Long?

    @Query("SELECT SUM(amountCents) FROM expenses WHERE date >= :from AND date <= :to")
    suspend fun getTotalExpensesInRange(from: java.time.Instant, to: java.time.Instant): Long?

    @Query("SELECT * FROM expenses WHERE date >= :from AND date <= :to")
    suspend fun getExpensesInRange(from: java.time.Instant, to: java.time.Instant): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE orderId = :orderId ORDER BY date DESC")
    fun getByOrder(orderId: Long): Flow<List<ExpenseEntity>>
}