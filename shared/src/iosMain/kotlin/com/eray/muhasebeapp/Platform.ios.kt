package com.eray.muhasebeapp

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun getEpochMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}
