package ru.automaster.app.domain.model

enum class OrderStatus {
    ACCEPTED, WAITING, BODY, PREP, PAINT, ISSUE, DONE, CANCELLED
}

enum class StagePhase {
    BODY, PREP, PAINT, ISSUE
}

enum class StageStatus {
    PLANNED, IN_PROGRESS, COMPLETED, INTERRUPTED, CANCELLED
}

enum class PostName {
    PREP_1, PREP_2, PAINT_1
}

enum class PostType {
    PREP, PAINT
}

enum class PaymentType {
    PAYMENT, REFUND
}

enum class PaymentMethod {
    CASH, CARD, TRANSFER
}

enum class PhotoType {
    BEFORE, AFTER
}

enum class AccrualType {
    PERCENT, PANEL
}

enum class TaxPaymentKind {
    USN_TAX, IP_FIXED_CONTRIBUTIONS, IP_EXTRA_CONTRIBUTIONS
}

enum class TaxRegime {
    USN_6
}

enum class OrderUnit {
    PCS, LITER, KG, METER, SET
}