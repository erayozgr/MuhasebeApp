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
import platform.darwin.NSObject
import platform.posix.memcpy

/**
 * iOS tarafında ".db" yedek dosyasını seçtirip byte dizisi olarak döner.
 * documentTypes = ["public.data"] kullanıyoruz ki kullanıcı Files uygulamasından
 * uzantısı ne olursa olsun (db, sqlite, vs.) seçebilsin.
 */
@OptIn(ExperimentalForeignApi::class)
class IosDosyaSecici : NSObject(), UIDocumentPickerDelegateProtocol {

    private var callback: ((ByteArray?) -> Unit)? = null

    fun dosyaSec(onSonuc: (ByteArray?) -> Unit) {
        callback = onSonuc

        val picker = UIDocumentPickerViewController(
            documentTypes = listOf("public.data"),
            inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
        )
        picker.delegate = this
        picker.allowsMultipleSelection = false

        val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (rootVC == null) {
            callback?.invoke(null)
            return
        }
        rootVC.presentViewController(picker, animated = true, completion = null)
    }

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        if (url == null) {
            callback?.invoke(null)
            return
        }

        val erisimBasladiMi = url.startAccessingSecurityScopedResource()
        val data: NSData? = NSData.dataWithContentsOfURL(url)
        if (erisimBasladiMi) url.stopAccessingSecurityScopedResource()

        val bytes = data?.let { nsData ->
            val size = nsData.length.toInt()
            ByteArray(size).apply {
                if (size > 0) {
                    usePinned { pinned ->
                        memcpy(pinned.addressOf(0), nsData.bytes, nsData.length)
                    }
                }
            }
        }
        callback?.invoke(bytes)
        callback = null
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        callback?.invoke(null)
        callback = null
    }
}