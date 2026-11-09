package ru.automaster.app.data.local.dao

import java.time.Instant
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.EmployeeEntity

@Dao
interface EmployeeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(employee: EmployeeEntity): Long

    @Update
    suspend fun update(employee: EmployeeEntity)

    @Query("SELECT * FROM employees WHERE archived = 0 ORDER BY fullName ASC")
    fun getActive(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE archived = 1 ORDER BY fullName ASC")
    fun getArchived(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE archived = 0")
    suspend fun getActiveSync(): List<ru.automaster.app.data.local.entity.EmployeeEntity>
}