package com.eray.muhasebeapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import com.eray.muhasebeapp.database.DriverFactory
import com.eray.muhasebeapp.database.shared.AppDatabase
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.timeIntervalSince1970

fun MainViewController() = ComposeUIViewController {

    val driverFactory = remember {
        DriverFactory()
    }

    // Driver state olarak tutuluyor.
    // Yedek geri yüklenince eski driver kapanacak ve yenisi oluşturulacak.
    var sqliteDriver by remember {
        mutableStateOf(driverFactory.createDriver())
    }

    // Database de state.
    // Yeni DB oluşturulunca Compose yeniden çizilecek.
    var database by remember {
        mutableStateOf(AppDatabase(sqliteDriver))
    }

    val formatter = remember {
        NSDateFormatter().apply {
            dateFormat = "d MMMM, EEEE"
            locale = NSLocale(localeIdentifier = "tr_TR")
        }
    }

    val iosTarih = formatter.stringFromDate(NSDate())

    val platformDbManager = remember {
        PlatformDatabaseManager()
    }

    val iosDosyaSecici = remember {
        IosDosyaSecici()
    }

    val simdiMillis =
        (NSDate().timeIntervalSince1970 * 1000).toLong()

    App(
        database = database,
        platformDbManager = platformDbManager,
        guncelTarih = iosTarih,
        simdiMillis = simdiMillis,

        onYedekYukleIstegi = {

            iosDosyaSecici.dosyaSec { bytes ->

                if (bytes == null || bytes.isEmpty()) {
                    println("Yedek seçilmedi veya dosya boş.")
                    return@dosyaSec
                }

                try {

                    println("Yedek geri yükleme başlatılıyor...")

                    /*
                     * ÇOK ÖNEMLİ:
                     * DB dosyasını değiştirmeden önce açık SQLite
                     * bağlantısını mutlaka kapatıyoruz.
                     */
                    sqliteDriver.close()

                    val basarili =
                        platformDbManager.restoreDatabaseBytes(bytes)

                    /*
                     * Restore başarılı olsa da olmasa da yeni driver
                     * oluşturuyoruz. Çünkü eski driver kapatıldı.
                     */
                    sqliteDriver =
                        driverFactory.createDriver()

                    database =
                        AppDatabase(sqliteDriver)

                    if (basarili) {
                        println("Yedek başarıyla geri yüklendi.")
                    } else {
                        println("Yedek geri yüklenirken hata oluştu.")
                    }

                } catch (e: Exception) {

                    println(
                        "Yedek yükleme hatası: ${e.message}"
                    )

                    /*
                     * Hata durumunda uygulamanın DB'siz kalmaması
                     * için driver'ı tekrar açıyoruz.
                     */
                    try {
                        sqliteDriver =
                            driverFactory.createDriver()

                        database =
                            AppDatabase(sqliteDriver)

                    } catch (driverException: Exception) {
                        println(
                            "Driver tekrar açılamadı: " +
                                    "${driverException.message}"
                        )
                    }
                }
            }
        }
    )
}