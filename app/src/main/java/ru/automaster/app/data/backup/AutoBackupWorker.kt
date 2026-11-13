package ru.automaster.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.automaster.app.data.backup.BackupManager
import ru.automaster.app.data.settings.SettingsRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupManager: BackupManager,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val backupDir = applicationContext.getExternalFilesDir("backups") ?: return@withContext Result.failure()
            if (!backupDir.exists()) backupDir.mkdirs()

            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "auto_backup_$dateStr.zip")

            backupFile.outputStream().use { output ->
                val result = backupManager.createBackup(output)
                if (result.isFailure) throw result.exceptionOrNull() ?: Exception("Backup failed")
            }

            // Получаем настройки, чтобы узнать, сколько копий хранить
            // (В реальном проекте тут нужен suspend метод получения настроек,
            // но так как SettingsRepository возвращает Flow, для простоты пока используем 7)
            backupManager.cleanupOldBackups(7)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}