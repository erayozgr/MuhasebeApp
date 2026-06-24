package com.eray.muhasebeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
//  SQLDelight ve Sürücü sınıflarımızı içeri aktarıyoruz
import com.eray.muhasebeapp.database.shared.AppDatabase
import com.eray.muhasebeapp.database.DriverFactory


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge() // Kenardan kenara tam ekran desteğinizi koruduk
        super.onCreate(savedInstanceState)

        //  1. Android sistem referansını (applicationContext) vererek SQLite sürücüsünü başlatıyoruz
        val driverFactory = DriverFactory(applicationContext)
        val sqliteDriver = driverFactory.createDriver()

        //  2. Sürücüyü kullanarak gerçek veritabanı nesnemizi oluşturuyoruz
        val database = AppDatabase(sqliteDriver)

        setContent {
            //  3. Az önce oluşturduğumuz canlı veritabanını App fonksiyonuna paslıyoruz
            App(database = database)
        }
    }
}

