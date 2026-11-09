package ru.automaster.app.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.automaster.app.data.local.dao.CarDao
import ru.automaster.app.data.local.dao.ClientDao
import ru.automaster.app.data.local.dao.OrderDao
import ru.automaster.app.data.local.entity.CarEntity
import ru.automaster.app.data.local.entity.ClientEntity
import ru.automaster.app.data.local.entity.OrderEntity
import ru.automaster.app.domain.model.OrderStatus
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class AddOrderViewModel @Inject constructor(
    private val clientDao: ClientDao,
    private val carDao: CarDao,
    private val orderDao: OrderDao
) : ViewModel() {

    fun createOrder(
        clientName: String,
        clientPhone: String,
        carMake: String,
        carModel: String,
        licensePlate: String,
        vin: String,
        estimateCents: Long,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            // 1. Создаем клиента
            val clientId = clientDao.insert(ClientEntity(name = clientName, phone = clientPhone))

            // 2. Создаем машину
            val searchIndex = licensePlate.replace(" ", "").uppercase()
            val carId = carDao.insert(
                CarEntity(
                    clientId = clientId,
                    licensePlate = licensePlate,
                    licensePlateSearchIndex = searchIndex,
                    vin = vin.uppercase(),
                    make = carMake,
                    model = carModel
                )
            )

            // 3. Создаем заказ
            orderDao.insert(
                OrderEntity(
                    clientId = clientId,
                    carId = carId,
                    createdAt = Instant.now(),
                    plannedDeliveryDate = Instant.now().plusSeconds(86400 * 3), // +3 дня по умолчанию
                    agreedEstimateCents = estimateCents,
                    status = OrderStatus.ACCEPTED
                )
            )
            onSuccess()
        }
    }
}