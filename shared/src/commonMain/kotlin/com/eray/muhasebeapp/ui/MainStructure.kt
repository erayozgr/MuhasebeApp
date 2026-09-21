@file:OptIn(kotlin.time.ExperimentalTime::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.eray.muhasebeapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.eray.muhasebeapp.data.network.AuthService
import com.eray.muhasebeapp.util.odemeSuresiDoldu
import com.eray.muhasebeapp.util.ODEME_UYARISI
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
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
    simdiMillis: Long = 0L // Kullanılmıyorsa çağrıldığı yerleri bozmamak için default değer verebilir veya kaldırabilirsiniz
) {
    HesapBenimTheme {
        var currentScreen by rememberSaveable {
            mutableStateOf(0)
        }

        val authService = remember { AuthService() }
        val scope = rememberCoroutineScope()
        var checking by remember(kullaniciId) { mutableStateOf(true) }
        var checked by remember(kullaniciId) { mutableStateOf(false) }
        var deadline by remember(kullaniciId) { mutableStateOf<Long?>(null) }
        var checkError by remember(kullaniciId) { mutableStateOf<String?>(null) }
        var now by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
        var refreshJob by remember { mutableStateOf<Job?>(null) }
        val expired = odemeSuresiDoldu(deadline, now)
        val allowed = checked && !checking && checkError == null && !expired

        fun refresh(target: Int = currentScreen) {
            refreshJob?.cancel()
            checking = true
            checkError = null
            refreshJob = scope.launch {
                // Duration overload kullanımı (Legacy Long uyarısını giderir)
                val result = withTimeoutOrNull(15.seconds) {
                    withContext(Dispatchers.Default) {
                        authService.getKullaniciBilgileri(kullaniciId)
                    }
                }
                ensureActive()
                now = Clock.System.now().toEpochMilliseconds()
                if (result == null) {
                    checkError = "Hesap kontrolü zaman aşımına uğradı. Lütfen tekrar deneyin."
                    currentScreen = 0
                    checking = false
                    return@launch
                }
                result.onSuccess { account ->
                    if (account.basarili) {
                        deadline = account.sonOdemeTarihi
                        checked = true
                        currentScreen = if (target == 9 || !odemeSuresiDoldu(deadline, now)) target else 0
                    } else {
                        checkError = "Hesap bilgileri doğrulanamadı. Lütfen tekrar deneyin."
                        currentScreen = 0
                    }
                }.onFailure {
                    checkError = "Hesap bilgileri alınamadı. Bağlantınızı kontrol edip tekrar deneyin."
                    currentScreen = 0
                }
                checking = false
            }
        }

        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle, kullaniciId) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) refresh()
            }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                refreshJob?.cancel()
            }
        }
        LaunchedEffect(deadline) {
            now = Clock.System.now().toEpochMilliseconds()
            val due = deadline
            if (due != null && due > now) {
                // Duration overload kullanımı
                delay((due - now).milliseconds)
                now = Clock.System.now().toEpochMilliseconds()
            }
        }
        LaunchedEffect(expired) {
            if (expired && currentScreen != 9) currentScreen = 0
        }

        CommonBackHandler(enabled = currentScreen != 0) {
            currentScreen = 0
        }

        Scaffold(
            containerColor = BrandColors.Background,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { paddingValues ->

            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
                    .imePadding()
            ) {
                when (if (currentScreen == 9 || allowed) currentScreen else 0) {
                    0 -> AnaMenuScreen(
                        apiService = apiService,
                        kullaniciAdi = kullaniciAdi,
                        kullaniciEmail = kullaniciEmail,
                        onCikisYap = onCikisYap,
                        guncelTarih = guncelTarih,
                        ozelliklerAcik = allowed,
                        abonelikMesaji = if (expired) ODEME_UYARISI else checkError
                            ?: if (checking) "Hesap bilgileri kontrol ediliyor…" else null,
                        odemeGerekli = expired,
                        kontrolEdiliyor = checking,
                        onAbonelikYenile = { refresh(0) },
                        onNavigateToUrunler = { refresh(1) },
                        onNavigateToMusteriler = { refresh(2) },
                        onNavigateToTedarikciler = { refresh(3) },
                        onNavigateToSatis = { refresh(4) },
                        onNavigateToAlis = { refresh(5) },
                        onNavigateToMasraf = { refresh(6) },
                        onNavigateToRaporlama = { refresh(7) },
                        onNavigateToStok = { refresh(8) },
                        onNavigateToBilgiler = { currentScreen = 9 }
                    )
                    1 -> UrunlerScreen(apiService = apiService, onNavigateBack = { currentScreen = 0 })
                    2 -> MusterilerScreen(apiService = apiService, onNavigateBack = { currentScreen = 0 })
                    3 -> TedarikcilerScreen(apiService = apiService, onNavigateBack = { currentScreen = 0 })
                    4 -> SatisScreen(apiService = apiService, onNavigateBack = { currentScreen = 0 })
                    5 -> AlisScreen(apiService = apiService, onNavigateBack = { currentScreen = 0 })
                    6 -> MasrafScreen(apiService = apiService, onNavigateBack = { currentScreen = 0 })
                    7 -> RaporlamaScreen(apiService = apiService, onNavigateBack = { currentScreen = 0 })
                    8 -> StokScreen(apiService = apiService, onNavigateBack = { currentScreen = 0 })
                    9 -> BilgilerScreen(kullaniciId = kullaniciId, onNavigateBack = { currentScreen = 0 }, onCikisYap = onCikisYap)
                }
            }
        }
    }
}