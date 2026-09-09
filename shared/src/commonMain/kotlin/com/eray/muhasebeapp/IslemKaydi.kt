package com.eray.muhasebeapp

import com.eray.muhasebeapp.data.model.*

sealed class IslemKaydi(val tarih: String, val tutar: Double) {
    // Null gelme ihtimaline karşı ?: varsayılan değerler eklendi
    data class SatisIslemi(val satis: Satis, val kalemler: List<SatisKalemi> = emptyList()) :
        IslemKaydi(satis.tarih ?: "", satis.toplamTutar ?: 0.0)

    data class AlisIslemi(val alis: Alis, val kalemler: List<AlisKalemi> = emptyList()) :
        IslemKaydi(alis.tarih ?: "", alis.toplamTutar ?: 0.0)

    data class MasrafIslemi(val masraf: Masraf) :
        IslemKaydi(masraf.tarih ?: "", masraf.tutar ?: 0.0)

    data class StokIslemi(val stokHareketi: StokHareketi) :
        IslemKaydi(stokHareketi.tarih ?: "", (stokHareketi.birimFiyat ?: 0.0) * (stokHareketi.miktar ?: 0L))

    data class TahsilatIslemi(val tahsilat: Tahsilat) :
        IslemKaydi(tahsilat.tarih ?: "", tahsilat.tutar ?: 0.0)

    data class TedarikciOdemeIslemi(val odeme: TedarikciOdemesi) :
        IslemKaydi(odeme.tarih ?: "", odeme.tutar ?: 0.0)
}

// CSV içeriğini oluşturan fonksiyon
fun csvRaporuOlustur(
    satislar: List<Satis>,
    alislar: List<Alis>,
    masraflar: List<Masraf>,
    stokHareketleri: List<StokHareketi> = emptyList(),
    tahsilatlar: List<Tahsilat> = emptyList(),
    tedarikciOdemeleri: List<TedarikciOdemesi> = emptyList()
): String {
    val sb = StringBuilder()
    sb.appendLine("Tip;Tarih;Karsi Taraf / Kategori;Tutar")

    satislar.forEach {
        sb.appendLine("Satis;${formatTarih(it.tarih)};${it.musteriAdi ?: "Genel Müşteri"};${it.toplamTutar ?: 0.0}")
    }
    alislar.forEach {
        sb.appendLine("Alis;${formatTarih(it.tarih)};${it.tedarikciAdi ?: "Tedarikçi"};${it.toplamTutar ?: 0.0}")
    }
    masraflar.forEach {
        sb.appendLine("Masraf;${formatTarih(it.tarih)};${it.kategori} - ${it.aciklama};${it.tutar ?: 0.0}")
    }
    stokHareketleri.forEach {
        val tutar = (it.birimFiyat ?: 0.0) * (it.miktar ?: 0L)
        sb.appendLine("Stok;${formatTarih(it.tarih)};${it.urunAdi} (${it.hareketTuru}, ${it.miktar} adet) - ${it.aciklama};$tutar")
    }
    tahsilatlar.forEach {
        sb.appendLine("Tahsilat;${formatTarih(it.tarih)};${it.musteriAdi ?: "Müşteri"} (Müşteri Tahsilat);${it.tutar ?: 0.0}")
    }
    tedarikciOdemeleri.forEach {
        sb.appendLine("Tedarikçi Ödemesi;${formatTarih(it.tarih)};${it.tedarikciAdi ?: "Tedarikçi"} (Tedarikçiye Ödeme);${it.tutar ?: 0.0}")
    }

    // sumOf işlemlerinde olası null alanlar 0.0'a bağlandı
    val toplamSatis = satislar.sumOf { it.toplamTutar ?: 0.0 }
    val toplamAlis = alislar.sumOf { it.toplamTutar ?: 0.0 }
    val toplamMasraf = masraflar.sumOf { it.tutar ?: 0.0 }
    val toplamTahsilat = tahsilatlar.sumOf { it.tutar ?: 0.0 }
    val toplamOdeme = tedarikciOdemeleri.sumOf { it.tutar ?: 0.0 }
    val netKar = toplamSatis - toplamAlis - toplamMasraf

    sb.appendLine()
    sb.appendLine("ÖZET")
    sb.appendLine("Toplam Satis;;;${toplamSatis}")
    sb.appendLine("Toplam Alis;;;${toplamAlis}")
    sb.appendLine("Toplam Masraf;;;${toplamMasraf}")
    sb.appendLine("Toplam Tahsilat;;;${toplamTahsilat}")
    sb.appendLine("Toplam Tedarikçi Ödemesi;;;${toplamOdeme}")
    sb.appendLine("Net Kar/Zarar;;;${netKar}")

    return sb.toString()
}

