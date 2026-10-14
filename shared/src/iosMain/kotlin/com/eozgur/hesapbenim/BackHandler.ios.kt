package com.eozgur.hesapbenim.util

import androidx.compose.runtime.Composable

@Composable
actual fun CommonBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS doesn't have a hardware back button, so this does nothing.
}
