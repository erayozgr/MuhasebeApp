package com.eray.muhasebeapp

import com.eray.muhasebeapp.database.shared.AppDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fwrite
import platform.posix.fclose
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.SEEK_END
import platform.posix.SEEK_SET

actual class PlatformDatabaseManager {
    private val dbName = "muhasebe.db"

    @OptIn(ExperimentalForeignApi::class)
    actual fun getDatabaseBytes(database: AppDatabase): ByteArray? {
        return try {
            val fileManager = NSFileManager.defaultManager
            val appSupportDir = fileManager.URLForDirectory(
                NSApplicationSupportDirectory,
                NSUserDomainMask,
                null,
                true,
                null
            )
            val dbPath = appSupportDir?.URLByAppendingPathComponent("databases/$dbName")?.path
            if (dbPath == null || !fileManager.fileExistsAtPath(dbPath)) return null

            val file = fopen(dbPath, "rb") ?: return null
            fseek(file, 0, SEEK_END)
            val fileSize = ftell(file)
            fseek(file, 0, SEEK_SET)

            val bytes = ByteArray(fileSize.toInt())
            if (bytes.isNotEmpty()) {
                bytes.usePinned { pinned ->
                    fread(pinned.addressOf(0), 1.toULong(), fileSize.toULong(), file)
                }
            }
            fclose(file)
            bytes
        } catch (e: Exception) {
            null
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun restoreDatabaseBytes(bytes: ByteArray): Boolean {
        return try {
            val fileManager = NSFileManager.defaultManager
            val appSupportDir = fileManager.URLForDirectory(
                NSApplicationSupportDirectory,
                NSUserDomainMask,
                null,
                true,
                null
            )
            val dbPath = appSupportDir?.URLByAppendingPathComponent("databases/$dbName")?.path ?: return false

            val file = fopen(dbPath, "wb") ?: return false
            if (bytes.isNotEmpty()) {
                bytes.usePinned { pinned ->
                    fwrite(pinned.addressOf(0), 1.toULong(), bytes.size.toULong(), file)
                }
            }
            fclose(file)

            fileManager.removeItemAtPath(dbPath + "-wal", null)
            fileManager.removeItemAtPath(dbPath + "-shm", null)
            true
        } catch (e: Exception) {
            false
        }
    }
}