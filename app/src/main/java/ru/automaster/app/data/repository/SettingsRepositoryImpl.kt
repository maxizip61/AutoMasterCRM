package ru.automaster.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import ru.automaster.app.domain.model.TaxConfig
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor() : SettingsRepository {
    private val _taxConfig = MutableStateFlow(TaxConfig())
    override val taxConfigFlow: Flow<TaxConfig> = _taxConfig

    override suspend fun getTaxConfig(): TaxConfig = _taxConfig.value

    override suspend fun saveTaxConfig(config: TaxConfig) {
        _taxConfig.value = config
    }
}