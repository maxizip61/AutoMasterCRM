package ru.automaster.app.domain.model

/** Зоны кузова для разметки повреждений (вид сверху). */
object CarPanels {
    const val FRONT_BUMPER = "FRONT_BUMPER"
    const val HOOD = "HOOD"
    const val FENDER_FL = "FENDER_FL"
    const val FENDER_FR = "FENDER_FR"
    const val DOOR_FL = "DOOR_FL"
    const val DOOR_FR = "DOOR_FR"
    const val DOOR_RL = "DOOR_RL"
    const val DOOR_RR = "DOOR_RR"
    const val FENDER_RL = "FENDER_RL"
    const val FENDER_RR = "FENDER_RR"
    const val ROOF = "ROOF"
    const val TRUNK = "TRUNK"
    const val REAR_BUMPER = "REAR_BUMPER"

    val labels = mapOf(
        FRONT_BUMPER to "Передний бампер", HOOD to "Капот",
        FENDER_FL to "Крыло переднее левое", FENDER_FR to "Крыло переднее правое",
        DOOR_FL to "Дверь передняя левая", DOOR_FR to "Дверь передняя правая",
        DOOR_RL to "Дверь задняя левая", DOOR_RR to "Дверь задняя правая",
        FENDER_RL to "Крыло заднее левое", FENDER_RR to "Крыло заднее правое",
        ROOF to "Крыша", TRUNK to "Багажник", REAR_BUMPER to "Задний бампер"
    )

    val short = mapOf(
        FRONT_BUMPER to "ПБ", HOOD to "Капот",
        FENDER_FL to "КрПЛ", FENDER_FR to "КрПП",
        DOOR_FL to "ДвПЛ", DOOR_FR to "ДвПП",
        DOOR_RL to "ДвЗЛ", DOOR_RR to "ДвЗП",
        FENDER_RL to "КрЗЛ", FENDER_RR to "КрЗП",
        ROOF to "Крыша", TRUNK to "Багаж", REAR_BUMPER to "ЗБ"
    )

    /** Категория для стартового прайса. */
    val category = mapOf(
        FRONT_BUMPER to "BUMPER", REAR_BUMPER to "BUMPER", HOOD to "HOOD",
        FENDER_FL to "FENDER", FENDER_FR to "FENDER",
        FENDER_RL to "FENDER", FENDER_RR to "FENDER",
        DOOR_FL to "DOOR", DOOR_FR to "DOOR", DOOR_RL to "DOOR", DOOR_RR to "DOOR",
        ROOF to "ROOF", TRUNK to "TRUNK"
    )

    /** Раскладка вид-сверху: null = стекло/пропуск. */
    val layout: List<List<String?>> = listOf(
        listOf(FRONT_BUMPER),
        listOf(HOOD),
        listOf(FENDER_FL, null, FENDER_FR),
        listOf(DOOR_FL, ROOF, DOOR_FR),
        listOf(DOOR_RL, null, DOOR_RR),
        listOf(FENDER_RL, TRUNK, FENDER_RR),
        listOf(REAR_BUMPER)
    )

    val damageTypes = listOf("SCRATCH", "DENT", "CRACK", "REPLACE", "PAINT")
    val damageLabels = mapOf(
        "SCRATCH" to "Царапина", "DENT" to "Вмятина", "CRACK" to "Трещина",
        "REPLACE" to "Замена", "PAINT" to "Покраска"
    )

    val carClasses = listOf("ECONOMY", "COMFORT", "BUSINESS", "PREMIUM", "SUV")
    val carClassLabels = mapOf(
        "ECONOMY" to "Эконом", "COMFORT" to "Комфорт", "BUSINESS" to "Бизнес",
        "PREMIUM" to "Премиум", "SUV" to "Кроссовер"
    )
}