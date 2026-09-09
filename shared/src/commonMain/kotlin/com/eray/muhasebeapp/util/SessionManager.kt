package com.eray.muhasebeapp.util

import com.eray.muhasebeapp.getEpochMillis
import com.russhwolf.settings.Settings

object SessionManager {

    private val settings = Settings()

    private const val KEY_TOKEN = "auth_token"
    private const val KEY_TOKEN_EXPIRE = "auth_token_expire"

    private const val KEY_USER_ID = "user_id"
    private const val KEY_AD_SOYAD = "ad_soyad"
    private const val KEY_EMAIL = "email"

    fun saveSession(
        token: String,
        tokenBitisZamani: Long,
        kullaniciId: Long,
        adSoyad: String,
        email: String
    ) {

        settings.putString(
            KEY_TOKEN,
            token
        )

        settings.putLong(
            KEY_TOKEN_EXPIRE,
            tokenBitisZamani
        )

        settings.putLong(
            KEY_USER_ID,
            kullaniciId
        )

        settings.putString(
            KEY_AD_SOYAD,
            adSoyad
        )

        settings.putString(
            KEY_EMAIL,
            email
        )
    }

    fun isSessionValid(): Boolean {

        val token =
            settings.getStringOrNull(KEY_TOKEN)
                ?: return false

        if (token.isBlank()) {
            return false
        }

        val bitisZamani =
            settings.getLong(
                KEY_TOKEN_EXPIRE,
                0L
            )

        if (bitisZamani <= 0L) {
            return false
        }

        val simdiMillis =
            getEpochMillis()

        /*
         * Backend epoch seconds döndürüyorsa
         * milisaniyeye çevir.
         *
         * Örnek:
         * seconds -> 1788940000
         * millis  -> 1788940000000
         */
        val bitisMillis =
            if (bitisZamani < 10_000_000_000L) {
                bitisZamani * 1000L
            } else {
                bitisZamani
            }

        return simdiMillis < bitisMillis
    }

    fun getToken(): String? {

        if (!isSessionValid()) {

            clearSession()

            return null
        }

        return settings.getStringOrNull(
            KEY_TOKEN
        )
    }

    fun getUserId(): Long {

        return settings.getLong(
            KEY_USER_ID,
            -1L
        )
    }

    fun getAdSoyad(): String {

        return settings.getString(
            KEY_AD_SOYAD,
            ""
        )
    }

    fun getEmail(): String {

        return settings.getString(
            KEY_EMAIL,
            ""
        )
    }

    fun clearSession() {

        settings.remove(KEY_TOKEN)
        settings.remove(KEY_TOKEN_EXPIRE)

        settings.remove(KEY_USER_ID)
        settings.remove(KEY_AD_SOYAD)
        settings.remove(KEY_EMAIL)
    }
}