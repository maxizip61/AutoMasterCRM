package ru.automaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_schedule")
data class WorkScheduleEntity(
    @PrimaryKey val id: Long = 1,
    val workdaysCsv: String = "1,2,3,4,5,6,7",
    val startMinute: Int = 540,
    val endMinute: Int = 1080
)