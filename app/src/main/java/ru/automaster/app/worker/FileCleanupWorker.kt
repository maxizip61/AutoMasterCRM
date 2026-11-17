package ru.automaster.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ru.automaster.app.data.local.dao.FileDeletionTaskDao
import ru.automaster.app.data.local.dao.PhotoDao
import java.io.File

@HiltWorker
class FileCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val fileDeletionTaskDao: FileDeletionTaskDao,
    private val photoDao: PhotoDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val tasks = fileDeletionTaskDao.getAll()
            var hasRetry = false

            for (task in tasks) {
                val file = File(applicationContext.filesDir, task.filePath)
                if (file.exists()) {
                    if (file.delete()) {
                        fileDeletionTaskDao.delete(task)
                    } else {
                        val updatedTask = task.copy(
                            attempts = task.attempts + 1,
                            lastError = "Failed to delete file"
                        )
                        if (updatedTask.attempts >= 5) {
                            // После 5 попыток прекращаем мучить систему — оставляем запись
                            // для ручного разбора владельцем через БД.
                            fileDeletionTaskDao.update(updatedTask)
                            // Без hasRetry=true — за эту запись больше не боремся.
                        } else {
                            fileDeletionTaskDao.update(updatedTask)
                            hasRetry = true
                        }
                    }
                } else {
                    // Файла уже нет — задачу закрываем.
                    fileDeletionTaskDao.delete(task)
                }
            }

            sweepOrphanFiles()

            if (hasRetry) Result.retry() else Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun sweepOrphanFiles() {
        val dbPaths = photoDao.getAllFilePaths().toSet()
        val photosDir = File(applicationContext.filesDir, "photos")
        if (!photosDir.exists()) return

        val twoHoursAgo = System.currentTimeMillis() - (2 * 60 * 60 * 1000)

        photosDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "jpg") {
                val relativePath = file.absolutePath.substringAfter("files/")
                // Удаляем только «сиротские» файлы, которых нет в БД и которые
                // старше 2 часов — чтобы не убить pending-файл открытой камеры.
                if (!dbPaths.contains(relativePath) && file.lastModified() < twoHoursAgo) {
                    file.delete()
                }
            }
        }
    }
}