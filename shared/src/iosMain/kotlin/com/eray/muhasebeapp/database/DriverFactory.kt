package com.eray.muhasebeapp.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.eray.muhasebeapp.database.shared.AppDatabase

class DriverFactory {

    fun createDriver(): SqlDriver {

        val databaseDirectory =
            IosDatabasePath.getDatabaseDirectory()

        println(
            "iOS: SQLDelight DB klasörü = $databaseDirectory"
        )

        val driver =
            NativeSqliteDriver(
                schema = AppDatabase.Schema,
                name = IosDatabasePath.DB_NAME,
                onConfiguration = { config ->

                    config.copy(
                        extendedConfig =
                            config.extendedConfig.copy(
                                basePath =
                                    databaseDirectory
                            )
                    )
                }
            )

        println(
            "iOS: NativeSqliteDriver oluşturuldu."
        )

        return driver
    }
}