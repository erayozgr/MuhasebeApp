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
    val driverFactory = DriverFactory()
    val sqliteDriver = driverFactory.createDriver()
    val database = AppDatabase(sqliteDriver)

    // 2. iOS için Türkçe Tarihi Hazırlıyoruz (Örn: "2 Temmuz, Perşembe")
    val formatter = NSDateFormatter().apply {
        dateFormat = "d MMMM, EEEE"
        locale = NSLocale(localeIdentifier = "tr_TR")
    }
    val iosTarih = formatter.stringFromDate(NSDate())

    // 3. iOS için platform veritabanı yöneticisi (yedekleme/geri yükleme için)
    val platformDbManager = PlatformDatabaseManager()

    // 3b. 🎯 Yedek yükleme (import) için dosya seçici — tek instance yeterli
    val iosDosyaSecici = IosDosyaSecici()

    // 4. Şu anki zamanı milisaniye olarak alıyoruz
    val simdiMillis = (NSDate().timeIntervalSince1970 * 1000).toLong()

    // 5. Tıpkı Android'deki gibi veritabanını, tarihi ve yeni parametreleri App'e paslıyoruz
    App(
        database = database,
        platformDbManager = platformDbManager,
        guncelTarih = iosTarih,
        simdiMillis = simdiMillis,
        onYedekYukleIstegi = {
            iosDosyaSecici.dosyaSec { bytes ->
                if (bytes != null) {
                    val basarili = platformDbManager.restoreDatabaseBytes(bytes)
                    if (basarili) {
                        // ⚠️ ÖNEMLİ: 'database' zaten eski dosya üzerinden açılmış bir
                        // SQLDelight bağlantısı tutuyor. Dosyayı diskte değiştirmek
                        // bu haliyle uygulama içindeki verileri anında güncellemez.
                        // En güvenli yol: kullanıcıyı bilgilendirip uygulamayı yeniden
                        // başlatmasını istemek (veya burada sqliteDriver'ı kapatıp
                        // driverFactory.createDriver() ile yeni bir AppDatabase daha
                        // oluşturup App'in state'ini yeniden set etmek).
                        println("Yedek başarıyla geri yüklendi. Uygulamayı yeniden başlatın.")
                    } else {
                        println("Yedek geri yüklenirken hata oluştu.")
                    }
                }
            }
        }
    )
}