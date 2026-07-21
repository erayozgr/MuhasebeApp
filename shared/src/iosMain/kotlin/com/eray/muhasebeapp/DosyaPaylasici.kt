package com.eray.muhasebeapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = this.usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
class IosDosyaPaylasici : DosyaPaylasici {

    override fun paylas(dosyaAdi: String, icerik: String) {
        val filePath = NSTemporaryDirectory() + dosyaAdi
        val nsIcerik = icerik as NSString
        nsIcerik.writeToFile(
            filePath,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )
        paylasDosya(filePath)
    }

    override fun paylasBytes(dosyaAdi: String, icerik: ByteArray) {
        val filePath = NSTemporaryDirectory() + dosyaAdi
        val data = icerik.toNSData()
        data.writeToFile(filePath, atomically = true)
        paylasDosya(filePath)
    }

    private fun paylasDosya(filePath: String) {
        val url = NSURL.fileURLWithPath(filePath)
        val activityVC = UIActivityViewController(listOf(url), null)

        val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootVC?.presentViewController(activityVC, animated = true, completion = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberDosyaPaylasici(): DosyaPaylasici {
    return remember { IosDosyaPaylasici() }
}