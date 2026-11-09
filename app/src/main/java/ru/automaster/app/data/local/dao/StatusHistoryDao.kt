package ru.automaster.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.StatusHistoryEntity

@Dao
interface StatusHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: StatusHistoryEntity): Long

    @Query("SELECT * FROM status_history WHERE orderId = :orderId ORDER BY changedAt DESC")
    fun getByOrder(orderId: Long): Flow<List<StatusHistoryEntity>>
}