package com.eray.muhasebeapp.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.eray.muhasebeapp.database.shared.AppDatabase

class DriverFactory {

    fun createDriver(): SqlDriver {

        println("iOS: Yeni SQLDelight driver oluşturuluyor.")

        return NativeSqliteDriver(
            AppDatabase.Schema,
            "muhasebe.db"
        )
    }
}