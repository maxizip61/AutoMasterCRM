package ru.automaster.app.domain.document

/**
 * Черновик данных автомобиля, извлечённых из фото документа.
 * ВСЕГДА требует подтверждения владельцем перед записью в БД.
 * ИИ/OCR не пишет в бизнес-таблицы напрямую.
 */
data class CarDraft(
    val licensePlate: String? = null,
    val vin: String? = null,
    val make: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val rawText: String = ""
) {
    /** Сколько полей удалось извлечь — для показа уверенности в UI. */
    val filledCount: Int
        get() = listOf(licensePlate, vin, make, model, year?.toString())
            .count { !it.isNullOrBlank() }
}

object StsParser {

    /**
     * Латинские буквы, визуально идентичные кириллическим,
     * разрешённым в госномерах РФ. OCR всегда возвращает латиницу.
     */
    private val LAT_TO_CYR = mapOf(
        'A' to 'А', 'B' to 'В', 'E' to 'Е', 'K' to 'К', 'M' to 'М',
        'H' to 'Н', 'O' to 'О', 'P' to 'Р', 'C' to 'С', 'T' to 'Т',
        'Y' to 'У', 'X' to 'Х'
    )

    /** Формат госномера РФ: буква, 3 цифры, 2 буквы, 2-3 цифры региона. */
    private val PLATE_REGEX = Regex("""([ABEKMHOPCTYX])\s*(\d{3})\s*([ABEKMHOPCTYX]{2})\s*(\d{2,3})""")

    /** VIN: 17 символов, латиница без I, O, Q + цифры. */
    private val VIN_REGEX = Regex("""\b[A-HJ-NPR-Z0-9]{17}\b""")

    private val YEAR_REGEX = Regex("""\b(19[5-9]\d|20[0-4]\d)\b""")

    private val KNOWN_MAKES = listOf(
        "VOLKSWAGEN", "TOYOTA", "BMW", "MERCEDES", "AUDI", "SKODA", "RENAULT",
        "NISSAN", "HYUNDAI", "KIA", "MAZDA", "HONDA", "FORD", "OPEL", "PEUGEOT",
        "CITROEN", "MITSUBISHI", "SUZUKI", "SUBARU", "LEXUS", "INFINITI", "VOLVO",
        "CHEVROLET", "DAEWOO", "CHERY", "GEELY", "HAVAL", "EXEED", "OMODA",
        "LADA", "UAZ", "GAZ", "MOSKVICH"
    )

    /**
     * Разбирает распознанный текст документа.
     * Не бросает исключений: при неудаче возвращает черновик с rawText,
     * чтобы владелец мог заполнить вручную, видя исходный текст.
     */
    fun parse(rawText: String): CarDraft {
        if (rawText.isBlank()) return CarDraft(rawText = rawText)

        val upper = rawText.uppercase()
        val compact = upper.replace(Regex("""[\s\-–—]"""), "")

        return CarDraft(
            licensePlate = findPlate(compact) ?: findPlate(upper),
            vin = findVin(compact),
            make = findMake(upper),
            model = null, // модель надёжно не отделяется от марки — вводится вручную
            year = findYear(upper),
            rawText = rawText
        )
    }

    private fun findPlate(text: String): String? {
        val match = PLATE_REGEX.find(text) ?: return null
        val (letter, digits, letters, region) = match.destructured
        val cyr = (letter + letters).map { LAT_TO_CYR[it] ?: it }.joinToString("")
        return "${cyr[0]}$digits${cyr[1]}${cyr[2]}$region"
    }

    private fun findVin(text: String): String? {
        val candidate = VIN_REGEX.find(text)?.value ?: return null
        // VIN не может состоять только из цифр и должен содержать буквы
        return if (candidate.any { it.isLetter() }) candidate else null
    }

    private fun findMake(upperText: String): String? =
        KNOWN_MAKES.firstOrNull { upperText.contains(it) }
            ?.lowercase()
            ?.replaceFirstChar { it.uppercase() }

    private fun findYear(upperText: String): Int? =
        YEAR_REGEX.findAll(upperText)
            .mapNotNull { it.value.toIntOrNull() }
            .maxOrNull()   // год выпуска обычно последний из упомянутых
}