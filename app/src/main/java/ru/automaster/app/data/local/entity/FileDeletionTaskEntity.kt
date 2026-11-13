package ru.automaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "file_deletion_tasks")
data class FileDeletionTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val attempts: Int = 0,          // <-- НОВОЕ ПОЛЕ
    val lastError: String? = null,  // <-- НОВОЕ ПОЛЕ
    val createdAt: Instant
)