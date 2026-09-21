package com.eray.muhasebeapp.ui.screens

import androidx.compose.foundation.BorderStroke
import com.eray.muhasebeapp.ui.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eray.muhasebeapp.data.model.*
import com.eray.muhasebeapp.data.network.ApiService
import com.eray.muhasebeapp.rememberUrlAcici
import com.eray.muhasebeapp.telefonLinkOlustur
import com.eray.muhasebeapp.whatsappLinkOlustur
import com.eray.muhasebeapp.formatTarih
import com.eray.muhasebeapp.parseTarihMillis
import kotlinx.coroutines.launch

private fun bakiyeMetniVeRengi(bakiye: Double): Pair<String, Color> = when {
    bakiye > 0 -> "₺${formatKisaPara(bakiye)} (Borçlu)" to BrandColors.Danger
    bakiye < 0 -> "₺${formatKisaPara(-bakiye)} (Alacaklı)" to BrandColors.Navy
    else -> "₺0,00 (Dengede)" to BrandColors.Success
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusterilerScreen(
    apiService: ApiService,
    onNavigateBack: () -> Unit
) {

    var yenilemeTetikleyici by remember { mutableStateOf(0) }
    var musteriler by remember { mutableStateOf<List<Musteri>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    // ---------------------------------------------------------
    // ARAMA
    // ---------------------------------------------------------

    var aramaAcik by remember { mutableStateOf(false) }
    var aramaMetni by remember { mutableStateOf("") }

    val filtrelenmisMusteriler = remember(musteriler, aramaMetni) {
        val aranacak = aramaMetni.trim()
        if (aranacak.isBlank()) musteriler
        else musteriler.filter { it.ad.contains(aranacak, ignoreCase = true) }
    }

    // ---------------------------------------------------------
    // VERİLERİ YÜKLE
    // ---------------------------------------------------------

    LaunchedEffect(yenilemeTetikleyici) {
        loading = true
        try {
            musteriler = apiService.getMusteriler()
        } catch (e: Exception) {
            println("Müşteri Yükleme Hatası: ${e.message}")
            e.printStackTrace()
        } finally {
            loading = false
        }
    }

    // ---------------------------------------------------------
    // DIALOGLAR
    // ---------------------------------------------------------

    var dialogAcikMi by remember { mutableStateOf(false) }
    var detayGosterilenMusteri by remember { mutableStateOf<Musteri?>(null) }
    var duzenlenenMusteri by remember { mutableStateOf<Musteri?>(null) }
    var bakiyeDuzenlenenMusteri by remember { mutableStateOf<Musteri?>(null) }
    var tahsilatMusteri by remember { mutableStateOf<Musteri?>(null) }
    var raporMusteri by remember { mutableStateOf<Musteri?>(null) }
    var silinecekMusteri by remember { mutableStateOf<Musteri?>(null) }
    var genelMusteriDialogAcikMi by remember { mutableStateOf(false) }

    val toplamBakiye = remember(musteriler) { musteriler.sumOf { it.bakiye } }

    // ---------------------------------------------------------
    // SAYFA
    // ---------------------------------------------------------

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = BrandColors.Background,
        modifier = Modifier.swipeToBack(onBack = onNavigateBack),

        topBar = {
            BrandTopBar("Müşteriler", "Cari hesaplar ve tahsilatlar", onNavigateBack) {
                IconButton(
                    onClick = {
                        aramaAcik = !aramaAcik
                        if (!aramaAcik) aramaMetni = ""
                    }
                ) {
                    Icon(
                        imageVector = if (aramaAcik) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (aramaAcik) "Aramayı Kapat" else "Müşteri Ara",
                        tint = Color.White
                    )
                }
            }
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = { dialogAcikMi = true },
                containerColor = BrandColors.Navy,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Yeni müşteri")
            }
        }

    ) { padding ->

        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            if (loading && musteriler.isEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // -------------------------------------------------
            // ARAMA ALANI
            // -------------------------------------------------

            if (aramaAcik) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color.White)
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    OutlinedTextField(
                        value = aramaMetni,
                        onValueChange = { aramaMetni = it },
                        placeholder = {
                            Text("Müşteri adına göre ara...", color = BrandColors.Muted, fontSize = 13.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = BrandColors.Muted)
                        },
                        trailingIcon = {
                            if (aramaMetni.isNotEmpty()) {
                                IconButton(onClick = { aramaMetni = "" }) {
                                    Icon(
                                        Icons.Default.Cancel,
                                        contentDescription = "Aramayı Temizle",
                                        tint = BrandColors.Muted
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BrandColors.Background,
                            unfocusedContainerColor = BrandColors.Background,
                            focusedBorderColor = BrandColors.Navy,
                            unfocusedBorderColor = BrandColors.Border,
                            focusedTextColor = BrandColors.Ink,
                            unfocusedTextColor = BrandColors.Ink,
                            cursorColor = BrandColors.Navy
                        ),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    )

                    if (aramaMetni.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${filtrelenmisMusteriler.size} müşteri bulundu",
                            fontSize = 11.sp,
                            color = BrandColors.Muted,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            // -------------------------------------------------
            // ÖZET KARTI
            // -------------------------------------------------

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BrandMetric(
                    "Toplam Müşteri", musteriler.size.toString(),
                    BrandColors.Navy, Modifier.weight(1f)
                )
                BrandMetric(
                    "Müşteri borcu", "₺${formatKisaPara(toplamBakiye)}",
                    if (toplamBakiye > 0) BrandColors.Warning else BrandColors.Success,
                    Modifier.weight(1f)
                )
            }

            // -------------------------------------------------
            // GENEL MÜŞTERİ
            // -------------------------------------------------

            Card(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BrandColors.Border),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp)
                    .clickable { genelMusteriDialogAcikMi = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(33.dp).background(
                                BrandColors.Muted.copy(alpha = 0.12f), RoundedCornerShape(10.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Groups, contentDescription = null,
                                tint = BrandColors.Muted, modifier = Modifier.size(19.dp)
                            )
                        }

                        Text(
                            "Genel Müşteri Satışları",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandColors.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrandColors.Muted)
                }
            }

            // -------------------------------------------------
            // MÜŞTERİ LİSTESİ
            // -------------------------------------------------

            when {

                musteriler.isEmpty() && !loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PeopleOutline, contentDescription = null,
                                tint = BrandColors.Muted, modifier = Modifier.size(42.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Henüz müşteri eklenmedi", color = BrandColors.Muted, fontSize = 14.sp)
                        }
                    }
                }

                filtrelenmisMusteriler.isEmpty() && aramaMetni.isNotBlank() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.SearchOff, contentDescription = null,
                                tint = BrandColors.Muted, modifier = Modifier.size(42.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Müşteri bulunamadı", fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold, color = BrandColors.Ink
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "\"$aramaMetni\" ile eşleşen müşteri yok",
                                fontSize = 12.sp,
                                color = BrandColors.Muted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(
                            start = 12.dp, end = 12.dp, top = 2.dp, bottom = 88.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            items = filtrelenmisMusteriler,
                            key = { musteri -> musteri.id ?: musteri.ad }
                        ) { musteri ->
                            MusteriKart(
                                musteri = musteri,
                                onTikla = { detayGosterilenMusteri = musteri },
                                onOdeme = { tahsilatMusteri = musteri },
                                onSil = { silinecekMusteri = musteri }
                            )
                        }
                    }
                }
            }
        }
    }

    // =========================================================
    // YENİ MÜŞTERİ EKLE
    // =========================================================

    if (dialogAcikMi) {

        var isSaving by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        MusteriEkleDialog(
            isSaving = isSaving,
            errorMessage = errorMessage,
            onDismiss = { if (!isSaving) dialogAcikMi = false },
            onKaydet = { ad, telefon, adres, bakiye ->
                scope.launch {
                    isSaving = true
                    errorMessage = null
                    try {
                        val result = apiService.createMusteri(
                            Musteri(ad = ad, telefon = telefon, adres = adres, bakiye = bakiye)
                        )
                        if (result.isSuccess) {
                            yenilemeTetikleyici++
                            dialogAcikMi = false
                        } else {
                            println("Müşteri Kayıt Hatası (Sunucu): ${result.exceptionOrNull()?.message}")
                            errorMessage = "İşlem başarısız. Tekrar deneyin."
                        }
                    } catch (e: Exception) {
                        println("Müşteri Kayıt Hatası (Kritik): ${e.message}")
                        errorMessage = "İşlem başarısız. Tekrar deneyin."
                        e.printStackTrace()
                    } finally {
                        isSaving = false
                    }
                }
            }
        )
    }

    // =========================================================
    // MÜŞTERİ DETAY
    // =========================================================

    detayGosterilenMusteri?.let { musteri ->
        MusteriDetayDialog(
            apiService = apiService,
            musteri = musteri,
            onDismiss = { detayGosterilenMusteri = null },
            onDuzenle = {
                duzenlenenMusteri = musteri
                detayGosterilenMusteri = null
            },
            onBakiyeDuzenle = {
                bakiyeDuzenlenenMusteri = musteri
                detayGosterilenMusteri = null
            },
            onTahsilatGir = {
                tahsilatMusteri = musteri
                detayGosterilenMusteri = null
            },
            onRaporGoster = {
                raporMusteri = musteri
                detayGosterilenMusteri = null
            }
        )
    }

    // =========================================================
    // MÜŞTERİ DÜZENLE
    // =========================================================

    duzenlenenMusteri?.let { musteri ->
        MusteriDuzenleDialog(
            musteri = musteri,
            onDismiss = { duzenlenenMusteri = null },
            onKaydet = { ad, telefon, adres ->
                scope.launch {
                    try {
                        apiService.updateMusteri(
                            musteri.id ?: 0L,
                            musteri.copy(ad = ad, telefon = telefon, adres = adres)
                        )
                        yenilemeTetikleyici++
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                duzenlenenMusteri = null
            }
        )
    }

    // =========================================================
    // BAKİYE DÜZENLE
    // =========================================================

    bakiyeDuzenlenenMusteri?.let { musteri ->
        BakiyeDuzenleDialog(
            musteri = musteri,
            onDismiss = { bakiyeDuzenlenenMusteri = null },
            onKaydet = { yeniBakiye ->
                scope.launch {
                    try {
                        apiService.updateMusteri(musteri.id ?: 0L, musteri.copy(bakiye = yeniBakiye))
                        yenilemeTetikleyici++
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                bakiyeDuzenlenenMusteri = null
            }
        )
    }

    // =========================================================
    // TAHSİLAT
    // =========================================================

    tahsilatMusteri?.let { musteri ->
        CariOdemeDialog(
            apiService = apiService,
            id = musteri.id ?: 0L,
            ad = musteri.ad,
            bakiye = musteri.bakiye,
            tahsilatMi = true,
            onDismiss = { tahsilatMusteri = null },
            onSaved = { yenilemeTetikleyici++; tahsilatMusteri = null }
        )
    }
    // =========================================================
    // RAPOR
    // =========================================================

    raporMusteri?.let { musteri ->
        TarihAralikliSatisRaporDialog(
            apiService = apiService,
            musteri = musteri,
            onDismiss = { raporMusteri = null }
        )
    }

    // =========================================================
    // GENEL MÜŞTERİ
    // =========================================================

    if (genelMusteriDialogAcikMi) {
        GenelMusteriSatisDialog(
            apiService = apiService,
            onDismiss = { genelMusteriDialogAcikMi = false }
        )
    }

    // =========================================================
    // MÜŞTERİ SİL
    // =========================================================

    silinecekMusteri?.let { musteri ->
        AlertDialog(
            onDismissRequest = { silinecekMusteri = null },
            title = { Text("Müşteriyi Sil", fontWeight = FontWeight.Bold) },
            text = {
                Text("\"${musteri.ad}\" müşterisini silmek istediğinize emin misiniz? Bu işlem geri alınamaz.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                apiService.deleteMusteri(musteri.id ?: 0L)
                                yenilemeTetikleyici++
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        silinecekMusteri = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandColors.Danger)
                ) {
                    Text("Sil", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { silinecekMusteri = null }) {
                    Text("Vazgeç", color = BrandColors.Navy)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

// =============================================================
// MÜŞTERİ KARTI
// =============================================================

@Composable
fun MusteriKart(
    musteri: Musteri,
    onTikla: () -> Unit,
    onOdeme: () -> Unit,
    onSil: () -> Unit
) {
    val (balance, accent) = bakiyeMetniVeRengi(musteri.bakiye)
    BrandRecordCard(
        title = musteri.ad, detail = musteri.telefon,
        value = balance, caption = "Cari bakiye",
        icon = Icons.Default.Person, accent = accent, onClick = onTikla
    ) {
        IconButton(onClick = onOdeme, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.Payments, contentDescription = "Tahsilat yap", tint = BrandColors.Success)
        }
        IconButton(onClick = onSil, modifier = Modifier.size(34.dp)) {
            Icon(
                Icons.Default.DeleteOutline, contentDescription = "Müşteri sil",
                tint = BrandColors.Danger, modifier = Modifier.size(19.dp)
            )
        }
    }
}

// =============================================================
// MÜŞTERİ DETAY
// =============================================================

@Composable
fun MusteriDetayDialog(
    apiService: ApiService,
    musteri: Musteri,
    onDismiss: () -> Unit,
    onDuzenle: () -> Unit,
    onBakiyeDuzenle: () -> Unit,
    onTahsilatGir: () -> Unit,
    onRaporGoster: () -> Unit
) {

    val urlAcici = rememberUrlAcici()
    val (bakiyeFormatli, renk) = bakiyeMetniVeRengi(musteri.bakiye)

    var gecmisSatislar by remember { mutableStateOf<List<Satis>>(emptyList()) }
    var satisKalemleri by remember { mutableStateOf<Map<Long, List<SatisKalemi>>>(emptyMap()) }

    LaunchedEffect(musteri.id) {
        try {
            val tumSatislar = apiService.getSatislar().filter { it.musteriId == musteri.id }
            gecmisSatislar = tumSatislar.take(6)

            val kalemMap = mutableMapOf<Long, List<SatisKalemi>>()
            gecmisSatislar.forEach { satis ->
                kalemMap[satis.id ?: 0L] = apiService.getSatisKalemler(satis.id ?: 0L)
            }
            satisKalemleri = kalemMap
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,

        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    musteri.ad, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onDuzenle, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = BrandColors.Navy)
                }
            }
        },

        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())
            ) {
                Text(musteri.adres, fontSize = 13.sp, color = BrandColors.Muted)

                Spacer(Modifier.height(6.dp))

                // Tam tutar burada gösterilir (kısaltma yok).
                Text(
                    "Mevcut Durum: ₺${formatMusteriCariIkiBasamak(musteri.bakiye)}",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = renk
                )
                Text(
                    bakiyeFormatli.substringAfter(" "),
                    fontSize = 11.sp, color = BrandColors.Muted
                )

                Spacer(Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    IletisimButonu(
                        "Tahsilat Gir (Ödeme Al)", Icons.Default.Payments,
                        BrandColors.Success, Modifier.fillMaxWidth()
                    ) { onTahsilatGir() }

                    IletisimButonu(
                        "Tarih Aralıklı Satış Detayı", Icons.Default.DateRange,
                        Color(0xFFD435CD), Modifier.fillMaxWidth()
                    ) { onRaporGoster() }

                    IletisimButonu(
                        "Doğrudan Bakiye/Borç Düzenle", Icons.Default.Edit,
                        BrandColors.Teal, Modifier.fillMaxWidth()
                    ) { onBakiyeDuzenle() }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IletisimButonu("Ara", Icons.Default.Call, BrandColors.Navy, Modifier.weight(1f)) {
                            urlAcici.ac(telefonLinkOlustur(musteri.telefon))
                        }
                        IletisimButonu("WhatsApp", Icons.Default.Chat, BrandColors.Success, Modifier.weight(1f)) {
                            urlAcici.ac(whatsappLinkOlustur(musteri.telefon))
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    "GEÇMİŞ SATIŞLAR (SON 6)", fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold, color = BrandColors.Muted
                )

                Spacer(Modifier.height(6.dp))

                if (gecmisSatislar.isEmpty()) {
                    Text("Henüz kayıtlı satış yapılmadı", fontSize = 13.sp, color = BrandColors.Muted)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        gecmisSatislar.forEach { satis ->
                            GecmisSatisKarti(
                                satis = satis,
                                kalemler = satisKalemleri[satis.id ?: 0L] ?: emptyList()
                            )
                        }
                    }
                }
            }
        },

        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat", color = BrandColors.Navy, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

// =============================================================
// TAHSİLAT
// =============================================================


// =============================================================
// ORTAK RAPOR GÖVDESİ (müşteri bazlı + genel müşteri)
// =============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SatisRaporDialog(
    apiService: ApiService,
    baslik: String,
    aciklama: String,
    satisFiltresi: (Satis) -> Boolean,
    onDismiss: () -> Unit
) {

    var baslangicSeciciAcik by remember { mutableStateOf(false) }
    var bitisSeciciAcik by remember { mutableStateOf(false) }

    val baslangicTarihState = rememberDatePickerState()
    val bitisTarihState = rememberDatePickerState()

    var tumSatislar by remember { mutableStateOf<List<Satis>>(emptyList()) }
    var loadingSatislar by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            tumSatislar = apiService.getSatislar().filter(satisFiltresi)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            loadingSatislar = false
        }
    }

    val filtrelenmisSatislar = remember(
        tumSatislar,
        baslangicTarihState.selectedDateMillis,
        bitisTarihState.selectedDateMillis
    ) {
        tumSatislar.filter { satis ->
            val satisZamani = parseTarihMillis(satis.tarih)
            val baslangicKosulu = baslangicTarihState.selectedDateMillis?.let { satisZamani >= it } ?: true
            val bitisKosulu = bitisTarihState.selectedDateMillis?.let { satisZamani <= (it + 86400000L) } ?: true
            baslangicKosulu && bitisKosulu
        }
    }

    val gruplanmisSatislar = remember(filtrelenmisSatislar) {
        filtrelenmisSatislar.groupBy { formatTarih(it.tarih).substringBefore(" ") }
    }

    val toplamRaporTutari = remember(filtrelenmisSatislar) {
        filtrelenmisSatislar.sumOf { it.toplamTutar ?: 0.0 }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(baslik, fontWeight = FontWeight.Bold) },

        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {

                Text(aciklama, fontSize = 13.sp, color = BrandColors.Muted)

                Spacer(Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { baslangicSeciciAcik = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandColors.Background, contentColor = BrandColors.Ink
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val basMetni = baslangicTarihState.selectedDateMillis
                            ?.let { formatTarih(it.toString()).substringBefore(" ") } ?: "Başlangıç Seç"
                        Text(basMetni, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    }

                    Button(
                        onClick = { bitisSeciciAcik = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandColors.Background, contentColor = BrandColors.Ink
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val bitMetni = bitisTarihState.selectedDateMillis
                            ?.let { formatTarih(it.toString()).substringBefore(" ") } ?: "Bitiş Seç"
                        Text(bitMetni, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = BrandColors.Background, thickness = 1.dp)
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Dönem Toplam Satış:", fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, color = BrandColors.Ink
                    )
                    Text(
                        "₺${formatMusteriCariIkiBasamak(toplamRaporTutari)}",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandColors.Success,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 180.dp), textAlign = TextAlign.End
                    )
                }

                Spacer(Modifier.height(10.dp))

                if (loadingSatislar) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if (gruplanmisSatislar.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Seçilen aralıkta satış kaydı bulunamadı.",
                            color = BrandColors.Muted, fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gruplanmisSatislar.forEach { (tarihBasligi, satislarListesi) ->

                            item {
                                Text(
                                    tarihBasligi, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                    color = BrandColors.Navy,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                                )
                            }

                            items(satislarListesi) { satis ->

                                var kalemler by remember { mutableStateOf<List<SatisKalemi>>(emptyList()) }

                                LaunchedEffect(satis.id) {
                                    try {
                                        kalemler = apiService.getSatisKalemler(satis.id ?: 0L)
                                    } catch (_: Exception) {
                                    }
                                }

                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                        .background(BrandColors.Background, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Satış", fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold, color = BrandColors.Muted
                                        )
                                        Text(
                                            formatSaat(satis.tarih), fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold, color = BrandColors.Navy
                                        )
                                    }

                                    Spacer(Modifier.height(4.dp))
                                    HorizontalDivider(color = BrandColors.Border, thickness = 0.5.dp)
                                    Spacer(Modifier.height(4.dp))

                                    kalemler.forEach { kalem ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${kalem.urunAdi} (${kalem.adet} ${kalem.birim} × ₺${formatMusteriCariIkiBasamak(kalem.birimFiyat)})",
                                                fontSize = 13.sp, color = BrandColors.Ink,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 2, overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "₺${formatMusteriCariIkiBasamak(kalem.toplam)}",
                                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                                color = BrandColors.Ink,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 140.dp),
                                                textAlign = TextAlign.End
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },

        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat", color = BrandColors.Navy, fontWeight = FontWeight.SemiBold)
            }
        }
    )

    if (baslangicSeciciAcik) {
        DatePickerDialog(
            onDismissRequest = { baslangicSeciciAcik = false },
            confirmButton = {
                TextButton(onClick = { baslangicSeciciAcik = false }) { Text("Seç") }
            }
        ) {
            DatePicker(state = baslangicTarihState)
        }
    }

    if (bitisSeciciAcik) {
        DatePickerDialog(
            onDismissRequest = { bitisSeciciAcik = false },
            confirmButton = {
                TextButton(onClick = { bitisSeciciAcik = false }) { Text("Seç") }
            }
        ) {
            DatePicker(state = bitisTarihState)
        }
    }
}

