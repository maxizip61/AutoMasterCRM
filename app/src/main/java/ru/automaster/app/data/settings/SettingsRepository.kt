package ru.automaster.app.data.settings

import kotlinx.coroutines.flow.Flow
import ru.automaster.app.domain.model.AppSettings

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun update(block: (AppSettings) -> AppSettings)
}