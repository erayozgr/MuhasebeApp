package com.eray.muhasebeapp.util

import androidx.compose.runtime.Composable

@Composable
actual fun CommonBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS doesn't have a hardware back button, so this does nothing.
}
