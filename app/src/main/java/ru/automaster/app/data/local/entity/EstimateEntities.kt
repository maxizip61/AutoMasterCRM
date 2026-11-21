package ru.automaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/** Сессия предварительной оценки ремонта по фото. */
@Entity(
    tableName = "damage_assessments",
    foreignKeys = [
        ForeignKey(entity = OrderEntity::class, parentColumns = ["id"], childColumns = ["orderId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("orderId")]
)
data class DamageAssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carMake: String,
    val carModel: String,
    val carClass: String,
    val source: String,          // LOCAL | CLIENT
    val status: String,          // DRAFT | SENT | CONVERTED
    val orderId: Long? = null,
    val totalMinCents: Long = 0,
    val totalMaxCents: Long = 0,
    val photoPaths: String = "", // пути через ';'
    val createdAt: Instant
)

/** Позиция оценки: зона + тип повреждения + диапазон стоимости. */
@Entity(
    tableName = "damage_items",
    foreignKeys = [
        ForeignKey(entity = DamageAssessmentEntity::class, parentColumns = ["id"], childColumns = ["assessmentId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("assessmentId")]
)
data class DamageItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assessmentId: Long,
    val panel: String,
    val damageType: String,
    val laborMinCents: Long,
    val laborMaxCents: Long,
    val materialsCents: Long,
    val totalMinCents: Long,
    val totalMaxCents: Long,
    val actualCents: Long? = null, // факт после ремонта (для обучения)
    val createdAt: Instant
)

/** Прайс по зонам: стартовый + дообучаемый на факте ремонтов. */
@Entity(
    tableName = "panel_price_book",
    indices = [Index(value = ["panel", "damageType", "workType"], unique = true)]
)
data class PanelPriceBookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val panel: String,
    val damageType: String,
    val workType: String,
    val baseLaborCents: Long,
    val samplesCount: Int = 0,
    val source: String = "SEED", // SEED | LEARNED
    val updatedAt: Instant
)