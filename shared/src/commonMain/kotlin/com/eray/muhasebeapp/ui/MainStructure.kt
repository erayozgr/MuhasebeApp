package com.eray.muhasebeapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.eray.muhasebeapp.ui.screens.AnaMenuScreen
import com.eray.muhasebeapp.ui.screens.UrunlerScreen
import com.eray.muhasebeapp.database.shared.AppDatabase
import com.eray.muhasebeapp.ui.screens.MusterilerScreen
import com.eray.muhasebeapp.ui.screens.TedarikcilerScreen
import com.eray.muhasebeapp.ui.screens.SatisScreen
import com.eray.muhasebeapp.ui.screens.AlisScreen
import com.eray.muhasebeapp.ui.screens.MasrafScreen
import com.eray.muhasebeapp.ui.screens.RaporlamaScreen
import com.eray.muhasebeapp.ui.screens.StokScreen

@Composable
fun MainStructure(
    database: AppDatabase,
    guncelTarih: String // Üst katmandan (App.kt) gelen canlı tarih
) {
    var currentScreen by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = Color(0xFFF2F2F7),

    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                0 -> AnaMenuScreen(
                    database = database,
                    guncelTarih = guncelTarih,
                    onNavigateToUrunler = { currentScreen = 1 },
                    onNavigateToMusteriler = { currentScreen = 2 },
                    onNavigateToTedarikciler = { currentScreen = 3},
                    onNavigateToSatis = { currentScreen = 4},
                    onNavigateToAlis = { currentScreen = 5},
                    onNavigateToMasraf = { currentScreen = 6},
                    onNavigateToRaporlama = { currentScreen = 7},
                    onNavigateToStok = { currentScreen = 8}
                )
                1 -> UrunlerScreen(
                    database = database,
                    onNavigateBack = { currentScreen = 0 }
                )
                2 -> MusterilerScreen(
                    database = database,
                    onNavigateBack = { currentScreen = 0 }
                )
                3 -> TedarikcilerScreen(
                    database = database,
                    onNavigateBack = { currentScreen = 0 }
                )
                4 -> SatisScreen(
                    database = database,
                    onNavigateBack = { currentScreen = 0 }
                )
                5 -> AlisScreen(
                    database = database,
                    onNavigateBack = { currentScreen = 0 }
                )
                6 -> MasrafScreen(
                    database = database,
                    onNavigateBack = { currentScreen = 0 }
                )
                7 -> RaporlamaScreen(
                    database = database,
                    onNavigateBack = { currentScreen = 0 }
                )
                8 -> StokScreen(
                    database = database,
                    onNavigateBack = { currentScreen = 0 }
                )
            }
        }
    }
}