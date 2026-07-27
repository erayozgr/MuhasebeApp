package com.eray.muhasebeapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.eray.muhasebeapp.database.shared.AppDatabase
import com.eray.muhasebeapp.database.DriverFactory
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

        val platformDbManager = PlatformDatabaseManager(applicationContext)

        val yedekSeciciLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    contentResolver.openInputStream(it)?.use { inputStream ->
                        val bytes = inputStream.readBytes()
                        val basariliMi = platformDbManager.restoreDatabaseBytes(bytes)
                        if (basariliMi) {
                            Toast.makeText(
                                this,
                                "Yedek başarıyla yüklendi! Uygulama yeniden başlatılıyor...",
                                Toast.LENGTH_LONG
                            ).show()

                            // DÜZELTİLDİ: Canlı SQLite bağlantı kilitlerini ve -wal/-shm önbellek dosyalarını
                            // temizlemek için süreci (process) sıfırlıyoruz. Uygulama kendini güvenle kapatacak.
                            kotlin.system.exitProcess(0)
                        } else {
                            Toast.makeText(this, "Yedek yüklenirken bir hata oluştu!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Dosya okunurken hata oluştu!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val formatter = SimpleDateFormat("d MMMM, EEEE", Locale("tr"))
        val androidTarih = formatter.format(Date())

        setContent {
            App(
                database = database,
                platformDbManager = platformDbManager,
                guncelTarih = androidTarih,
                simdiMillis = System.currentTimeMillis(),
                onYedekYukleIstegi = {
                    yedekSeciciLauncher.launch(arrayOf("*/*"))
                }
            )
        }
    }
}