package com.eray.muhasebeapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.eray.muhasebeapp.ui.screens.AnaMenuScreen
import com.eray.muhasebeapp.ui.screens.UrunlerScreen
import com.eray.muhasebeapp.database.shared.AppDatabase

@Composable
fun MainStructure(database: AppDatabase) {
    var currentScreen by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = Color(0xFFF2F2F7)
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                0 -> AnaMenuScreen(
                    database = database,
                    onNavigateToUrunler = { currentScreen = 1 }
                )
                1 -> UrunlerScreen(
                    database = database,
                    onNavigateBack = { currentScreen = 0 }
                )
            }
        }
    }
}