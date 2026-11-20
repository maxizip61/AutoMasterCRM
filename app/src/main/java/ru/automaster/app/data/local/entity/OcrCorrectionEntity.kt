package ru.automaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Память обучающего слоя OCR.
 *
 * Хранит пары «что прочитал OCR → что подтвердил владелец».
 * Позволяет в следующих сканах автоматически подменять известные
 * ошибочные чтения верными значениями.
 *
 * field: VIN | PLATE | MAKE | MODEL
 */
@Entity(
    tableName = "ocr_corrections",
    indices = [Index(value = ["field", "rawToken"], unique = true)]
)
data class OcrCorrectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val field: String,
    val rawToken: String,
    val correctedToken: String,
    /** Сколько раз встретилось именно это чтение. */
    val occurrences: Int = 1,
    /** Сколько раз OCR оказался прав без правок (для оценки точности). */
    val confirmations: Int = 0,
    val createdAt: Instant,
    val lastUsedAt: Instant
) {
    /** Насколько записи можно доверять: правок много и они стабильны. */
    val confidence: Int get() = occurrences
}