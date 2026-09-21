package com.eray.muhasebeapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Musteri(
    val id: Long? = null,
    val ad: String,
    val telefon: String,
    val adres: String,
    val bakiye: Double = 0.0
)

@Serializable
data class Tedarikci(
    val id: Long? = null,
    val ad: String,
    val telefon: String,
    val adres: String,
    val bakiye: Double = 0.0
)

@Serializable
data class Urun(
    val id: Long? = null,
    val barkod: String,
    val ad: String,
    val alisFiyati: Double,
    val satisFiyati: Double,
    val stokAdedi: Double,
    val birim: String,
    val kdvOrani: Int
)

@Serializable
data class Masraf(
    val id: Long? = null,
    val kategori: String,
    val aciklama: String,
    val tutar: Double,
    val tarih: String
)

@Serializable
data class Satis(
    val id: Long? = null,
    val uuid: String? = null,
    val musteriId: Long? = null,       // '= null' eklendi
    val musteriAdi: String? = null,
    val tarih: String? = null,
    val toplamTutar: Double = 0.0
)

@Serializable
data class SatisKalemi(
    val id: Long? = null,
    val satisId: Long?,
    val urunId: Long,
    val urunAdi: String,
    val adet: Double,
    val birim: String,
    val birimFiyat: Double,
    val toplam: Double
)

@Serializable
data class Alis(
    val id: Long? = null,
    val uuid: String? = null,
    val tedarikciId: Long? = null,
    val tedarikciAdi: String? = null,
    val tarih: String? = null,
    val toplamTutar: Double = 0.0
)

@Serializable
data class AlisKalemi(
    val id: Long? = null,
    val alisId: Long?,
    val urunId: Long,
    val urunAdi: String,
    val adet: Double,
    val birim: String = "",
    val birimFiyat: Double,
    val toplam: Double
)

@Serializable
data class StokHareketi(
    val id: Long? = null,
    val urunId: Long,
    val urunAdi: String,
    val hareketTuru: String,
    val miktar: Double,
    val birimFiyat: Double,
    val aciklama: String,
    val tarih: String
)

@Serializable
data class Tahsilat(
    val id: Long? = null,
    val musteriId: Long?,
    val musteriAdi: String,
    val tutar: Double,
    val tarih: String
)

@Serializable
data class TedarikciOdemesi(
    val id: Long? = null,
    val tedarikciId: Long?,
    val tedarikciAdi: String,
    val tutar: Double,
    val tarih: String
)

@Serializable
data class SatisKayitRequest(
    val musteriId: Long?,
    val kalemler: List<SatisKalemiRequest>,
    val tarih: String? = null
)

@Serializable
data class SatisKalemiRequest(
    val urunId: Long,
    val adet: Double,
    val birimFiyat: Double
)

@Serializable
data class AlisKayitRequest(
    val tedarikciId: Long?,
    val kalemler: List<AlisKalemiRequest>,
    val tarih: String? = null
)

@Serializable
data class AlisKalemiRequest(
    val urunId: Long,
    val adet: Double,
    val birimFiyat: Double
)

@Serializable
data class TahsilatRequest(
    val musteriId: Long,
    val tutar: Double,
    val tarih: String? = null
)

@Serializable
data class TedarikciOdemeRequest(
    val tedarikciId: Long,
    val tutar: Double,
    val tarih: String? = null
)
