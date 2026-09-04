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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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

        /*
         * DriverFactory
         */
        val driverFactory =
            remember {
                DriverFactory()
            }

        /*
         * Aktif SQLDelight Driver
         */
        var sqliteDriver by remember {

            mutableStateOf(
                driverFactory.createDriver()
            )
        }

        /*
         * Aktif AppDatabase
         */
        var database by remember {

            mutableStateOf(
                AppDatabase(
                    sqliteDriver
                )
            )
        }

        /*
         * Restore UI durumları
         */
        var yedekYukleniyor by remember {
            mutableStateOf(false)
        }

        var sonucMesaji by remember {
            mutableStateOf<String?>(null)
        }

        var sonucBasarili by remember {
            mutableStateOf(false)
        }

        val coroutineScope =
            rememberCoroutineScope()

        /*
         * Tarih
         */
        val formatter =
            remember {

                NSDateFormatter().apply {

                    dateFormat =
                        "d MMMM, EEEE"

                    locale =
                        NSLocale(
                            localeIdentifier =
                                "tr_TR"
                        )
                }
            }

        val iosTarih =
            formatter.stringFromDate(
                NSDate()
            )

        /*
         * DB Manager
         */
        val platformDbManager =
            remember {

                PlatformDatabaseManager()
            }

        /*
         * Dosya seçiciyi MUTLAKA remember ile tut.
         */
        val iosDosyaSecici =
            remember {

                IosDosyaSecici()
            }

        val simdiMillis =
            (
                    NSDate()
                        .timeIntervalSince1970 *
                            1000
                    ).toLong()

        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {

            /*
             * Restore sırasında App'i composition'dan
             * çıkarıyoruz.
             *
             * Çünkü driver kapalıyken eski database
             * üzerinden sorgu yapılmasını istemiyoruz.
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
                                            "Boyut=${bytes.size}"
                                )

                                /*
                                 * Yükleme ekranını hemen aç.
                                 */
                                yedekYukleniyor =
                                    true

                                sonucMesaji =
                                    null

                                coroutineScope.launch {

                                    try {

                                        println(
                                            "iOS: Restore işlemi başlıyor."
                                        )

                                        /*
                                         * Dosya IO işlemini ana thread
                                         * dışında gerçekleştiriyoruz.
                                         *
                                         * Böylece progress bar gerçekten
                                         * ekranda hareket eder.
                                         */
                                        val basarili =
                                            withContext(
                                                Dispatchers.Default
                                            ) {

                                                /*
                                                 * 1. Eski driver'ı kapat.
                                                 */
                                                println(
                                                    "iOS: Eski driver kapatılıyor."
                                                )

                                                sqliteDriver.close()

                                                println(
                                                    "iOS: Eski driver kapatıldı."
                                                )

                                                /*
                                                 * 2. DB dosyasını değiştir.
                                                 */
                                                platformDbManager
                                                    .restoreDatabaseBytes(
                                                        bytes
                                                    )
                                            }

                                        if (!basarili) {

                                            println(
                                                "iOS: RestoreDatabaseBytes false döndü."
                                            )

                                            /*
                                             * Restore başarısız olsa bile
                                             * driver kapatıldığı için
                                             * tekrar açıyoruz.
                                             */
                                            val yeniDriver =
                                                driverFactory
                                                    .createDriver()

                                            sqliteDriver =
                                                yeniDriver

                                            database =
                                                AppDatabase(
                                                    yeniDriver
                                                )

                                            sonucBasarili =
                                                false

                                            sonucMesaji =
                                                "Yedek geri yüklenemedi.\n\n" +
                                                        "Dosya geçersiz olabilir veya " +
                                                        "veritabanı değiştirilemedi."

                                            return@launch
                                        }

                                        /*
                                         * 3. Restore başarılı.
                                         *
                                         * YENİ NativeSqliteDriver oluştur.
                                         */
                                        println(
                                            "iOS: Yeni driver açılıyor."
                                        )

                                        val yeniDriver =
                                            driverFactory
                                                .createDriver()

                                        /*
                                         * 4. Restore edilen DB'yi
                                         * gerçekten açmayı dene.
                                         */
                                        val yeniDatabase =
                                            AppDatabase(
                                                yeniDriver
                                            )

                                        /*
                                         * Basit test sorgusu.
                                         *
                                         * Bu çalışıyorsa yeni DB SQLDelight
                                         * tarafından gerçekten açılmış demektir.
                                         */
                                        val urunSayisi =
                                            yeniDatabase
                                                .appDatabaseQueries
                                                .selectAllUrun()
                                                .executeAsList()
                                                .size

                                        println(
                                            "iOS: Restore edilmiş DB açıldı."
                                        )

                                        println(
                                            "iOS: Ürün sayısı = $urunSayisi"
                                        )

                                        /*
                                         * 5. Aktif state'leri değiştir.
                                         */
                                        sqliteDriver =
                                            yeniDriver

                                        database =
                                            yeniDatabase

                                        sonucBasarili =
                                            true

                                        sonucMesaji =
                                            "Yedek başarıyla geri yüklendi.\n\n" +
                                                    "Veritabanı yeniden açıldı."

                                        println(
                                            "================================"
                                        )

                                        println(
                                            "iOS: YEDEK YÜKLEME TAMAMLANDI"
                                        )

                                        println(
                                            "Ürün sayısı = $urunSayisi"
                                        )

                                        println(
                                            "================================"
                                        )

                                    } catch (
                                        e: Throwable
                                    ) {

                                        println(
                                            "iOS RESTORE HATA: " +
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
                                         * Uygulamanın DB'siz
                                         * kalmaması için tekrar aç.
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

                                        } catch (
                                            driverError: Throwable
                                        ) {

                                            println(
                                                "iOS: DB tekrar açılamadı: " +
                                                        "${driverError.message}"
                                            )
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
                 * ==========================
                 * YEDEK YÜKLENİYOR EKRANI
                 * ==========================
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
             * ==========================
             * SONUÇ DIALOG
             * ==========================
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