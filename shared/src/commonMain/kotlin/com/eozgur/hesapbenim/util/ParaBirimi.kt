package com.eozgur.hesapbenim.util

import kotlin.math.abs

enum class GosterimParaBirimi(val kod: String, val simge: String) {
    TRY("TRY", "₺"), USD("USD", "$"), EUR("EUR", "€");

    companion object {
        fun fromKod(kod: String?): GosterimParaBirimi =
            entries.firstOrNull { it.kod == kod } ?: TRY
    }
}

object ParaBirimiDurumu {
    val secili = kotlinx.coroutines.flow.MutableStateFlow(GosterimParaBirimi.TRY)
    var usdTry: Double? = null
    var eurTry: Double? = null
}

fun paraKurAyarla(usdTry: Double?, eurTry: Double?) {
    ParaBirimiDurumu.usdTry = usdTry
    ParaBirimiDurumu.eurTry = eurTry
}

fun kurMetni(deger: Double?, basamak: Int = 2): String =
    if (deger == null) "—" else paraMetni(deger, basamak, 1.0, 1.0)

fun paraSimge(): String = ParaBirimiDurumu.secili.value.simge

fun paraMetni(tlDegeri: Double?, basamak: Int = 2, usdTry: Double? = ParaBirimiDurumu.usdTry, eurTry: Double? = ParaBirimiDurumu.eurTry): String {
    if (tlDegeri == null) return "—"
    val birim = ParaBirimiDurumu.secili.value
    val kur = when (birim) {
        GosterimParaBirimi.TRY -> 1.0
        GosterimParaBirimi.USD -> usdTry?.takeIf { it > 0.0 } ?: 1.0
        GosterimParaBirimi.EUR -> eurTry?.takeIf { it > 0.0 } ?: 1.0
    }
    val displayed = tlDegeri / kur
    val scale = when (basamak) { 0 -> 1.0; 1 -> 10.0; 2 -> 100.0; 3 -> 1000.0; else -> 10000.0 }
    val rounded = (abs(displayed) * scale + 0.5).toLong()
    val integer = (rounded / scale.toLong()).toString().reversed().chunked(3).joinToString(".").reversed()
    val decimals = (rounded % scale.toLong()).toString().padStart(basamak, '0')
    return (if (displayed < 0) "-" else "") + integer + if (basamak > 0) ",$decimals" else ""
}

fun paraGoster(tlDegeri: Double?, basamak: Int = 2): String =
    "${paraSimge()}${paraMetni(tlDegeri, basamak, ParaBirimiDurumu.usdTry, ParaBirimiDurumu.eurTry)}"
