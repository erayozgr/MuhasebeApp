package com.eray.muhasebeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeUIViewController
import com.eray.muhasebeapp.database.DriverFactory
import com.eray.muhasebeapp.database.shared.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.timeIntervalSince1970

fun MainViewController() =
    ComposeUIViewController {

        val driverFactory =
            remember {
                DriverFactory()
            }

        /*
         * Aktif SQLite driver.
         */
        var sqliteDriver by remember {

            mutableStateOf(
                driverFactory.createDriver()
            )
        }

        /*
         * Aktif AppDatabase.
         */
        var database by remember {

            mutableStateOf(
                AppDatabase(
                    sqliteDriver
                )
            )
        }

        /*
         * Yedek geri yükleme ekranı.
         */
        var yedekYukleniyor by remember {
            mutableStateOf(false)
        }

        /*
         * Sonuç dialogu.
         */
        var sonucMesaji by remember {
            mutableStateOf<String?>(null)
        }

        var sonucBasarili by remember {
            mutableStateOf(false)
        }

        val coroutineScope =
            rememberCoroutineScope()

        /*
         * Tarih.
         */
        val formatter =
            remember {

                NSDateFormatter().apply {

                    dateFormat =
                        "d MMMM, EEEE"

                    locale =
                        NSLocale(
                            localeIdentifier = "tr_TR"
                        )
                }
            }

        val iosTarih =
            formatter.stringFromDate(
                NSDate()
            )

        /*
         * Platform DB Manager.
         */
        val platformDbManager =
            remember {
                PlatformDatabaseManager()
            }

        /*
         * ÇOK ÖNEMLİ:
         *
         * Yedek alınırken WAL checkpoint yapabilmesi için
         * manager'a mevcut aktif driver'ı veriyoruz.
         *
         * Restore sonrası sqliteDriver değiştiğinde
         * Compose yeniden çalışır ve yeni driver buraya gelir.
         */
        platformDbManager.setDriver(
            sqliteDriver
        )

        /*
         * Dosya seçici mutlaka remember içinde tutulmalı.
         */
        val iosDosyaSecici =
            remember {
                IosDosyaSecici()
            }

        val simdiMillis =
            (
                    NSDate().timeIntervalSince1970 *
                            1000
                    ).toLong()

        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {

            /*
             * Restore yapılmıyorsa normal uygulama.
             */
            if (!yedekYukleniyor) {

                App(
                    database = database,

                    platformDbManager =
                        platformDbManager,

                    guncelTarih =
                        iosTarih,

                    simdiMillis =
                        simdiMillis,

                    onYedekYukleIstegi = {

                        println(
                            "iOS: DB Yedek Yükle tıklandı."
                        )

                        iosDosyaSecici
                            .dosyaSec { bytes ->

                                if (
                                    bytes == null ||
                                    bytes.isEmpty()
                                ) {

                                    sonucBasarili =
                                        false

                                    sonucMesaji =
                                        "Yedek dosyası seçilemedi veya dosya boş."

                                    return@dosyaSec
                                }

                                println(
                                    "iOS: Yedek seçildi. " +
                                            "Boyut=${bytes.size} byte"
                                )

                                /*
                                 * Progress ekranını aç.
                                 */
                                yedekYukleniyor =
                                    true

                                sonucMesaji =
                                    null

                                coroutineScope.launch {

                                    try {

                                        println(
                                            "================================"
                                        )

                                        println(
                                            "iOS: YEDEK GERİ YÜKLEME BAŞLADI"
                                        )

                                        println(
                                            "================================"
                                        )

                                        /*
                                         * DB dosyası üzerinde işlem yapacağımız
                                         * için IO işlemlerini arka tarafta yap.
                                         */
                                        val basarili =
                                            withContext(
                                                Dispatchers.Default
                                            ) {

                                                /*
                                                 * Eski SQLite bağlantısını kapat.
                                                 */
                                                println(
                                                    "iOS: Eski driver kapatılıyor."
                                                )

                                                sqliteDriver.close()

                                                println(
                                                    "iOS: Eski driver kapatıldı."
                                                )

                                                /*
                                                 * Yedeği gerçek DB'nin
                                                 * yerine koy.
                                                 */
                                                platformDbManager
                                                    .restoreDatabaseBytes(
                                                        bytes
                                                    )
                                            }

                                        /*
                                         * Eski driver artık kapalı.
                                         * Her durumda YENİ driver aç.
                                         */
                                        println(
                                            "iOS: Yeni SQLite driver açılıyor."
                                        )

                                        val yeniDriver =
                                            driverFactory
                                                .createDriver()

                                        val yeniDatabase =
                                            AppDatabase(
                                                yeniDriver
                                            )

                                        /*
                                         * Yeni DB'yi gerçekten okuyabiliyor
                                         * muyuz kontrol et.
                                         */
                                        val musteriSayisi =
                                            yeniDatabase
                                                .appDatabaseQueries
                                                .selectAllMusteri()
                                                .executeAsList()
                                                .size

                                        println(
                                            "iOS: Restore sonrası müşteri sayısı = " +
                                                    musteriSayisi
                                        )

                                        /*
                                         * Yeni driver ve database'i
                                         * uygulamaya geçir.
                                         */
                                        sqliteDriver =
                                            yeniDriver

                                        database =
                                            yeniDatabase

                                        /*
                                         * Manager'a da yeni driver'ı ver.
                                         */
                                        platformDbManager
                                            .setDriver(
                                                yeniDriver
                                            )

                                        if (basarili) {

                                            sonucBasarili =
                                                true

                                            sonucMesaji =
                                                "Yedek başarıyla geri yüklendi.\n\n" +
                                                        "Müşteri sayısı: $musteriSayisi"

                                            println(
                                                "================================"
                                            )

                                            println(
                                                "iOS: YEDEK GERİ YÜKLEME BAŞARILI"
                                            )

                                            println(
                                                "Müşteri sayısı = $musteriSayisi"
                                            )

                                            println(
                                                "================================"
                                            )

                                        } else {

                                            sonucBasarili =
                                                false

                                            sonucMesaji =
                                                "Yedek geri yüklenemedi."

                                            println(
                                                "iOS: Restore false döndü."
                                            )
                                        }

                                    } catch (
                                        e: Throwable
                                    ) {

                                        println(
                                            "iOS: Yedek yükleme hatası: " +
                                                    "${e.message}"
                                        )

                                        e.printStackTrace()

                                        sonucBasarili =
                                            false

                                        sonucMesaji =
                                            "Yedek yüklenirken hata oluştu.\n\n" +
                                                    (
                                                            e.message
                                                                ?: "Bilinmeyen hata"
                                                            )

                                        /*
                                         * Driver kapatılmışsa uygulamanın
                                         * DB'siz kalmaması için tekrar aç.
                                         */
                                        try {

                                            val yeniDriver =
                                                driverFactory
                                                    .createDriver()

                                            sqliteDriver =
                                                yeniDriver

                                            database =
                                                AppDatabase(
                                                    yeniDriver
                                                )

                                            platformDbManager
                                                .setDriver(
                                                    yeniDriver
                                                )

                                            println(
                                                "iOS: DB bağlantısı yeniden açıldı."
                                            )

                                        } catch (
                                            driverException: Throwable
                                        ) {

                                            println(
                                                "iOS: Driver tekrar açılamadı: " +
                                                        "${driverException.message}"
                                            )

                                            driverException
                                                .printStackTrace()
                                        }

                                    } finally {

                                        /*
                                         * Progress ekranını kapat.
                                         */
                                        yedekYukleniyor =
                                            false
                                    }
                                }
                            }
                    }
                )

            } else {

                /*
                 * =====================================
                 * YEDEK YÜKLENİYOR EKRANI
                 * =====================================
                 */
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                Color(
                                    0xFF0B1F3A
                                )
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 32.dp
                                ),
                        shape =
                            RoundedCornerShape(
                                20.dp
                            ),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color.White
                            )
                    ) {

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        24.dp
                                    ),
                            horizontalAlignment =
                                Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.Center
                        ) {

                            Text(
                                text =
                                    "Yedek Yükleniyor",
                                fontSize =
                                    20.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                color =
                                    Color(
                                        0xFF0B1F3A
                                    )
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        12.dp
                                    )
                            )

                            Text(
                                text =
                                    "Veritabanı geri yükleniyor. " +
                                            "Lütfen uygulamayı kapatmayın.",
                                fontSize =
                                    14.sp,
                                color =
                                    Color.DarkGray
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        24.dp
                                    )
                            )

                            LinearProgressIndicator(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(
                                            6.dp
                                        )
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        12.dp
                                    )
                            )

                            Text(
                                text =
                                    "Veriler hazırlanıyor...",
                                fontSize =
                                    13.sp,
                                color =
                                    Color.Gray
                            )
                        }
                    }
                }
            }

            /*
             * =====================================
             * SONUÇ DIALOGU
             * =====================================
             */
            sonucMesaji?.let { mesaj ->

                AlertDialog(
                    onDismissRequest = {

                        sonucMesaji =
                            null
                    },

                    title = {

                        Text(
                            text =
                                if (
                                    sonucBasarili
                                ) {
                                    "Yedek Yüklendi"
                                } else {
                                    "Yükleme Başarısız"
                                },
                            fontWeight =
                                FontWeight.Bold
                        )
                    },

                    text = {

                        Text(
                            text = mesaj
                        )
                    },

                    confirmButton = {

                        TextButton(
                            onClick = {

                                sonucMesaji =
                                    null
                            }
                        ) {

                            Text(
                                "Tamam"
                            )
                        }
                    }
                )
            }
        }
    }