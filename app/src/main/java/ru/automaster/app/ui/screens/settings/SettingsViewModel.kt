package ru.automaster.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.automaster.app.data.local.dao.PostDao
import ru.automaster.app.data.security.AppLockManager
import ru.automaster.app.data.settings.SettingsRepository
import ru.automaster.app.domain.model.AppSettings
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val appLockManager: AppLockManager,
    private val postDao: PostDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // Получаем список всех постов
    val posts = postDao.getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Получаем текущие настройки
    val settings = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    fun renamePost(id: Long, newName: String) {
        viewModelScope.launch {
            postDao.rename(id, newName)
        }
    }

    fun togglePostActive(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            postDao.setActive(id, isActive)
        }
    }

    fun updateFinancials(taxRate: Int, insuranceCents: Long, fixedCostsCents: Long) {
        viewModelScope.launch {
            settingsRepository.update { current ->
                current.copy(
                    taxRatePercent = taxRate,
                    insuranceContributionsCents = insuranceCents,
                    monthlyFixedCostsCents = fixedCostsCents
                )
            }
        }
    }
}