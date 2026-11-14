package ru.automaster.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.OrderStageEntity
import java.time.Instant

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

    @Query("SELECT * FROM order_stages WHERE orderId = :orderId AND stageStatus = 'IN_PROGRESS' LIMIT 1")
    suspend fun getActiveStageForOrder(orderId: Long): OrderStageEntity?

    /**
     * Закрывает все активные этапы заказа перед назначением нового.
     * Предотвращает появление двух этапов IN_PROGRESS у одного заказа.
     */
    @Query("""
        UPDATE order_stages
        SET stageStatus = 'COMPLETED', isCurrent = 0, actualEnd = :now
        WHERE orderId = :orderId AND stageStatus = 'IN_PROGRESS'
    """)
    suspend fun completeActiveStages(orderId: Long, now: Instant)

    /**
     * Атомарное переназначение: закрыть старые этапы + вставить новый.
     * Выполняется в одной транзакции, чтобы избежать рассинхронизации.
     */
    @Transaction
    suspend fun reassign(orderId: Long, now: Instant, stage: OrderStageEntity) {
        completeActiveStages(orderId, now)
        clearCurrentStages(orderId)
        insert(stage)
    }
}