package com.eray.muhasebeapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@OptIn(ExperimentalForeignApi::class)
class IosUrlAcici : UrlAcici {
    override fun ac(url: String) {
        val nsUrl = NSURL.URLWithString(url)
        nsUrl?.let {
            if (UIApplication.sharedApplication.canOpenURL(it)) {
                UIApplication.sharedApplication.openURL(it)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberUrlAcici(): UrlAcici {
    return remember { IosUrlAcici() }
}