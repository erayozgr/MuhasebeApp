package com.eray.muhasebeapp

import androidx.compose.runtime.Composable

interface DosyaPaylasici {
    fun paylas(dosyaAdi: String, icerik: String)
    fun paylasBytes(dosyaAdi: String, icerik: ByteArray)
}

@Composable
expect fun rememberDosyaPaylasici(): DosyaPaylasici