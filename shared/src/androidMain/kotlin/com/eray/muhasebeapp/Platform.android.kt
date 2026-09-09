package com.eray.muhasebeapp

actual fun getEpochMillis(): Long {
    return java.lang.System.currentTimeMillis()
}
