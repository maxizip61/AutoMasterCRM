package ru.automaster.app.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "app_lock_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
    }

    /**
     * Устанавливает новый PIN-код. Сохраняет только хэш и соль.
     */
    fun setPin(pin: String) {
        val salt = generateSalt()
        val hash = hashPin(pin, salt)

        sharedPreferences.edit()
            .putString(KEY_PIN_SALT, bytesToHex(salt))
            .putString(KEY_PIN_HASH, bytesToHex(hash))
            .apply()
    }

    /**
     * Проверяет, совпадает ли введенный PIN-код с сохраненным.
     */
    fun verifyPin(pin: String): Boolean {
        val saltHex = sharedPreferences.getString(KEY_PIN_SALT, null) ?: return false
        val hashHex = sharedPreferences.getString(KEY_PIN_HASH, null) ?: return false

        val salt = hexToBytes(saltHex)
        val expectedHash = hexToBytes(hashHex)
        val actualHash = hashPin(pin, salt)

        return MessageDigest.isEqual(expectedHash, actualHash)
    }

    /**
     * Проверяет, установлен ли вообще PIN-код.
     */
    fun isPinSet(): Boolean {
        return sharedPreferences.contains(KEY_PIN_HASH)
    }

    /**
     * Удаляет PIN-код (при отключении блокировки).
     */
    fun clearPin() {
        sharedPreferences.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .apply()
    }

    // --- Криптография ---

    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(pin.toByteArray())
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) +
                    Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}