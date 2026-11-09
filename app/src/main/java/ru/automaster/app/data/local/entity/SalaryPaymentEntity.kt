package ru.automaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.automaster.app.domain.model.PaymentMethod
import java.time.Instant

@Entity(
    tableName = "salary_payments",
    foreignKeys = [
        ForeignKey(entity = EmployeeEntity::class, parentColumns = ["id"], childColumns = ["employeeId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("employeeId"), Index("date")]
)
data class SalaryPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val periodStart: Instant,
    val periodEnd: Instant,
    val amountCents: Long,
    val date: Instant,
    val method: PaymentMethod,
    val comment: String? = null,
    val createdAt: Instant
)