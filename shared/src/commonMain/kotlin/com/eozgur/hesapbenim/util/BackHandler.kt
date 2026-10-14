package com.eozgur.hesapbenim.util

import androidx.compose.runtime.Composable

@Composable
expect fun CommonBackHandler(enabled: Boolean = true, onBack: () -> Unit)
