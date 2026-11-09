package ru.automaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.automaster.app.domain.model.OrderUnit
import java.math.BigDecimal
import java.time.Instant

@Entity(
    tableName = "material_usage",
    foreignKeys = [
        ForeignKey(entity = OrderEntity::class, parentColumns = ["id"], childColumns = ["orderId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("orderId"), Index("createdAt")]
)
data class MaterialUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val name: String,
    val quantity: BigDecimal,
    val unit: OrderUnit,
    val unitCostCents: Long,
    val totalCostCents: Long,
    val createdAt: Instant
)