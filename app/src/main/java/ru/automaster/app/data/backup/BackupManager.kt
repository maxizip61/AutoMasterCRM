package ru.automaster.app.data.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ru.automaster.app.data.local.AppDatabase
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase
) {
    private val dbName = "automaster_database"
    private val photosDir = File(context.filesDir, "photos")

    suspend fun createBackup(output: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
            val dbFile = context.getDatabasePath(dbName)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            ZipOutputStream(output).use { zos ->
                val manifest = JSONObject().apply {
                    put("version", 1)
                    put("createdAt", dateFormat.format(Date()))
                    put("appVersion", "1.0")
                }
                zos.putNextEntry(ZipEntry("manifest.json"))
                zos.write(manifest.toString().toByteArray())
                zos.closeEntry()

                if (dbFile.exists()) {
                    zos.putNextEntry(ZipEntry("database/$dbName"))
                    dbFile.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }

                if (photosDir.exists()) {
                    photosDir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val entryPath = "photos/${file.relativeTo(photosDir).path}"
                            zos.putNextEntry(ZipEntry(entryPath))
                            file.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(input: InputStream): Result<Unit> = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "restore_temp")
        val safetyDir = File(context.cacheDir, "restore_safety")

        try {
            if (tempDir.exists()) tempDir.deleteRecursively()
            tempDir.mkdirs()

            ZipInputStream(input).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val file = File(tempDir, entry.name)
                    if (entry.isDirectory) file.mkdirs()
                    else {
                        file.parentFile?.mkdirs()
                        file.outputStream().use { zis.copyTo(it) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val manifestFile = File(tempDir, "manifest.json")
            if (!manifestFile.exists()) throw Exception("Неверный формат: нет manifest.json")

            val extractedDb = File(tempDir, "database/$dbName")
            if (!extractedDb.exists()) throw Exception("Неверный формат: нет базы данных")

            if (safetyDir.exists()) safetyDir.deleteRecursively()
            safetyDir.mkdirs()

            val currentDb = context.getDatabasePath(dbName)
            if (currentDb.exists()) currentDb.copyTo(File(safetyDir, dbName), overwrite = true)
            if (photosDir.exists()) photosDir.copyRecursively(File(safetyDir, "photos"), overwrite = true)

            db.close()

            currentDb.delete()

            // ВОТ ЗДЕСЬ БЫЛА ОШИБКА, ТЕПЕРЬ ИСПРАВЛЕНО:
            context.getDatabasePath("$dbName-wal").delete()
            context.getDatabasePath("$dbName-shm").delete()

            extractedDb.copyTo(currentDb, overwrite = true)

            if (photosDir.exists()) photosDir.deleteRecursively()
            val tempPhotos = File(tempDir, "photos")
            if (tempPhotos.exists()) tempPhotos.copyRecursively(photosDir, overwrite = true)

            if (safetyDir.exists()) safetyDir.deleteRecursively()

            Result.success(Unit)
        } catch (e: Exception) {
            val currentDb = context.getDatabasePath(dbName)
            if (!currentDb.exists() && File(safetyDir, dbName).exists()) {
                File(safetyDir, dbName).copyTo(currentDb)
                val tempPhotos = File(safetyDir, "photos")
                if (tempPhotos.exists()) tempPhotos.copyRecursively(photosDir, overwrite = true)
            }
            Result.failure(e)
        } finally {
            if (tempDir.exists()) tempDir.deleteRecursively()
        }
    }

    fun getLocalBackups(): List<File> {
        val backupDir = context.getExternalFilesDir("backups") ?: return emptyList()
        if (!backupDir.exists()) return emptyList()
        return backupDir.listFiles { file -> file.extension == "zip" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun cleanupOldBackups(keepCount: Int) {
        val backups = getLocalBackups()
        if (backups.size > keepCount) {
            backups.drop(keepCount).forEach { it.delete() }
        }
    }
}