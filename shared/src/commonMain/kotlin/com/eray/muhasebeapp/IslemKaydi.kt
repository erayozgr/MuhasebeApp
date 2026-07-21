package com.eray.muhasebeapp

import com.eray.muhasebeapp.database.Alis
import com.eray.muhasebeapp.database.Masraf
import com.eray.muhasebeapp.database.Satis
import com.eray.muhasebeapp.database.StokHareketi
import com.eray.muhasebeapp.database.AlisKalemi
import com.eray.muhasebeapp.database.SatisKalemi


sealed class IslemKaydi(val tarih: String, val tutar: Double) {
    data class SatisIslemi(val satis: Satis, val kalemler: List<SatisKalemi> = emptyList()) : IslemKaydi(satis.tarih, satis.toplamTutar)
    data class AlisIslemi(val alis: Alis, val kalemler: List<AlisKalemi> = emptyList()) : IslemKaydi(alis.tarih, alis.toplamTutar)
    data class MasrafIslemi(val masraf: Masraf) : IslemKaydi(masraf.tarih, masraf.tutar)
    data class StokIslemi(val stokHareketi: StokHareketi) : IslemKaydi(stokHareketi.tarih, stokHareketi.birimFiyat * stokHareketi.miktar)
}

// CSV içeriğini komple oluşturan fonksiyon - tamamen commonMain, platform bağımsız
fun csvRaporuOlustur(
    satislar: List<Satis>,
    alislar: List<Alis>,
    masraflar: List<Masraf>,
    stokHareketleri: List<StokHareketi> = emptyList()
): String {
    val sb = StringBuilder()
    sb.appendLine("Tip;Tarih;Karsi Taraf / Kategori;Tutar;Odeme Turu")

    satislar.forEach {
        sb.appendLine("Satis;${it.tarih};${it.musteriAdi};${it.toplamTutar};${it.odemeTuru}")
    }
    alislar.forEach {
        sb.appendLine("Alis;${it.tarih};${it.tedarikciAdi};${it.toplamTutar};${it.odemeTuru}")
    }
    masraflar.forEach {
        sb.appendLine("Masraf;${it.tarih};${it.kategori} - ${it.aciklama};${it.tutar};-")
    }
    stokHareketleri.forEach {
        sb.appendLine("Stok;${it.tarih};${it.urunAdi} (${it.hareketTuru}, ${it.miktar} adet) - ${it.aciklama};${it.birimFiyat * it.miktar};-")
    }

    val toplamSatis = satislar.sumOf { it.toplamTutar }
    val toplamAlis = alislar.sumOf { it.toplamTutar }
    val toplamMasraf = masraflar.sumOf { it.tutar }
    val netKar = toplamSatis - toplamAlis - toplamMasraf

    sb.appendLine()
    sb.appendLine("ÖZET")
    sb.appendLine("Toplam Satis;;;${toplamSatis}")
    sb.appendLine("Toplam Alis;;;${toplamAlis}")
    sb.appendLine("Toplam Masraf;;;${toplamMasraf}")
    sb.appendLine("Net Kar/Zarar;;;${netKar}")

    return sb.toString()
}

fun formatTarih(tarih: String): String {
    val millis = tarih.toLongOrNull() ?: return tarih

    val toplamSaniye = millis / 1000
    val gunSayisi = toplamSaniye / 86400
    val gunIciSaniye = toplamSaniye % 86400

    val saat = (gunIciSaniye / 3600).toString().padStart(2, '0')
    val dakika = ((gunIciSaniye % 3600) / 60).toString().padStart(2, '0')

    // 1970-01-01'den itibaren gün sayısından takvim tarihi (proleptic Gregorian, civil_from_days algoritması)
    var z = gunSayisi + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    val yil = if (m <= 2) y + 1 else y

    val gun = d.toString().padStart(2, '0')
    val ay = m.toString().padStart(2, '0')

    return "$gun.$ay.$yil $saat:$dakika"
}