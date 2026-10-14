package com.eozgur.hesapbenim.util

/** Platform protected storage. Never fall back to plaintext on failure. */
internal expect object SecureTokenStorage {
    fun read(): String?
    fun write(token: String)
    fun clear()
}
