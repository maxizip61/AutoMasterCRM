package ru.automaster.app.domain.document

import ru.automaster.app.data.local.dao.EstimateDao
import ru.automaster.app.data.local.entity.PanelPriceBookEntity
import ru.automaster.app.domain.model.CarPanels
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class EstimateLine(
    val panel: String,
    val damageType: String,
    val laborMinCents: Long,
    val laborMaxCents: Long,
    val materialsCents: Long,
    val totalMinCents: Long,
    val totalMaxCents: Long
)

/**
 * Смета из собственной базы ремонтов.
 * Прайс дообучается на факте закрытых заказов (learn).
 */
@Singleton
class EstimateEngine @Inject constructor(private val dao: EstimateDao) {

    companion object {
        val CLASS_MULTIPLIER = mapOf(
            "ECONOMY" to 1.0, "COMFORT" to 1.15, "BUSINESS" to 1.4,
            "PREMIUM" to 1.8, "SUV" to 1.25
        )
        const val MATERIALS_PERCENT = 25L
        const val RANGE_PERCENT = 15L

        val DAMAGE_WORKS = mapOf(
            "SCRATCH" to listOf("PAINT"),
            "DENT" to listOf("BODY", "PAINT"),
            "CRACK" to listOf("BODY", "PAINT"),
            "PAINT" to listOf("PAINT"),
            "REPLACE" to listOf("BODY", "PAINT")
        )

        // Стартовые цены, ₽ (работы)
        val SEED_LABOR = mapOf(
            "BUMPER" to mapOf("BODY" to 3000L, "PAINT" to 3500L),
            "HOOD" to mapOf("BODY" to 5000L, "PAINT" to 4500L),
            "DOOR" to mapOf("BODY" to 4500L, "PAINT" to 4000L),
            "FENDER" to mapOf("BODY" to 3500L, "PAINT" to 3500L),
            "ROOF" to mapOf("BODY" to 6000L, "PAINT" to 5000L),
            "TRUNK" to mapOf("BODY" to 4500L, "PAINT" to 4000L)
        )
        // Стартовые цены запчасти при замене, ₽
        val SEED_PART = mapOf(
            "BUMPER" to 8000L, "HOOD" to 12000L, "DOOR" to 15000L,
            "FENDER" to 9000L, "ROOF" to 20000L, "TRUNK" to 12000L
        )
    }

    suspend fun estimateItem(panel: String, damageType: String, carClass: String): EstimateLine {
        val cat = CarPanels.category[panel] ?: "DOOR"
        val mult = CLASS_MULTIPLIER[carClass] ?: 1.0
        val works = DAMAGE_WORKS[damageType] ?: listOf("PAINT")

        var labor = 0L
        for (w in works) {
            val row = dao.findPrice(panel, damageType, w)
            val base = if (row != null && row.samplesCount >= 3) {
                row.baseLaborCents
            } else {
                val seed = (SEED_LABOR[cat]?.get(w) ?: 3000L) * 100
                if (row == null) {
                    dao.insertPrice(
                        PanelPriceBookEntity(
                            panel = panel, damageType = damageType, workType = w,
                            baseLaborCents = seed, samplesCount = 0,
                            source = "SEED", updatedAt = Instant.now()
                        )
                    )
                }
                seed
            }
            labor += base
        }
        labor = (labor * mult).toLong()

        val materials = if (damageType == "REPLACE") {
            (SEED_PART[cat] ?: 10000L) * 100
        } else {
            labor * MATERIALS_PERCENT / 100
        }

        val total = labor + materials
        val min = total * (100 - RANGE_PERCENT) / 100
        val max = total * (100 + RANGE_PERCENT) / 100
        return EstimateLine(panel, damageType, min, max, materials, min, max)
    }

    /** Обучение: факт закрытого заказа уточняет прайс (экспоненциальное среднее). */
    suspend fun learn(panel: String, damageType: String, actualCents: Long) {
        val works = DAMAGE_WORKS[damageType] ?: return
        if (works.isEmpty() || actualCents <= 0) return
        val per = actualCents / works.size
        val now = Instant.now()
        for (w in works) {
            val row = dao.findPrice(panel, damageType, w)
            if (row == null) {
                dao.insertPrice(
                    PanelPriceBookEntity(
                        panel = panel, damageType = damageType, workType = w,
                        baseLaborCents = per, samplesCount = 1,
                        source = "LEARNED", updatedAt = now
                    )
                )
            } else {
                val n = row.samplesCount + 1
                val newBase = (row.baseLaborCents * row.samplesCount + per) / n
                dao.updatePrice(
                    row.copy(baseLaborCents = newBase, samplesCount = n, source = "LEARNED", updatedAt = now)
                )
            }
        }
    }
}