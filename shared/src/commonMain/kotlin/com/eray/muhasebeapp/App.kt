package com.eray.muhasebeapp

import androidx.compose.runtime.Composable
import com.eray.muhasebeapp.ui.MainStructure
import com.eray.muhasebeapp.database.shared.AppDatabase

@Composable
fun App(
    database: AppDatabase,
    guncelTarih: String // Bu parametreyi buraya da ekledik
) {
    // Aldığımız tarihi MainStructure'a emanet ediyoruz
    MainStructure(database = database, guncelTarih = guncelTarih)
}