package ru.automaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.automaster.app.domain.model.StagePhase
import ru.automaster.app.domain.model.StageStatus
import java.time.Instant

@Entity(
    tableName = "order_stages",
    foreignKeys = [
        ForeignKey(entity = OrderEntity::class, parentColumns = ["id"], childColumns = ["orderId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PostEntity::class, parentColumns = ["id"], childColumns = ["postId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index(value = ["orderId", "sequence"], unique = true), Index("postId"), Index("isCurrent")]
)
data class OrderStageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val phase: StagePhase,
    val postId: Long? = null,
    val sequence: Int,
    val isCurrent: Boolean = false,
    val stageStatus: StageStatus,
    val plannedStart: Instant,
    val plannedEnd: Instant,
    val actualStart: Instant? = null,
    val actualEnd: Instant? = null,
    val notes: String? = null,
    val accrualDecisionAmountCents: Long? = null
)