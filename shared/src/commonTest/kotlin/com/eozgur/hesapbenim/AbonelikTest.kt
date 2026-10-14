package com.eozgur.hesapbenim

import com.eozgur.hesapbenim.data.model.AuthResponse
import com.eozgur.hesapbenim.util.odemeYapGoster
import com.eozgur.hesapbenim.util.odemeSuresiDoldu
import com.eozgur.hesapbenim.util.odemeTarihiMetni
import kotlinx.datetime.Instant
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.*

class AbonelikTest {
    private fun decode(value: String) = Json.decodeFromString<AuthResponse>(
        """{"basarili":true,"mesaj":"OK","sonOdemeTarihi":$value}"""
    ).sonOdemeTarihi

    @Test fun serverDateUsesTurkeyTime() {
        val expected = Instant.parse("2026-09-25T09:30:00Z").toEpochMilliseconds()
        assertEquals(expected, decode("\"2026-09-25T12:30:00\""))
        assertEquals(expected, decode("\"2026-09-25T09:30:00Z\""))
        assertEquals(expected, decode("\"2026-09-25T12:30:00+03:00\""))
        assertEquals(expected, decode(expected.toString()))
        assertEquals("25.09.2026", odemeTarihiMetni(expected))
    }

    @Test fun deadlineBoundaryAndOldAccounts() {
        assertFalse(odemeSuresiDoldu(1000, 999))
        assertTrue(odemeSuresiDoldu(1000, 1000))
        assertTrue(odemeSuresiDoldu(1000, 1001))
        assertNull(decode("null"))
        assertFalse(odemeSuresiDoldu(null, 1000))
        assertNull(Json.decodeFromString<AuthResponse>("""{"basarili":true,"mesaj":"OK"}""").sonOdemeTarihi)
    }

    @Test fun paymentButtonUsesThreeTurkeyCalendarDays() {
        val now = Instant.parse("2026-09-22T21:00:00Z").toEpochMilliseconds()
        fun at(value: String) = Instant.parse(value).toEpochMilliseconds()
        assertFalse(odemeYapGoster(null, now))
        assertFalse(odemeYapGoster(at("2026-09-27T00:00:00+03:00"), now))
        assertTrue(odemeYapGoster(at("2026-09-26T23:59:59+03:00"), now))
        assertTrue(odemeYapGoster(at("2026-09-25T12:00:00+03:00"), now))
        assertTrue(odemeYapGoster(at("2026-09-24T12:00:00+03:00"), now))
        assertTrue(odemeYapGoster(at("2026-09-23T23:59:59+03:00"), now))
        assertTrue(odemeYapGoster(at("2026-09-22T23:59:59+03:00"), now))
        assertFalse(odemeSuresiDoldu(at("2026-09-26T23:59:59+03:00"), now))
    }

    @Test fun malformedDateDoesNotUnlockAccount() {
        assertFailsWith<SerializationException> { decode("\"invalid\"") }
    }
}
