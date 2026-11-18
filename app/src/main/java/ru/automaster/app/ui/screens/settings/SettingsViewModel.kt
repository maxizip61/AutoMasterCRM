package ru.automaster.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.automaster.app.data.local.dao.EmployeeDao
import ru.automaster.app.data.local.dao.PostDao
import ru.automaster.app.data.local.entity.EmployeeEntity
import ru.automaster.app.data.security.AppLockManager
import ru.automaster.app.data.settings.SettingsRepository
import ru.automaster.app.domain.model.AppSettings
import ru.automaster.app.domain.model.EmployeeSpecialty
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val appLockManager: AppLockManager,
    private val postDao: PostDao,
    private val employeeDao: EmployeeDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() { _message.value = null }

    val posts = postDao.getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val employees = employeeDao.getActive().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val archivedEmployees = employeeDao.getArchived().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val settings = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    // ---------- ПОСТЫ ----------

    fun renamePost(id: Long, newName: String) {
        viewModelScope.launch {
            runCatching { postDao.rename(id, newName) }
                .onFailure { _message.value = "Ошибка: ${it.message}" }
        }
    }

    fun togglePostActive(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            runCatching { postDao.setActive(id, isActive) }
                .onFailure { _message.value = "Ошибка: ${it.message}" }
        }
    }

    // ---------- ФИНАНСЫ ----------

    fun updateFinancials(taxRate: Int, insuranceCents: Long, fixedCostsCents: Long) {
        viewModelScope.launch {
            runCatching {
                val safeRate = taxRate.coerceIn(1, 100)
                settingsRepository.update { current ->
                    current.copy(
                        taxRatePercent = safeRate,
                        insuranceContributionsCents = insuranceCents.coerceAtLeast(0L),
                        monthlyFixedCostsCents = fixedCostsCents.coerceAtLeast(0L)
                    )
                }
                _message.value = if (safeRate != taxRate)
                    "Ставка УСН приведена к $safeRate%"
                else "Финансовые настройки сохранены"
            }.onFailure { _message.value = "Ошибка: ${it.message}" }
        }
    }

    // ---------- СОТРУДНИКИ ----------

    private fun validate(fullName: String, specialty: String, rate: Long): String? = when {
        fullName.isBlank() -> "Введите ФИО"
        specialty !in EmployeeSpecialty.all -> "Выберите специализацию"
        rate <= 0L -> "Ставка должна быть больше нуля"
        specialty == EmployeeSpecialty.BODY && rate > 100L -> "Процент не может быть больше 100"
        else -> null
    }

    /** Новый мастер. Специализация задаётся один раз — при назначении спрашиваться не будет. */
    fun addEmployee(fullName: String, specialty: String, rate: Long) {
        viewModelScope.launch {
            runCatching {
                validate(fullName, specialty, rate)?.let { error(it) }
                employeeDao.insert(
                    EmployeeEntity(
                        fullName = fullName.trim(),
                        position = specialty,
                        archived = false,
                        compensationType = EmployeeSpecialty.compensationTypeFor(specialty),
                        rateValue = rate
                    )
                )
                _message.value = "$specialty ${fullName.trim()} добавлен"
            }.onFailure { _message.value = it.message ?: "Ошибка добавления" }
        }
    }

    /**
     * Правка мастера: ФИО, специализация, ставка.
     * Прошлые начисления НЕ меняются — они хранят свою ставку на момент расчёта.
     */
    fun updateEmployee(
        original: EmployeeEntity,
        fullName: String,
        specialty: String,
        rate: Long
    ) {
        viewModelScope.launch {
            runCatching {
                validate(fullName, specialty, rate)?.let { error(it) }
                val specialtyChanged = original.position != specialty
                employeeDao.update(
                    original.copy(
                        fullName = fullName.trim(),
                        position = specialty,
                        compensationType = EmployeeSpecialty.compensationTypeFor(specialty),
                        rateValue = rate
                    )
                )
                _message.value = if (specialtyChanged)
                    "${fullName.trim()} → $specialty. Прошлые начисления сохранены."
                else "Данные ${fullName.trim()} обновлены"
            }.onFailure { _message.value = it.message ?: "Ошибка сохранения" }
        }
    }

    /**
     * Увольнение: мастер уходит в архив и исчезает из списков назначения.
     * История начислений остаётся — физическое удаление запрещено ForeignKey RESTRICT.
     */
    fun archiveEmployee(employee: EmployeeEntity) {
        viewModelScope.launch {
            runCatching {
                employeeDao.update(employee.copy(archived = true))
                _message.value = "${employee.fullName} в архиве. История ЗП сохранена."
            }.onFailure { _message.value = "Ошибка: ${it.message}" }
        }
    }

    /** Возврат из архива — если человек снова работает. */
    fun restoreEmployee(employee: EmployeeEntity) {
        viewModelScope.launch {
            runCatching {
                employeeDao.update(employee.copy(archived = false))
                _message.value = "${employee.fullName} снова в цехе"
            }.onFailure { _message.value = "Ошибка: ${it.message}" }
        }
    }
}