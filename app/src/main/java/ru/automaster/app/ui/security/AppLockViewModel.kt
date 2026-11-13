package ru.automaster.app.ui.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.automaster.app.data.security.AppLockManager
import ru.automaster.app.data.settings.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val appLockManager: AppLockManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _useBiometric = MutableStateFlow(true)
    val useBiometric: StateFlow<Boolean> = _useBiometric.asStateFlow()

    private var lastPausedTime = 0L
    private var isCameraActive = false

    init {
        // Изначально блокируем, если PIN установлен
        _isLocked.value = appLockManager.isPinSet()

        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _useBiometric.value = settings.useBiometric
                if (!settings.appLockEnabled) {
                    _isLocked.value = false
                }
            }
        }
    }

    fun onAppBackgrounded() {
        lastPausedTime = System.currentTimeMillis()
    }

    fun onAppForegrounded(timeoutSeconds: Int) {
        if (!appLockManager.isPinSet()) return
        if (isCameraActive) return // Не блокируем, если открыта камера

        val elapsed = System.currentTimeMillis() - lastPausedTime
        if (elapsed > timeoutSeconds * 1000L) {
            _isLocked.value = true
        }
    }

    fun unlock() {
        _isLocked.value = false
    }

    fun verifyPin(pin: String): Boolean {
        return appLockManager.verifyPin(pin)
    }

    fun setCameraActive(active: Boolean) {
        isCameraActive = active
    }
}