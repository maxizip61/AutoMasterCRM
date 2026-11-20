package ru.automaster.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ru.automaster.app.data.local.entity.OcrCorrectionEntity
import java.time.Instant

@Dao
interface OcrCorrectionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: OcrCorrectionEntity): Long

    @Update
    suspend fun update(entity: OcrCorrectionEntity)

    /** Точное попадание: это чтение мы уже видели и знаем, как исправить. */
    @Query(
        "SELECT * FROM ocr_corrections WHERE field = :field AND rawToken = :raw LIMIT 1"
    )
    suspend fun find(field: String, raw: String): OcrCorrectionEntity?

    /**
     * Записывает урок: OCR сказал [raw], владелец подтвердил [corrected].
     * Повторное чтение увеличивает счётчик, а не создаёт дубль.
     */
    suspend fun record(field: String, raw: String, corrected: String, now: Instant) {
        if (raw.isBlank() || corrected.isBlank()) return
        val existing = find(field, raw)
        if (existing == null) {
            insert(
                OcrCorrectionEntity(
                    field = field,
                    rawToken = raw,
                    correctedToken = corrected,
                    occurrences = 1,
                    confirmations = if (raw == corrected) 1 else 0,
                    createdAt = now,
                    lastUsedAt = now
                )
            )
        } else {
            update(
                existing.copy(
                    occurrences = existing.occurrences + 1,
                    confirmations = existing.confirmations +
                            if (raw == corrected) 1 else 0,
                    correctedToken = if (raw != corrected) corrected
                    else existing.correctedToken,
                    lastUsedAt = now
                )
            )
        }
    }

    /** Надёжные уроки: встречались минимум [min] раз. */
    @Query(
        "SELECT * FROM ocr_corrections WHERE field = :field AND occurrences >= :min " +
                "ORDER BY occurrences DESC"
    )
    suspend fun strong(field: String, min: Int): List<OcrCorrectionEntity>

    /** Статистика точности: сколько чтений не требовали правок. */
    @Query("SELECT SUM(confirmations) FROM ocr_corrections")
    suspend fun totalConfirmed(): Int?

    @Query("SELECT SUM(occurrences) FROM ocr_corrections")
    suspend fun totalSeen(): Int?
}