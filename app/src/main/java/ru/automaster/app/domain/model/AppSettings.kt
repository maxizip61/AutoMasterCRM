package ru.automaster.app.domain.model

data class AppSettings(
    val taxRatePercent: Int = 6,
    val insuranceContributionsCents: Long = 0L,
    val monthlyFixedCostsCents: Long = 0L,
    val workDayStartHour: Int = 9,
    val workDayEndHour: Int = 19,
    val workingDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6), // 1 - Пн, 7 - Вс
    val currencySymbol: String = "₽",
    val autoBackupEnabled: Boolean = true,
    val autoBackupKeepCount: Int = 7,
    val appLockEnabled: Boolean = false,
    val lockTimeoutSeconds: Int = 60,
    val useBiometric: Boolean = true
)