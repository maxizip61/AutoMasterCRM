package ru.automaster.app.ui.screens.payroll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.automaster.app.data.local.dao.EmployeeDao
import ru.automaster.app.data.local.dao.OrderEmployeeAccrualDao
import ru.automaster.app.data.local.entity.EmployeeEntity
import javax.inject.Inject

data class EmployeePayroll(
    val employee: EmployeeEntity,
    val totalAccruedCents: Long
)

@HiltViewModel
class PayrollViewModel @Inject constructor(
    private val employeeDao: EmployeeDao,
    private val accrualDao: OrderEmployeeAccrualDao
) : ViewModel() {

    // Собираем список сотрудников и их начисления
    val payrollList: StateFlow<List<EmployeePayroll>> = employeeDao.getActive()
        .map { employees ->
            employees.map { emp ->
                val accrued = accrualDao.getTotalAccruedForEmployee(emp.id) ?: 0L
                EmployeePayroll(emp, accrued)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addEmployee(name: String, position: String, type: String, rate: Long) {
        viewModelScope.launch {
            employeeDao.insert(
                EmployeeEntity(
                    fullName = name,
                    position = position,
                    archived = false,
                    compensationType = type,
                    rateValue = rate
                )
            )
        }
    }
}