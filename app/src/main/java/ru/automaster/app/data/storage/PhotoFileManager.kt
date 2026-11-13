package ru.automaster.app.data.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoFileManager @Inject constructor(
    @ApplicationContext private val context: Context // <-- ДОБАВЛЕНА АННОТАЦИЯ
) {
    fun createImageFile(orderId: Long): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = File(context.filesDir, "photos/$orderId")
        if (!storageDir.exists()) storageDir.mkdirs()
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    fun getUriForFile(file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}