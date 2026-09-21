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

private fun tedarikciBakiyeMetniVeRengi(bakiye: Double): Pair<String, Color> = when {
    bakiye > 0 -> "₺${formatKisaPara(bakiye)} (Borcumuz)" to BrandColors.Danger
    bakiye < 0 -> "₺${formatKisaPara(-bakiye)} (Alacaklıyız)" to BrandColors.Navy
    else -> "₺0,00 (Dengede)" to BrandColors.Success
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TedarikcilerScreen(
    apiService: ApiService,
    onNavigateBack: () -> Unit
) {
    var yenilemeTetikleyici by remember { mutableStateOf(0) }
    var tedarikciler by remember { mutableStateOf<List<Tedarikci>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    // ---------------------------------------------------------
    // ARAMA
    // ---------------------------------------------------------

    var aramaAcik by remember { mutableStateOf(false) }
    var aramaMetni by remember { mutableStateOf("") }

    val filtrelenmisTedarikciler = remember(tedarikciler, aramaMetni) {
        val aranacak = aramaMetni.trim()
        if (aranacak.isBlank()) tedarikciler
        else tedarikciler.filter { it.ad.contains(aranacak, ignoreCase = true) }
    }

    // ---------------------------------------------------------
    // VERİLERİ YÜKLE
    // ---------------------------------------------------------

    LaunchedEffect(yenilemeTetikleyici) {
        loading = true
        try {
            tedarikciler = apiService.getTedarikciler()
        } catch (e: Exception) {
            println("Tedarikçi Yükleme Hatası: ${e.message}")
            e.printStackTrace()
        } finally {
            loading = false
        }
    }

    // ---------------------------------------------------------
    // DIALOGLAR
    // ---------------------------------------------------------

    var dialogAcikMi by remember { mutableStateOf(false) }
    var detayGosterilenTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var duzenlenenTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var bakiyeDuzenlenenTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var odemeTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var raporTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var silinecekTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var genelTedarikciDialogAcikMi by remember { mutableStateOf(false) }

    val toplamBorc = remember(tedarikciler) { tedarikciler.sumOf { it.bakiye } }

    // ---------------------------------------------------------
    // SAYFA
    // ---------------------------------------------------------

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = BrandColors.Background,
        modifier = Modifier.swipeToBack(onBack = onNavigateBack),

        topBar = {
            BrandTopBar("Tedarikçiler", "İş ortakları ve ödemeler", onNavigateBack) {
                IconButton(
                    onClick = {
                        aramaAcik = !aramaAcik
                        if (!aramaAcik) aramaMetni = ""
                    }
                ) {
                    Icon(
                        imageVector = if (aramaAcik) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (aramaAcik) "Aramayı Kapat" else "Tedarikçi Ara",
                        tint = Color.White
                    )
                }
            }
        },

        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { dialogAcikMi = true },
                containerColor = BrandColors.Navy,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Yeni tedarikçi", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->

        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            if (loading && tedarikciler.isEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // -------------------------------------------------
            // ARAMA ALANI
            // -------------------------------------------------

            if (aramaAcik) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color.White)
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp)
                ) {
                    OutlinedTextField(
                        value = aramaMetni,
                        onValueChange = { aramaMetni = it },
                        placeholder = {
                            Text("Tedarikçi adına göre ara...", color = BrandColors.Muted, fontSize = 14.sp)
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
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BrandColors.Background,
                            unfocusedContainerColor = BrandColors.Background,
                            focusedBorderColor = BrandColors.Teal,
                            unfocusedBorderColor = BrandColors.Border,
                            focusedTextColor = BrandColors.Ink,
                            unfocusedTextColor = BrandColors.Ink,
                            cursorColor = BrandColors.Teal
                        ),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    )

                    if (aramaMetni.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "${filtrelenmisTedarikciler.size} tedarikçi bulundu",
                            fontSize = 12.sp,
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
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BrandMetric(
                    "Toplam Tedarikçi", tedarikciler.size.toString(),
                    BrandColors.Navy, Modifier.weight(1f)
                )
                BrandMetric(
                    "Toplam borcumuz", "₺${formatKisaPara(toplamBorc)}",
                    if (toplamBorc > 0) BrandColors.Warning else BrandColors.Success,
                    Modifier.weight(1f)
                )
            }

            // -------------------------------------------------
            // GENEL TEDARİKÇİ
            // -------------------------------------------------

            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BrandColors.Border),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
                    .clickable { genelTedarikciDialogAcikMi = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(
                                BrandColors.Muted.copy(alpha = 0.12f), RoundedCornerShape(10.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = BrandColors.Muted)
                        }

                        Text(
                            "Genel Tedarikçi Alışları",
                            fontSize = 15.sp,
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
            // TEDARİKÇİ LİSTESİ
            // -------------------------------------------------

            when {
                tedarikciler.isEmpty() && !loading -> {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.LocalShipping, contentDescription = null,
                                tint = BrandColors.Muted, modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Henüz tedarikçi eklenmedi", color = BrandColors.Muted, fontSize = 15.sp)
                        }
                    }
                }

                filtrelenmisTedarikciler.isEmpty() && aramaMetni.isNotBlank() -> {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.SearchOff, contentDescription = null,
                                tint = BrandColors.Muted, modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Tedarikçi bulunamadı", fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold, color = BrandColors.Ink
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "\"$aramaMetni\" ile eşleşen tedarikçi yok",
                                fontSize = 13.sp,
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
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = filtrelenmisTedarikciler,
                            key = { tedarikci -> tedarikci.id ?: tedarikci.ad }
                        ) { tedarikci ->
                            TedarikciKart(
                                tedarikci = tedarikci,
                                onTikla = { detayGosterilenTedarikci = tedarikci },
                                onOdeme = { odemeTedarikci = tedarikci },
                                onSil = { silinecekTedarikci = tedarikci }
                            )
                        }
                    }
                }
            }
        }
    }

    // =========================================================
    // YENİ TEDARİKÇİ EKLE
    // =========================================================

    if (dialogAcikMi) {
        var isSaving by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        TedarikciEkleDialog(
            isSaving = isSaving,
            errorMessage = errorMessage,
            onDismiss = { if (!isSaving) dialogAcikMi = false },
            onKaydet = { ad, telefon, adres, bakiye ->
                scope.launch {
                    isSaving = true
                    errorMessage = null
                    try {
                        val result = apiService.createTedarikci(
                            Tedarikci(ad = ad, telefon = telefon, adres = adres, bakiye = bakiye)
                        )
                        if (result.isSuccess) {
                            yenilemeTetikleyici++
                            dialogAcikMi = false
                        } else {
                            println("Tedarikçi Kayıt Hatası (Sunucu): ${result.exceptionOrNull()?.message}")
                            errorMessage = "İşlem başarısız. Tekrar deneyin."
                        }
                    } catch (e: Exception) {
                        println("Tedarikçi Kayıt Hatası (Kritik): ${e.message}")
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
    // TEDARİKÇİ DETAY
    // =========================================================

    detayGosterilenTedarikci?.let { tedarikci ->
        TedarikciDetayDialog(
            apiService = apiService,
            tedarikci = tedarikci,
            onDismiss = { detayGosterilenTedarikci = null },
            onDuzenle = {
                duzenlenenTedarikci = tedarikci
                detayGosterilenTedarikci = null
            },
            onBakiyeDuzenle = {
                bakiyeDuzenlenenTedarikci = tedarikci
                detayGosterilenTedarikci = null
            },
            onOdemeYapGir = {
                odemeTedarikci = tedarikci
                detayGosterilenTedarikci = null
            },
            onRaporGoster = {
                raporTedarikci = tedarikci
                detayGosterilenTedarikci = null
            }
        )
    }

    // =========================================================
    // TEDARİKÇİ DÜZENLE
    // =========================================================

    duzenlenenTedarikci?.let { tedarikci ->
        TedarikciDuzenleDialog(
            tedarikci = tedarikci,
            onDismiss = { duzenlenenTedarikci = null },
            onKaydet = { ad, telefon, adres ->
                scope.launch {
                    try {
                        apiService.updateTedarikci(
                            tedarikci.id ?: 0L,
                            tedarikci.copy(ad = ad, telefon = telefon, adres = adres)
                        )
                        yenilemeTetikleyici++
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                duzenlenenTedarikci = null
            }
        )
    }

    // =========================================================
    // BAKİYE DÜZENLE
    // =========================================================

    bakiyeDuzenlenenTedarikci?.let { tedarikci ->
        TedarikciBakiyeDuzenleDialog(
            tedarikci = tedarikci,
            onDismiss = { bakiyeDuzenlenenTedarikci = null },
            onKaydet = { yeniBakiye ->
                scope.launch {
                    try {
                        apiService.updateTedarikci(tedarikci.id ?: 0L, tedarikci.copy(bakiye = yeniBakiye))
                        yenilemeTetikleyici++
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                bakiyeDuzenlenenTedarikci = null
            }
        )
    }

    // =========================================================
    // ÖDEME YAP
    // =========================================================

    odemeTedarikci?.let { tedarikci ->
        CariOdemeDialog(
            apiService = apiService,
            id = tedarikci.id ?: 0L,
            ad = tedarikci.ad,
            bakiye = tedarikci.bakiye,
            tahsilatMi = false,
            onDismiss = { odemeTedarikci = null },
            onSaved = { yenilemeTetikleyici++; odemeTedarikci = null }
        )
    }
    // =========================================================
    // RAPOR
    // =========================================================

    raporTedarikci?.let { tedarikci ->
        TarihAralikliAlisRaporDialog(
            apiService = apiService,
            tedarikci = tedarikci,
            onDismiss = { raporTedarikci = null }
        )
    }

    // =========================================================
    // GENEL TEDARİKÇİ
    // =========================================================

    if (genelTedarikciDialogAcikMi) {
        GenelTedarikciAlisDialog(
            apiService = apiService,
            onDismiss = { genelTedarikciDialogAcikMi = false }
        )
    }

    // =========================================================
    // TEDARİKÇİ SİL
    // =========================================================

    silinecekTedarikci?.let { tedarikci ->
        AlertDialog(
            onDismissRequest = { silinecekTedarikci = null },
            title = { Text("Tedarikçiyi Sil", fontWeight = FontWeight.Bold) },
            text = {
                Text("\"${tedarikci.ad}\" tedarikçisini silmek istediğinize emin misiniz? Bu işlem geri alınamaz.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                apiService.deleteTedarikci(tedarikci.id ?: 0L)
                                yenilemeTetikleyici++
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        silinecekTedarikci = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandColors.Danger)
                ) {
                    Text("Sil", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { silinecekTedarikci = null }) {
                    Text("Vazgeç", color = BrandColors.Teal)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

// =============================================================
// TEDARİKÇİ KARTI
// =============================================================

@Composable
fun TedarikciKart(
    tedarikci: Tedarikci,
    onTikla: () -> Unit,
    onOdeme: () -> Unit,
    onSil: () -> Unit
) {
    val (balance, accent) = tedarikciBakiyeMetniVeRengi(tedarikci.bakiye)
    BrandRecordCard(
        title = tedarikci.ad, detail = tedarikci.telefon,
        value = balance, caption = "Cari bakiye",
        icon = Icons.Default.LocalShipping, accent = accent, onClick = onTikla
    ) {
        IconButton(onClick = onOdeme, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.Payments, contentDescription = "Ödeme yap", tint = BrandColors.Success)
        }
        IconButton(onClick = onSil, modifier = Modifier.size(34.dp)) {
            Icon(
                Icons.Default.DeleteOutline, contentDescription = "Tedarikçi sil",
                tint = BrandColors.Danger, modifier = Modifier.size(19.dp)
            )
        }
    }
}

// =============================================================
// TEDARİKÇİ DETAY
// =============================================================

@Composable
fun TedarikciDetayDialog(
    apiService: ApiService,
    tedarikci: Tedarikci,
    onDismiss: () -> Unit,
    onDuzenle: () -> Unit,
    onBakiyeDuzenle: () -> Unit,
    onOdemeYapGir: () -> Unit,
    onRaporGoster: () -> Unit
) {
    val urlAcici = rememberUrlAcici()
    val (bakiyeFormatli, renk) = tedarikciBakiyeMetniVeRengi(tedarikci.bakiye)

    var gecmisAlislar by remember { mutableStateOf<List<Alis>>(emptyList()) }
    var alisKalemleri by remember { mutableStateOf<Map<Long, List<AlisKalemi>>>(emptyMap()) }

    LaunchedEffect(tedarikci.id) {
        try {
            val tumAlislar = apiService.getAlislar().filter { it.tedarikciId == tedarikci.id }
            gecmisAlislar = tumAlislar.take(6)
            val kalemMap = mutableMapOf<Long, List<AlisKalemi>>()
            gecmisAlislar.forEach { a ->
                kalemMap[a.id ?: 0L] = apiService.getAlisKalemler(a.id ?: 0L)
            }
            alisKalemleri = kalemMap
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
                    tedarikci.ad, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onDuzenle, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = BrandColors.Teal)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())
            ) {
                Text(tedarikci.adres, fontSize = 13.sp, color = BrandColors.Muted)

                Spacer(modifier = Modifier.height(6.dp))

                // Tam tutar burada gösterilir (kısaltma yok).
                Text(
                    text = "Mevcut Durum: ₺${formatTedarikciCariIkiBasamak(tedarikci.bakiye)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = renk
                )
                Text(
                    bakiyeFormatli.substringAfter(" "),
                    fontSize = 11.sp,
                    color = BrandColors.Muted
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TedarikciIletisimButonu(
                        baslik = "Ödeme Yap (Tedarikçiye Öde)",
                        ikon = Icons.Default.Payments,
                        renk = BrandColors.Success,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOdemeYapGir() }
                    )
                    TedarikciIletisimButonu(
                        baslik = "Tarih Aralıklı Alış Detayı",
                        ikon = Icons.Default.DateRange,
                        renk = Color(0xFFD435CD),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onRaporGoster() }
                    )
                    TedarikciIletisimButonu(
                        baslik = "Doğrudan Borç Düzenle",
                        ikon = Icons.Default.Edit,
                        renk = BrandColors.Teal,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onBakiyeDuzenle() }
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TedarikciIletisimButonu(
                            baslik = "Ara",
                            ikon = Icons.Default.Call,
                            renk = BrandColors.Teal,
                            modifier = Modifier.weight(1f),
                            onClick = { urlAcici.ac(telefonLinkOlustur(tedarikci.telefon)) }
                        )
                        TedarikciIletisimButonu(
                            baslik = "WhatsApp",
                            ikon = Icons.Default.Chat,
                            renk = BrandColors.Success,
                            modifier = Modifier.weight(1f),
                            onClick = { urlAcici.ac(whatsappLinkOlustur(tedarikci.telefon)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    "GEÇMİŞ ALIŞLAR (SON 6)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandColors.Muted
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (gecmisAlislar.isEmpty()) {
                    Text("Henüz kayıtlı alış yapılmadı", fontSize = 13.sp, color = BrandColors.Muted)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        gecmisAlislar.forEach { alis ->
                            GecmisAlisKarti(
                                alis = alis,
                                kalemler = alisKalemleri[alis.id ?: 0L] ?: emptyList()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat", color = BrandColors.Teal, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

// =============================================================
// ÖDEME GİR
// =============================================================


// =============================================================
// ORTAK RAPOR GÖVDESİ (tedarikçi bazlı + genel tedarikçi)
// =============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlisRaporDialog(
    apiService: ApiService,
    baslik: String,
    aciklama: String,
    alisFiltresi: (Alis) -> Boolean,
    onDismiss: () -> Unit
) {
    var baslangicSeciciAcik by remember { mutableStateOf(false) }
    var bitisSeciciAcik by remember { mutableStateOf(false) }

    val baslangicTarihState = rememberDatePickerState()
    val bitisTarihState = rememberDatePickerState()

    var tumAlislar by remember { mutableStateOf<List<Alis>>(emptyList()) }
    var loadingAlislar by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            tumAlislar = apiService.getAlislar().filter(alisFiltresi)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            loadingAlislar = false
        }
    }

    val filtrelenmisAlislar = remember(
        tumAlislar,
        baslangicTarihState.selectedDateMillis,
        bitisTarihState.selectedDateMillis
    ) {
        tumAlislar.filter { alis ->
            val alisZamani = parseTarihMillis(alis.tarih)
            val baslangicKosulu = baslangicTarihState.selectedDateMillis?.let { alisZamani >= it } ?: true
            val bitisKosulu = bitisTarihState.selectedDateMillis?.let { alisZamani <= (it + 86400000L) } ?: true
            baslangicKosulu && bitisKosulu
        }
    }

    val gruplanmisAlislar = remember(filtrelenmisAlislar) {
        filtrelenmisAlislar.groupBy { alis -> formatTarih(alis.tarih).substringBefore(" ") }
    }

    val toplamRaporTutari = remember(filtrelenmisAlislar) {
        filtrelenmisAlislar.sumOf { it.toplamTutar }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(baslik, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                Text(aciklama, fontSize = 13.sp, color = BrandColors.Muted)

                Spacer(modifier = Modifier.height(10.dp))

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

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BrandColors.Background, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Dönem Toplam Alış:", fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, color = BrandColors.Ink
                    )
                    Text(
                        "₺${formatTedarikciCariIkiBasamak(toplamRaporTutari)}",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandColors.Warning,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 180.dp), textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (loadingAlislar) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if (gruplanmisAlislar.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Seçilen aralıkta alış kaydı bulunamadı.",
                            color = BrandColors.Muted, fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gruplanmisAlislar.forEach { (tarihBasligi, alislarListesi) ->
                            item {
                                Text(
                                    text = tarihBasligi,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandColors.Teal,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                                )
                            }
                            items(alislarListesi) { alis ->
                                var kalemler by remember { mutableStateOf<List<AlisKalemi>>(emptyList()) }
                                LaunchedEffect(alis.id) {
                                    try {
                                        kalemler = apiService.getAlisKalemler(alis.id ?: 0L)
                                    } catch (_: Exception) {}
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BrandColors.Background, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Alış",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = BrandColors.Muted
                                        )
                                        Text(
                                            text = formatSaat(alis.tarih),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandColors.Teal
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(color = BrandColors.Border, thickness = 0.5.dp)
                                    Spacer(modifier = Modifier.height(4.dp))

                                    kalemler.forEach { kalem ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${kalem.urunAdi} (${kalem.adet} × ₺${formatTedarikciCariIkiBasamak(kalem.birimFiyat)})",
                                                fontSize = 13.sp,
                                                color = BrandColors.Ink,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "₺${formatTedarikciCariIkiBasamak(kalem.toplam)}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandColors.Ink,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
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
                Text("Kapat", color = BrandColors.Teal, fontWeight = FontWeight.SemiBold)
            }
        }
    )

    if (baslangicSeciciAcik) {
        DatePickerDialog(
            onDismissRequest = { baslangicSeciciAcik = false },
            confirmButton = {
                TextButton(onClick = { baslangicSeciciAcik = false }) { Text("Seç") }
            }
        ) { DatePicker(state = baslangicTarihState) }
    }

    if (bitisSeciciAcik) {
        DatePickerDialog(
            onDismissRequest = { bitisSeciciAcik = false },
            confirmButton = {
                TextButton(onClick = { bitisSeciciAcik = false }) { Text("Seç") }
            }
        ) { DatePicker(state = bitisTarihState) }
    }
}

// =============================================================
// TARİH ARALIKLI TEDARİKÇİ ALIŞ RAPORU
// =============================================================

@Composable
fun TarihAralikliAlisRaporDialog(
    apiService: ApiService,
    tedarikci: Tedarikci,
    onDismiss: () -> Unit
) {
    AlisRaporDialog(
        apiService = apiService,
        baslik = "Tarih Bazlı Alış Detayı",
        aciklama = "${tedarikci.ad} firmasından yapılan tarih filtreli alış raporu.",
        alisFiltresi = { it.tedarikciId == tedarikci.id },
        onDismiss = onDismiss
    )
}

// =============================================================
// GENEL TEDARİKÇİ ALIŞLARI
// =============================================================

@Composable
fun GenelTedarikciAlisDialog(
    apiService: ApiService,
    onDismiss: () -> Unit
) {
    AlisRaporDialog(
        apiService = apiService,
        baslik = "Genel Tedarikçi Alışları",
        aciklama = "Tedarikçi seçilmeden yapılan alışların tarih filtreli dökümü.",
        alisFiltresi = { it.tedarikciId == null },
        onDismiss = onDismiss
    )
}

// =============================================================
// GEÇMİŞ ALIŞ KARTI
// =============================================================

@Composable
fun GecmisAlisKarti(alis: Alis, kalemler: List<AlisKalemi>) {
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
            Text("Alış", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandColors.Ink)

            Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = 170.dp)) {
                Text(
                    "₺${formatTedarikciCariIkiBasamak(alis.toplamTutar)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandColors.Warning,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
                Text(formatTarih(alis.tarih), fontSize = 10.sp, color = BrandColors.Muted, maxLines = 1)
            }
        }

        if (kalemler.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = BrandColors.Border, thickness = 1.dp)
            Spacer(modifier = Modifier.height(6.dp))

            kalemler.forEach { kalem ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${kalem.urunAdi} — ${kalem.adet} × ₺${formatTedarikciCariIkiBasamak(kalem.birimFiyat)}",
                        fontSize = 12.sp,
                        color = BrandColors.Muted,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "₺${formatTedarikciCariIkiBasamak(kalem.toplam)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = BrandColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 130.dp),
                        textAlign = TextAlign.End
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
fun TedarikciIletisimButonu(
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
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            baslik, color = renk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

// =============================================================
// TEDARİKÇİ EKLE
// =============================================================

@Composable
fun TedarikciEkleDialog(
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
        title = { Text("Yeni Tedarikçi Kaydı", fontWeight = FontWeight.Bold) },
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
                    label = { Text("Firma / Ad Soyad") },
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
                    label = { Text("Mevcut Başlangıç Borcumuz (₺)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { kaydet() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(bakiyeFocusRequester),
                    enabled = !isSaving
                )
                if (isSaving) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !isSaving, onClick = { kaydet() }) {
                Text(
                    if (isSaving) "Kaydediliyor..." else "Kaydet",
                    color = BrandColors.Teal,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                },
                enabled = !isSaving
            ) { Text("İptal", color = BrandColors.Muted) }
        }
    )
}

// =============================================================
// TEDARİKÇİ DÜZENLE
// =============================================================

@Composable
fun TedarikciDuzenleDialog(
    tedarikci: Tedarikci,
    onDismiss: () -> Unit,
    onKaydet: (ad: String, telefon: String, adres: String) -> Unit
) {
    var ad by remember { mutableStateOf(tedarikci.ad) }
    var telefon by remember { mutableStateOf(tedarikci.telefon) }
    var adres by remember { mutableStateOf(tedarikci.adres) }

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
        title = { Text("Tedarikçi Bilgilerini Düzenle", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = ad,
                    onValueChange = { ad = it },
                    label = { Text("Firma / Ad Soyad") },
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
                    "Not: Borç bilgisi bu ekrandan değiştirilemez, mal alışı veya ödeme menülerinden otomatik güncellenmesi önerilir.",
                    fontSize = 11.sp,
                    color = BrandColors.Muted
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { kaydet() }) {
                Text("Kaydet", color = BrandColors.Teal, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                onDismiss()
            }) { Text("İptal", color = BrandColors.Muted) }
        }
    )
}

// =============================================================
// BAKİYE DÜZENLE
// =============================================================

@Composable
fun TedarikciBakiyeDuzenleDialog(
    tedarikci: Tedarikci,
    onDismiss: () -> Unit,
    onKaydet: (yeniBakiye: Double) -> Unit
) {
    var yeniBakiyeText by remember { mutableStateOf(tedarikci.bakiye.toString()) }

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
                    "Mevcut durum: ₺${formatTedarikciCariIkiBasamak(tedarikci.bakiye)}",
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
                    "Dikkat: Gireceğiniz tutar firmaya olan güncel toplam net borcumuz sayılacaktır. Firmaya borçluysak düz rakam (Örn: 1500), eğer firmadan alacaklıysak eksi değer (Örn: -300) giriniz.",
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
            TextButton(onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                onDismiss()
            }) { Text("İptal", color = BrandColors.Muted) }
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
private fun formatTedarikciCariIkiBasamak(deger: Double): String {
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
        else -> formatTedarikciCariIkiBasamak(deger)
    }
}

private fun birOndalik(deger: Double): String {
    val x = ((deger * 10.0) + 0.5).toLong()
    return "${x / 10},${x % 10}"
}
