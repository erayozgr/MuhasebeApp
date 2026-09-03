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

    override fun paylas(dosyaAdi: String, icerik: String) {
        paylasBytes(dosyaAdi, icerik.encodeToByteArray())
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun paylasBytes(dosyaAdi: String, icerik: ByteArray) {
        val tempDir = NSTemporaryDirectory().removeSuffix("/")
        val dosyaYolu = "$tempDir/$dosyaAdi"

        val nsData = if (icerik.isNotEmpty()) {
            icerik.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), icerik.size.toULong())
            }
        } else {
            NSData()
        }

        val basarili = nsData.writeToFile(dosyaYolu, atomically = true)
        if (basarili) {
            paylasDosya(dosyaYolu)
        }
    }

    private fun paylasDosya(dosyaYolu: String) {
        val url = NSURL.fileURLWithPath(dosyaYolu)
        val activityViewController = UIActivityViewController(
            activityItems = listOf(url),
            applicationActivities = null
        )

        val window = UIApplication.sharedApplication.keyWindow
            ?: (UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow)

        var topController = window?.rootViewController
        while (topController?.presentedViewController != null) {
            topController = topController.presentedViewController
        }

        // iPad üzerinde uygulamanın çökmesini engeller
        activityViewController.popoverPresentationController?.sourceView = topController?.view

        topController?.presentViewController(
            activityViewController,
            animated = true,
            completion = null
        )
    }
}

@Composable
actual fun rememberDosyaPaylasici(): DosyaPaylasici {
    return remember { IosDosyaPaylasici() }
}