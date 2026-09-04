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

    /*
     * DriverFactory uygulama boyunca aynı nesne olarak kalabilir.
     * createDriver() her çağrıldığında YENİ driver üretir.
     */
    val driverFactory = remember {
        DriverFactory()
    }

    /*
     * İlk SQLite driver.
     */
    var sqliteDriver by remember {
        mutableStateOf(
            driverFactory.createDriver()
        )
    }

    /*
     * AppDatabase aktif driver üzerinden oluşturuluyor.
     *
     * Restore sonrasında bu state değişeceği için
     * Compose yeni database nesnesini kullanacak.
     */
    var database by remember {
        mutableStateOf(
            AppDatabase(sqliteDriver)
        )
    }

    /*
     * Tarih
     */
    val formatter = remember {

        NSDateFormatter().apply {

            dateFormat = "d MMMM, EEEE"

            locale = NSLocale(
                localeIdentifier = "tr_TR"
            )
        }
    }

    val iosTarih =
        formatter.stringFromDate(
            NSDate()
        )

    /*
     * Platform DB yöneticisi
     */
    val platformDbManager = remember {
        PlatformDatabaseManager()
    }

    /*
     * ÇOK ÖNEMLİ:
     *
     * IosDosyaSecici remember içinde tutuluyor.
     * Böylece UIDocumentPicker delegate nesnesi
     * dosya seçilirken yok olmaz.
     */
    val iosDosyaSecici = remember {
        IosDosyaSecici()
    }

    val simdiMillis =
        (
                NSDate().timeIntervalSince1970 *
                        1000
                ).toLong()

    App(
        database = database,

        platformDbManager = platformDbManager,

        guncelTarih = iosTarih,

        simdiMillis = simdiMillis,

        onYedekYukleIstegi = {

            println(
                "iOS: Yedek yükleme isteği başladı."
            )

            iosDosyaSecici.dosyaSec { bytes ->

                /*
                 * Dosya seçilemediyse işlem yok.
                 */
                if (
                    bytes == null ||
                    bytes.isEmpty()
                ) {

                    println(
                        "iOS: Yedek seçilmedi veya dosya boş."
                    )

                    return@dosyaSec
                }

                println(
                    "iOS: Seçilen yedek boyutu = " +
                            "${bytes.size} byte"
                )

                try {

                    println(
                        "=============================="
                    )

                    println(
                        "iOS: YEDEK GERİ YÜKLEME BAŞLADI"
                    )

                    println(
                        "=============================="
                    )

                    /*
                     * 1.
                     *
                     * ÇOK ÖNEMLİ:
                     * DB dosyasına dokunmadan önce
                     * açık SQLDelight/SQLite bağlantısını kapat.
                     */
                    println(
                        "iOS: Eski SQLite driver kapatılıyor..."
                    )

                    sqliteDriver.close()

                    println(
                        "iOS: Eski SQLite driver kapatıldı."
                    )

                    /*
                     * 2.
                     *
                     * Seçilen ByteArray'i mevcut muhasebe.db
                     * dosyasının yerine koyuyoruz.
                     */
                    println(
                        "iOS: DB dosyası geri yükleniyor..."
                    )

                    val basarili =
                        platformDbManager
                            .restoreDatabaseBytes(
                                bytes
                            )

                    /*
                     * 3.
                     *
                     * Eski driver kapalı.
                     * Mutlaka YENİ bir NativeSqliteDriver açıyoruz.
                     */
                    println(
                        "iOS: Yeni SQLite driver oluşturuluyor..."
                    )

                    val yeniDriver =
                        driverFactory.createDriver()

                    /*
                     * State'i yeni driver'a geçiriyoruz.
                     */
                    sqliteDriver =
                        yeniDriver

                    /*
                     * 4.
                     *
                     * AppDatabase'i yeni driver üzerinden
                     * yeniden oluştur.
                     */
                    database =
                        AppDatabase(
                            yeniDriver
                        )

                    println(
                        "iOS: AppDatabase yeniden oluşturuldu."
                    )

                    if (basarili) {

                        println(
                            "=============================="
                        )

                        println(
                            "iOS: YEDEK BAŞARIYLA " +
                                    "GERİ YÜKLENDİ"
                        )

                        println(
                            "=============================="
                        )

                    } else {

                        println(
                            "=============================="
                        )

                        println(
                            "iOS: YEDEK GERİ YÜKLEME " +
                                    "BAŞARISIZ"
                        )

                        println(
                            "=============================="
                        )
                    }

                } catch (e: Throwable) {

                    println(
                        "iOS: Yedek yükleme hatası: " +
                                "${e.message}"
                    )

                    e.printStackTrace()

                    /*
                     * Eski driver kapatılmış olabilir.
                     *
                     * Uygulamanın DB bağlantısız kalmaması
                     * için yeni driver açmayı deniyoruz.
                     */
                    try {

                        println(
                            "iOS: DB bağlantısı tekrar açılıyor..."
                        )

                        val yeniDriver =
                            driverFactory.createDriver()

                        sqliteDriver =
                            yeniDriver

                        database =
                            AppDatabase(
                                yeniDriver
                            )

                        println(
                            "iOS: DB bağlantısı tekrar açıldı."
                        )

                    } catch (
                        driverException: Throwable
                    ) {

                        println(
                            "iOS: Driver tekrar açılamadı: " +
                                    "${driverException.message}"
                        )

                        driverException.printStackTrace()
                    }
                }
            }
        }
    )
}