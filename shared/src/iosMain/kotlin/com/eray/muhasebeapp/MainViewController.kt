package com.eray.muhasebeapp

import androidx.compose.ui.window.ComposeUIViewController
import com.eray.muhasebeapp.database.DriverFactory
import com.eray.muhasebeapp.database.shared.AppDatabase
// Apple'ın yerel tarih kütüphanelerini içeri aktarıyoruz
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale

fun MainViewController() = ComposeUIViewController {

    // 1. iOS için SQLite Sürücüsünü Başlatıyoruz
    // iOS tarafında 'context' olmadığı için DriverFactory() boş çağrılır
    val driverFactory = DriverFactory()
    val sqliteDriver = driverFactory.createDriver()
    val database = AppDatabase(sqliteDriver)

    // 2. iOS için Türkçe Tarihi Hazırlıyoruz (Örn: "2 Temmuz, Perşembe")
    val formatter = NSDateFormatter().apply {
        dateFormat = "d MMMM, EEEE"
        locale = NSLocale(localeIdentifier = "tr_TR") // Gün isimlerinin Türkçe gelmesi için
    }
    val iosTarih = formatter.stringFromDate(NSDate())

    // 3. Tıpkı Android'deki gibi veritabanını ve tarihi App'e paslıyoruz
    App(
        database = database,
        guncelTarih = iosTarih
    )
}