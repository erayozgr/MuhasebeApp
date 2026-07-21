package com.eray.muhasebeapp

import androidx.compose.runtime.Composable

interface UrlAcici {
    fun ac(url: String)
}

@Composable
expect fun rememberUrlAcici(): UrlAcici

// Telefon numarasını temizleyip WhatsApp linki oluşturan yardımcı fonksiyon
fun whatsappLinkOlustur(telefon: String): String {
    val temizNumara = telefon.filter { it.isDigit() }
    // Başında 0 varsa kaldır, Türkiye kodu (90) yoksa ekle
    val ulkeKoduluNumara = when {
        temizNumara.startsWith("90") -> temizNumara
        temizNumara.startsWith("0") -> "90" + temizNumara.drop(1)
        else -> "90$temizNumara"
    }
    return "https://wa.me/$ulkeKoduluNumara"
}

fun telefonLinkOlustur(telefon: String): String {
    val temizNumara = telefon.filter { it.isDigit() || it == '+' }
    return "tel:$temizNumara"
}