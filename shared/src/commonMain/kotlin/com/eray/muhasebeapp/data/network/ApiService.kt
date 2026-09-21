package com.eray.muhasebeapp.data.network

import com.eray.muhasebeapp.data.model.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class ApiService {
    private val baseUrl = "http://91.151.84.74:8080/api"

    // Müşteriler
    suspend fun getMusteriler(): List<Musteri> = try {
        val response = NetworkClient.httpClient.get("$baseUrl/musteriler")
        if (response.status == HttpStatusCode.OK) response.body() else emptyList()
    } catch (e: Exception) { emptyList() }

    suspend fun createMusteri(musteri: Musteri): Result<Musteri> = try {
        println("API_REQ: POST /musteriler -> DATA: $musteri")
        val response = NetworkClient.httpClient.post("$baseUrl/musteriler") {
            contentType(ContentType.Application.Json)
            setBody(musteri)
        }
        println("API_RES: POST /musteriler -> STATUS: ${response.status}")
        
        if (response.status.value in 200..299) {
            Result.success(response.body())
        } else {
            val rawError = response.bodyAsText()
            val shortError = if (rawError.length > 100) rawError.take(100) + "..." else rawError
            println("API_ERROR_BODY: $rawError")
            Result.failure(Exception("Yetki Hatası veya Sunucu Reddi (${response.status.value}). Detay loglarda."))
        }
    } catch (e: Exception) {
        println("API_FATAL: ${e.message}")
        Result.failure(e)
    }

    suspend fun updateMusteri(id: Long, musteri: Musteri): Result<Musteri> = try {
        val response = NetworkClient.httpClient.put("$baseUrl/musteriler/$id") {
            contentType(ContentType.Application.Json)
            setBody(musteri)
        }
        if (response.status.value in 200..299) Result.success(response.body())
        else Result.failure(Exception("Hata (${response.status.value}): ${response.bodyAsText()}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteMusteri(id: Long): Result<Unit> = try {
        val response = NetworkClient.httpClient.delete("$baseUrl/musteriler/$id")
        if (response.status.value in 200..299) Result.success(Unit)
        else Result.failure(Exception("Hata (${response.status.value}): ${response.bodyAsText()}"))
    } catch (e: Exception) { Result.failure(e) }

    // Ürünler
    suspend fun getUrunler(): List<Urun> = try {
        val response = NetworkClient.httpClient.get("$baseUrl/urunler")
        if (response.status == HttpStatusCode.OK) response.body() else emptyList()
    } catch (e: Exception) { emptyList() }

    suspend fun searchUrunler(q: String): List<Urun> = try {
        val response = NetworkClient.httpClient.get("$baseUrl/urunler/ara") {
            parameter("q", q)
        }
        if (response.status == HttpStatusCode.OK) response.body() else emptyList()
    } catch (e: Exception) { emptyList() }

    suspend fun createUrun(urun: Urun): Result<Urun> = try {
        println("API_REQ: POST /urunler -> DATA: $urun")
        val response = NetworkClient.httpClient.post("$baseUrl/urunler") {
            contentType(ContentType.Application.Json)
            setBody(urun)
        }
        println("API_RES: POST /urunler -> STATUS: ${response.status}")
        
        if (response.status.value in 200..299) {
            Result.success(response.body())
        } else {
            val rawError = response.bodyAsText()
            println("API_ERROR_BODY: $rawError")
            Result.failure(Exception("Yetki Hatası veya Sunucu Reddi (${response.status.value}). Detay loglarda."))
        }
    } catch (e: Exception) {
        println("API_FATAL: ${e.message}")
        Result.failure(e)
    }

    suspend fun updateUrun(id: Long, urun: Urun): Result<Urun> = try {
        val response = NetworkClient.httpClient.put("$baseUrl/urunler/$id") {
            contentType(ContentType.Application.Json)
            setBody(urun)
        }
        if (response.status.value in 200..299) Result.success(response.body())
        else Result.failure(Exception("Hata (${response.status.value}): ${response.bodyAsText()}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteUrun(id: Long): Result<Unit> = try {
        val response = NetworkClient.httpClient.delete("$baseUrl/urunler/$id")
        if (response.status.value in 200..299) Result.success(Unit)
        else Result.failure(Exception("Hata (${response.status.value}): ${response.bodyAsText()}"))
    } catch (e: Exception) { Result.failure(e) }

    // Tedarikçiler
    suspend fun getTedarikciler(): List<Tedarikci> = try {
        val response = NetworkClient.httpClient.get("$baseUrl/tedarikciler")
        if (response.status == HttpStatusCode.OK) response.body() else emptyList()
    } catch (e: Exception) { emptyList() }

    suspend fun createTedarikci(tedarikci: Tedarikci): Result<Tedarikci> = try {
        val response = NetworkClient.httpClient.post("$baseUrl/tedarikciler") {
            contentType(ContentType.Application.Json)
            setBody(tedarikci)
        }
        if (response.status.value in 200..299) Result.success(response.body())
        else Result.failure(Exception("Hata (${response.status.value}): ${response.bodyAsText()}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun updateTedarikci(id: Long, tedarikci: Tedarikci): Result<Tedarikci> = try {
        val response = NetworkClient.httpClient.put("$baseUrl/tedarikciler/$id") {
            contentType(ContentType.Application.Json)
            setBody(tedarikci)
        }
        if (response.status.value in 200..299) Result.success(response.body())
        else Result.failure(Exception("Hata (${response.status.value}): ${response.bodyAsText()}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteTedarikci(id: Long): Result<Unit> = try {
        val response = NetworkClient.httpClient.delete("$baseUrl/tedarikciler/$id")
        if (response.status.value in 200..299) Result.success(Unit)
        else Result.failure(Exception("Hata (${response.status.value}): ${response.bodyAsText()}"))
    } catch (e: Exception) { Result.failure(e) }

    // Satışlar
    suspend fun getSatislar(): List<Satis> = try {
        val response = NetworkClient.httpClient.get("$baseUrl/satislar")
        if (response.status == HttpStatusCode.OK) response.body() else emptyList()
    } catch (e: Exception) { emptyList() }

    suspend fun createSatis(request: SatisKayitRequest): Result<Satis> = try {
        println("API_DEBUG: createSatis başlatıldı. URL: $baseUrl/satislar")
        val response = NetworkClient.httpClient.post("$baseUrl/satislar") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        println("API_DEBUG: createSatis yanıtı geldi. Kod: ${response.status}")
        
        if (response.status.value in 200..299) {
            val body: Satis = response.body()
            println("API_DEBUG: createSatis başarılı! Kayıt ID: ${body.id}")
            Result.success(body)
        } else {
            val errorText = response.bodyAsText()
            println("API_DEBUG: createSatis HATA (Sunucu): $errorText")
            Result.failure(Exception("Hata (${response.status.value}): $errorText"))
        }
    } catch (e: Exception) {
        println("API_DEBUG: createSatis FATAL HATA: ${e.message}")
        e.printStackTrace()
        Result.failure(e)
    }

    suspend fun getSatisKalemler(satisId: Long): List<SatisKalemi> = try {
        val response = NetworkClient.httpClient.get("$baseUrl/satislar/$satisId/kalemler")
        if (response.status == HttpStatusCode.OK) response.body() else emptyList()
    } catch (e: Exception) { emptyList() }

    // Alışlar
    suspend fun getAlislar(): List<Alis> = try {
        val response = NetworkClient.httpClient.get("$baseUrl/alislar")
        if (response.status == HttpStatusCode.OK) response.body() else emptyList()
    } catch (e: Exception) { emptyList() }

    suspend fun createAlis(request: AlisKayitRequest): Result<Alis> = try {
        println("API_DEBUG: createAlis başlatıldı. URL: $baseUrl/alislar")
        val response = NetworkClient.httpClient.post("$baseUrl/alislar") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        println("API_DEBUG: createAlis yanıtı geldi. Kod: ${response.status}")

        if (response.status.value in 200..299) {
            val body: Alis = response.body()
            println("API_DEBUG: createAlis başarılı! Kayıt ID: ${body.id}")
            Result.success(body)
        } else {
            val errorText = response.bodyAsText()
            println("API_DEBUG: createAlis HATA (Sunucu): $errorText")
            Result.failure(Exception("Hata (${response.status.value}): $errorText"))
        }
    } catch (e: Exception) {
        println("API_DEBUG: createAlis FATAL HATA: ${e.message}")
        e.printStackTrace()
        Result.failure(e)
    }

    suspend fun getAlisKalemler(alisId: Long): List<AlisKalemi> = try {
        val response = NetworkClient.httpClient.get("$baseUrl/alislar/$alisId/kalemler")
        if (response.status == HttpStatusCode.OK) response.body() else emptyList()
    } catch (e: Exception) { emptyList() }

    // Masraflar
    suspend fun getMasraflar(): List<Masraf> = try {
        val response = NetworkClient.httpClient.get("$baseUrl/masraflar")
        if (response.status == HttpStatusCode.OK) response.body() else emptyList()
    } catch (e: Exception) { emptyList() }

    suspend fun createMasraf(masraf: Masraf): Result<Masraf> = try {
        val response = NetworkClient.httpClient.post("$baseUrl/masraflar") {
            contentType(ContentType.Application.Json)
            setBody(masraf)
        }
        if (response.status.value in 200..299) Result.success(response.body())
        else Result.failure(Exception("Hata (${response.status.value}): ${response.bodyAsText()}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteMasraf(id: Long): Result<Unit> = try {
        val response = NetworkClient.httpClient.delete("$baseUrl/masraflar/$id")
        if (response.status.value in 200..299) Result.success(Unit)
        else Result.failure(Exception("Hata (${response.status.value}): ${response.bodyAsText()}"))
    } catch (e: Exception) { Result.failure(e) }

    // Stok Hareketleri
    suspend fun getStokHareketleri(): List<StokHareketi> = try {
        val response = NetworkClient.httpClient.get("$baseUrl/stok-hareketleri")
        if (response.status == HttpStatusCode.OK) response.body() else emptyList()
    } catch (e: Exception) { emptyList() }

    suspend fun createStokHareketi(hareket: StokHareketi): Result<StokHareketi> = try {
        val response = NetworkClient.httpClient.post("$baseUrl/stok-hareketleri") {
            contentType(ContentType.Application.Json)
            setBody(hareket)
        }
        if (response.status.value in 200..299) Result.success(response.body())
        else Result.failure(Exception("Hata (${response.status.value}): ${response.bodyAsText()}"))
    } catch (e: Exception) { Result.failure(e) }

    // Tahsilatlar
    suspend fun getTahsilatlar(): List<Tahsilat> = try {
        val response = NetworkClient.httpClient.get("$baseUrl/tahsilatlar")
        if (response.status == HttpStatusCode.OK) response.body() else emptyList()
    } catch (e: Exception) { emptyList() }

    suspend fun createTahsilat(request: TahsilatRequest): Result<Tahsilat> = try {
        val response = NetworkClient.httpClient.post("$baseUrl/tahsilatlar") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.value in 200..299) Result.success(response.body())
        else Result.failure(Exception("Hata (${response.status.value}): ${response.bodyAsText()}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteTahsilat(id: Long): Result<Unit> = try {
        val response = NetworkClient.httpClient.delete("$baseUrl/tahsilatlar/$id")
        if (response.status.value in 200..299) Result.success(Unit)
        else Result.failure(Exception("Hata (${response.status.value}): ${response.bodyAsText()}"))
    } catch (e: Exception) { Result.failure(e) }

    // Tedarikçi Ödemeleri
    suspend fun getTedarikciOdemeleri(): List<TedarikciOdemesi> = try {
        val response = NetworkClient.httpClient.get("$baseUrl/tedarikci-odemeleri")
        if (response.status == HttpStatusCode.OK) response.body() else emptyList()
    } catch (e: Exception) { emptyList() }

    suspend fun createTedarikciOdemesi(request: TedarikciOdemeRequest): Result<TedarikciOdemesi> = try {
        val response = NetworkClient.httpClient.post("$baseUrl/tedarikci-odemeleri") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.value in 200..299) Result.success(response.body())
        else Result.failure(Exception("Hata (${response.status.value}): ${response.bodyAsText()}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteTedarikciOdemesi(id: Long): Result<Unit> = try {
        val response = NetworkClient.httpClient.delete("$baseUrl/tedarikci-odemeleri/$id")
        if (response.status.value in 200..299) Result.success(Unit)
        else Result.failure(Exception("Hata (${response.status.value}): ${response.bodyAsText()}"))
    } catch (e: Exception) { Result.failure(e) }
    suspend fun updateSatis(id: Long, request: SatisKayitRequest): Result<Satis> = try {
        val response = NetworkClient.httpClient.put("$baseUrl/satislar/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.value in 200..299) Result.success(response.body())
        else Result.failure(Exception(response.bodyAsText()))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun updateAlis(id: Long, request: AlisKayitRequest): Result<Alis> = try {
        val response = NetworkClient.httpClient.put("$baseUrl/alislar/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.value in 200..299) Result.success(response.body())
        else Result.failure(Exception(response.bodyAsText()))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun tahsilatGecmisi(): List<Tahsilat> {
        val response = NetworkClient.httpClient.get("$baseUrl/tahsilatlar")
        check(response.status.value in 200..299) { "Tahsilat geçmişi yüklenemedi." }
        return response.body()
    }

    suspend fun odemeGecmisi(): List<TedarikciOdemesi> {
        val response = NetworkClient.httpClient.get("$baseUrl/tedarikci-odemeleri")
        check(response.status.value in 200..299) { "Ödeme geçmişi yüklenemedi." }
        return response.body()
    }

    suspend fun updateTahsilat(id: Long, request: TahsilatRequest): Result<Tahsilat> = try {
        val response = NetworkClient.httpClient.put("$baseUrl/tahsilatlar/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.value in 200..299) Result.success(response.body())
        else Result.failure(Exception(response.bodyAsText()))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun updateTedarikciOdemesi(id: Long, request: TedarikciOdemeRequest): Result<TedarikciOdemesi> = try {
        val response = NetworkClient.httpClient.put("$baseUrl/tedarikci-odemeleri/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.value in 200..299) Result.success(response.body())
        else Result.failure(Exception(response.bodyAsText()))
    } catch (e: Exception) { Result.failure(e) }
}
