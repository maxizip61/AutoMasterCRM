package ru.automaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.automaster.app.domain.model.PhotoType
import java.time.Instant

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(entity = OrderEntity::class, parentColumns = ["id"], childColumns = ["orderId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("orderId")]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val type: PhotoType,
    val filePath: String,
    val createdAt: Instant
)