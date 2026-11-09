package ru.automaster.app.data.local.dao

import java.time.Instant
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.OrderEntity
import ru.automaster.app.domain.model.OrderStatus

@Dao
interface OrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity): Long

    @Update
    suspend fun update(order: OrderEntity)

    @Delete
    suspend fun delete(order: OrderEntity)

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getById(id: Long): OrderEntity?

    @Query("SELECT * FROM orders WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<OrderEntity?>

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAll(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE status IN (:statuses) ORDER BY plannedDeliveryDate ASC")
    fun getByStatuses(statuses: List<OrderStatus>): Flow<List<OrderEntity>>

    @Query("SELECT SUM(closedEstimateCents) FROM orders WHERE closedAt >= :from AND closedAt <= :to")
    suspend fun getRevenueInRange(from: java.time.Instant, to: java.time.Instant): Long?

    @Query("SELECT * FROM orders WHERE closedAt >= :from AND closedAt <= :to")
    suspend fun getClosedOrdersInRange(from: java.time.Instant, to: java.time.Instant): List<ru.automaster.app.data.local.entity.OrderEntity>
}