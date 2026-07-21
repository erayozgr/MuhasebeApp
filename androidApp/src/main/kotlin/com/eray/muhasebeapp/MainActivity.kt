package com.eray.muhasebeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.eray.muhasebeapp.database.shared.AppDatabase
import com.eray.muhasebeapp.database.DriverFactory
// Hata veren java.time yerine, eski ama altın değerindeki bu importları ekliyoruz:
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val driverFactory = DriverFactory(applicationContext)
        val sqliteDriver = driverFactory.createDriver()
        val database = AppDatabase(sqliteDriver)

        // ─── HER SÜRÜMDE KESİN ÇALIŞAN TÜRKÇE TARİH ───
        // SimpleDateFormat hem eski sürümleri destekler hem de 'tr' lokaliyle günleri Türkçe yazar.
        val formatter = SimpleDateFormat("d MMMM, EEEE", Locale("tr"))
        val androidTarih = formatter.format(Date())

        setContent {

            App(database = database, guncelTarih = androidTarih)
        }
    }
}