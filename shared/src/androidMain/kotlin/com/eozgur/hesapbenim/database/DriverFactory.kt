package com.eozgur.hesapbenim.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.eozgur.hesapbenim.database.shared.AppDatabase

// 🎯 actual kelimesini sildik, tertemiz bir düz sınıf oldu:
class DriverFactory(private val context: Context) {
    fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(AppDatabase.Schema, context, "muhasebe.db")
    }
}