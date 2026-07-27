package com.eray.muhasebeapp

import androidx.compose.ui.window.ComposeUIViewController
import com.eray.muhasebeapp.database.DriverFactory
import com.eray.muhasebeapp.database.shared.AppDatabase
// Apple'ın yerel tarih kütüphanelerini içeri aktarıyoruz
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.timeIntervalSince1970

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

    // 3. iOS için platform veritabanı yöneticisi (yedekleme/geri yükleme için)
    val platformDbManager = PlatformDatabaseManager()

    // 4. Şu anki zamanı milisaniye olarak alıyoruz
    val simdiMillis = (NSDate().timeIntervalSince1970 * 1000).toLong()

    // 5. Tıpkı Android'deki gibi veritabanını, tarihi ve yeni parametreleri App'e paslıyoruz
    App(
        database = database,
        platformDbManager = platformDbManager,
        guncelTarih = iosTarih,
        simdiMillis = simdiMillis,
        onYedekYukleIstegi = {
            // TODO: iOS tarafında yedek yükleme akışı (dosya seçici vs.) henüz bağlanmadı
        }
    )
}
