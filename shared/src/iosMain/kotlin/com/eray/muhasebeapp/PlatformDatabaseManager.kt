package com.eray.muhasebeapp

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
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

    /*
     * Yedek alınırken WAL checkpoint yapabilmek için
     * aktif SQLDelight driver burada tutuluyor.
     */
    private var aktifDriver: SqlDriver? = null

    fun setDriver(driver: SqlDriver) {
        aktifDriver = driver
    }

    /**
     * Gerçek SQLite DB yolunu döndürür.
     */
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
            "iOS: DB yolu = $dbPath"
        )

        return dbPath
    }

    /**
     * ÇOK ÖNEMLİ:
     *
     * SQLite'ın WAL dosyasında duran son INSERT,
     * UPDATE ve DELETE işlemlerini ana muhasebe.db
     * dosyasına aktarır.
     */
    private fun walCheckpointYap(): Boolean {

        val driver =
            aktifDriver

        if (driver == null) {

            println(
                "iOS YEDEK: Aktif SQLDelight driver bulunamadı."
            )

            return false
        }

        return try {

            println(
                "================================"
            )

            println(
                "iOS YEDEK: WAL CHECKPOINT BAŞLIYOR"
            )

            val sonuc =
                driver.executeQuery(
                    null,
                    "PRAGMA wal_checkpoint(TRUNCATE)",
                    { cursor ->

                        val satirVar =
                            cursor.next().value

                        if (!satirVar) {

                            println(
                                "iOS YEDEK: Checkpoint sonuç döndürmedi."
                            )

                            QueryResult.Value(
                                false
                            )

                        } else {

                            /*
                             * wal_checkpoint üç değer döndürür:
                             *
                             * 0 -> busy
                             * 1 -> WAL sayfa sayısı
                             * 2 -> ana DB'ye aktarılan sayfa
                             */
                            val busy =
                                cursor.getLong(0)
                                    ?: 1L

                            val walSayfa =
                                cursor.getLong(1)
                                    ?: 0L

                            val aktarilanSayfa =
                                cursor.getLong(2)
                                    ?: 0L

                            println(
                                "iOS YEDEK: " +
                                        "busy=$busy, " +
                                        "wal=$walSayfa, " +
                                        "aktarilan=$aktarilanSayfa"
                            )

                            QueryResult.Value(
                                busy == 0L
                            )
                        }
                    },
                    0,
                    null
                ).value

            if (sonuc) {

                println(
                    "iOS YEDEK: WAL başarıyla ana DB'ye aktarıldı."
                )

            } else {

                println(
                    "iOS YEDEK: WAL checkpoint tamamlanamadı."
                )
            }

            println(
                "================================"
            )

            sonuc

        } catch (e: Throwable) {

            println(
                "iOS YEDEK: WAL checkpoint hatası: ${e.message}"
            )

            e.printStackTrace()

            false
        }
    }

    /**
     * DB'yi ByteArray olarak yedekler.
     */
    actual fun getDatabaseBytes(
        database: AppDatabase
    ): ByteArray? {

        var file: CPointer<FILE>? = null

        return try {

            println(
                "================================"
            )

            println(
                "iOS: DB YEDEKLEME BAŞLADI"
            )

            /*
             * EN ÖNEMLİ KISIM.
             *
             * Ana DB'yi okumadan önce WAL'daki
             * bütün son kayıtları muhasebe.db
             * dosyasına geçir.
             */
            val checkpointBasarili =
                walCheckpointYap()

            if (!checkpointBasarili) {

                println(
                    "iOS: WAL checkpoint başarısız. " +
                            "Eksik yedek oluşturmamak için işlem iptal."
                )

                return null
            }

            val fileManager =
                NSFileManager.defaultManager

            val dbPath =
                getDatabasePath()
                    ?: return null

            println(
                "iOS: Yedeklenecek DB = $dbPath"
            )

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
                )

            if (file == null) {

                println(
                    "iOS: DB dosyası açılamadı."
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
                                "Beklenen=$fileSize, " +
                                "Okunan=$okunan"
                    )
                }
            }

            println(
                "================================"
            )

            println(
                "iOS: DB YEDEĞİ BAŞARILI"
            )

            println(
                "iOS: Yedek boyutu = ${bytes.size} byte"
            )

            println(
                "================================"
            )

            bytes

        } catch (e: Throwable) {

            println(
                "iOS: DB okuma hatası: ${e.message}"
            )

            e.printStackTrace()

            null

        } finally {

            file?.let {

                try {
                    fclose(it)
                } catch (_: Throwable) {
                }
            }

            file = null
        }
    }

    /**
     * Seçilen yedeği mevcut DB'nin yerine koyar.
     */
    actual fun restoreDatabaseBytes(
        bytes: ByteArray
    ): Boolean {

        if (bytes.isEmpty()) {

            println(
                "iOS RESTORE: Gelen yedek boş."
            )

            return false
        }

        var file: CPointer<FILE>? = null

        return try {

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

            println(
                "================================"
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
                "================================"
            )

            /*
             * Eski geçici dosyayı temizle.
             */
            silDosyaVarsa(
                fileManager,
                tempPath
            )

            /*
             * Driver MainViewController'da kapatıldığı için
             * eski WAL / SHM güvenle temizlenebilir.
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
             * Önce yedeği temp dosyasına yaz.
             */
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

            if (
                !fileManager.fileExistsAtPath(
                    tempPath
                )
            ) {

                println(
                    "iOS: Restore temp dosyası oluşmadı."
                )

                return false
            }

            println(
                "iOS: Yedek geçici dosyaya yazıldı."
            )

            /*
             * Eski DB'yi kaldır.
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
             * Temp DB'yi muhasebe.db olarak taşı.
             */
            val tasindi =
                fileManager.moveItemAtPath(
                    tempPath,
                    dbPath,
                    null
                )

            if (!tasindi) {

                println(
                    "iOS: Restore edilen DB taşınamadı."
                )

                return false
            }

            println(
                "iOS: Yeni DB yerine taşındı."
            )

            /*
             * Son kez yan dosyaları temizle.
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
                "================================"
            )

            println(
                "iOS: DB RESTORE BAŞARILI"
            )

            println(
                "iOS: Boyut = ${bytes.size} byte"
            )

            println(
                "================================"
            )

            true

        } catch (e: Throwable) {

            println(
                "iOS: DB restore hatası: ${e.message}"
            )

            e.printStackTrace()

            false

        } finally {

            file?.let {

                try {
                    fclose(it)
                } catch (_: Throwable) {
                }
            }
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
                    "iOS: Dosya temizlendi: " +
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