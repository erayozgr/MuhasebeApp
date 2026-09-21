package com.eray.muhasebeapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.eray.muhasebeapp.data.network.ApiService
import com.eray.muhasebeapp.ui.screens.*
import com.eray.muhasebeapp.util.CommonBackHandler

@Composable
fun MainStructure(
    apiService: ApiService,
    kullaniciId: Long,
    kullaniciAdi: String,
    kullaniciEmail: String,
    onCikisYap: () -> Unit,
    guncelTarih: String,
    simdiMillis: Long
) {

    var currentScreen by remember {
        mutableStateOf(0)
    }

    CommonBackHandler(enabled = currentScreen != 0) {
        currentScreen = 0
    }

    Scaffold(
        containerColor = Color(0xFFF2F2F7)
    ) { paddingValues ->

        Box(
            modifier = Modifier.padding(
                paddingValues
            )
        ) {

            when (currentScreen) {

                0 -> AnaMenuScreen(
                    apiService = apiService,
                    kullaniciAdi = kullaniciAdi,
                    kullaniciEmail = kullaniciEmail,
                    onCikisYap = onCikisYap,
                    guncelTarih = guncelTarih,

                    onNavigateToUrunler = {
                        currentScreen = 1
                    },

                    onNavigateToMusteriler = {
                        currentScreen = 2
                    },

                    onNavigateToTedarikciler = {
                        currentScreen = 3
                    },

                    onNavigateToSatis = {
                        currentScreen = 4
                    },

                    onNavigateToAlis = {
                        currentScreen = 5
                    },

                    onNavigateToMasraf = {
                        currentScreen = 6
                    },

                    onNavigateToRaporlama = {
                        currentScreen = 7
                    },

                    onNavigateToStok = {
                        currentScreen = 8
                    },

                    onNavigateToBilgiler = {
                        currentScreen = 9
                    }
                )

                1 -> UrunlerScreen(
                    apiService = apiService,
                    onNavigateBack = {
                        currentScreen = 0
                    }
                )

                2 -> MusterilerScreen(
                    apiService = apiService,
                    onNavigateBack = {
                        currentScreen = 0
                    }
                )

                3 -> TedarikcilerScreen(
                    apiService = apiService,
                    onNavigateBack = {
                        currentScreen = 0
                    }
                )

                4 -> SatisScreen(
                    apiService = apiService,
                    onNavigateBack = {
                        currentScreen = 0
                    }
                )

                5 -> AlisScreen(
                    apiService = apiService,
                    onNavigateBack = {
                        currentScreen = 0
                    }
                )

                6 -> MasrafScreen(
                    apiService = apiService,
                    onNavigateBack = {
                        currentScreen = 0
                    }
                )

                7 -> RaporlamaScreen(
                    apiService = apiService,
                    onNavigateBack = {
                        currentScreen = 0
                    }
                )

                8 -> StokScreen(
                    apiService = apiService,
                    onNavigateBack = {
                        currentScreen = 0
                    }
                )

                9 -> BilgilerScreen(
                    kullaniciId = kullaniciId,
                    onNavigateBack = {
                        currentScreen = 0
                    },
                    onCikisYap = onCikisYap
                )
            }
        }
    }
}