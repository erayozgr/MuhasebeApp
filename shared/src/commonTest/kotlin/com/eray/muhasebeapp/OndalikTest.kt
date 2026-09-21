package com.eray.muhasebeapp

import com.eray.muhasebeapp.data.model.*
import com.eray.muhasebeapp.ui.screens.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class OndalikTest {
    @Test fun commaAndDotAreEquivalentAndPrecisionIsLimited() {
        assertEquals(1.25, ondalikSayi("1,25"))
        assertEquals(1.25, ondalikSayi("1.25"))
        assertEquals("12.34", ondalikGirdi("12,345"))
        assertEquals(-1.5, ondalikSayi("-1,5"))
        listOf("1,2.3", "NaN", "Infinity", "1e3", "12abc", "1.234", "--1", "").forEach { assertNull(ondalikSayi(it), it) }
    }

    @Test fun stockArithmeticAndDisplayKeepFractions() {
        assertEquals(0.3, miktarYuvarla(0.1 + 0.2))
        assertEquals(0.0, miktarYuvarla(0.3 - 0.1 - 0.2))
        assertEquals("1,25", miktarMetni(1.25))
        assertEquals("2", miktarMetni(2.0))
    }

    @Test fun apiRequestsAndResponsesPreserveFractionalQuantities() {
        val request = SatisKayitRequest(1L, listOf(SatisKalemiRequest(2L, 1.25, 10.0)))
        assertEquals(request, Json.decodeFromString<SatisKayitRequest>(Json.encodeToString(request)))
        val alis = AlisKayitRequest(null, listOf(AlisKalemiRequest(2L, 0.75, 2.0)))
        assertEquals(0.75, Json.decodeFromString<AlisKayitRequest>(Json.encodeToString(alis)).kalemler.single().adet)
        val urun = Json.decodeFromString<Urun>("""{"barkod":"","ad":"Un","alisFiyati":10,"satisFiyati":20,"stokAdedi":-0.25,"birim":"Kg","kdvOrani":20}""")
        assertEquals(-0.25, urun.stokAdedi)
        val kalem = Json.decodeFromString<SatisKalemi>("""{"satisId":1,"urunId":2,"urunAdi":"Un","adet":1.25,"birim":"Kg","birimFiyat":20,"toplam":25}""")
        assertEquals(1.25, kalem.adet)
    }
}
