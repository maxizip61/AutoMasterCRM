package ru.automaster.app.domain.document

import kotlinx.coroutines.flow.first
import ru.automaster.app.data.local.dao.CarDao
import ru.automaster.app.data.local.dao.OcrCorrectionDao
import ru.automaster.app.data.local.entity.CarEntity
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Обучающий слой поверх OCR.
 *
 * Три механизма:
 * 1. Словарь исправлений: «OCR прочитал → владелец исправил» → автоподмена в следующий раз
 * 2. Привязка к известным машинам: совпадение с точностью до 1–2 символов → эталон
 * 3. Рост словаря марок: новые марки запоминаются автоматически
 *
 * Работает полностью офлайн, база исправлений шифруется в бэкапе.
 */
@Singleton
class OcrLearningEngine @Inject constructor(
    private val correctionDao: OcrCorrectionDao,
    private val carDao: CarDao
) {

    /**
     * Применяет словарь исправлений к черновику.
     * Если OCR дал токен, который мы уже видели и знаем как исправить — подменяем.
     */
    suspend fun applyCorrections(draft: CarDraft): CarDraft {
        if (draft.filledCount == 0) return draft

        val plate = draft.licensePlate?.let { applyCorrection("PLATE", it) }
        val vin = draft.vin?.let { applyCorrection("VIN", it) }
        val make = draft.make?.let { applyCorrection("MAKE", it) }
        val model = draft.model?.let { applyCorrection("MODEL", it) }

        return draft.copy(
            licensePlate = plate,
            vin = vin,
            make = make,
            model = model
        )
    }

    private suspend fun applyCorrection(field: String, raw: String): String {
        val correction = correctionDao.find(field, raw)
        return correction?.correctedToken ?: raw
    }

    /**
     * Привязка к известным машинам: если распознанный VIN или номер
     * совпадает с эталоном с точностью до 2 символов — подтягиваем эталон.
     * Это лечит ошибки OCR, которые словарь ещё не запомнил.
     */
    suspend fun linkToKnownCars(draft: CarDraft): CarDraft {
        if (draft.filledCount == 0) return draft

        val allCars = carDao.search("").first()

        var plate = draft.licensePlate
        var vin = draft.vin

        // Поиск по номеру
        plate?.let { p ->
            val match = allCars.firstOrNull { car ->
                car.licensePlateSearchIndex.isNotBlank() &&
                        distance(p.uppercase(), car.licensePlateSearchIndex) <= 2
            }
            if (match != null) plate = match.licensePlate
        }

        // Поиск по VIN
        vin?.let { v ->
            val match = allCars.firstOrNull { car ->
                car.vin.isNotBlank() && distance(v.uppercase(), car.vin.uppercase()) <= 2
            }
            if (match != null) vin = match.vin
        }

        return draft.copy(licensePlate = plate, vin = vin)
    }

    /**
     * Записывает урок: OCR сказал [raw], владелец подтвердил [corrected].
     * Вызывается после сохранения заказа.
     */
    suspend fun recordLesson(field: String, raw: String?, corrected: String) {
        if (raw.isNullOrBlank()) return
        correctionDao.record(field, raw, corrected, Instant.now())
    }

    /** Расстояние Левенштейна: минимальное число вставок/удалений/замен. */
    private fun distance(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[m][n]
    }
}