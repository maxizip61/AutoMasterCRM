package ru.automaster.app.ui.screens.estimate

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.automaster.app.data.local.dao.EstimateDao
import ru.automaster.app.data.local.entity.DamageAssessmentEntity
import ru.automaster.app.data.local.entity.DamageItemEntity
import ru.automaster.app.domain.document.EstimateEngine
import ru.automaster.app.domain.document.EstimateLine
import ru.automaster.app.domain.model.CarPanels
import java.io.File
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class EstimateViewModel @Inject constructor(
    private val dao: EstimateDao,
    private val engine: EstimateEngine,
    @ApplicationContext private val context: Context
) : ViewModel() {

    var carMake = MutableStateFlow("")
    var carModel = MutableStateFlow("")
    private val _carClass = MutableStateFlow("COMFORT")
    val carClass: StateFlow<String> = _carClass.asStateFlow()

    private val _selected = MutableStateFlow<Map<String, String>>(emptyMap())
    val selected: StateFlow<Map<String, String>> = _selected.asStateFlow()

    private val _lines = MutableStateFlow<List<EstimateLine>>(emptyList())
    val lines: StateFlow<List<EstimateLine>> = _lines.asStateFlow()

    private val _photos = MutableStateFlow<List<String>>(emptyList())
    val photos: StateFlow<List<String>> = _photos.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    fun clearMessage() { _message.value = null }

    fun setClass(c: String) { _carClass.value = c; recalc() }

    fun togglePanel(panel: String) {
        val cur = _selected.value.toMutableMap()
        if (cur.containsKey(panel)) cur.remove(panel) else cur[panel] = "DENT"
        _selected.value = cur
        recalc()
    }

    fun setDamage(panel: String, type: String) {
        val cur = _selected.value.toMutableMap()
        if (cur.containsKey(panel)) cur[panel] = type
        _selected.value = cur
        recalc()
    }

    fun addPhoto(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val dir = File(context.filesDir, "estimates")
                if (!dir.exists()) dir.mkdirs()
                val f = File(dir, "est_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    f.outputStream().use { input.copyTo(it) }
                }
                _photos.value = _photos.value + f.absolutePath
            }.onFailure { _message.value = "Не удалось прикрепить фото" }
        }
    }

    private fun recalc() {
        viewModelScope.launch {
            val sel = _selected.value
            val cls = _carClass.value
            _lines.value = sel.map { (panel, type) -> engine.estimateItem(panel, type, cls) }
        }
    }

    fun save() {
        viewModelScope.launch {
            runCatching {
                val ls = _lines.value
                require(ls.isNotEmpty()) { "Отметьте хотя бы одну зону повреждения" }
                val now = Instant.now()
                val min = ls.sumOf { it.totalMinCents }
                val max = ls.sumOf { it.totalMaxCents }
                val id = dao.insertAssessment(
                    DamageAssessmentEntity(
                        carMake = carMake.value.trim(),
                        carModel = carModel.value.trim(),
                        carClass = _carClass.value,
                        source = "LOCAL", status = "DRAFT",
                        totalMinCents = min, totalMaxCents = max,
                        photoPaths = _photos.value.joinToString(";"),
                        createdAt = now
                    )
                )
                dao.clearItems(id)
                ls.forEach { l ->
                    dao.insertItem(
                        DamageItemEntity(
                            assessmentId = id, panel = l.panel, damageType = l.damageType,
                            laborMinCents = l.laborMinCents, laborMaxCents = l.laborMaxCents,
                            materialsCents = l.materialsCents,
                            totalMinCents = l.totalMinCents, totalMaxCents = l.totalMaxCents,
                            createdAt = now
                        )
                    )
                }
                _message.value = "Оценка сохранена (черновик)"
            }.onFailure { _message.value = it.message ?: "Ошибка сохранения" }
        }
    }

    fun reset() {
        _selected.value = emptyMap()
        _lines.value = emptyList()
        _photos.value = emptyList()
        carMake.value = ""
        carModel.value = ""
    }
}