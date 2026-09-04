package com.eray.muhasebeapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
class IosDosyaSecici :
    NSObject(),
    UIDocumentPickerDelegateProtocol {

    private var callback: ((ByteArray?) -> Unit)? = null

    fun dosyaSec(onSonuc: (ByteArray?) -> Unit) {

        println("iOS: Dosya seçici açılıyor.")

        callback = onSonuc

        val picker = UIDocumentPickerViewController(
            documentTypes = listOf("public.data"),
            inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
        )

        picker.delegate = this
        picker.allowsMultipleSelection = false

        val viewController = aktifViewController()

        if (viewController == null) {

            println("iOS: Aktif ViewController bulunamadı.")

            tamamla(null)
            return
        }

        viewController.presentViewController(
            picker,
            animated = true,
            completion = null
        )
    }

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>
    ) {

        println("iOS: documentPicker callback çalıştı.")

        val url = didPickDocumentsAtURLs
            .firstOrNull() as? NSURL

        if (url == null) {

            println("iOS: Seçilen dosyanın URL'si alınamadı.")

            tamamla(null)
            return
        }

        println("iOS: Seçilen dosya yolu = ${url.path}")

        val erisimBasladi =
            url.startAccessingSecurityScopedResource()

        try {

            val data: NSData? =
                NSData.dataWithContentsOfURL(url)

            if (data == null) {

                println("iOS: Dosya NSData olarak okunamadı.")

                tamamla(null)
                return
            }

            val size = data.length.toInt()

            println("iOS: Seçilen dosya boyutu = $size byte")

            if (size <= 0) {

                println("iOS: Seçilen dosya boş.")

                tamamla(null)
                return
            }

            val bytes = ByteArray(size)

            bytes.usePinned { pinned ->

                memcpy(
                    pinned.addressOf(0),
                    data.bytes,
                    data.length
                )
            }

            println(
                "iOS: Dosya ByteArray olarak başarıyla okundu. " +
                        "Boyut = ${bytes.size}"
            )

            tamamla(bytes)

        } catch (e: Throwable) {

            println(
                "iOS: Dosya okuma hatası = ${e.message}"
            )

            tamamla(null)

        } finally {

            if (erisimBasladi) {
                url.stopAccessingSecurityScopedResource()
            }
        }
    }

    override fun documentPickerWasCancelled(
        controller: UIDocumentPickerViewController
    ) {

        println("iOS: Dosya seçimi iptal edildi.")

        tamamla(null)
    }

    private fun tamamla(bytes: ByteArray?) {

        val mevcutCallback = callback

        callback = null

        mevcutCallback?.invoke(bytes)
    }

    private fun aktifViewController(): UIViewController? {

        val application =
            UIApplication.sharedApplication

        val windows =
            application.windows
                .mapNotNull {
                    it as? UIWindow
                }

        val window =
            windows.firstOrNull {
                it.isKeyWindow()
            } ?: windows.firstOrNull()

        var controller =
            window?.rootViewController

        while (
            controller?.presentedViewController != null
        ) {

            controller =
                controller.presentedViewController
        }

        return controller
    }
}

@Composable
fun rememberIosDosyaSecici(): IosDosyaSecici {

    return remember {
        IosDosyaSecici()
    }
}