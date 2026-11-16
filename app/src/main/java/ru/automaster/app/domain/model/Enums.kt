package ru.automaster.app.domain.model

enum class OrderStatus { ACCEPTED, WAITING, BODY, PREP, PAINT, ISSUE, DONE, CANCELLED }
enum class StagePhase { BODY, PREP, PAINT, ISSUE }
enum class StageStatus { PLANNED, IN_PROGRESS, COMPLETED, INTERRUPTED, CANCELLED }

enum class PostName { PREP_1, PREP_2, PAINT_1 }

enum class PostType { PREP, PAINT }
enum class PaymentType { PAYMENT, REFUND }
enum class PaymentMethod { CASH, CARD, TRANSFER }
enum class PhotoType { BEFORE, AFTER }
enum class AccrualType { PERCENT, PANEL }
enum class TaxPaymentKind { USN_TAX, IP_FIXED_CONTRIBUTIONS, IP_EXTRA_CONTRIBUTIONS }
enum class TaxRegime { USN_6 }
enum class OrderUnit { PCS, LITER, KG, METER, SET }

enum class WorkType { BODY, PREP, PAINT, MATERIAL, OTHER }
enum class WorkRole { BODY, PREP, PAINT, GENERAL }

// ===== РУССКИЕ НАЗВАНИЯ ДЛЯ UI =====

fun WorkRole.displayName(): String = when (this) {
    WorkRole.BODY -> "Кузовщик"
    WorkRole.PREP -> "Подготовщик"
    WorkRole.PAINT -> "Маляр"
    WorkRole.GENERAL -> "Общее"
}

fun WorkType.displayName(): String = when (this) {
    WorkType.BODY -> "Кузовные работы"
    WorkType.PREP -> "Подготовка"
    WorkType.PAINT -> "Покраска"
    WorkType.MATERIAL -> "Материалы"
    WorkType.OTHER -> "Прочее"
}

fun OrderStatus.displayName(): String = when (this) {
    OrderStatus.ACCEPTED -> "Принят"
    OrderStatus.WAITING -> "Ожидает"
    OrderStatus.BODY -> "Кузовные работы"
    OrderStatus.PREP -> "Подготовка"
    OrderStatus.PAINT -> "Покраска"
    OrderStatus.ISSUE -> "Готов к выдаче"
    OrderStatus.DONE -> "Выдан"
    OrderStatus.CANCELLED -> "Отменён"
}