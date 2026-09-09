package com.eray.muhasebeapp.data.network

import com.eray.muhasebeapp.data.model.AuthResponse
import com.eray.muhasebeapp.data.model.KullaniciAdiGuncelleRequest
import com.eray.muhasebeapp.data.model.LoginRequest
import com.eray.muhasebeapp.data.model.RegisterRequest
import com.eray.muhasebeapp.data.model.SifreDegistirRequest
import com.eray.muhasebeapp.util.HashUtils
import com.eray.muhasebeapp.util.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json


object NetworkClient {

    val httpClient = HttpClient {

        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                }
            )
        }

        install(DefaultRequest) {

            val token = SessionManager.getToken()

            if (!token.isNullOrBlank()) {
                header(
                    HttpHeaders.Authorization,
                    "Bearer $token"
                )
            }
        }
    }
}

class AuthService {

    private val baseUrl =
        "http://91.151.84.74:8080/api/auth"

    // ---------------------------------------------------------
    // KAYIT
    // ---------------------------------------------------------

    suspend fun register(
        request: RegisterRequest
    ): Result<AuthResponse> {

        return try {

            val hashedPass =
                HashUtils.sha256(request.sifre)

            val secureRequest =
                request.copy(
                    sifre = hashedPass
                )

            val response: AuthResponse =
                NetworkClient.httpClient
                    .post("$baseUrl/register") {

                        contentType(
                            ContentType.Application.Json
                        )

                        setBody(secureRequest)
                    }
                    .body()

            oturumuKaydet(response)

            Result.success(response)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    // ---------------------------------------------------------
    // GİRİŞ
    // ---------------------------------------------------------

    suspend fun login(
        request: LoginRequest
    ): Result<AuthResponse> {

        return try {

            val hashedPass =
                HashUtils.sha256(request.sifre)

            val secureRequest =
                request.copy(
                    sifre = hashedPass
                )

            val response: AuthResponse =
                NetworkClient.httpClient
                    .post("$baseUrl/login") {

                        contentType(
                            ContentType.Application.Json
                        )

                        setBody(secureRequest)
                    }
                    .body()

            oturumuKaydet(response)

            Result.success(response)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    // ---------------------------------------------------------
    // KULLANICI BİLGİLERİNİ DB'DEN GETİR
    // ---------------------------------------------------------

    suspend fun getKullaniciBilgileri(
        kullaniciId: Long
    ): Result<AuthResponse> {

        return try {

            val response: AuthResponse =
                NetworkClient.httpClient
                    .get(
                        "$baseUrl/kullanici/$kullaniciId"
                    )
                    .body()

            Result.success(response)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    // ---------------------------------------------------------
    // AD SOYAD / İŞLETME ADI DEĞİŞTİR
    // ---------------------------------------------------------

    suspend fun kullaniciAdiGuncelle(
        kullaniciId: Long,
        yeniAdSoyad: String
    ): Result<AuthResponse> {

        return try {

            val response: AuthResponse =
                NetworkClient.httpClient
                    .put(
                        "$baseUrl/kullanici/$kullaniciId/ad-soyad"
                    ) {

                        contentType(
                            ContentType.Application.Json
                        )

                        setBody(
                            KullaniciAdiGuncelleRequest(
                                adSoyad = yeniAdSoyad
                            )
                        )
                    }
                    .body()

            Result.success(response)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    // ---------------------------------------------------------
    // ŞİFRE DEĞİŞTİR
    // ---------------------------------------------------------

    suspend fun sifreDegistir(
        kullaniciId: Long,
        mevcutSifre: String,
        yeniSifre: String
    ): Result<AuthResponse> {

        return try {

            // Mevcut sistemde login/register şifreyi SHA256
            // gönderdiği için aynı mantık burada da kullanılıyor.
            val mevcutHash =
                HashUtils.sha256(mevcutSifre)

            val yeniHash =
                HashUtils.sha256(yeniSifre)

            val response: AuthResponse =
                NetworkClient.httpClient
                    .put(
                        "$baseUrl/kullanici/$kullaniciId/sifre"
                    ) {

                        contentType(
                            ContentType.Application.Json
                        )

                        setBody(
                            SifreDegistirRequest(
                                mevcutSifre = mevcutHash,
                                yeniSifre = yeniHash
                            )
                        )
                    }
                    .body()

            Result.success(response)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    // ---------------------------------------------------------
    // OTURUM
    // ---------------------------------------------------------

    private fun oturumuKaydet(
        response: AuthResponse
    ) {

        if (
            response.basarili &&
            response.kullaniciId != null &&
            response.token != null &&
            response.tokenBitisZamani != null
        ) {

            SessionManager.saveSession(
                token = response.token,
                tokenBitisZamani = response.tokenBitisZamani,
                kullaniciId = response.kullaniciId,
                adSoyad = response.adSoyad.orEmpty(),
                email = response.email.orEmpty()
            )
        }
    }
}