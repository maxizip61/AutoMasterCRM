package ru.automaster.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.MaterialUsageEntity
import java.time.Instant

@Dao
interface MaterialUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(material: MaterialUsageEntity): Long

    @Delete
    suspend fun delete(material: MaterialUsageEntity)

    @Query("SELECT SUM(totalCostCents) FROM material_usage WHERE orderId IN (SELECT id FROM orders WHERE closedAt >= :from AND closedAt <= :to)")
    suspend fun getMaterialsForClosedOrders(from: java.time.Instant, to: java.time.Instant): Long?

    @Query("SELECT * FROM material_usage WHERE orderId = :orderId ORDER BY createdAt DESC")
    fun getByOrder(orderId: Long): Flow<List<MaterialUsageEntity>>
}