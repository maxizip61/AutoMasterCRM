package ru.automaster.app.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.automaster.app.data.backup.BackupManager
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _localBackups = MutableStateFlow<List<File>>(emptyList())
    val localBackups: StateFlow<List<File>> = _localBackups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadBackups()
    }

    fun loadBackups() {
        viewModelScope.launch(Dispatchers.IO) {
            _localBackups.value = backupManager.getLocalBackups()
        }
    }

    fun createBackupToUri(uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        val result = backupManager.createBackup(output)
                        if (result.isFailure) throw result.exceptionOrNull() ?: Exception("Ошибка")
                    } ?: throw Exception("Не удалось открыть файл")
                }
                _message.value = "Бэкап успешно создан!"
                onComplete(true)
            } catch (e: Exception) {
                _message.value = "Ошибка: ${e.message}"
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun restoreFromUri(uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val result = backupManager.restoreBackup(input)
                        if (result.isFailure) throw result.exceptionOrNull() ?: Exception("Ошибка")
                    } ?: throw Exception("Не удалось открыть файл")
                }
                _message.value = "База данных восстановлена! Требуется перезапуск."
                onComplete(true)
            } catch (e: Exception) {
                _message.value = "Ошибка восстановления: ${e.message}"
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}