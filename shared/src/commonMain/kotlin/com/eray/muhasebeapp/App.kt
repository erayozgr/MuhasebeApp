package com.eray.muhasebeapp

import androidx.compose.runtime.Composable
import com.eray.muhasebeapp.ui.MainStructure
import com.eray.muhasebeapp.database.shared.AppDatabase

@Composable
fun App(
    database: AppDatabase,
    platformDbManager: PlatformDatabaseManager,
    guncelTarih: String,
    simdiMillis: Long,
    onYedekYukleIstegi: () -> Unit
) {
    MainStructure(
        database = database,
        platformDbManager = platformDbManager,
        guncelTarih = guncelTarih,
        simdiMillis = simdiMillis,
        onYedekYukleIstegi = onYedekYukleIstegi
    )
}