package ru.automaster.app.domain.model

data class TaxConfig(
    val regime: TaxRegime = TaxRegime.USN_6,
    val usnRateBasisPoints: Int = 600,
    val ipFixedContributionsAnnualCents: Long = 4_950_000L,
    val ipExtraContributionsRateBasisPoints: Int = 100,
    val ipExtraContributionsThresholdCents: Long = 30_000_000L,
    val contributionsOffsetCapBps: Int = 10_000,
    val initialSalaryDebtCents: Long = 0L,
    val initialTaxDebtCents: Long = 0L
)