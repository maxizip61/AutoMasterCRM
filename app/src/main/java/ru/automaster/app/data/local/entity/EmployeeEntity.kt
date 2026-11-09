package ru.automaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "employees", indices = [Index("archived")])
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val position: String,
    val archived: Boolean = false,
    val hiredAt: Instant? = null,
    val notes: String? = null,

    // НОВЫЕ ПОЛЯ ДЛЯ ЗАРПЛАТЫ
    val compensationType: String = "PERCENT", // "PERCENT" (процент) или "PANEL" (за деталь)
    val rateValue: Long = 30 // Если процент, то 30 (означает 30%). Если деталь, то 3000 (означает 3000 руб)
)