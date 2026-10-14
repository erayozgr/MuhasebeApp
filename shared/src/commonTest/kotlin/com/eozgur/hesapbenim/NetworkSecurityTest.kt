package com.eozgur.hesapbenim

import com.eozgur.hesapbenim.data.network.createApiClient
import com.eozgur.hesapbenim.util.AppConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NetworkSecurityTest {
    @Test
    fun rejectsUntrustedDestinationsBeforeReadingTokenOrSending() = runTest {
        val client = createApiClient(MockEngine { error("Request escaped the origin guard") }) {
            error("Token read for an untrusted destination")
        }
        try {
            for (url in listOf(
                "http://www.hesapbenim.com/api/config",
                "https://attacker.example/api/config",
                "https://www.hesapbenim.com.attacker.example/api/config",
                "https://www.hesapbenim.com:8443/api/config",
                "https://www.hesapbenim.com/web/giris",
                "https://user:pass@www.hesapbenim.com/api/config"
            )) {
                assertFailsWith<IllegalArgumentException> { client.get(url) }
            }
        } finally { client.close() }
    }

    @Test
    fun doesNotFollowRedirectsAndUsesCurrentToken() = runTest {
        var token: String? = "first-token"
        val headers = mutableListOf<String?>()
        val client = createApiClient(MockEngine { request ->
            headers += request.headers[HttpHeaders.Authorization]
            respond("", HttpStatusCode.TemporaryRedirect,
                headersOf(HttpHeaders.Location, "https://attacker.example/api/config"))
        }) { token }
        try {
            assertEquals(HttpStatusCode.TemporaryRedirect, client.get("${AppConfig.API_BASE_URL}/config").status)
            token = null
            client.get("${AppConfig.API_BASE_URL}/config")
            assertEquals(listOf("Bearer first-token", null), headers)
        } finally { client.close() }
    }
}
