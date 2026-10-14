package com.eozgur.hesapbenim.data.network

import com.eozgur.hesapbenim.data.model.AuthResponse
import com.eozgur.hesapbenim.data.model.KullaniciAdiGuncelleRequest
import com.eozgur.hesapbenim.data.model.LoginRequest
import com.eozgur.hesapbenim.data.model.RegisterRequest
import com.eozgur.hesapbenim.data.model.SifreDegistirRequest
import com.eozgur.hesapbenim.util.HashUtils
import com.eozgur.hesapbenim.util.SessionManager
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import com.eozgur.hesapbenim.data.model.EmailDogrulamaRequest
import com.eozgur.hesapbenim.data.model.KayitEmailDogrulamaRequest
import com.eozgur.hesapbenim.data.model.HesapSilRequest
import kotlinx.serialization.Serializable


class AuthService {

    @Serializable
    private data class ParaBirimiTercihiRequest(val tercihEdilenParaBirimi: String)

    suspend fun paraBirimiTercihiniGuncelle(kullaniciId: Long, paraBirimi: String): Result<AuthResponse> = try {
        val response: AuthResponse = NetworkClient.httpClient.put("$baseUrl/kullanici/$kullaniciId/para-birimi") {
            contentType(ContentType.Application.Json)
            setBody(ParaBirimiTercihiRequest(paraBirimi))
        }.body()
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Revoke exactly the session that was closed, even if a new login starts meanwhile. */
    suspend fun revokeSession(token: String): Result<Unit> {
        val client = createApiClient(tokenProvider = { token })
        return try {
            val response = client.post("${com.eozgur.hesapbenim.util.AppConfig.API_BASE_URL}/oturum/cikis")
            check(response.status.value in 200..299) { "Oturum sunucuda kapatılamadı." }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            client.close()
        }
    }

    private val baseUrl =
        "${com.eozgur.hesapbenim.util.AppConfig.API_BASE_URL}/auth"

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



    // ---------------------------------------------------------
// E-POSTA DOĞRULAMA KODU GÖNDER
// ---------------------------------------------------------

    suspend fun emailDogrulamaKoduGonder(
        kullaniciId: Long
    ): Result<AuthResponse> {

        return try {

            val response: AuthResponse =
                NetworkClient.httpClient
                    .post(
                        "$baseUrl/kullanici/$kullaniciId/email-dogrulama-kodu"
                    ) {

                        contentType(
                            ContentType.Application.Json
                        )
                    }
                    .body()

            Result.success(response)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }


// ---------------------------------------------------------
// E-POSTA DOĞRULA
// ---------------------------------------------------------

    suspend fun emailDogrula(
        kullaniciId: Long,
        kod: String
    ): Result<AuthResponse> {

        return try {

            val response: AuthResponse =
                NetworkClient.httpClient
                    .post(
                        "$baseUrl/kullanici/$kullaniciId/email-dogrula"
                    ) {

                        contentType(
                            ContentType.Application.Json
                        )

                        setBody(
                            EmailDogrulamaRequest(
                                kod = kod
                            )
                        )
                    }
                    .body()

            Result.success(response)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    suspend fun kayitEmailDogrula(
        email: String,
        kod: String
    ): Result<AuthResponse> {

        return try {

            val response: AuthResponse =
                NetworkClient.httpClient
                    .post(
                        "$baseUrl/register/email-dogrula"
                    ) {

                        contentType(
                            ContentType.Application.Json
                        )

                        setBody(
                            KayitEmailDogrulamaRequest(
                                email = email,
                                kod = kod
                            )
                        )
                    }
                    .body()


            oturumuKaydet(
                response
            )


            Result.success(
                response
            )

        } catch (e: Exception) {

            Result.failure(
                e
            )
        }
    }

    suspend fun hesapSilmeKoduGonder(
        kullaniciId: Long
    ): Result<AuthResponse> {

        return try {

            val response: AuthResponse =
                NetworkClient.httpClient
                    .post(
                        "$baseUrl/kullanici/$kullaniciId/hesap-silme-kodu"
                    ) {

                        contentType(
                            ContentType.Application.Json
                        )
                    }
                    .body()


            Result.success(
                response
            )


        } catch (e: Exception) {

            Result.failure(
                e
            )
        }
    }


    suspend fun hesabiSil(
        kullaniciId: Long,
        kod: String
    ): Result<AuthResponse> {

        return try {

            val response: AuthResponse =
                NetworkClient.httpClient
                    .post(
                        "$baseUrl/kullanici/$kullaniciId/hesap-sil"
                    ) {

                        contentType(
                            ContentType.Application.Json
                        )

                        setBody(
                            HesapSilRequest(
                                kod = kod
                            )
                        )
                    }
                    .body()


            Result.success(
                response
            )


        } catch (e: Exception) {

            Result.failure(
                e
            )
        }
    }
}
