@file:OptIn(com.russhwolf.settings.ExperimentalSettingsImplementation::class,
    com.russhwolf.settings.ExperimentalSettingsApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class)

package com.eozgur.hesapbenim.util

import com.russhwolf.settings.KeychainSettings
import platform.Foundation.CFBridgingRetain
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrService

internal actual object SecureTokenStorage {
    // Retained for the lifetime of this singleton; never synced or restored to another device.
    private val service = CFBridgingRetain("com.eozgur.hesapbenim.session.v1")
    private val settings = KeychainSettings(
        kSecAttrService to service,
        kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly
    )
    actual fun read(): String? = settings.getStringOrNull("token")
    actual fun write(token: String) { settings.putString("token", token) }
    actual fun clear() { settings.remove("token") }
}
