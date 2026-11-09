package ru.automaster.app.domain.usecase

import ru.automaster.app.data.local.dao.PaymentDao
import ru.automaster.app.data.repository.SettingsRepository
import ru.automaster.app.domain.model.TaxReport
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class TaxCalculationUseCase @Inject constructor(
    private val paymentDao: PaymentDao,
    private val settingsRepository: SettingsRepository
) {
    suspend fun execute(from: Instant, to: Instant): TaxReport {
        val config = settingsRepository.getTaxConfig()
        val receipts = paymentDao.getReceiptsInRange(from, to) ?: 0L
        val refunds = paymentDao.getRefundsInRange(from, to) ?: 0L
        val revenue = maxOf(0L, receipts - refunds)

        val taxGross = revenue * config.usnRateBasisPoints / 10000
        val periodDays = ChronoUnit.DAYS.between(from, to).coerceAtLeast(1)
        val yearDays = 365L

        val fixedPart = config.ipFixedContributionsAnnualCents * periodDays / yearDays
        val threshold = config.ipExtraContributionsThresholdCents * periodDays / yearDays
        val extraPart = maxOf(0L, revenue - threshold) * config.ipExtraContributionsRateBasisPoints / 10000
        val contributions = fixedPart + extraPart

        val offset = minOf(taxGross * config.contributionsOffsetCapBps / 10000, contributions)
        val taxToPay = taxGross - offset

        return TaxReport(
            receiptsCents = receipts, refundsCents = refunds, revenueCents = revenue,
            taxGrossCents = taxGross, contributionsCents = contributions,
            offsetCents = offset, taxToPayCents = taxToPay
        )
    }
}