package com.eray.muhasebeapp.database

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
object IosDatabasePath {

    const val DB_NAME = "muhasebe.db"

    fun getDatabaseDirectory(): String {

        val fileManager =
            NSFileManager.defaultManager

        val appSupportUrl =
            fileManager.URLsForDirectory(
                directory = NSApplicationSupportDirectory,
                inDomains = NSUserDomainMask
            ).firstOrNull() as? NSURL
                ?: error(
                    "iOS: Application Support klasörü bulunamadı."
                )

        val databasesUrl =
            appSupportUrl.URLByAppendingPathComponent(
                "databases"
            )
                ?: error(
                    "iOS: databases yolu oluşturulamadı."
                )

        val databasesPath =
            databasesUrl.path
                ?: error(
                    "iOS: databases path alınamadı."
                )

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
                error(
                    "iOS: databases klasörü oluşturulamadı."
                )
            }
        }

        println(
            "iOS: Database klasörü = $databasesPath"
        )

        return databasesPath
    }

    fun getDatabasePath(): String {

        val path =
            "${getDatabaseDirectory()}/$DB_NAME"

        println(
            "iOS: Database tam yolu = $path"
        )

        return path
    }
}