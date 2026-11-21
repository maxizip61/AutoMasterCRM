package ru.automaster.app.ui.screens.orders

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.automaster.app.data.local.AppDatabase
import ru.automaster.app.data.local.dao.CarDao
import ru.automaster.app.data.local.dao.ClientDao
import ru.automaster.app.data.local.dao.OrderDao
import ru.automaster.app.data.local.entity.CarEntity
import ru.automaster.app.data.local.entity.ClientEntity
import ru.automaster.app.data.local.entity.OrderEntity
import ru.automaster.app.data.storage.PhotoFileManager
import ru.automaster.app.domain.document.CarDraft
import ru.automaster.app.domain.document.DocumentScanner
import ru.automaster.app.domain.document.OcrLearningEngine
import ru.automaster.app.domain.model.OrderStatus
import java.io.File
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class AddOrderViewModel @Inject constructor(
    private val clientDao: ClientDao,
    private val carDao: CarDao,
    private val orderDao: OrderDao,
    private val db: AppDatabase,
    private val documentScanner: DocumentScanner,
    private val learningEngine: OcrLearningEngine,
    private val photoFileManager: PhotoFileManager
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() { _message.value = null }

    private val _draft = MutableStateFlow<CarDraft?>(null)
    val draft: StateFlow<CarDraft?> = _draft.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    /** Сигнал экрану: очистить форму после успешного создания заказа. */
    private val _resetForm = MutableStateFlow(0)
    val resetForm: StateFlow<Int> = _resetForm.asStateFlow()

    fun clearDraft() { _draft.value = null }

    private var stsPhotoFile: File? = null

    fun prepareStsCameraUri(): Uri {
        val file = photoFileManager.createStsImageFile()
        stsPhotoFile = file
        return photoFileManager.getUriForFile(file)
    }

    fun onStsPhotoTaken() {
        val file = stsPhotoFile ?: return
        val uri = photoFileManager.getUriForFile(file)
        scanDocument(uri)
    }

    /** Создаёт файл для снимка СТС (использует встроенная камера). */
    fun newStsFile(): File = photoFileManager.createStsImageFile()

    /** Распознаёт уже снятый файл (без FileProvider, без падений). */
    fun scanStsFile(file: File) {
        scanDocument(android.net.Uri.fromFile(file))
    }

    fun scanDocument(uri: Uri) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                documentScanner.scan(uri)
                    .onSuccess { rawDraft ->
                        val learned = learningEngine
                            .applyCorrections(rawDraft)
                            .let { learningEngine.linkToKnownCars(it) }
                        _draft.value = learned
                        _message.value = if (learned.filledCount > 0)
                            "Распознано полей: ${learned.filledCount}. Проверьте и подтвердите."
                        else "Текст не распознан. Заполните поля вручную."
                    }
                    .onFailure { _message.value = it.message ?: "Ошибка распознавания" }
            } finally {
                _isScanning.value = false
                stsPhotoFile = null
            }
        }
    }

    private fun normalizePhone(phone: String): String {
        val d = phone.filter { it.isDigit() }
        return when {
            d.length == 11 && d.startsWith("8") -> "7" + d.drop(1)
            d.length == 11 && d.startsWith("7") -> d
            d.length == 10 -> "7$d"
            else -> d
        }
    }

    private val latToCyr = mapOf(
        'A' to 'А', 'B' to 'В', 'E' to 'Е', 'K' to 'К', 'M' to 'М',
        'H' to 'Н', 'O' to 'О', 'P' to 'Р', 'C' to 'С', 'T' to 'Т',
        'Y' to 'У', 'X' to 'Х'
    )

    private fun normalizePlate(plate: String): String =
        plate.uppercase()
            .filter { it.isLetterOrDigit() }
            .map { latToCyr[it] ?: it }
            .joinToString("")

    fun createOrder(
        clientName: String,
        clientPhone: String,
        carMake: String,
        carModel: String,
        licensePlate: String,
        vin: String,
        estimateCents: Long,
        year: Int? = null,
        ocrDraft: CarDraft? = null
    ) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                runCatching {
                    require(clientName.isNotBlank()) { "Введите ФИО клиента" }
                    require(licensePlate.isNotBlank()) { "Введите госномер" }
                    require(estimateCents >= 0L) { "Смета не может быть отрицательной" }

                    val phoneKey = normalizePhone(clientPhone)
                    val plateKey = normalizePlate(licensePlate)
                    val vinKey = vin.trim().uppercase()
                    val now = Instant.now()

                    Log.i(TAG, "Поиск: phone=$phoneKey plate=$plateKey vin=$vinKey")

                    val allClients = clientDao.getAll().first()
                    val existingClient = if (phoneKey.length >= 10) {
                        allClients.firstOrNull { normalizePhone(it.phone) == phoneKey }
                    } else null
                    Log.i(TAG, "Клиент: ${existingClient?.id ?: "не найден"}")

                    val byVin = if (vinKey.isNotBlank()) carDao.getByVin(vinKey) else null
                    val existingCar = byVin ?: run {
                        carDao.search("").first().firstOrNull {
                            normalizePlate(it.licensePlateSearchIndex) == plateKey ||
                                    normalizePlate(it.licensePlate) == plateKey
                        }
                    }
                    Log.i(TAG, "Машина: ${existingCar?.id ?: "не найдена"}")

                    var clientId = 0L
                    var carId = 0L

                    db.withTransaction {
                        clientId = if (existingClient != null) {
                            existingClient.id
                        } else {
                            clientDao.insert(
                                ClientEntity(
                                    name = clientName.trim(),
                                    phone = clientPhone.trim()
                                )
                            )
                        }

                        carId = if (existingCar != null) {
                            if (existingCar.clientId != clientId ||
                                (year != null && existingCar.year == null) ||
                                existingCar.vin.isBlank()
                            ) {
                                carDao.update(
                                    existingCar.copy(
                                        clientId = clientId,
                                        year = existingCar.year ?: year,
                                        vin = existingCar.vin.ifBlank { vinKey }
                                    )
                                )
                            }
                            existingCar.id
                        } else {
                            carDao.insert(
                                CarEntity(
                                    clientId = clientId,
                                    licensePlate = licensePlate.trim().uppercase(),
                                    licensePlateSearchIndex = plateKey,
                                    vin = vinKey,
                                    make = carMake.trim(),
                                    model = carModel.trim(),
                                    year = year
                                )
                            )
                        }

                        orderDao.insert(
                            OrderEntity(
                                clientId = clientId,
                                carId = carId,
                                createdAt = now,
                                plannedDeliveryDate = now.plusSeconds(86400L * 3),
                                agreedEstimateCents = estimateCents,
                                status = OrderStatus.ACCEPTED
                            )
                        )
                    }

                    Log.i(TAG, "Записано: clientId=$clientId carId=$carId")

                    if (ocrDraft != null) {
                        learningEngine.recordLesson("PLATE", ocrDraft.licensePlate, licensePlate)
                        learningEngine.recordLesson("VIN", ocrDraft.vin, vin)
                        learningEngine.recordLesson("MAKE", ocrDraft.make, carMake)
                        learningEngine.recordLesson("MODEL", ocrDraft.model, carModel)
                        Log.i(TAG, "Уроки записаны: ${ocrDraft.filledCount} полей")
                    }

                    _message.value = when {
                        existingClient != null && existingCar != null ->
                            "✓ Клиент и машина найдены в базе — заказ добавлен"
                        existingClient != null ->
                            "✓ Клиент найден, создана новая машина"
                        existingCar != null ->
                            "✓ Машина найдена, создан новый клиент"
                        else -> "✓ Заказ создан: новый клиент и машина"
                    }

                    // Сигнал экрану: очистить форму после успеха
                    _resetForm.value = _resetForm.value + 1
                }.onFailure { e ->
                    Log.e(TAG, "Создание заказа провалено", e)
                    _message.value = "✗ ${e.message ?: "Не удалось создать заказ"}"
                }
            }
        }
    }

    private companion object {
        const val TAG = "AddOrder"
    }
}