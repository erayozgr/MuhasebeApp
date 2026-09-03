package com.eray.muhasebeapp

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

            callback?.invoke(null)
            callback = null

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

        val url =
            didPickDocumentsAtURLs.firstOrNull() as? NSURL

        if (url == null) {

            println("iOS: Dosya URL'si alınamadı.")

            callback?.invoke(null)
            callback = null

            return
        }

        try {

            val erisimBasladi =
                url.startAccessingSecurityScopedResource()

            try {

                val data: NSData? =
                    NSData.dataWithContentsOfURL(url)

                if (data == null) {

                    println("iOS: Dosya okunamadı.")

                    callback?.invoke(null)

                    return
                }

                val size =
                    data.length.toInt()

                if (size <= 0) {

                    println("iOS: Seçilen dosya boş.")

                    callback?.invoke(null)

                    return
                }

                val bytes =
                    ByteArray(size)

                bytes.usePinned { pinned ->

                    memcpy(
                        pinned.addressOf(0),
                        data.bytes,
                        data.length
                    )
                }

                println(
                    "iOS: Yedek dosyası başarıyla okundu. " +
                            "Boyut: $size byte"
                )

                callback?.invoke(bytes)

            } finally {

                if (erisimBasladi) {
                    url.stopAccessingSecurityScopedResource()
                }
            }

        } catch (e: Exception) {

            println(
                "iOS dosya okuma hatası: ${e.message}"
            )

            callback?.invoke(null)

        } finally {

            callback = null
        }
    }

    override fun documentPickerWasCancelled(
        controller: UIDocumentPickerViewController
    ) {

        println("iOS: Dosya seçimi iptal edildi.")

        callback?.invoke(null)
        callback = null
    }

    private fun aktifViewController(): UIViewController? {

        val application =
            UIApplication.sharedApplication

        val windows =
            application.windows
                .mapNotNull { it as? UIWindow }

        val window =
            windows.firstOrNull { it.isKeyWindow() }
                ?: windows.firstOrNull()

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