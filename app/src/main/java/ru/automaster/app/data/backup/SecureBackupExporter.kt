package ru.automaster.app.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.io.PushbackInputStream
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Шифрованный экспорт/импорт бэкапа через системный файловый диалог (SAF).
 *
 * Формат файла .amb:
 *   [4 байта "AMB1"][16 байт соль][12 байт IV][AES-256-GCM ciphertext + tag]
 *
 * Ключ выводится из пароля PBKDF2-HMAC-SHA256. Пароль нигде не хранится.
 * Ни один байт открытых данных не покидает устройство.
 */
@Singleton
class SecureBackupExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager
) {

    /**
     * Шифрует бэкап и пишет в выбранный Uri.
     * @return размер записанного файла в байтах.
     */
    suspend fun export(target: Uri, password: CharArray): Result<Long> =
        withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Экспорт начат: $target")
                require(password.isNotEmpty()) { "Пароль не может быть пустым" }

                val random = SecureRandom()
                val salt = ByteArray(SALT_LEN).also { random.nextBytes(it) }
                val iv = ByteArray(IV_LEN).also { random.nextBytes(it) }
                val key = deriveKey(password, salt)

                val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                    init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
                }

                val raw = context.contentResolver.openOutputStream(target, "w")
                    ?: error("Не удалось открыть файл для записи")

                val counting = CountingOutputStream(raw)
                counting.use { out ->
                    out.write(MAGIC)
                    out.write(salt)
                    out.write(iv)
                    CipherOutputStream(out, cipher).use { encrypted ->
                        backupManager.createBackup(encrypted).getOrThrow()
                    }
                }

                val bytes = counting.count
                Log.i(TAG, "Экспорт ОК: $bytes байт")
                Result.success(bytes)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Экспорт провален", e)
                Result.failure(mapError(e, "Ошибка шифрования. Попробуйте снова."))
            }
        }

    /**
     * Читает зашифрованный бэкап и восстанавливает его.
     * После успеха вызывающий код ОБЯЗАН перезапустить процесс.
     */
    suspend fun import(source: Uri, password: CharArray): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Импорт начат: $source")
                require(password.isNotEmpty()) { "Введите пароль бэкапа" }

                val raw = context.contentResolver.openInputStream(source)
                    ?: error("Не удалось открыть файл")

                raw.use { input ->
                    val magic = ByteArray(MAGIC.size)
                    readFully(input, magic)
                    require(magic.contentEquals(MAGIC)) {
                        "Это не бэкап Автомастера (.amb) или файл не зашифрован"
                    }

                    val salt = ByteArray(SALT_LEN)
                    readFully(input, salt)
                    val iv = ByteArray(IV_LEN)
                    readFully(input, iv)

                    val key = deriveKey(password, salt)
                    val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                        init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
                    }

                    CipherInputStream(input, cipher).use { decrypted ->
                        // CipherInputStream молча глотает ошибку тега при неверном пароле.
                        // Проверяем ZIP-сигнатуру: настоящие данные всегда начинаются с "PK".
                        val pushback = PushbackInputStream(decrypted, 2)
                        val signature = ByteArray(2)
                        readFully(pushback, signature)
                        require(
                            signature[0] == 'P'.code.toByte() &&
                                    signature[1] == 'K'.code.toByte()
                        ) { "Неверный пароль или файл повреждён" }
                        pushback.unread(signature)

                        backupManager.restoreBackup(pushback).getOrThrow()
                    }
                }

                Log.i(TAG, "Импорт ОК, требуется рестарт")
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Импорт провален", e)
                Result.failure(mapError(e, "Неверный пароль или файл повреждён"))
            }
        }

    private fun mapError(e: Exception, cryptoMessage: String): Throwable =
        if (e is AEADBadTagException) IllegalStateException(cryptoMessage) else e

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)
        return try {
            val bytes = SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded
            SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun readFully(input: InputStream, buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val read = input.read(buffer, offset, buffer.size - offset)
            if (read < 0) error("Файл повреждён: неожиданный конец данных")
            offset += read
        }
    }

    private class CountingOutputStream(
        private val delegate: OutputStream
    ) : OutputStream() {
        var count: Long = 0
            private set

        override fun write(b: Int) {
            delegate.write(b)
            count++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            delegate.write(b, off, len)
            count += len
        }

        override fun flush() = delegate.flush()
        override fun close() = delegate.close()
    }

    private companion object {
        const val TAG = "BackupExporter"
        val MAGIC = byteArrayOf(
            'A'.code.toByte(), 'M'.code.toByte(), 'B'.code.toByte(), '1'.code.toByte()
        )
        const val SALT_LEN = 16
        const val IV_LEN = 12
        const val TAG_BITS = 128
        const val ITERATIONS = 120_000
        const val KEY_BITS = 256
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KDF = "PBKDF2WithHmacSHA256"
    }
}