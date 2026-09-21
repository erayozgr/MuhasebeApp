package com.eray.muhasebeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val formatter = SimpleDateFormat("d MMMM, EEEE", Locale("tr"))
        val androidTarih = formatter.format(Date())

        setContent {
            App(
                guncelTarih = androidTarih,
                simdiMillis = System.currentTimeMillis()
            )
        }
    }
}