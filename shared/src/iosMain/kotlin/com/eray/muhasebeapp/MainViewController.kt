package com.eray.muhasebeapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.timeIntervalSince1970

fun MainViewController() =
    ComposeUIViewController {

        /*
         * Tarih.
         */
        val formatter =
            NSDateFormatter().apply {
                dateFormat = "d MMMM, EEEE"
                locale = NSLocale(localeIdentifier = "tr_TR")
            }

        val iosTarih =
            formatter.stringFromDate(
                NSDate()
            )

        val simdiMillis =
            (NSDate().timeIntervalSince1970 * 1000).toLong()

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            App(
                guncelTarih = iosTarih,
                simdiMillis = simdiMillis
            )
        }
    }
