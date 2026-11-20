package ru.automaster.app.domain.document

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Распознавание текста документа на устройстве (ML Kit, bundled-модель).
 * Работает БЕЗ интернета и без Google Play Services.
 * Ни один байт изображения не покидает телефон.
 *
 * Результат — ЧЕРНОВИК. В бизнес-таблицы пишет только человек после подтверждения.
 */
@Singleton
class DocumentScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val recognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Распознаёт документ по Uri (из камеры или галереи).
     * Не бросает исключений — возвращает Result с черновиком или ошибкой.
     */
    suspend fun scan(uri: Uri): Result<CarDraft> =
        withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Распознавание начато: $uri")
                val image = InputImage.fromFilePath(context, uri)
                val rawText = processImage(image)
                val draft = StsParser.parse(rawText)
                Log.i(
                    TAG,
                    "Распознавание ОК: полей ${draft.filledCount}, символов ${rawText.length}"
                )
                Result.success(draft)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Распознавание провалено", e)
                Result.failure(
                    IllegalStateException(
                        "Не удалось распознать документ: ${e.message ?: "неизвестная ошибка"}"
                    )
                )
            }
        }

    /**
     * Обёртка ML Kit Task в корутину — без зависимости coroutines-play-services.
     *
     * Task из Google Play Services API НЕ поддерживает cancel(), поэтому
     * при отмене корутины результат просто игнорируется через cont.isActive.
     * Само распознавание длится около секунды и завершится в фоне.
     */
    private suspend fun processImage(image: InputImage): String =
        suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    if (cont.isActive) cont.resume(result.text)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "ML Kit вернул ошибку: ${e.message}")
                    if (cont.isActive) cont.resume("")
                }
        }

    private companion object {
        const val TAG = "DocumentScanner"
    }
}