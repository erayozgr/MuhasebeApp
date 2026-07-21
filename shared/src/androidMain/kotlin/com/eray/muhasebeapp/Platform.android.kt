package com.eray.muhasebeapp

import android.os.Build
import java.util.Calendar

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getEpochMillis(): Long {
    return java.lang.System.currentTimeMillis()
}


actual fun getBugununTarihiString(): String {
    val calendar = Calendar.getInstance()
    val yil = calendar.get(Calendar.YEAR)
    // Calendar sınıfında aylar 0'dan başladığı için 1 ekliyoruz
    val ay = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
    val gun = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')

    return "$yil-$ay-$gun" // Örn: "2026-07-15"
}