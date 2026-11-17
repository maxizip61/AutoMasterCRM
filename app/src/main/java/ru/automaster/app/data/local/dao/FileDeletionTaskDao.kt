package ru.automaster.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ru.automaster.app.data.local.entity.FileDeletionTaskEntity

@Dao
interface FileDeletionTaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: FileDeletionTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<FileDeletionTaskEntity>)

    /**
     * Обновление по PK. Обязателен вместо insert(...) при инкременте attempts:
     * REPLACE в Room реализован как DELETE+INSERT и может нарушать FK-связи
     * с родительскими таблицами, а также менять внутреннюю идентичность записи.
     */
    @Update
    suspend fun update(task: FileDeletionTaskEntity)

    @Query("SELECT * FROM file_deletion_tasks")
    suspend fun getAll(): List<FileDeletionTaskEntity>

    @Delete
    suspend fun delete(task: FileDeletionTaskEntity)
}