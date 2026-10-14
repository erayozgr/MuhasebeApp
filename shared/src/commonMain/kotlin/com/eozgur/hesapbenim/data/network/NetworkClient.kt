package com.eozgur.hesapbenim.data.network

import com.eozgur.hesapbenim.util.AppConfig
import com.eozgur.hesapbenim.util.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.plugin
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object NetworkClient {
    val httpClient by lazy { createApiClient() }
}

internal fun createApiClient(
    engine: HttpClientEngine? = null,
    tokenProvider: () -> String? = { SessionManager.getToken() }
): HttpClient {
    val config: HttpClientConfig<*>.() -> Unit = {
        // A redirect must never replay credentials or request bodies to another endpoint.
        followRedirects = false
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    val client = if (engine == null) HttpClient(config) else HttpClient(engine, config)
    val allowed = Url(AppConfig.API_BASE_URL)
    client.plugin(HttpSend).intercept { request ->
        val url = request.url.build()
        require(url.protocol == allowed.protocol && url.host == allowed.host &&
            url.port == allowed.port && url.user.isNullOrEmpty() && url.password.isNullOrEmpty() &&
            url.encodedPath.startsWith("${allowed.encodedPath}/")) {
            "Güvenli olmayan API adresi engellendi."
        }
        request.headers.remove(HttpHeaders.Authorization)
        tokenProvider()?.takeIf { it.isNotBlank() }?.let {
            request.headers.append(HttpHeaders.Authorization, "Bearer $it")
        }
        execute(request)
    }
    return client
}
