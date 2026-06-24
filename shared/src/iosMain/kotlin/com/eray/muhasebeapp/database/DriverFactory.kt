package com.eray.muhasebeapp.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.eray.muhasebeapp.database.shared.AppDatabase

// 🎯 actual kelimesini buradan da sildik:
class DriverFactory {
    fun createDriver(): SqlDriver {
        return NativeSqliteDriver(AppDatabase.Schema, "muhasebe.db")
    }
}