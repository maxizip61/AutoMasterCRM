package ru.automaster.app.domain.model

/**
 * Специализация мастера. Хранится в EmployeeEntity.position точной строкой.
 * Позволяет не спрашивать роль при назначении на заказ и при начислении ЗП.
 */
object EmployeeSpecialty {

    const val BODY = "Кузовщик"
    const val PREP = "Подготовщик"
    const val PAINT = "Маляр"

    /** Порядок для чипов в UI. */
    val all = listOf(BODY, PREP, PAINT)

    /** Кузовщик работает процентом, остальные — за деталь. */
    fun compensationTypeFor(position: String): String =
        if (position == BODY) "PERCENT" else "PANEL"

    /** Подпись поля ставки. */
    fun rateLabelFor(position: String): String =
        if (position == BODY) "Процент от кузовных работ, %" else "Цена за деталь, ₽"

    /**
     * Определяет роль по должности. Сначала точное совпадение,
     * затем ключевые слова (для старых записей), затем тип оплаты.
     */
    fun roleOf(position: String, compensationType: String): WorkRole {
        val p = position.trim().lowercase()
        return when {
            p == BODY.lowercase() -> WorkRole.BODY
            p == PREP.lowercase() -> WorkRole.PREP
            p == PAINT.lowercase() -> WorkRole.PAINT

            p.contains("кузов") || p.contains("рихт") || p.contains("жестян") -> WorkRole.BODY
            p.contains("подгот") || p.contains("шпакл") || p.contains("шлиф") -> WorkRole.PREP
            p.contains("маляр") || p.contains("покрас") || p.contains("колор") -> WorkRole.PAINT

            compensationType == "PERCENT" -> WorkRole.BODY
            else -> WorkRole.GENERAL
        }
    }

    /** true, если должность распознана — можно назначать без вопросов. */
    fun isKnown(position: String, compensationType: String): Boolean =
        roleOf(position, compensationType) != WorkRole.GENERAL

    /** Фаза работ выводится из специализации мастера. */
    fun phaseOf(role: WorkRole): StagePhase? = when (role) {
        WorkRole.BODY -> StagePhase.BODY
        WorkRole.PREP -> StagePhase.PREP
        WorkRole.PAINT -> StagePhase.PAINT
        WorkRole.GENERAL -> null
    }

    /** Совместимость мастера и поста. */
    fun canWorkOn(role: WorkRole, postType: PostType): Boolean = when (postType) {
        PostType.PAINT -> role == WorkRole.PAINT
        PostType.PREP -> role == WorkRole.BODY || role == WorkRole.PREP
    }

    fun incompatibilityReason(role: WorkRole, postType: PostType): String =
        when {
            postType == PostType.PAINT && role != WorkRole.PAINT ->
                "В покрасочную камеру можно назначить только маляра"
            postType == PostType.PREP && role == WorkRole.PAINT ->
                "Маляр работает только в покрасочной камере"
            else -> "Специализация мастера не определена. Уточните её в настройках."
        }
}