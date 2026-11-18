package ru.automaster.app.ui.screens.payroll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.automaster.app.data.local.dao.EmployeeDao
import ru.automaster.app.data.local.dao.OrderEmployeeAccrualDao
import ru.automaster.app.data.local.entity.EmployeeEntity
import javax.inject.Inject

data class EmployeePayroll(
    val employee: EmployeeEntity,
    val totalAccruedCents: Long
)

/**
 * Только начисления и суммы к выплате.
 * Управление мастерами (добавить / изменить / в архив) переехало
 * в Настройки → Сотрудники цеха.
 */
@HiltViewModel
class PayrollViewModel @Inject constructor(
    employeeDao: EmployeeDao,
    accrualDao: OrderEmployeeAccrualDao
) : ViewModel() {

    val payrollList: StateFlow<List<EmployeePayroll>> = employeeDao.getActive()
        .map { employees ->
            employees.map { emp ->
                EmployeePayroll(
                    employee = emp,
                    totalAccruedCents = accrualDao.getTotalAccruedForEmployee(emp.id) ?: 0L
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}