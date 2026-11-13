package ru.automaster.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.automaster.app.data.local.entity.FileDeletionTaskEntity

@Dao
interface FileDeletionTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: FileDeletionTaskEntity): Long

    @Query("SELECT * FROM file_deletion_tasks")
    suspend fun getAll(): List<FileDeletionTaskEntity>

    @Delete
    suspend fun delete(task: FileDeletionTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<FileDeletionTaskEntity>)
}