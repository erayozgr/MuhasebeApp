@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.eray.muhasebeapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eray.muhasebeapp.data.network.AuthService
import com.eray.muhasebeapp.ui.swipeToBack
import com.eray.muhasebeapp.util.AppConfig.SURUM
import com.eray.muhasebeapp.util.PasswordValidator
import kotlinx.coroutines.launch
import com.eray.muhasebeapp.util.odemeTarihiMetni
import com.eray.muhasebeapp.util.odemeSuresiDoldu
import com.eray.muhasebeapp.util.ODEME_UYARISI
import com.eray.muhasebeapp.util.AppConfig.ODEME_WEB_URL
import com.eray.muhasebeapp.rememberUrlAcici
import kotlin.time.Clock

private val ArkaPlanGradyan = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF13B0A5),
        Color(0xFF0E7C8C),
        Color(0xFF14406B),
        Color(0xFF0B1F3A)
    )
)

private val BeyazYariSeffaf = Color.White.copy(alpha = 0.14f)

@Composable
fun BilgilerScreen(
    kullaniciId: Long,
    onNavigateBack: () -> Unit,
    onCikisYap: () -> Unit
) {
    var adSoyad by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telefon by remember { mutableStateOf("") }
    var sonOdemeTarihi by remember { mutableStateOf<Long?>(null) }
    val urlAcici = rememberUrlAcici()

    // =========================================================
    // E-POSTA DOĞRULAMA
    // =========================================================

    var emailDogrulandiMi by remember { mutableStateOf(false) }
    var emailDogrulamaAcik by remember { mutableStateOf(false) }

    // =========================================================
    // HESAP SİLME
    // =========================================================

    var hesapSilmeAcik by remember { mutableStateOf(false) }

    var yukleniyor by remember { mutableStateOf(true) }
    var islemYapiliyor by remember { mutableStateOf(false) }

    var hataMesaji by remember { mutableStateOf<String?>(null) }
    var basariMesaji by remember { mutableStateOf<String?>(null) }

    var adDuzenlemeAcik by remember { mutableStateOf(false) }
    var sifreDegistirmeAcik by remember { mutableStateOf(false) }

    val authService = remember { AuthService() }
    val coroutineScope = rememberCoroutineScope()

    // =========================================================
    // KULLANICI BİLGİLERİNİ GETİR
    // =========================================================

    LaunchedEffect(kullaniciId) {
        yukleniyor = true
        hataMesaji = null

        val result = authService.getKullaniciBilgileri(kullaniciId)
        yukleniyor = false

        result.onSuccess { res ->
            adSoyad = res.adSoyad.orEmpty()
            email = res.email.orEmpty()
            telefon = res.telefon.orEmpty()
            sonOdemeTarihi = res.sonOdemeTarihi
            emailDogrulandiMi = res.emailDogrulandiMi ?: false
        }.onFailure { err ->
            println("Kullanıcı Bilgileri Yükleme Hatası: ${err.message}")
            hataMesaji = "İşlem başarısız. Tekrar deneyin."
        }
    }

    // =========================================================
    // E-POSTA DOĞRULAMA EKRANI
    // =========================================================

    if (emailDogrulamaAcik) {
        EmailDogrulamaScreen(
            kullaniciId = kullaniciId,
            email = email,
            authService = authService,
            onGeri = { emailDogrulamaAcik = false },
            onDogrulandi = {
                emailDogrulandiMi = true
                emailDogrulamaAcik = false
                hataMesaji = null
                basariMesaji = "E-posta adresiniz başarıyla doğrulandı."
            }
        )
        return
    }

    // =========================================================
    // HESAP SİLME EKRANI
    // =========================================================

    if (hesapSilmeAcik) {
        HesapSilmeScreen(
            kullaniciId = kullaniciId,
            email = email,
            authService = authService,
            onGeri = { hesapSilmeAcik = false },
            onHesapSilindi = {
                hesapSilmeAcik = false
                onCikisYap()
            }
        )
        return
    }

    // =========================================================
    // ANA EKRAN
    // =========================================================

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArkaPlanGradyan)
            .swipeToBack(onBack = onNavigateBack)
    ) {
        if (yukleniyor) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // =================================================
                // ÜST BAR
                // =================================================

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Hesap Bilgileri",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // =================================================
                // PROFİL
                // =================================================

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(BeyazYariSeffaf),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = adSoyad.ifBlank { "İşletme Hesabı" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Text(
                        text = email,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // =================================================
                // BİLGİLER
                // =================================================

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // -------------------------------------------------
                    // AD SOYAD
                    // -------------------------------------------------

                    BilgiKart(
                        ikon = Icons.Default.Person,
                        baslik = "Ad Soyad / İşletme Adı",
                        deger = adSoyad.ifBlank { "Belirtilmemiş" }
                    )

                    BilgiKart(
                        ikon = Icons.Default.CalendarToday,
                        baslik = "Son Ödeme Tarihi",
                        deger = odemeTarihiMetni(sonOdemeTarihi)
                    )
                    if (odemeSuresiDoldu(sonOdemeTarihi, Clock.System.now().toEpochMilliseconds())) {
                        Text(ODEME_UYARISI, color = Color.White)
                        Button(onClick = { urlAcici.ac(ODEME_WEB_URL) }) { Text("Web sitesine git") }
                    }

                    // -------------------------------------------------
                    // E-POSTA
                    // -------------------------------------------------

                    BilgiKart(
                        ikon = Icons.Default.Email,
                        baslik = "E-posta Adresi",
                        deger = email.ifBlank { "Belirtilmemiş" },
                        sagIcerik = {
                            if (emailDogrulandiMi) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Doğrulandı",
                                        tint = Color(0xFF69F0AE),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Doğrulandı",
                                        color = Color(0xFF69F0AE),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                TextButton(
                                    enabled = email.isNotBlank(),
                                    onClick = {
                                        hataMesaji = null
                                        basariMesaji = null
                                        emailDogrulamaAcik = true
                                    }
                                ) {
                                    Text(
                                        text = "Doğrula",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    )

                    // -------------------------------------------------
                    // TELEFON
                    // -------------------------------------------------

                    BilgiKart(
                        ikon = Icons.Default.Phone,
                        baslik = "Telefon Numarası",
                        deger = telefon.ifBlank { "Belirtilmemiş" }
                    )

                    // -------------------------------------------------
                    // KULLANICI ADI DEĞİŞTİR
                    // -------------------------------------------------

                    Button(
                        onClick = {
                            hataMesaji = null
                            basariMesaji = null
                            adDuzenlemeAcik = true
                        },
                        enabled = !islemYapiliyor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.18f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Kullanıcı Adını Değiştir",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // -------------------------------------------------
                    // ŞİFRE DEĞİŞTİR
                    // -------------------------------------------------

                    Button(
                        onClick = {
                            hataMesaji = null
                            basariMesaji = null
                            sifreDegistirmeAcik = true
                        },
                        enabled = !islemYapiliyor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.18f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Şifreyi Değiştir",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // =================================================
                    // BAŞARI MESAJI
                    // =================================================

                    AnimatedVisibility(visible = basariMesaji != null) {
                        Text(
                            text = basariMesaji.orEmpty(),
                            color = Color(0xFFB9F6CA),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }

                    // =================================================
                    // HATA MESAJI
                    // =================================================

                    AnimatedVisibility(visible = hataMesaji != null) {
                        Text(
                            text = hataMesaji.orEmpty(),
                            color = Color(0xFFFF8A80),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // =================================================
                    // ÇIKIŞ
                    // =================================================

                    Button(
                        onClick = onCikisYap,
                        enabled = !islemYapiliyor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Oturumu Kapat",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // =================================================
                    // HESABI SİL (KÜÇÜK VE SADE TASARIM)
                    // =================================================

                    TextButton(
                        onClick = {
                            hataMesaji = null
                            basariMesaji = null
                            hesapSilmeAcik = true
                        },
                        enabled = !islemYapiliyor && email.isNotBlank(),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 4.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFFF8A80).copy(alpha = 0.85f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hesabımı Sil",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(70.dp))
            }
        }

        // =====================================================
        // VERSİYON
        // =====================================================

        Text(
            text = "v$SURUM",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    // =========================================================
    // KULLANICI ADI DEĞİŞTİR DIALOG
    // =========================================================

    if (adDuzenlemeAcik) {
        AdSoyadDegistirDialog(
            mevcutAd = adSoyad,
            yukleniyor = islemYapiliyor,
            onDismiss = {
                if (!islemYapiliyor) adDuzenlemeAcik = false
            },
            onKaydet = { yeniAd ->
                if (yeniAd.isBlank()) {
                    hataMesaji = "Kullanıcı adı boş bırakılamaz."
                } else {
                    hataMesaji = null
                    basariMesaji = null
                    islemYapiliyor = true

                    coroutineScope.launch {
                        val result = authService.kullaniciAdiGuncelle(
                            kullaniciId = kullaniciId,
                            yeniAdSoyad = yeniAd.trim()
                        )
                        islemYapiliyor = false

                        result.onSuccess { response ->
                            if (response.basarili) {
                                adSoyad = response.adSoyad ?: yeniAd.trim()
                                basariMesaji = response.mesaj.ifBlank {
                                    "Kullanıcı adı başarıyla güncellendi."
                                }
                                adDuzenlemeAcik = false
                            } else {
                                println("Ad Soyad Güncelleme Hatası: ${response.mesaj}")
                                hataMesaji = response.mesaj.ifBlank {
                                    "İşlem başarısız. Tekrar deneyin."
                                }
                            }
                        }.onFailure { error ->
                            println("Ad Soyad Güncelleme Hatası: ${error.message}")
                            hataMesaji = "İşlem başarısız. Tekrar deneyin."
                        }
                    }
                }
            }
        )
    }

    // =========================================================
    // ŞİFRE DEĞİŞTİR DIALOG
    // =========================================================

    if (sifreDegistirmeAcik) {
        SifreDegistirDialog(
            yukleniyor = islemYapiliyor,
            onDismiss = {
                if (!islemYapiliyor) sifreDegistirmeAcik = false
            },
            onKaydet = { mevcutSifre, yeniSifre ->
                val sifreHatasi = PasswordValidator.hataMesaji(yeniSifre)

                when {
                    sifreHatasi != null -> {
                        hataMesaji = sifreHatasi
                    }
                    mevcutSifre == yeniSifre -> {
                        hataMesaji = "Yeni şifre mevcut şifreden farklı olmalıdır."
                    }
                    else -> {
                        hataMesaji = null
                        basariMesaji = null
                        islemYapiliyor = true

                        coroutineScope.launch {
                            val result = authService.sifreDegistir(
                                kullaniciId = kullaniciId,
                                mevcutSifre = mevcutSifre,
                                yeniSifre = yeniSifre
                            )
                            islemYapiliyor = false

                            result.onSuccess { response ->
                                if (response.basarili) {
                                    basariMesaji = response.mesaj.ifBlank {
                                        "Şifreniz başarıyla değiştirildi."
                                    }
                                    sifreDegistirmeAcik = false
                                } else {
                                    println("Şifre Değiştirme Hatası: ${response.mesaj}")
                                    hataMesaji = response.mesaj.ifBlank {
                                        "İşlem başarısız. Tekrar deneyin."
                                    }
                                }
                            }.onFailure { error ->
                                println("Şifre Değiştirme Hatası: ${error.message}")
                                hataMesaji = "İşlem başarısız. Tekrar deneyin."
                            }
                        }
                    }
                }
            }
        )
    }
}

// =============================================================
// E-POSTA DOĞRULAMA EKRANI
// =============================================================

@Composable
private fun EmailDogrulamaScreen(
    kullaniciId: Long,
    email: String,
    authService: AuthService,
    onGeri: () -> Unit,
    onDogrulandi: () -> Unit
) {
    var kod by remember { mutableStateOf("") }
    var kodGonderiliyor by remember { mutableStateOf(false) }
    var dogrulaniyor by remember { mutableStateOf(false) }
    var hataMesaji by remember { mutableStateOf<String?>(null) }
    var bilgiMesaji by remember { mutableStateOf<String?>(null) }
    var gonderimSayaci by remember { mutableStateOf(0) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(gonderimSayaci) {
        kodGonderiliyor = true
        hataMesaji = null
        bilgiMesaji = null

        val result = authService.emailDogrulamaKoduGonder(kullaniciId)
        kodGonderiliyor = false

        result.onSuccess { response ->
            if (response.basarili) {
                bilgiMesaji = response.mesaj.ifBlank {
                    "6 haneli doğrulama kodu e-posta adresinize gönderildi."
                }
            } else {
                hataMesaji = response.mesaj.ifBlank {
                    "Doğrulama kodu gönderilemedi."
                }
            }
        }.onFailure { error ->
            println("E-posta Doğrulama Kodu Gönderme Hatası: ${error.message}")
            hataMesaji = "Doğrulama kodu gönderilemedi. Tekrar deneyin."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArkaPlanGradyan)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    enabled = !dogrulaniyor,
                    onClick = onGeri
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Geri",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "E-posta Doğrulama",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(BeyazYariSeffaf),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailUnread,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "E-posta Adresinizi Doğrulayın",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (kodGonderiliyor) {
                        "Doğrulama kodu gönderiliyor..."
                    } else {
                        "$email adresine gönderilen 6 haneli doğrulama kodunu girin."
                    },
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = BeyazYariSeffaf)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Doğrulama Kodu",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = kod,
                            onValueChange = { yeniDeger ->
                                kod = yeniDeger.filter { it.isDigit() }.take(6)
                                hataMesaji = null
                            },
                            enabled = !dogrulaniyor && !kodGonderiliyor,
                            label = { Text("6 Haneli Kod") },
                            placeholder = { Text("000000") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Pin, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            enabled = kod.length == 6 && !dogrulaniyor && !kodGonderiliyor,
                            onClick = {
                                hataMesaji = null
                                bilgiMesaji = null
                                dogrulaniyor = true

                                coroutineScope.launch {
                                    val result = authService.emailDogrula(
                                        kullaniciId = kullaniciId,
                                        kod = kod
                                    )
                                    dogrulaniyor = false

                                    result.onSuccess { response ->
                                        if (response.basarili) {
                                            onDogrulandi()
                                        } else {
                                            hataMesaji = response.mesaj.ifBlank {
                                                "Doğrulama kodu hatalı."
                                            }
                                        }
                                    }.onFailure { error ->
                                        println("E-posta Doğrulama Hatası: ${error.message}")
                                        hataMesaji = "Doğrulama işlemi başarısız. Tekrar deneyin."
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (dogrulaniyor) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(imageVector = Icons.Default.Verified, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "E-postayı Doğrula", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            enabled = !kodGonderiliyor && !dogrulaniyor,
                            onClick = {
                                kod = ""
                                hataMesaji = null
                                bilgiMesaji = null
                                gonderimSayaci++
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (kodGonderiliyor) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Kod Gönderiliyor...", color = Color.White)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Kodu Tekrar Gönder",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = bilgiMesaji != null) {
                    Text(
                        text = bilgiMesaji.orEmpty(),
                        color = Color(0xFFB9F6CA),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                }

                AnimatedVisibility(visible = hataMesaji != null) {
                    Text(
                        text = hataMesaji.orEmpty(),
                        color = Color(0xFFFF8A80),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// =============================================================
// HESAP SİLME EKRANI
// =============================================================

@Composable
private fun HesapSilmeScreen(
    kullaniciId: Long,
    email: String,
    authService: AuthService,
    onGeri: () -> Unit,
    onHesapSilindi: () -> Unit
) {
    var kod by remember { mutableStateOf("") }
    var kodGonderiliyor by remember { mutableStateOf(false) }
    var siliniyor by remember { mutableStateOf(false) }
    var hataMesaji by remember { mutableStateOf<String?>(null) }
    var bilgiMesaji by remember { mutableStateOf<String?>(null) }
    var gonderimSayaci by remember { mutableStateOf(0) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(gonderimSayaci) {
        kodGonderiliyor = true
        hataMesaji = null
        bilgiMesaji = null

        val result = authService.hesapSilmeKoduGonder(kullaniciId)
        kodGonderiliyor = false

        result.onSuccess { response ->
            if (response.basarili) {
                bilgiMesaji = response.mesaj.ifBlank {
                    "Hesap silme doğrulama kodu e-posta adresinize gönderildi."
                }
            } else {
                hataMesaji = response.mesaj.ifBlank {
                    "Doğrulama kodu gönderilemedi."
                }
            }
        }.onFailure { error ->
            println("Hesap Silme Kodu Gönderme Hatası: ${error.message}")
            hataMesaji = "Doğrulama kodu gönderilemedi. Tekrar deneyin."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArkaPlanGradyan)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    enabled = !siliniyor && !kodGonderiliyor,
                    onClick = onGeri
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Geri",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Hesabı Sil",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB71C1C).copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Hesabınızı Silmek Üzeresiniz",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Bu işlem geri alınamaz. Hesabınızı silmek için $email adresine gönderilen 6 haneli doğrulama kodunu girin.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = BeyazYariSeffaf)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Hesap Silme Doğrulama Kodu",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = kod,
                            onValueChange = { yeniDeger ->
                                kod = yeniDeger.filter { it.isDigit() }.take(6)
                                hataMesaji = null
                            },
                            enabled = !siliniyor && !kodGonderiliyor,
                            label = { Text("6 Haneli Kod") },
                            placeholder = { Text("000000") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Key, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            enabled = kod.length == 6 && !siliniyor && !kodGonderiliyor,
                            onClick = {
                                hataMesaji = null
                                bilgiMesaji = null
                                siliniyor = true

                                coroutineScope.launch {
                                    val result = authService.hesabiSil(
                                        kullaniciId = kullaniciId,
                                        kod = kod
                                    )
                                    siliniyor = false

                                    result.onSuccess { response ->
                                        if (response.basarili) {
                                            onHesapSilindi()
                                        } else {
                                            hataMesaji = response.mesaj.ifBlank {
                                                "Hesap silinemedi."
                                            }
                                        }
                                    }.onFailure { error ->
                                        println("Hesap Silme Hatası: ${error.message}")
                                        hataMesaji = "Hesap silme işlemi başarısız. Tekrar deneyin."
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (siliniyor) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Hesabımı Kalıcı Olarak Sil", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            enabled = !kodGonderiliyor && !siliniyor,
                            onClick = {
                                kod = ""
                                hataMesaji = null
                                bilgiMesaji = null
                                gonderimSayaci++
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (kodGonderiliyor) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Kod Gönderiliyor...", color = Color.White)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Kodu Tekrar Gönder",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = bilgiMesaji != null) {
                    Text(
                        text = bilgiMesaji.orEmpty(),
                        color = Color(0xFFB9F6CA),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                }

                AnimatedVisibility(visible = hataMesaji != null) {
                    Text(
                        text = hataMesaji.orEmpty(),
                        color = Color(0xFFFF8A80),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// =============================================================
// BİLGİ KARTI
// =============================================================

@Composable
fun BilgiKart(
    ikon: ImageVector,
    baslik: String,
    deger: String,
    sagIcerik: (@Composable () -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BeyazYariSeffaf),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ikon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = baslik,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = deger,
                    fontSize = 15.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (sagIcerik != null) {
                Spacer(modifier = Modifier.width(8.dp))
                sagIcerik()
            }
        }
    }
}

// =============================================================
// KULLANICI ADI DEĞİŞTİR DIALOG
// =============================================================

@Composable
private fun AdSoyadDegistirDialog(
    mevcutAd: String,
    yukleniyor: Boolean,
    onDismiss: () -> Unit,
    onKaydet: (String) -> Unit
) {
    var yeniAd by remember(mevcutAd) { mutableStateOf(mevcutAd) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
        title = { Text(text = "Kullanıcı Adını Değiştir") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Yeni ad soyad veya işletme adınızı girin.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = yeniAd,
                    onValueChange = { yeniAd = it },
                    label = { Text("Yeni Kullanıcı Adı") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !yukleniyor && yeniAd.isNotBlank(),
                onClick = { onKaydet(yeniAd.trim()) }
            ) {
                if (yukleniyor) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Güncelle")
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !yukleniyor,
                onClick = onDismiss
            ) {
                Text("İptal")
            }
        }
    )
}

// =============================================================
// ŞİFRE DEĞİŞTİR DIALOG
// =============================================================

@Composable
private fun SifreDegistirDialog(
    yukleniyor: Boolean,
    onDismiss: () -> Unit,
    onKaydet: (mevcutSifre: String, yeniSifre: String) -> Unit
) {
    var mevcutSifre by remember { mutableStateOf("") }
    var yeniSifre by remember { mutableStateOf("") }
    var yeniSifreTekrar by remember { mutableStateOf("") }

    var mevcutSifreGoster by remember { mutableStateOf(false) }
    var yeniSifreGoster by remember { mutableStateOf(false) }
    var tekrarSifreGoster by remember { mutableStateOf(false) }

    var dialogHata by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
        title = { Text(text = "Şifreyi Değiştir") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // -------------------------------------------------
                // MEVCUT ŞİFRE
                // -------------------------------------------------

                OutlinedTextField(
                    value = mevcutSifre,
                    onValueChange = {
                        mevcutSifre = it
                        dialogHata = null
                    },
                    label = { Text("Mevcut Şifre") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { mevcutSifreGoster = !mevcutSifreGoster }) {
                            Icon(
                                imageVector = if (mevcutSifreGoster) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (mevcutSifreGoster) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // -------------------------------------------------
                // YENİ ŞİFRE
                // -------------------------------------------------

                OutlinedTextField(
                    value = yeniSifre,
                    onValueChange = {
                        yeniSifre = it
                        dialogHata = if (it.isNotEmpty()) PasswordValidator.hataMesaji(it) else null
                    },
                    label = { Text("Yeni Şifre") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Key, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { yeniSifreGoster = !yeniSifreGoster }) {
                            Icon(
                                imageVector = if (yeniSifreGoster) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (yeniSifreGoster) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Şifre en az 8 karakter olmalı ve en az 1 büyük harf, 1 küçük harf, 1 rakam ve 1 özel karakter içermelidir.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // -------------------------------------------------
                // YENİ ŞİFRE TEKRAR
                // -------------------------------------------------

                OutlinedTextField(
                    value = yeniSifreTekrar,
                    onValueChange = {
                        yeniSifreTekrar = it
                        dialogHata = null
                    },
                    label = { Text("Yeni Şifre Tekrar") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Key, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { tekrarSifreGoster = !tekrarSifreGoster }) {
                            Icon(
                                imageVector = if (tekrarSifreGoster) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (tekrarSifreGoster) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = dialogHata != null) {
                    Text(
                        text = dialogHata.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !yukleniyor,
                onClick = {
                    when {
                        mevcutSifre.isBlank() -> dialogHata = "Mevcut şifrenizi giriniz."
                        yeniSifre.isBlank() -> dialogHata = "Yeni şifrenizi giriniz."
                        PasswordValidator.hataMesaji(yeniSifre) != null -> {
                            dialogHata = PasswordValidator.hataMesaji(yeniSifre)
                        }
                        yeniSifreTekrar.isBlank() -> dialogHata = "Yeni şifrenizi tekrar giriniz."
                        yeniSifre != yeniSifreTekrar -> dialogHata = "Yeni şifreler eşleşmiyor."
                        mevcutSifre == yeniSifre -> dialogHata = "Yeni şifre mevcut şifreden farklı olmalıdır."
                        else -> {
                            dialogHata = null
                            onKaydet(mevcutSifre, yeniSifre)
                        }
                    }
                }
            ) {
                if (yukleniyor) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Şifreyi Değiştir")
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !yukleniyor,
                onClick = onDismiss
            ) {
                Text("İptal")
            }
        }
    )
}