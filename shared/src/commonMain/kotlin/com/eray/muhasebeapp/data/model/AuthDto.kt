package com.eray.muhasebeapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val adSoyad: String,
    val email: String,
    val telefon: String,
    val sifre: String,
    val kullaniciSozlesmesiKabul: Boolean = false,
    val sozlesmeSurumu: String? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val sifre: String
)

@Serializable
data class KullaniciAdiGuncelleRequest(
    val adSoyad: String
)

@Serializable
data class SifreDegistirRequest(
    val mevcutSifre: String,
    val yeniSifre: String
)

@Serializable
data class EmailDogrulamaRequest(
    val kod: String
)

@Serializable
data class AuthResponse(
    val basarili: Boolean,
    val mesaj: String,

    val kullaniciId: Long? = null,
    val adSoyad: String? = null,
    val email: String? = null,
    val telefon: String? = null,

    val emailDogrulandiMi: Boolean? = null,
    val telefonDogrulandiMi: Boolean? = null,

    @Serializable(with = com.eray.muhasebeapp.util.OdemeTarihiSerializer::class)
    val sonOdemeTarihi: Long? = null,

    val hesapAktifMi: Boolean? = null,
    val ozelHesapMi: Boolean? = null,

    val token: String? = null,
    val tokenBitisZamani: Long? = null
)
@Serializable
data class KayitEmailDogrulamaRequest(
    val email: String,
    val kod: String
)
@Serializable
data class HesapSilRequest(
    val kod: String
)
