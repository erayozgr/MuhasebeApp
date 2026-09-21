package com.eray.muhasebeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eray.muhasebeapp.data.network.ApiService
import com.eray.muhasebeapp.data.network.AppConfigService
import com.eray.muhasebeapp.ui.MainStructure
import com.eray.muhasebeapp.ui.screens.AppDurumEkrani
import com.eray.muhasebeapp.ui.screens.GirisScreen
import com.eray.muhasebeapp.util.AppBaslangicDurumu
import com.eray.muhasebeapp.util.AppConfig.SURUM
import com.eray.muhasebeapp.util.AppConfig.SIFRE_SIFIRLAMA_URL
import com.eray.muhasebeapp.util.SessionManager
import com.eray.muhasebeapp.util.SurumYoneticisi
import kotlinx.coroutines.launch

private val ArkaPlanGradyan = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF13B0A5),
        Color(0xFF0E7C8C),
        Color(0xFF14406B),
        Color(0xFF0B1F3A)
    )
)

@Composable
fun App(
    guncelTarih: String,
    simdiMillis: Long
) {
    val coroutineScope = rememberCoroutineScope()
    val configService = remember { AppConfigService() }
    val apiService = remember { ApiService() }
    val urlAcici = rememberUrlAcici()

    var baslangicDurumu by remember {
        mutableStateOf<AppBaslangicDurumu>(AppBaslangicDurumu.KontrolEdiliyor)
    }

    var butonYukleniyor by remember { mutableStateOf(false) }

    val kayitliOturumVar = remember { SessionManager.isSessionValid() }
    var oturumAcildiMi by remember { mutableStateOf(kayitliOturumVar) }

    var aktifKullaniciId by remember {
        mutableStateOf<Long?>(
            if (kayitliOturumVar) {
                val id = SessionManager.getUserId()
                if (id > 0) id else null
            } else null
        )
    }

    var aktifKullaniciAdi by remember {
        mutableStateOf(if (kayitliOturumVar) SessionManager.getAdSoyad() else "")
    }

    var aktifKullaniciEmail by remember {
        mutableStateOf(if (kayitliOturumVar) SessionManager.getEmail() else "")
    }

    // -------------------------------------------------------------
    // HATA MESAJINDAN İNTERNET HATASINI AYIRT ETME
    // -------------------------------------------------------------
    fun isInternetHatasi(e: Throwable): Boolean {
        val msg = (e.message ?: "").lowercase()
        val sinifAdi = (e::class.simpleName ?: "").lowercase()

        return msg.contains("resolve") ||
                msg.contains("unresolved") ||
                msg.contains("unknownhost") ||
                msg.contains("failed to connect") ||
                msg.contains("network is unreachable") ||
                msg.contains("timeout") ||
                sinifAdi.contains("unresolved") ||
                sinifAdi.contains("unknownhost") ||
                sinifAdi.contains("connectexception")
    }

    // -------------------------------------------------------------
    // KONTROL TETİKLEME
    // -------------------------------------------------------------
    fun configKontrolEt(butonlaTetiklendi: Boolean = false) {
        if (butonlaTetiklendi) {
            butonYukleniyor = true
        } else {
            baslangicDurumu = AppBaslangicDurumu.KontrolEdiliyor
        }

        coroutineScope.launch {
            val result = configService.getAppConfig()
            butonYukleniyor = false

            result.onSuccess { config ->
                when {
                    config.isMaintenanceMode -> {
                        val mesaj = config.maintenanceMessage?.takeIf { it.isNotBlank() }
                            ?: "Sistemde planlı bakım çalışması yapılmaktadır. Lütfen daha sonra tekrar deneyiniz."
                        baslangicDurumu = AppBaslangicDurumu.BakimModu(mesaj)
                    }

                    SurumYoneticisi.guncellemeGerekliMi(SURUM, config.currentVersion) -> {
                        baslangicDurumu = AppBaslangicDurumu.GuncellemeGerekli(config.currentVersion)
                    }

                    else -> {
                        baslangicDurumu = AppBaslangicDurumu.Basarili
                    }
                }
            }.onFailure { err ->
                if (isInternetHatasi(err)) {
                    baslangicDurumu = AppBaslangicDurumu.InternetYok()
                } else {
                    baslangicDurumu = AppBaslangicDurumu.BaglantiHatasi(
                        err.message ?: "Sunucuyla bağlantı kurulamadı."
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        configKontrolEt()
    }

    // -------------------------------------------------------------
    // GATEKEEPER AKIŞI
    // -------------------------------------------------------------
    when (val durum = baslangicDurumu) {
        is AppBaslangicDurumu.KontrolEdiliyor -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ArkaPlanGradyan),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sistem kontrol ediliyor...",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        is AppBaslangicDurumu.InternetYok -> {
            AppDurumEkrani(
                baslik = "İnternet Bağlantısı Yok",
                aciklama = durum.mesaj,
                ikon = Icons.Default.Refresh,
                butonMetni = "Tekrar Dene",
                yukleniyor = butonYukleniyor,
                onButonTiklandi = { configKontrolEt(butonlaTetiklendi = true) }
            )
        }

        is AppBaslangicDurumu.BakimModu -> {
            AppDurumEkrani(
                baslik = "Sistem Bakımda",
                aciklama = durum.mesaj,
                ikon = Icons.Default.Build,
                butonMetni = "Yeniden Dene",
                yukleniyor = butonYukleniyor,
                onButonTiklandi = { configKontrolEt(butonlaTetiklendi = true) }
            )
        }

        is AppBaslangicDurumu.GuncellemeGerekli -> {
            AppDurumEkrani(
                baslik = "Güncelleme Gerekli",
                aciklama = "Uygulamanızın sürümü güncel değildir.\nKullanılan: v$SURUM\nGüncel Sürüm: v${durum.sunucuSurumu}\n\nDevam edebilmek için lütfen uygulamayı güncelleyin.",
                ikon = Icons.Default.Upgrade,
                butonMetni = "Güncelle",
                onButonTiklandi = {
                    // Google Play Store yönlendirmesi
                }
            )
        }

        is AppBaslangicDurumu.BaglantiHatasi -> {
            AppDurumEkrani(
                baslik = "Bağlantı Hatası",
                aciklama = "Sunucuya erişilemedi. Lütfen daha sonra tekrar deneyiniz.",
                ikon = Icons.Default.Warning,
                butonMetni = "Tekrar Dene",
                yukleniyor = butonYukleniyor,
                onButonTiklandi = { configKontrolEt(butonlaTetiklendi = true) }
            )
        }

        is AppBaslangicDurumu.Basarili -> {
            if (!oturumAcildiMi) {
                GirisScreen(
                    onSifremiUnuttum = { urlAcici.ac(SIFRE_SIFIRLAMA_URL) },
                    onGirisBasarili = { id, adSoyad, email ->
                        aktifKullaniciId = id
                        aktifKullaniciAdi = adSoyad
                        aktifKullaniciEmail = email
                        oturumAcildiMi = true
                    }
                )
            } else {
                val kullaniciId = aktifKullaniciId

                if (kullaniciId != null) {
                    MainStructure(
                        apiService = apiService,
                        kullaniciId = kullaniciId,
                        kullaniciAdi = aktifKullaniciAdi,
                        kullaniciEmail = aktifKullaniciEmail,
                        onCikisYap = {
                            SessionManager.clearSession()
                            oturumAcildiMi = false
                            aktifKullaniciId = null
                            aktifKullaniciAdi = ""
                            aktifKullaniciEmail = ""
                        },
                        guncelTarih = guncelTarih,
                        simdiMillis = simdiMillis
                    )
                } else {
                    SessionManager.clearSession()
                    oturumAcildiMi = false
                    aktifKullaniciAdi = ""
                    aktifKullaniciEmail = ""
                }
            }
        }
    }
}
