package com.eray.muhasebeapp

import platform.UIKit.UIDevice
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getEpochMillis(): Long {
    return (platform.Foundation.NSDate().timeIntervalSince1970 * 1000).toLong()
}

actual fun getBugununTarihiString(): String {
    val formatter = NSDateFormatter().apply {
        dateFormat = "yyyy-MM-dd"
    }
    return formatter.stringFromDate(NSDate())
}