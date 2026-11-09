package ru.automaster.app.data.local.dao

import androidx.room.*
import ru.automaster.app.data.local.entity.WorkScheduleEntity

@Dao
interface WorkScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(schedule: WorkScheduleEntity)

    @Query("SELECT * FROM work_schedule WHERE id = 1")
    suspend fun getSchedule(): WorkScheduleEntity?
}