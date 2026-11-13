package ru.automaster.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.automaster.app.domain.model.AppSettings
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val TAX_RATE = intPreferencesKey("tax_rate")
        val INSURANCE = longPreferencesKey("insurance")
        val MONTHLY_FIXED = longPreferencesKey("monthly_fixed")
        val WORK_START = intPreferencesKey("work_start")
        val WORK_END = intPreferencesKey("work_end")
        val WORKING_DAYS = stringSetPreferencesKey("working_days")
        val CURRENCY = stringPreferencesKey("currency")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val AUTO_BACKUP_KEEP = intPreferencesKey("auto_backup_keep")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val LOCK_TIMEOUT = intPreferencesKey("lock_timeout")
        val USE_BIOMETRIC = booleanPreferencesKey("use_biometric")
    }

    override val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            taxRatePercent = prefs[Keys.TAX_RATE] ?: 6,
            insuranceContributionsCents = prefs[Keys.INSURANCE] ?: 0L,
            monthlyFixedCostsCents = prefs[Keys.MONTHLY_FIXED] ?: 0L,
            workDayStartHour = prefs[Keys.WORK_START] ?: 9,
            workDayEndHour = prefs[Keys.WORK_END] ?: 19,
            workingDays = prefs[Keys.WORKING_DAYS]?.map { it.toInt() }?.toSet() ?: setOf(1, 2, 3, 4, 5, 6),
            currencySymbol = prefs[Keys.CURRENCY] ?: "₽",
            autoBackupEnabled = prefs[Keys.AUTO_BACKUP_ENABLED] ?: true,
            autoBackupKeepCount = prefs[Keys.AUTO_BACKUP_KEEP] ?: 7,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            lockTimeoutSeconds = prefs[Keys.LOCK_TIMEOUT] ?: 60,
            useBiometric = prefs[Keys.USE_BIOMETRIC] ?: true
        )
    }

    override suspend fun update(block: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = AppSettings(
                taxRatePercent = prefs[Keys.TAX_RATE] ?: 6,
                insuranceContributionsCents = prefs[Keys.INSURANCE] ?: 0L,
                monthlyFixedCostsCents = prefs[Keys.MONTHLY_FIXED] ?: 0L,
                workDayStartHour = prefs[Keys.WORK_START] ?: 9,
                workDayEndHour = prefs[Keys.WORK_END] ?: 19,
                workingDays = prefs[Keys.WORKING_DAYS]?.map { it.toInt() }?.toSet() ?: setOf(1, 2, 3, 4, 5, 6),
                currencySymbol = prefs[Keys.CURRENCY] ?: "₽",
                autoBackupEnabled = prefs[Keys.AUTO_BACKUP_ENABLED] ?: true,
                autoBackupKeepCount = prefs[Keys.AUTO_BACKUP_KEEP] ?: 7,
                appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
                lockTimeoutSeconds = prefs[Keys.LOCK_TIMEOUT] ?: 60,
                useBiometric = prefs[Keys.USE_BIOMETRIC] ?: true
            )
            val updated = block(current)

            prefs[Keys.TAX_RATE] = updated.taxRatePercent
            prefs[Keys.INSURANCE] = updated.insuranceContributionsCents
            prefs[Keys.MONTHLY_FIXED] = updated.monthlyFixedCostsCents
            prefs[Keys.WORK_START] = updated.workDayStartHour
            prefs[Keys.WORK_END] = updated.workDayEndHour
            prefs[Keys.WORKING_DAYS] = updated.workingDays.map { it.toString() }.toSet()
            prefs[Keys.CURRENCY] = updated.currencySymbol
            prefs[Keys.AUTO_BACKUP_ENABLED] = updated.autoBackupEnabled
            prefs[Keys.AUTO_BACKUP_KEEP] = updated.autoBackupKeepCount
            prefs[Keys.APP_LOCK_ENABLED] = updated.appLockEnabled
            prefs[Keys.LOCK_TIMEOUT] = updated.lockTimeoutSeconds
            prefs[Keys.USE_BIOMETRIC] = updated.useBiometric
        }
    }
}