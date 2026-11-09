package ru.automaster.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.OrderStageEntity

@Dao
interface OrderStageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stage: OrderStageEntity): Long

    @Update
    suspend fun update(stage: OrderStageEntity)

    @Query("SELECT * FROM order_stages WHERE orderId = :orderId ORDER BY sequence ASC")
    fun getByOrder(orderId: Long): Flow<List<OrderStageEntity>>

    @Query("SELECT * FROM order_stages WHERE postId = :postId AND stageStatus IN ('PLANNED', 'IN_PROGRESS')")
    fun getActiveOrPlannedForPost(postId: Long): Flow<List<OrderStageEntity>>

    @Query("UPDATE order_stages SET isCurrent = 0 WHERE orderId = :orderId")
    suspend fun clearCurrentStages(orderId: Long)

    @Query("SELECT * FROM order_stages WHERE stageStatus = 'IN_PROGRESS'")
    fun getActiveStages(): Flow<List<OrderStageEntity>>

    // ВОТ ЭТОТ МЕТОД ДОБАВЛЕН:
    @Query("SELECT * FROM order_stages WHERE orderId = :orderId AND stageStatus = 'IN_PROGRESS' LIMIT 1")
    suspend fun getActiveStageForOrder(orderId: Long): OrderStageEntity?
}