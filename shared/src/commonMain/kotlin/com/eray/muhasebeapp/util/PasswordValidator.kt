package com.eray.muhasebeapp.util

object PasswordValidator {

    fun hataMesaji(sifre: String): String? {

        if (sifre.length < 8) {
            return "Şifre en az 8 karakter olmalıdır."
        }

        if (!sifre.any { it.isUpperCase() }) {
            return "Şifre en az 1 büyük harf içermelidir."
        }

        if (!sifre.any { it.isLowerCase() }) {
            return "Şifre en az 1 küçük harf içermelidir."
        }

        if (!sifre.any { it.isDigit() }) {
            return "Şifre en az 1 rakam içermelidir."
        }

        if (!sifre.any { !it.isLetterOrDigit() && !it.isWhitespace() }) {
            return "Şifre en az 1 özel karakter içermelidir."
        }

        return null
    }

    fun gecerliMi(sifre: String): Boolean {
        return hataMesaji(sifre) == null
    }
}