// =============================================================
// TARİH ARALIKLI MÜŞTERİ SATIŞ RAPORU
// =============================================================

@Composable
fun TarihAralikliSatisRaporDialog(
    apiService: ApiService,
    musteri: Musteri,
    onDismiss: () -> Unit
) {
    SatisRaporDialog(
        apiService = apiService,
        baslik = "Tarih Bazlı Satış Detayı",
        aciklama = "${musteri.ad} için tarih filtreli satış raporu.",
        satisFiltresi = { it.musteriId == musteri.id },
        onDismiss = onDismiss
    )
}

// =============================================================
// GENEL MÜŞTERİ SATIŞLARI
// =============================================================

@Composable
fun GenelMusteriSatisDialog(
    apiService: ApiService,
    onDismiss: () -> Unit
) {
    SatisRaporDialog(
        apiService = apiService,
        baslik = "Genel Müşteri Satışları",
        aciklama = "Müşteri seçilmeden yapılan satışların tarih filtreli dökümü.",
        satisFiltresi = { it.musteriId == null },
        onDismiss = onDismiss
    )
}

// =============================================================
// GEÇMİŞ SATIŞ KARTI
// =============================================================

@Composable
fun GecmisSatisKarti(
    satis: Satis,
    kalemler: List<SatisKalemi>
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(BrandColors.Background, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Satış", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandColors.Ink)

            Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = 170.dp)) {
                Text(
                    "₺${formatMusteriCariIkiBasamak(satis.toplamTutar ?: 0.0)}",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandColors.Success,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End
                )
                Text(formatTarih(satis.tarih), fontSize = 10.sp, color = BrandColors.Muted, maxLines = 1)
            }
        }

        if (kalemler.isNotEmpty()) {

            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = BrandColors.Border, thickness = 1.dp)
            Spacer(Modifier.height(6.dp))

            kalemler.forEach { kalem ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${kalem.urunAdi} — ${kalem.adet} ${kalem.birim} × ₺${formatMusteriCariIkiBasamak(kalem.birimFiyat)}",
                        fontSize = 12.sp, color = BrandColors.Muted,
                        modifier = Modifier.weight(1f),
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "₺${formatMusteriCariIkiBasamak(kalem.toplam)}",
                        fontSize = 12.sp, fontWeight = FontWeight.Medium, color = BrandColors.Ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 130.dp), textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

