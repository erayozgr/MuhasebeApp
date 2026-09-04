package com.eray.muhasebeapp

import com.eray.muhasebeapp.database.IosDatabasePath
import com.eray.muhasebeapp.database.shared.AppDatabase
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSFileManager
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

    private fun getDatabasePath(): String {
        return IosDatabasePath.getDatabasePath()
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

            println(
                "iOS YEDEK: Okunacak DB = $dbPath"
            )

            if (
                !fileManager.fileExistsAtPath(
                    dbPath
                )
            ) {

                println(
                    "iOS YEDEK HATA: DB bulunamadı."
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
                    "iOS YEDEK HATA: fopen başarısız."
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
                    "iOS YEDEK HATA: DB boş."
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
                "iOS YEDEK: DB okundu. " +
                        "Boyut=${bytes.size}"
            )

            bytes

        } catch (e: Throwable) {

            println(
                "iOS YEDEK HATA: ${e.message}"
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
                "iOS RESTORE HATA: Gelen dosya boş."
            )

            return false
        }

        /*
         * Normal SQLite DB'nin ilk 16 byte'ı:
         *
         * SQLite format 3\0
         */
        if (!sqliteDosyasiMi(bytes)) {

            println(
                "iOS RESTORE HATA: Seçilen dosya " +
                        "geçerli SQLite veritabanı değil."
            )

            return false
        }

        val fileManager =
            NSFileManager.defaultManager

        val dbPath =
            getDatabasePath()

        val tempPath =
            "$dbPath.restore_temp"

        val walPath =
            "$dbPath-wal"

        val shmPath =
            "$dbPath-shm"

        val journalPath =
            "$dbPath-journal"

        var file: CPointer<FILE>? = null

        try {

            println(
                "================================"
            )

            println(
                "iOS RESTORE BAŞLADI"
            )

            println(
                "Hedef = $dbPath"
            )

            println(
                "Yedek boyutu = ${bytes.size}"
            )

            println(
                "================================"
            )

            /*
             * Önce eski temp'i temizle.
             */
            silVarsa(
                fileManager,
                tempPath
            )

            /*
             * Yedeği temp dosyasına yaz.
             */
            file =
                fopen(
                    tempPath,
                    "wb"
                )

            if (file == null) {

                println(
                    "iOS RESTORE HATA: " +
                            "Temp dosyası açılamadı."
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
                    "iOS RESTORE HATA: Eksik yazıldı. " +
                            "Beklenen=${bytes.size} " +
                            "Yazılan=$yazilan"
                )

                silVarsa(
                    fileManager,
                    tempPath
                )

                return false
            }

            /*
             * Temp gerçekten oluştu mu?
             */
            if (
                !fileManager.fileExistsAtPath(
                    tempPath
                )
            ) {

                println(
                    "iOS RESTORE HATA: Temp DB yok."
                )

                return false
            }

            println(
                "iOS RESTORE: Yedek temp'e yazıldı."
            )

            /*
             * Driver MainViewController'da zaten
             * kapatılmış olacak.
             *
             * Şimdi yan dosyaları temizleyebiliriz.
             */
            silVarsa(
                fileManager,
                walPath
            )

            silVarsa(
                fileManager,
                shmPath
            )

            silVarsa(
                fileManager,
                journalPath
            )

            /*
             * Eski DB'yi sil.
             */
            if (
                fileManager.fileExistsAtPath(
                    dbPath
                )
            ) {

                println(
                    "iOS RESTORE: Eski DB siliniyor..."
                )

                val silindi =
                    fileManager.removeItemAtPath(
                        dbPath,
                        null
                    )

                if (!silindi) {

                    println(
                        "iOS RESTORE HATA: Eski DB silinemedi."
                    )

                    silVarsa(
                        fileManager,
                        tempPath
                    )

                    return false
                }
            }

            /*
             * Temp -> gerçek muhasebe.db
             */
            println(
                "iOS RESTORE: Yeni DB yerine taşınıyor..."
            )

            val tasindi =
                fileManager.moveItemAtPath(
                    tempPath,
                    dbPath,
                    null
                )

            if (!tasindi) {

                println(
                    "iOS RESTORE HATA: " +
                            "DB yerine taşınamadı."
                )

                return false
            }

            /*
             * Yan dosyaları son kez temizle.
             */
            silVarsa(
                fileManager,
                walPath
            )

            silVarsa(
                fileManager,
                shmPath
            )

            silVarsa(
                fileManager,
                journalPath
            )

            if (
                !fileManager.fileExistsAtPath(
                    dbPath
                )
            ) {

                println(
                    "iOS RESTORE HATA: " +
                            "İşlem sonrası DB bulunamadı."
                )

                return false
            }

            println(
                "================================"
            )

            println(
                "iOS RESTORE BAŞARILI"
            )

            println(
                "Yeni DB = $dbPath"
            )

            println(
                "================================"
            )

            return true

        } catch (e: Throwable) {

            println(
                "iOS RESTORE EXCEPTION: ${e.message}"
            )

            e.printStackTrace()

            return false

        } finally {

            file?.let {

                try {
                    fclose(it)
                } catch (_: Throwable) {
                }
            }

            silVarsa(
                fileManager,
                tempPath
            )
        }
    }

    private fun sqliteDosyasiMi(
        bytes: ByteArray
    ): Boolean {

        if (bytes.size < 16) {
            return false
        }

        val sqliteHeader =
            byteArrayOf(
                0x53,
                0x51,
                0x4C,
                0x69,
                0x74,
                0x65,
                0x20,
                0x66,
                0x6F,
                0x72,
                0x6D,
                0x61,
                0x74,
                0x20,
                0x33,
                0x00
            )

        for (
        index in
        sqliteHeader.indices
        ) {

            if (
                bytes[index] !=
                sqliteHeader[index]
            ) {

                return false
            }
        }

        return true
    }

    private fun silVarsa(
        fileManager: NSFileManager,
        path: String
    ) {

        try {

            if (
                fileManager.fileExistsAtPath(
                    path
                )
            ) {

                val silindi =
                    fileManager.removeItemAtPath(
                        path,
                        null
                    )

                println(
                    "iOS: Dosya temizlendi: " +
                            "$path -> $silindi"
                )
            }

        } catch (e: Throwable) {

            println(
                "iOS: Dosya temizleme hatası: " +
                        "$path -> ${e.message}"
            )
        }
    }
}