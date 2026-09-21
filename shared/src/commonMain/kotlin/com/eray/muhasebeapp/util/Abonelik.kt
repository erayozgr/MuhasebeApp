package com.eray.muhasebeapp.util

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

const val ODEME_UYARISI = "Son ödeme tarihiniz geçti. Uygulamayı kullanmaya devam etmek için lütfen web sitemiz üzerinden ödeme yapın."

fun odemeSuresiDoldu(sonOdemeTarihi: Long?, simdi: Long): Boolean =
    sonOdemeTarihi != null && sonOdemeTarihi <= simdi

fun odemeTarihiMetni(tarih: Long?): String {
    if (tarih == null) return "Belirlenmedi"
    val gun = Instant.fromEpochMilliseconds(tarih).toLocalDateTime(TimeZone.of("Europe/Istanbul")).date
    return "${gun.dayOfMonth.toString().padStart(2, '0')}.${gun.monthNumber.toString().padStart(2, '0')}.${gun.year}"
}

// Accept the server's LocalDateTime and legacy epoch milliseconds.
object OdemeTarihiSerializer : KSerializer<Long> {
    override val descriptor = PrimitiveSerialDescriptor("OdemeTarihi", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Long {
        val value = ((decoder as? JsonDecoder)?.decodeJsonElement() as? JsonPrimitive)?.content
            ?: throw SerializationException("Geçersiz son ödeme tarihi")
        value.toLongOrNull()?.let { return it }
        return try {
            val instant = runCatching { Instant.parse(value) }.getOrElse {
                LocalDateTime.parse(value).toInstant(TimeZone.of("Europe/Istanbul"))
            }
            instant.toEpochMilliseconds()
        } catch (error: IllegalArgumentException) {
            throw SerializationException("Geçersiz son ödeme tarihi", error)
        }
    }

    override fun serialize(encoder: Encoder, value: Long) =
        encoder.encodeString(Instant.fromEpochMilliseconds(value).toString())
}