// =============================================================
// İLETİŞİM BUTONU
// =============================================================

@Composable
fun IletisimButonu(
    baslik: String,
    ikon: ImageVector,
    renk: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .background(renk.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(ikon, contentDescription = null, tint = renk, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            baslik, color = renk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

// =============================================================
// MÜŞTERİ EKLE
// =============================================================

@Composable
fun MusteriEkleDialog(
    isSaving: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onKaydet: (ad: String, telefon: String, adres: String, bakiye: Double) -> Unit
) {
    var ad by remember { mutableStateOf("") }
    var telefon by remember { mutableStateOf("") }
    var adres by remember { mutableStateOf("") }
    var bakiye by remember { mutableStateOf("0") }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val telefonFocusRequester = remember { FocusRequester() }
    val adresFocusRequester = remember { FocusRequester() }
    val bakiyeFocusRequester = remember { FocusRequester() }

    fun sonrakiAlanaGit(focusRequester: FocusRequester) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    fun kaydet() {
        if (ad.isBlank() || isSaving) return
        keyboardController?.hide()
        focusManager.clearFocus()
        onKaydet(ad, telefon, adres, if (bakiye.isBlank()) 0.0 else (ondalikSayi(bakiye) ?: return))
    }

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) {
                keyboardController?.hide()
                focusManager.clearFocus()
                onDismiss()
            }
        },
        containerColor = Color.White,
        title = { Text("Yeni Müşteri Kaydı", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Text(errorMessage, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(
                    value = ad,
                    onValueChange = { ad = it },
                    label = { Text("Ad Soyad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { sonrakiAlanaGit(telefonFocusRequester) }),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = telefon,
                    onValueChange = { telefon = it },
                    label = { Text("Telefon (05XX XXX XX XX)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { sonrakiAlanaGit(adresFocusRequester) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(telefonFocusRequester),
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = adres,
                    onValueChange = { adres = it },
                    label = { Text("Adres") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { sonrakiAlanaGit(bakiyeFocusRequester) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(adresFocusRequester),
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = bakiye,
                    onValueChange = { bakiye = ondalikGirdi(it) },
                    label = { Text("Mevcut Başlangıç Borcu (₺)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { kaydet() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(bakiyeFocusRequester),
                    enabled = !isSaving
                )
                if (isSaving) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = !isSaving, onClick = { kaydet() }) {
                Text(
                    if (isSaving) "Kaydediliyor..." else "Kaydet",
                    color = BrandColors.Navy, fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) { Text("İptal", color = BrandColors.Muted) }
        }
    )
}

// =============================================================
// MÜŞTERİ DÜZENLE
// =============================================================

@Composable
fun MusteriDuzenleDialog(
    musteri: Musteri,
    onDismiss: () -> Unit,
    onKaydet: (ad: String, telefon: String, adres: String) -> Unit
) {
    var ad by remember { mutableStateOf(musteri.ad) }
    var telefon by remember { mutableStateOf(musteri.telefon) }
    var adres by remember { mutableStateOf(musteri.adres) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val telefonFocusRequester = remember { FocusRequester() }
    val adresFocusRequester = remember { FocusRequester() }

    fun sonrakiAlanaGit(focusRequester: FocusRequester) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    fun kaydet() {
        if (ad.isBlank()) return
        keyboardController?.hide()
        focusManager.clearFocus()
        onKaydet(ad, telefon, adres)
    }

    AlertDialog(
        onDismissRequest = {
            keyboardController?.hide()
            focusManager.clearFocus()
            onDismiss()
        },
        containerColor = Color.White,
        title = { Text("Müşteri Bilgilerini Düzenle", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = ad,
                    onValueChange = { ad = it },
                    label = { Text("Ad Soyad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { sonrakiAlanaGit(telefonFocusRequester) }),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = telefon,
                    onValueChange = { telefon = it },
                    label = { Text("Telefon") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { sonrakiAlanaGit(adresFocusRequester) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(telefonFocusRequester)
                )
                OutlinedTextField(
                    value = adres,
                    onValueChange = { adres = it },
                    label = { Text("Adres") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { kaydet() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(adresFocusRequester)
                )
                Text(
                    "Not: Borç/Bakiye bilgisi bu ekrandan değiştirilemez, satış veya tahsilat menülerinden otomatik güncellenmesi önerilir.",
                    fontSize = 11.sp,
                    color = BrandColors.Muted
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { kaydet() }) {
                Text("Kaydet", color = BrandColors.Navy, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) { Text("İptal", color = BrandColors.Muted) }
        }
    )
}

// =============================================================
// BAKİYE DÜZENLE
// =============================================================

@Composable
fun BakiyeDuzenleDialog(
    musteri: Musteri,
    onDismiss: () -> Unit,
    onKaydet: (yeniBakiye: Double) -> Unit
) {
    var yeniBakiyeText by remember { mutableStateOf(musteri.bakiye.toString()) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun kaydet() {
        val yeniBakiye = ondalikSayi(yeniBakiyeText)
        if (yeniBakiye != null) {
            keyboardController?.hide()
            focusManager.clearFocus()
            onKaydet(yeniBakiye)
        }
    }

    AlertDialog(
        onDismissRequest = {
            keyboardController?.hide()
            focusManager.clearFocus()
            onDismiss()
        },
        containerColor = Color.White,
        title = { Text("Net Borç Ayarla", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Mevcut durum: ₺${formatMusteriCariIkiBasamak(musteri.bakiye)}",
                    fontSize = 13.sp, color = BrandColors.Muted
                )
                OutlinedTextField(
                    value = yeniBakiyeText,
                    onValueChange = { yeniBakiyeText = ondalikGirdi(it) },
                    label = { Text("Yeni Net Borç Tutarı (₺)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { kaydet() }),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Dikkat: Gireceğiniz tutar müşterinin güncel net borcu sayılacaktır. Müşteri size borçluysa düz rakam (Örn: 500), eğer siz müşteriye borçluysanız eksi değer (Örn: -200) giriniz.",
                    fontSize = 11.sp,
                    color = BrandColors.Muted
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { kaydet() }) {
                Text("Güncelle", color = BrandColors.Teal, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) { Text("İptal", color = BrandColors.Muted) }
        }
    )
}

// =============================================================
// SAAT FORMATLA
// =============================================================

private fun formatSaat(tarih: String?): String {

    if (tarih.isNullOrBlank()) return ""

    if (tarih.contains("T")) {
        return try {
            val timePart = tarih.substringAfter("T")
            val parts = timePart.split(":")
            if (parts.size >= 2) "${parts[0]}:${parts[1]}" else timePart.take(5)
        } catch (_: Exception) {
            ""
        }
    }

    val millis = tarih.toLongOrNull() ?: return ""

    val gunIciSaniye = (millis / 1000) % 86400
    val toplamSaatSaniye = gunIciSaniye + (3 * 3600)
    val duzeltilmisSaniye = if (toplamSaatSaniye >= 86400) toplamSaatSaniye - 86400 else toplamSaatSaniye

    val saat = (duzeltilmisSaniye / 3600).toString().padStart(2, '0')
    val dakika = ((duzeltilmisSaniye % 3600) / 60).toString().padStart(2, '0')

    return "$saat:$dakika"
}

// =============================================================
// PARA FORMATLA
// =============================================================

/**
 * Tam tutar: binlik ayraçlı, iki ondalıklı. 70000000.0 -> "70.000.000,00"
 * Long üzerinden hesaplanır, Double yuvarlama hatası oluşmaz.
 */
private fun formatMusteriCariIkiBasamak(deger: Double): String {

    val negatifMi = deger < 0
    val mutlakDeger = if (negatifMi) -deger else deger

    val kurus = ((mutlakDeger * 100.0) + 0.5).toLong()
    val tamKisim = kurus / 100
    val kesirKisim = kurus % 100

    val tamStr = tamKisim.toString()
    val sb = StringBuilder()
    for (i in tamStr.indices) {
        if (i > 0 && (tamStr.length - i) % 3 == 0) sb.append('.')
        sb.append(tamStr[i])
    }

    val isaret = if (negatifMi) "-" else ""
    return "$isaret$sb,${kesirKisim.toString().padStart(2, '0')}"
}

/**
 * Dar alanlar (kart, metrik) için kısaltılmış tutar.
 * 70000000.0 -> "70,0 Mn"   1250.5 -> "1.250,50"   450000.0 -> "450,0 B"
 */
private fun formatKisaPara(deger: Double): String {

    val negatifMi = deger < 0
    val mutlak = if (negatifMi) -deger else deger
    val isaret = if (negatifMi) "-" else ""

    return when {
        mutlak >= 1_000_000_000 -> "$isaret${birOndalik(mutlak / 1_000_000_000)} Mr"
        mutlak >= 1_000_000 -> "$isaret${birOndalik(mutlak / 1_000_000)} Mn"
        mutlak >= 100_000 -> "$isaret${birOndalik(mutlak / 1_000)} B"
        else -> formatMusteriCariIkiBasamak(deger)
    }
}

private fun birOndalik(deger: Double): String {
    val x = ((deger * 10.0) + 0.5).toLong()
    return "${x / 10},${x % 10}"
}
