package com.eray.muhasebeapp

import android.content.Context
import com.eray.muhasebeapp.database.shared.AppDatabase
import java.io.File

actual class PlatformDatabaseManager(private val context: Context) {
    private val dbName = "muhasebe.db"

    actual fun getDatabaseBytes(database: AppDatabase): ByteArray? {
        return try {
            val dbFile: File = context.getDatabasePath(dbName)
            if (dbFile.exists()) dbFile.readBytes() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun restoreDatabaseBytes(bytes: ByteArray): Boolean {
        return try {
            val dbFile: File = context.getDatabasePath(dbName)
            dbFile.writeBytes(bytes)
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}