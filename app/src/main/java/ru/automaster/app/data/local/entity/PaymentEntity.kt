package ru.automaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.automaster.app.domain.model.PaymentMethod
import ru.automaster.app.domain.model.PaymentType
import java.time.Instant

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(entity = OrderEntity::class, parentColumns = ["id"], childColumns = ["orderId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("orderId"), Index("date")]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val type: PaymentType,
    val amountCents: Long,
    val date: Instant,
    val method: PaymentMethod,
    val comment: String? = null
)