// Hem ISO string ("2026-09-06T17:45:06") hem de milisaniye ("1725637200000") destekler
fun formatTarih(tarih: String?): String {
    if (tarih.isNullOrBlank()) return ""

    // 1. Durum: Sunucudan gelen ISO-8601 formatı (Örn: "2026-09-06T17:45:06.123")
    if (tarih.contains("T")) {
        return try {
            val parts = tarih.split("T")
            val dateParts = parts[0].split("-") // [2026, 09, 06]
            val timeParts = parts[1].split(":") // [17, 45, 06]
            if (dateParts.size == 3 && timeParts.size >= 2) {
                "${dateParts[2]}.${dateParts[1]}.${dateParts[0]} ${timeParts[0]}:${timeParts[1]}"
            } else {
                tarih
            }
        } catch (_: Exception) {
            tarih
        }
    }

    // 2. Durum: Milisaniye cinsinden epoch timestamp
    val millis = tarih.toLongOrNull() ?: return tarih
    val toplamSaniye = millis / 1000
    val gunSayisi = toplamSaniye / 86400
    val gunIciSaniye = toplamSaniye % 86400

    val saat = (gunIciSaniye / 3600).toString().padStart(2, '0')
    val dakika = ((gunIciSaniye % 3600) / 60).toString().padStart(2, '0')

    val z = gunSayisi + 719468
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

// Hem ISO string ("2026-09-06T17:45:06") hem de milisaniye ("1725637200000") destekler
fun formatSaat(tarih: String?): String {
    if (tarih.isNullOrBlank()) return ""

    if (tarih.contains("T")) {
        return try {
            val timePart = tarih.substringAfter("T")
            val parts = timePart.split(":")
            if (parts.size >= 2) "${parts[0]}:${parts[1]}" else timePart.take(5)
        } catch (_: Exception) {
            ""
        }
    }

    val millis = tarih.toLongOrNull() ?: return ""
    val toplamSaniye = millis / 1000
    val gunIciSaniye = toplamSaniye % 86400

    val toplamSaatSaniye = gunIciSaniye + (3 * 3600)
    val duzeltilmisSaniye = if (toplamSaatSaniye >= 86400) toplamSaatSaniye - 86400 else toplamSaatSaniye

    val saat = (duzeltilmisSaniye / 3600).toString().padStart(2, '0')
    val dakika = ((duzeltilmisSaniye % 3600) / 60).toString().padStart(2, '0')

    return "$saat:$dakika"
}

// Tarih seçici filtresi için ISO veya milisaniye stringini epoch millis'e dönüştürür
fun parseTarihMillis(tarih: String?): Long {
    if (tarih.isNullOrBlank()) return 0L
    tarih.toLongOrNull()?.let { return it }

    return try {
        val datePart = tarih.substringBefore("T")
        val parts = datePart.split("-")
        if (parts.size == 3) {
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            val y = if (month <= 2) year - 1 else year
            val era = (if (y >= 0) y else y - 399) / 400
            val yoe = y - era * 400
            val m = if (month > 2) month - 3 else month + 9
            val doy = (153 * m + 2) / 5 + day - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            val days = era * 146097 + doe - 719468
            days * 86400000L
        } else {
            0L
        }
    } catch (_: Exception) {
        0L
    }
}
