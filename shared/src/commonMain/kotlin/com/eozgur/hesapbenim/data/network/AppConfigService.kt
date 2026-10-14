package com.eozgur.hesapbenim.data.network

import com.eozgur.hesapbenim.data.model.AppConfigDto
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get

class AppConfigService {

    // Spring Boot endpoint adresin
    private val configUrl = "${com.eozgur.hesapbenim.util.AppConfig.API_BASE_URL}/config"

    suspend fun getAppConfig(): Result<AppConfigDto> {
        return try {
            val response: AppConfigDto = NetworkClient.httpClient
                .get(configUrl) { expectSuccess = true }
                .body()

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}