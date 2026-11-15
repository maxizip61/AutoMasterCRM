package ru.automaster.app.data.local.dao

import androidx.room.*
import ru.automaster.app.data.local.entity.FileDeletionTaskEntity

@Dao
interface FileDeletionTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: FileDeletionTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<FileDeletionTaskEntity>)

    @Query("SELECT * FROM file_deletion_tasks")
    suspend fun getAll(): List<FileDeletionTaskEntity>

    @Delete
    suspend fun delete(task: FileDeletionTaskEntity)
}