package ru.automaster.app.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import ru.automaster.app.data.local.dao.CarDao
import ru.automaster.app.data.local.dao.ClientDao
import ru.automaster.app.data.local.dao.OrderDao
import ru.automaster.app.data.local.entity.CarEntity
import ru.automaster.app.data.local.entity.ClientEntity
import ru.automaster.app.data.local.entity.OrderEntity
import javax.inject.Inject

data class OrderListItem(
    val order: OrderEntity,
    val client: ClientEntity?,
    val car: CarEntity?
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    orderDao: OrderDao,
    clientDao: ClientDao,
    carDao: CarDao
) : ViewModel() {

    // Получаем все заказы, а к ним "приклеиваем" данные клиента и машины
    val ordersList: StateFlow<List<OrderListItem>> = orderDao.getAll()
        .map { orders ->
            orders.map { order ->
                OrderListItem(
                    order = order,
                    client = clientDao.getById(order.clientId),
                    car = carDao.getById(order.carId)
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}