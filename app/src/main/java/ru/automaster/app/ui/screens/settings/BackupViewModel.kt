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
import ru.automaster.app.data.backup.SecureBackupExporter
import java.io.File
import javax.inject.Inject

enum class BackupFormat { ENCRYPTED, PLAIN_ZIP, UNKNOWN }

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager,
    private val secureExporter: SecureBackupExporter
) : ViewModel() {

    private val _localBackups = MutableStateFlow<List<File>>(emptyList())
    val localBackups: StateFlow<List<File>> = _localBackups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _restartRequired = MutableStateFlow(false)
    val restartRequired: StateFlow<Boolean> = _restartRequired.asStateFlow()

    init {
        loadBackups()
    }

    fun loadBackups() {
        viewModelScope.launch(Dispatchers.IO) {
            _localBackups.value = backupManager.getLocalBackups()
        }
    }

    /** Формат по первым байтам: "AMB1" — шифрованный, "PK" — старый ZIP. */
    suspend fun detectFormat(uri: Uri): BackupFormat = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val head = ByteArray(4)
                var read = 0
                while (read < 4) {
                    val n = input.read(head, read, 4 - read)
                    if (n < 0) break
                    read += n
                }
                when {
                    read >= 4 &&
                            head[0] == 'A'.code.toByte() &&
                            head[1] == 'M'.code.toByte() &&
                            head[2] == 'B'.code.toByte() &&
                            head[3] == '1'.code.toByte() -> BackupFormat.ENCRYPTED

                    read >= 2 &&
                            head[0] == 'P'.code.toByte() &&
                            head[1] == 'K'.code.toByte() -> BackupFormat.PLAIN_ZIP

                    else -> BackupFormat.UNKNOWN
                }
            } ?: BackupFormat.UNKNOWN
        }.getOrDefault(BackupFormat.UNKNOWN)
    }

    /** Шифрованный экспорт. Пароль затирается в памяти после использования. */
    fun exportEncrypted(uri: Uri, password: CharArray) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                secureExporter.export(uri, password)
                    .onSuccess { bytes ->
                        _message.value = "Зашифрованный бэкап сохранён (${bytes / 1024} КБ)"
                    }
                    .onFailure { e ->
                        _message.value = "Ошибка: ${e.message}"
                    }
            } finally {
                password.fill(' ')
                _isLoading.value = false
                loadBackups()
            }
        }
    }

    /** Восстановление: шифрованный .amb или старый незашифрованный .zip. */
    fun restore(uri: Uri, password: CharArray?, encrypted: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = if (encrypted) {
                    secureExporter.import(uri, password ?: CharArray(0))
                } else {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                backupManager.restoreBackup(input).getOrThrow()
                            } ?: error("Не удалось открыть файл")
                        }
                    }
                }
                result
                    .onSuccess {
                        _restartRequired.value = true
                    }
                    .onFailure { e ->
                        _message.value = "Ошибка восстановления: ${e.message}"
                    }
            } finally {
                password?.fill(' ')
                _isLoading.value = false
            }
        }
    }

    fun notify(text: String) {
        _message.value = text
    }

    fun clearMessage() {
        _message.value = null
    }
}