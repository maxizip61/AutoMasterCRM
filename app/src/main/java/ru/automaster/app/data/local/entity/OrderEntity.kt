package ru.automaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.automaster.app.domain.model.OrderStatus
import java.time.Instant

@Entity(
    tableName = "orders",
    foreignKeys = [
        ForeignKey(entity = ClientEntity::class, parentColumns = ["id"], childColumns = ["clientId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CarEntity::class, parentColumns = ["id"], childColumns = ["carId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("clientId"), Index("carId"), Index("status"), Index("plannedDeliveryDate"), Index("closedAt")]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val carId: Long,
    val createdAt: Instant,
    val plannedDeliveryDate: Instant,
    val actualDeliveryDate: Instant? = null,
    val closedAt: Instant? = null,
    val agreedEstimateCents: Long,
    val closedEstimateCents: Long? = null,
    val status: OrderStatus,
    val conditionOnAcceptance: String? = null,
    val damages: String? = null,
    val completeness: String? = null,
    val notes: String? = null
)