package ru.automaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.automaster.app.domain.model.AccrualType
import java.time.Instant

@Entity(
    tableName = "order_employee_accruals",
    foreignKeys = [
        ForeignKey(entity = OrderEntity::class, parentColumns = ["id"], childColumns = ["orderId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = EmployeeEntity::class, parentColumns = ["id"], childColumns = ["employeeId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [
        Index("orderId"),
        Index(value = ["idempotencyKey"], unique = true),
        Index("employeeId"),
        Index("closedAt")
    ]
)
data class OrderEmployeeAccrualEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val employeeId: Long,
    val type: AccrualType,
    val percentBps: Int? = null,
    val panelCents: Long? = null,
    val panelsCount: Int? = null,
    val amountCents: Long,
    val closedAt: Instant,
    val idempotencyKey: String? = null,
    val reversalOfId: Long? = null,
    val comment: String? = null
)