package com.eray.muhasebeapp

import com.eray.muhasebeapp.database.shared.AppDatabase
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.posix.FILE
import platform.posix.SEEK_END
import platform.posix.SEEK_SET
import platform.posix.fclose
import platform.posix.fflush
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.fwrite

@OptIn(ExperimentalForeignApi::class)
actual class PlatformDatabaseManager {

    private val dbName = "muhasebe.db"

    private fun getDatabasePath(): String? {

        val fileManager =
            NSFileManager.defaultManager

        val appSupportDir =
            fileManager.URLsForDirectory(
                directory = NSApplicationSupportDirectory,
                inDomains = NSUserDomainMask
            ).firstOrNull() as? NSURL
                ?: return null

        val databasesDir =
            appSupportDir.URLByAppendingPathComponent(
                "databases"
            ) ?: return null

        val databasesPath =
            databasesDir.path
                ?: return null

        if (
            !fileManager.fileExistsAtPath(
                databasesPath
            )
        ) {

            val olusturuldu =
                fileManager.createDirectoryAtPath(
                    databasesPath,
                    true,
                    null,
                    null
                )

            if (!olusturuldu) {

                println(
                    "iOS: databases klasörü oluşturulamadı."
                )

                return null
            }
        }

        val dbPath =
            databasesDir
                .URLByAppendingPathComponent(
                    dbName
                )
                ?.path

        println(
            "iOS: Gerçek DB yolu = $dbPath"
        )

        return dbPath
    }

    actual fun getDatabaseBytes(
        database: AppDatabase
    ): ByteArray? {

        var file: CPointer<FILE>? = null

        return try {

            val fileManager =
                NSFileManager.defaultManager

            val dbPath =
                getDatabasePath()
                    ?: return null

            if (
                !fileManager.fileExistsAtPath(
                    dbPath
                )
            ) {

                println(
                    "iOS: DB dosyası bulunamadı: $dbPath"
                )

                return null
            }

            file =
                fopen(
                    dbPath,
                    "rb"
                ) ?: run {

                    println(
                        "iOS: DB fopen ile açılamadı."
                    )

                    return null
                }

            fseek(
                file,
                0,
                SEEK_END
            )

            val fileSize =
                ftell(file)

            if (fileSize <= 0) {

                println(
                    "iOS: DB dosyası boş."
                )

                return null
            }

            fseek(
                file,
                0,
                SEEK_SET
            )

            val bytes =
                ByteArray(
                    fileSize.toInt()
                )

            bytes.usePinned { pinned ->

                val okunan =
                    fread(
                        pinned.addressOf(0),
                        1.toULong(),
                        fileSize.toULong(),
                        file
                    )

                if (
                    okunan !=
                    fileSize.toULong()
                ) {

                    throw Exception(
                        "DB tamamen okunamadı. " +
                                "Beklenen=$fileSize " +
                                "Okunan=$okunan"
                    )
                }
            }

            println(
                "iOS: DB yedeği başarıyla hazırlandı. " +
                        "Boyut=${bytes.size} byte"
            )

            bytes

        } catch (e: Throwable) {

            println(
                "iOS: DB okuma hatası = ${e.message}"
            )

            null

        } finally {

            file?.let {
                fclose(it)
            }

            file = null
        }
    }

    actual fun restoreDatabaseBytes(
        bytes: ByteArray
    ): Boolean {

        if (bytes.isEmpty()) {

            println(
                "iOS: Restore başarısız. Gelen ByteArray boş."
            )

            return false
        }

        var file: CPointer<FILE>? = null

        val fileManager =
            NSFileManager.defaultManager

        val dbPath =
            getDatabasePath()
                ?: return false

        val tempPath =
            "$dbPath.restore_temp"

        val walPath =
            "$dbPath-wal"

        val shmPath =
            "$dbPath-shm"

        val journalPath =
            "$dbPath-journal"

        try {

            println(
                "=============================="
            )

            println(
                "iOS: DB RESTORE BAŞLADI"
            )

            println(
                "iOS: DB yolu = $dbPath"
            )

            println(
                "iOS: Yedek boyutu = ${bytes.size}"
            )

            println(
                "=============================="
            )

            // Önce eski geçici dosyayı temizle
            silDosyaVarsa(
                fileManager,
                tempPath
            )

            /*
             * WAL/SHM/JOURNAL dosyalarını temizliyoruz.
             */
            silDosyaVarsa(
                fileManager,
                walPath
            )

            silDosyaVarsa(
                fileManager,
                shmPath
            )

            silDosyaVarsa(
                fileManager,
                journalPath
            )

            // Önce yedeği geçici dosyaya yaz.
            file =
                fopen(
                    tempPath,
                    "wb"
                )

            if (file == null) {

                println(
                    "iOS: Restore temp dosyası açılamadı."
                )

                return false
            }

            val yazilan =
                bytes.usePinned { pinned ->

                    fwrite(
                        pinned.addressOf(0),
                        1.toULong(),
                        bytes.size.toULong(),
                        file
                    )
                }

            fflush(file)

            fclose(file)

            file = null

            if (
                yazilan !=
                bytes.size.toULong()
            ) {

                println(
                    "iOS: DB tamamen yazılamadı. " +
                            "Beklenen=${bytes.size}, " +
                            "Yazılan=$yazilan"
                )

                silDosyaVarsa(
                    fileManager,
                    tempPath
                )

                return false
            }

            /*
             * Temp dosyasının gerçekten oluştuğunu kontrol et.
             */
            if (
                !fileManager.fileExistsAtPath(
                    tempPath
                )
            ) {

                println(
                    "iOS: Restore temp DB oluşmadı."
                )

                return false
            }

            println(
                "iOS: Yedek temp dosyaya yazıldı."
            )

            /*
             * Mevcut DB'yi kaldır.
             */
            if (
                fileManager.fileExistsAtPath(
                    dbPath
                )
            ) {

                val silindi =
                    fileManager.removeItemAtPath(
                        dbPath,
                        null
                    )

                if (!silindi) {

                    println(
                        "iOS: Eski DB silinemedi."
                    )

                    silDosyaVarsa(
                        fileManager,
                        tempPath
                    )

                    return false
                }

                println(
                    "iOS: Eski DB silindi."
                )
            }

            /*
             * Temp DB -> gerçek DB
             */
            val tasindi =
                fileManager.moveItemAtPath(
                    tempPath,
                    dbPath,
                    null
                )

            if (!tasindi) {

                println(
                    "iOS: Yeni DB gerçek konumuna taşınamadı."
                )

                return false
            }

            println(
                "iOS: Yeni DB yerine taşındı."
            )

            /*
             * SQLite yan dosyalarını son kez temizle.
             */
            silDosyaVarsa(
                fileManager,
                walPath
            )

            silDosyaVarsa(
                fileManager,
                shmPath
            )

            silDosyaVarsa(
                fileManager,
                journalPath
            )

            /*
             * Son kontrol.
             */
            if (
                !fileManager.fileExistsAtPath(
                    dbPath
                )
            ) {

                println(
                    "iOS: Restore sonrası DB bulunamadı."
                )

                return false
            }

            println(
                "=============================="
            )

            println(
                "iOS: DB RESTORE BAŞARILI"
            )

            println(
                "iOS: Boyut = ${bytes.size} byte"
            )

            println(
                "=============================="
            )

            return true

        } catch (e: Throwable) {

            println(
                "iOS: DB restore hatası = ${e.message}"
            )

            return false

        } finally {

            file?.let {

                try {
                    fclose(it)
                } catch (_: Throwable) {
                }
            }

            silDosyaVarsa(
                fileManager,
                tempPath
            )
        }
    }

    private fun silDosyaVarsa(
        fileManager: NSFileManager,
        path: String
    ) {

        try {

            if (
                fileManager.fileExistsAtPath(
                    path
                )
            ) {

                val sonuc =
                    fileManager.removeItemAtPath(
                        path,
                        null
                    )

                println(
                    "iOS: Yan dosya temizlendi: " +
                            "$path -> $sonuc"
                )
            }

        } catch (e: Throwable) {

            println(
                "iOS: Dosya silme hatası: " +
                        "$path -> ${e.message}"
            )
        }
    }
}