package com.eray.muhasebeapp.data.network

import com.eray.muhasebeapp.data.model.AppConfigDto
import io.ktor.client.call.body
import io.ktor.client.request.get

class AppConfigService {

    // Spring Boot endpoint adresin
    private val configUrl = "http://91.151.84.74:8080/api/config"

    suspend fun getAppConfig(): Result<AppConfigDto> {
        return try {
            val response: AppConfigDto = NetworkClient.httpClient
                .get(configUrl)
                .body()

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}