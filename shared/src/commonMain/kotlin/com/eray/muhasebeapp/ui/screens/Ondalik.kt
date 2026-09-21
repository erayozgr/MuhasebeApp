package com.eray.muhasebeapp.ui.screens

import kotlin.math.round

/** Virgül/nokta kabul edilir; karışık ayırıcılar geçersiz kalır. */
internal fun ondalikGirdi(text: String): String {
    val normalized = text.replace(',', '.')
    if (!Regex("-?\\d*(\\.\\d*)?").matches(normalized)) return normalized
    val separator = normalized.indexOf('.')
    return if (separator >= 0) normalized.take(separator + 3) else normalized
}

internal fun ondalikSayi(text: String): Double? {
    if (!Regex("-?(\\d+(\\.\\d{0,2})?|\\.\\d{1,2})").matches(text.replace(',', '.'))) return null
    return text.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
}

internal fun miktarYuvarla(value: Double): Double {
    val rounded = round(value * 100) / 100
    return if (rounded == 0.0) 0.0 else rounded
}

internal fun miktarMetni(value: Double): String = miktarYuvarla(value).toString().removeSuffix(".0").replace('.', ',')
