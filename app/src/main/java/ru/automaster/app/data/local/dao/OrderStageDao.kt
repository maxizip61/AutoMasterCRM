package ru.automaster.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.OrderStageEntity
import ru.automaster.app.domain.model.StageStatus
import java.time.Instant

@Dao
interface OrderStageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stage: OrderStageEntity): Long

    @Update
    suspend fun update(stage: OrderStageEntity)

    @Query("SELECT * FROM order_stages WHERE orderId = :orderId ORDER BY sequence ASC")
    fun getByOrder(orderId: Long): Flow<List<OrderStageEntity>>

    @Query(
        "SELECT * FROM order_stages " +
                "WHERE postId = :postId AND stageStatus IN ('PLANNED', 'IN_PROGRESS')"
    )
    fun getActiveOrPlannedForPost(postId: Long): Flow<List<OrderStageEntity>>

    @Query("UPDATE order_stages SET isCurrent = 0 WHERE orderId = :orderId")
    suspend fun clearCurrentStages(orderId: Long)

    @Query("SELECT * FROM order_stages WHERE stageStatus = 'IN_PROGRESS'")
    fun getActiveStages(): Flow<List<OrderStageEntity>>

    /**
     * Активный этап заказа. ORDER BY sequence DESC, id DESC даёт детерминированный
     * результат даже при исторических дублях (до введения инварианта
     * «один IN_PROGRESS на заказ»).
     */
    @Query(
        "SELECT * FROM order_stages " +
                "WHERE orderId = :orderId AND stageStatus = 'IN_PROGRESS' " +
                "ORDER BY sequence DESC, id DESC LIMIT 1"
    )
    suspend fun getActiveStageForOrder(orderId: Long): OrderStageEntity?

    /**
     * Атомарное назначение нового этапа.
     *
     * Гарантии внутри одной транзакции:
     * 1. Все IN_PROGRESS-этапы заказа закрываются (COMPLETED, isCurrent=0, actualEnd=now).
     * 2. Снимается isCurrent со всех этапов заказа (включая PLANNED, если были).
     * 3. Вставляется новый этап — единственный IN_PROGRESS и единственный isCurrent.
     *
     * Инвариант: после вызова MAX 1 этап заказа находится в IN_PROGRESS.
     */
    @Transaction
    suspend fun reassign(orderId: Long, now: Instant, newStage: OrderStageEntity) {
        // 1. Закрыть все текущие IN_PROGRESS.
        val current = getActiveStageForOrder(orderId)
        if (current != null) {
            update(
                current.copy(
                    stageStatus = StageStatus.COMPLETED,
                    isCurrent = false,
                    actualEnd = now
                )
            )
        }
        // На случай, если в старых данных были «висящие» IN_PROGRESS на других этапах
        // (до введения инварианта) — закрываем их тоже, чтобы не нарушить уникальность.
        // В норме список пуст.
        val extraActive = getByOrderSync(orderId).filter {
            it.id != current?.id && it.stageStatus == StageStatus.IN_PROGRESS
        }
        extraActive.forEach { stage ->
            update(
                stage.copy(
                    stageStatus = StageStatus.COMPLETED,
                    isCurrent = false,
                    actualEnd = now
                )
            )
        }

        // 2. Снять isCurrent со всех.
        clearCurrentStages(orderId)

        // 3. Вставить новый (он же единственный IN_PROGRESS + isCurrent).
        insert(
            newStage.copy(
                isCurrent = true,
                stageStatus = StageStatus.IN_PROGRESS
            )
        )
    }

    /** Служебный suspend-метод для транзакции reassign — синхронный снимок. */
    @Query("SELECT * FROM order_stages WHERE orderId = :orderId")
    suspend fun getByOrderSync(orderId: Long): List<OrderStageEntity>
}