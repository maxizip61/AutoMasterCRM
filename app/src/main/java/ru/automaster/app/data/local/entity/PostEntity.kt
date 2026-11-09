package ru.automaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.automaster.app.domain.model.PostName
import ru.automaster.app.domain.model.PostType

@Entity(tableName = "posts", indices = [Index(value = ["name"], unique = true)])
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: PostName,
    val type: PostType
)