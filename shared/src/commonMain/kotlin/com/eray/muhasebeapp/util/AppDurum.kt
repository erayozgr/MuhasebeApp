package com.eray.muhasebeapp.util

sealed interface AppBaslangicDurumu {
    object KontrolEdiliyor : AppBaslangicDurumu
    object Basarili : AppBaslangicDurumu
    data class BakimModu(val mesaj: String) : AppBaslangicDurumu
    data class GuncellemeGerekli(val sunucuSurumu: String) : AppBaslangicDurumu
    // Eklenen satır:
    data class InternetYok(val mesaj: String = "İnternet bağlantısı bulunamadı.\nLütfen bağlantınızı kontrol edip tekrar deneyiniz.") : AppBaslangicDurumu
    data class BaglantiHatasi(val mesaj: String) : AppBaslangicDurumu
}

object SurumYoneticisi {
    /**
     * Cihazdaki sürüm sunucudaki sürümden farklı veya küçükse true döner.
     */
    fun guncellemeGerekliMi(mevcutSurum: String, sunucuSurumu: String): Boolean {
        if (mevcutSurum == sunucuSurumu) return false

        val mevcutParcalar = mevcutSurum.split(".").map { it.toIntOrNull() ?: 0 }
        val sunucuParcalar = sunucuSurumu.split(".").map { it.toIntOrNull() ?: 0 }

        val uzunluk = maxOf(mevcutParcalar.size, sunucuParcalar.size)
        for (i in 0 until uzunluk) {
            val m = mevcutParcalar.getOrElse(i) { 0 }
            val s = sunucuParcalar.getOrElse(i) { 0 }
            if (m < s) return true
            if (m > s) return false
        }
        return false
    }
}