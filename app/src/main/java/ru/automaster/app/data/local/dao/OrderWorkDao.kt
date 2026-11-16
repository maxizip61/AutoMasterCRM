package ru.automaster.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.OrderWorkEntity
import ru.automaster.app.domain.model.WorkType

@Dao
interface OrderWorkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(work: OrderWorkEntity): Long

    @Delete
    suspend fun delete(work: OrderWorkEntity)

    @Query("SELECT * FROM order_works WHERE orderId = :orderId ORDER BY createdAt ASC")
    fun getByOrder(orderId: Long): Flow<List<OrderWorkEntity>>

    @Query("SELECT SUM(costCents) FROM order_works WHERE orderId = :orderId AND type = :type")
    suspend fun getTotalByType(orderId: Long, type: WorkType): Long?
}