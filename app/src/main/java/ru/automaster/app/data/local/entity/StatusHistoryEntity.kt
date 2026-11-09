package ru.automaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.automaster.app.domain.model.OrderStatus
import java.time.Instant

@Entity(
    tableName = "status_history",
    foreignKeys = [
        ForeignKey(entity = OrderEntity::class, parentColumns = ["id"], childColumns = ["orderId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("orderId")]
)
data class StatusHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val oldStatus: OrderStatus? = null,
    val newStatus: OrderStatus,
    val changedAt: Instant,
    val comment: String? = null
)