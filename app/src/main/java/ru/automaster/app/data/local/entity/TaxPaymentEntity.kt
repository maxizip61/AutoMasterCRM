package ru.automaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.automaster.app.domain.model.PaymentMethod
import ru.automaster.app.domain.model.TaxPaymentKind
import java.time.Instant

@Entity(tableName = "tax_payments", indices = [Index("kind"), Index("date")])
data class TaxPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: TaxPaymentKind,
    val amountCents: Long,
    val date: Instant,
    val method: PaymentMethod,
    val comment: String? = null,
    val createdAt: Instant
)