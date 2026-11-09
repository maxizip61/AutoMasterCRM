package ru.automaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "order_employee_assignments",
    foreignKeys = [
        ForeignKey(entity = OrderEntity::class, parentColumns = ["id"], childColumns = ["orderId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = EmployeeEntity::class, parentColumns = ["id"], childColumns = ["employeeId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = OrderStageEntity::class, parentColumns = ["id"], childColumns = ["stageId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("orderId"), Index("employeeId"), Index("stageId")]
)
data class OrderEmployeeAssignmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val employeeId: Long,
    val stageId: Long? = null,
    val role: String? = null,
    val createdAt: Instant
)