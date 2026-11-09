package ru.automaster.app.data.repository

import kotlinx.coroutines.flow.Flow
import ru.automaster.app.domain.model.TaxConfig

interface SettingsRepository {
    val taxConfigFlow: Flow<TaxConfig>
    suspend fun getTaxConfig(): TaxConfig
    suspend fun saveTaxConfig(config: TaxConfig)
}