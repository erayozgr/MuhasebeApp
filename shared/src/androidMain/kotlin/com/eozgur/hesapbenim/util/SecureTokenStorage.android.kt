package com.eozgur.hesapbenim.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.russhwolf.settings.Settings
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal actual object SecureTokenStorage {
    private const val KEY_ALIAS = "hesapbenim.session.v1"
    private const val STORAGE_KEY = "auth_token_encrypted_v1"
    private val settings by lazy { Settings() }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build())
        }.generateKey()
    }

    @Synchronized
    actual fun read(): String? {
        val encoded = settings.getStringOrNull(STORAGE_KEY) ?: return null
        return try {
            val parts = encoded.split(":")
            require(parts.size == 2)
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            require(iv.size == 12)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)).toString(Charsets.UTF_8)
        } catch (_: Exception) {
            // Restored/corrupt ciphertext or an invalidated key requires a new login.
            clear()
            null
        }
    }

    @Synchronized
    actual fun write(token: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        settings.putString(STORAGE_KEY,
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
                Base64.encodeToString(encrypted, Base64.NO_WRAP))
    }

    @Synchronized
    actual fun clear() { settings.remove(STORAGE_KEY) }
}
