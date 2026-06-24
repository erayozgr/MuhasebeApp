package com.eray.muhasebeapp

import androidx.compose.runtime.Composable
import com.eray.muhasebeapp.ui.MainStructure
import com.eray.muhasebeapp.database.shared.AppDatabase

@Composable
fun App(database: AppDatabase) {
    // Uygulama başlarken veritabanını ana yapıya emanet ediyor
    MainStructure(database = database)
}