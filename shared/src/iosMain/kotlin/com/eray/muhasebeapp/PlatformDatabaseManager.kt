package com.eray.muhasebeapp

import com.eray.muhasebeapp.database.shared.AppDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.posix.SEEK_END
import platform.posix.SEEK_SET
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.fwrite

// 1. Anotasyonu doğrudan sınıfın üzerine koyun; dosyadaki tüm metodlar erişebilsin
@OptIn(ExperimentalForeignApi::class)
actual class PlatformDatabaseManager {

    private val dbName = "muhasebe.db"

    /**
     * Gerçek SQLite DB yolunu döndürür.
     */
    private fun getDatabasePath(): String? {
        val fileManager = NSFileManager.defaultManager

        // URLsForDirectory C-pointer hatası üretmez
        val appSupportDir = fileManager.URLsForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomains = NSUserDomainMask
        ).firstOrNull() as? NSURL ?: return null

        val databasesDir = appSupportDir.URLByAppendingPathComponent("databases") ?: return null
        val databasesPath = databasesDir.path ?: return null

        // 2. Parametre isimlerini (path =, error =) kaldırıp pozisyonel olarak çağırın
        if (!fileManager.fileExistsAtPath(databasesPath)) {
            fileManager.createDirectoryAtPath(
                databasesPath,
                true,
                null,
                null
            )
        }

        return databasesDir.URLByAppendingPathComponent(dbName)?.path
    }

    actual fun getDatabaseBytes(database: AppDatabase): ByteArray? {
        // ... (Mevcut kodunuz aynen kalabilir)
        var file: kotlinx.cinterop.CPointer<platform.posix.FILE>? = null
        return try {
            val fileManager = NSFileManager.defaultManager
            val dbPath = getDatabasePath() ?: return null

            if (!fileManager.fileExistsAtPath(dbPath)) {
                println("DB dosyası bulunamadı: $dbPath")
                return null
            }

            file = fopen(dbPath, "rb") ?: return null
            fseek(file, 0, SEEK_END)
            val fileSize = ftell(file)

            if (fileSize <= 0) {
                fclose(file)
                file = null
                println("DB dosyası boş.")
                return null
            }

            fseek(file, 0, SEEK_SET)
            val bytes = ByteArray(fileSize.toInt())

            bytes.usePinned { pinned ->
                val okunan = fread(
                    pinned.addressOf(0),
                    1.toULong(),
                    fileSize.toULong(),
                    file
                )
                if (okunan != fileSize.toULong()) {
                    throw Exception("DB tamamen okunamadı. Beklenen: $fileSize, Okunan: $okunan")
                }
            }

            fclose(file)
            file = null
            println("DB yedeği hazırlandı: ${bytes.size} byte")
            bytes
        } catch (e: Exception) {
            println("DB okuma hatası: ${e.message}")
            file?.let { fclose(it) }
            null
        }
    }

    actual fun restoreDatabaseBytes(bytes: ByteArray): Boolean {
        // ... (Mevcut restore kodunuz aynen kalabilir)
        return try {
            val fileManager = NSFileManager.defaultManager
            val dbPath = getDatabasePath() ?: return false
            val tempPath = "$dbPath.restore_temp"
            val walPath = "$dbPath-wal"
            val shmPath = "$dbPath-shm"

            println("Restore DB yolu: $dbPath")

            if (fileManager.fileExistsAtPath(tempPath)) {
                fileManager.removeItemAtPath(tempPath, null)
            }
            if (fileManager.fileExistsAtPath(walPath)) {
                fileManager.removeItemAtPath(walPath, null)
            }
            if (fileManager.fileExistsAtPath(shmPath)) {
                fileManager.removeItemAtPath(shmPath, null)
            }

            var file: kotlinx.cinterop.CPointer<platform.posix.FILE>? = fopen(tempPath, "wb") ?: return false

            val yazilan = bytes.usePinned { pinned ->
                fwrite(pinned.addressOf(0), 1.toULong(), bytes.size.toULong(), file)
            }
            fclose(file)
            file = null

            if (yazilan != bytes.size.toULong()) {
                println("DB tamamen yazılamadı.")
                fileManager.removeItemAtPath(tempPath, null)
                return false
            }

            if (fileManager.fileExistsAtPath(dbPath)) {
                val silindi = fileManager.removeItemAtPath(dbPath, null)
                if (!silindi) {
                    println("Eski DB silinemedi.")
                    fileManager.removeItemAtPath(tempPath, null)
                    return false
                }
            }

            val tasindi = fileManager.moveItemAtPath(tempPath, dbPath, null)
            if (!tasindi) {
                println("Restore edilen DB yerine taşınamadı.")
                return false
            }

            if (fileManager.fileExistsAtPath(walPath)) {
                fileManager.removeItemAtPath(walPath, null)
            }
            if (fileManager.fileExistsAtPath(shmPath)) {
                fileManager.removeItemAtPath(shmPath, null)
            }

            println("DB restore başarılı. Boyut: ${bytes.size} byte")
            true
        } catch (e: Exception) {
            println("DB restore hatası: ${e.message}")
            false
        }
    }
}