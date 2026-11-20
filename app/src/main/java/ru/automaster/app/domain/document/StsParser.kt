package ru.automaster.app.domain.document

/**
 * Черновик данных автомобиля, извлечённых из фото документа.
 * ВСЕГДА требует подтверждения владельцем перед записью в БД.
 */
data class CarDraft(
    val licensePlate: String? = null,
    val vin: String? = null,
    val make: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val rawText: String = ""
) {
    val filledCount: Int
        get() = listOf(licensePlate, vin, make, model, year?.toString())
            .count { !it.isNullOrBlank() }
}

object StsParser {

    /** Латинские буквы, визуально идентичные кириллическим в номерах РФ. */
    private val LAT_TO_CYR = mapOf(
        'A' to 'А', 'B' to 'В', 'E' to 'Е', 'K' to 'К', 'M' to 'М',
        'H' to 'Н', 'O' to 'О', 'P' to 'Р', 'C' to 'С', 'T' to 'Т',
        'Y' to 'У', 'X' to 'Х'
    )

    /** Допустимые символы VIN: по стандарту I, O, Q исключены. */
    private val VIN_CHARS = "ABCDEFGHJKLMNPRSTUVWXYZ0123456789".toSet()

    /** Ровно 17 букв/цифр в пределах одной строки. */
    private val VIN_TOKEN = Regex("""\b[A-Z0-9]{17}\b""")

    /** Госномер РФ: буква, 3 цифры, 2 буквы, 2-3 цифры региона. */
    private val PLATE_REGEX =
        Regex("""([ABEKMHOPCTYX])\s?(\d{3})\s?([ABEKMHOPCTYX]{2})\s?(\d{2,3})\b""")

    private val YEAR_REGEX = Regex("""\b(19[5-9]\d|20[0-4]\d)\b""")

    private val KNOWN_MAKES = listOf(
        "VOLKSWAGEN", "MERCEDES", "MITSUBISHI", "HYUNDAI", "TOYOTA",
        "NISSAN", "RENAULT", "PEUGEOT", "CITROEN", "CHEVROLET",
        "SKODA", "AUDI", "BMW", "KIA", "MAZDA", "HONDA", "FORD", "OPEL",
        "SUZUKI", "SUBARU", "LEXUS", "INFINITI", "VOLVO", "DAEWOO",
        "CHERY", "GEELY", "HAVAL", "EXEED", "OMODA",
        "LADA", "UAZ", "GAZ", "MOSKVICH"
    )

    /** OCR-огрызки подписей полей СТС — чтобы не принять их за модель. */
    private val LABEL_FRAGMENTS = listOf(
        "MAPKA", "MOAEB", "МАРКА", "МОДЕЛЬ", "TUN", "ТИП", "KATEGOR",
        "B5INYSK", "ВЫПУСК", "WASSI", "KY3OB", "КУЗОВ", "UBET", "ЦВЕТ",
        "ODOBREN", "EKOLOG", "PACPORT", "ПАСПОРТ", "MACCA", "МАССА",
        "TEXH", "CERTIFICAT", "CVIDETEL", "СВИДЕТ", "ROSSI", "РОССИ",
        "HOMEP", "НОМЕР", "VIN", "OTCYTCTB", "ОТСУТСТВ", "ABCD"
    )

    fun parse(rawText: String): CarDraft {
        if (rawText.isBlank()) return CarDraft(rawText = rawText)

        val lines = rawText.lines()
            .map { it.uppercase().trim() }
            .filter { it.isNotEmpty() }

        val make = findMake(lines)

        return CarDraft(
            licensePlate = findPlate(lines),
            vin = findVin(lines),
            make = make?.let { prettify(it) },
            model = findModel(lines, make)?.let { prettify(it) },
            year = findYear(lines),
            rawText = rawText
        )
    }

    /** Госномер ищем построчно: в СТС он лежит отдельной строкой. */
    private fun findPlate(lines: List<String>): String? {
        for (line in lines) {
            val m = PLATE_REGEX.find(line) ?: continue
            val (letter, digits, letters, region) = m.destructured
            val cyr = (letter + letters).map { LAT_TO_CYR[it] ?: it }.joinToString("")
            return "${cyr[0]}$digits${cyr[1]}${cyr[2]}$region"
        }
        return null
    }

    /**
     * VIN ищем СТРОГО построчно.
     *
     * Раньше искал в тексте без пробелов — строки слипались и «VIN»
     * собирался из хвостов про массу (KT997237684524119 из теста).
     *
     * Эвристика: номер кузова в СТС дублирует VIN, поэтому при равном
     * числе вхождений берём ПОСЛЕДНЕЕ — на практике оно читается чище.
     */
    private fun findVin(lines: List<String>): String? {
        val candidates = lines.mapNotNull { line ->
            VIN_TOKEN.findAll(line)
                .mapNotNull { sanitizeVin(it.value) }
                .firstOrNull()
        }
        if (candidates.isEmpty()) return null
        val counts = candidates.groupingBy { it }.eachCount()
        val max = counts.values.max()
        return candidates.lastOrNull { counts[it] == max }
    }

    /** OCR часто путает O→0, I→1: в VIN таких букв нет, правим смело. */
    private fun sanitizeVin(token: String): String? {
        if (token.length != 17) return null
        val fixed = token.map { ch ->
            when (ch) {
                'O' -> '0'
                'I' -> '1'
                'Q' -> '0'
                else -> ch
            }
        }.joinToString("")
        if (!fixed.all { it in VIN_CHARS }) return null
        if (!fixed.any { it.isLetter() } || !fixed.any { it.isDigit() }) return null
        return fixed
    }

    private fun findMake(lines: List<String>): String? =
        KNOWN_MAKES.firstOrNull { make -> lines.any { it.contains(make) } }

    /**
     * Модель: хвост строки марки («HYUNDAI PALISADE» → «PALISADE»),
     * иначе следующая строка, если она не подпись поля.
     */
    private fun findModel(lines: List<String>, make: String?): String? {
        if (make == null) return null
        val idx = lines.indexOfFirst { it.contains(make) }
        if (idx < 0) return null

        val rest = lines[idx].substringAfter(make, "").trim()
        if (isModelCandidate(rest)) return rest

        for (i in (idx + 1)..minOf(idx + 2, lines.lastIndex)) {
            val next = lines[i]
            if (next.contains(make)) continue
            if (isModelCandidate(next)) return next
        }
        return null
    }

    private fun isModelCandidate(s: String): Boolean {
        if (s.length < 2 || s.length > 24) return false
        if (s.all { it.isDigit() }) return false
        if (s.any { it !in 'A'..'Z' && it !in '0'..'9' && it != ' ' && it != '-' && it != '.' }) {
            return false
        }
        if (LABEL_FRAGMENTS.any { s.contains(it) }) return false
        if (KNOWN_MAKES.any { s.contains(it) }) return false
        return true
    }

    private fun findYear(lines: List<String>): Int? =
        lines.firstNotNullOfOrNull { line ->
            YEAR_REGEX.find(line)?.value?.toIntOrNull()
        }

    private fun prettify(s: String): String =
        s.lowercase().replaceFirstChar { it.uppercase() }
}