package com.eray.muhasebeapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.popoverPresentationController

class IosDosyaPaylasici : DosyaPaylasici {

    override fun paylas(
        dosyaAdi: String,
        icerik: String
    ) {
        paylasBytes(
            dosyaAdi,
            icerik.encodeToByteArray()
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun paylasBytes(
        dosyaAdi: String,
        icerik: ByteArray
    ) {

        /*
         * Dosya uzantısını türüne göre koru.
         *
         * .xlsx -> .xlsx olarak kalır
         * .txt  -> .txt olarak kalır
         * .db   -> .txt yapılır (iOS yedek için)
         */
        val iosDosyaAdi =
            when {

                // Excel raporu
                dosyaAdi.endsWith(
                    ".xlsx",
                    ignoreCase = true
                ) -> {
                    dosyaAdi
                }

                // TXT dosyası
                dosyaAdi.endsWith(
                    ".txt",
                    ignoreCase = true
                ) -> {
                    dosyaAdi
                }

                // Veritabanı yedeği
                dosyaAdi.endsWith(
                    ".db",
                    ignoreCase = true
                ) -> {
                    dosyaAdi.dropLast(3) + ".txt"
                }

                // Uzantısız dosya
                else -> {
                    "$dosyaAdi.txt"
                }
            }

        println(
            "iOS: Dosya adı = $iosDosyaAdi"
        )

        println(
            "iOS: Dosya boyutu = ${icerik.size} byte"
        )

        val tempDir =
            NSTemporaryDirectory()
                .removeSuffix("/")

        val dosyaYolu =
            "$tempDir/$iosDosyaAdi"

        val nsData =
            if (icerik.isNotEmpty()) {

                icerik.usePinned { pinned ->

                    NSData.dataWithBytes(
                        pinned.addressOf(0),
                        icerik.size.toULong()
                    )
                }

            } else {

                NSData()
            }

        val basarili =
            nsData.writeToFile(
                dosyaYolu,
                atomically = true
            )

        if (basarili) {

            println(
                "iOS: Dosya oluşturuldu: $dosyaYolu"
            )

            paylasDosya(
                dosyaYolu
            )

        } else {

            println(
                "iOS: Dosya oluşturulamadı."
            )
        }
    }

    private fun paylasDosya(
        dosyaYolu: String
    ) {

        val url =
            NSURL.fileURLWithPath(
                dosyaYolu
            )

        val activityViewController =
            UIActivityViewController(
                activityItems = listOf(url),
                applicationActivities = null
            )

        val window =
            UIApplication
                .sharedApplication
                .keyWindow
                ?: (
                        UIApplication
                            .sharedApplication
                            .windows
                            .firstOrNull() as? UIWindow
                        )

        var topController =
            window?.rootViewController

        while (
            topController
                ?.presentedViewController != null
        ) {

            topController =
                topController
                    ?.presentedViewController
        }

        /*
         * iPad crash engelleme
         */
        activityViewController
            .popoverPresentationController
            ?.sourceView =
            topController?.view

        topController
            ?.presentViewController(
                activityViewController,
                animated = true,
                completion = null
            )
    }
}

@Composable
actual fun rememberDosyaPaylasici(): DosyaPaylasici {

    return remember {
        IosDosyaPaylasici()
    }
}