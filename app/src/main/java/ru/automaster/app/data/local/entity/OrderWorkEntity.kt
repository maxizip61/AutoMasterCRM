package ru.automaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.automaster.app.domain.model.WorkType
import java.time.Instant

@Entity(
    tableName = "order_works",
    foreignKeys = [
        ForeignKey(entity = OrderEntity::class, parentColumns = ["id"], childColumns = ["orderId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = EmployeeEntity::class, parentColumns = ["id"], childColumns = ["assignedEmployeeId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("orderId"), Index("assignedEmployeeId")]
)
data class OrderWorkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val name: String,
    val type: WorkType,
    val costCents: Long,
    val assignedEmployeeId: Long? = null,
    val createdAt: Instant,
    /** Зона кузова (для обучения оценки по фото). null = не размечено. */
    val panel: String? = null
)