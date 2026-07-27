package com.eray.muhasebeapp

import com.eray.muhasebeapp.database.Musteri
import com.eray.muhasebeapp.database.Tedarikci
import com.eray.muhasebeapp.database.shared.AppDatabase


expect class PlatformDatabaseManager {
    fun getDatabaseBytes(database: AppDatabase): ByteArray?
    fun restoreDatabaseBytes(bytes: ByteArray): Boolean
}