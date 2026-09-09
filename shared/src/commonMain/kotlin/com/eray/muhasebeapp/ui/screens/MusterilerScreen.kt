package com.eray.muhasebeapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

private fun bakiyeMetniVeRengi(bakiye: Double): Pair<String, Color> {
    val formatliBakiye = formatMusteriCariIkiBasamak(bakiye)
    return when {
        bakiye > 0 -> "₺$formatliBakiye (Borçlu)" to Color(0xFFFF3B30)
        bakiye < 0 -> "₺${formatMusteriCariIkiBasamak(-bakiye)} (Alacaklı)" to Color(0xFF007AFF)
        else -> "₺0.00 (Dengede)" to Color(0xFF34C759)
    }
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

    LaunchedEffect(yenilemeTetikleyici) {
        loading = true
        try {
            musteriler = apiService.getMusteriler()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            loading = false
        }
    }

    var dialogAcikMi by remember { mutableStateOf(false) }
    var detayGosterilenMusteri by remember { mutableStateOf<Musteri?>(null) }
    var duzenlenenMusteri by remember { mutableStateOf<Musteri?>(null) }
    var bakiyeDuzenlenenMusteri by remember { mutableStateOf<Musteri?>(null) }
    var tahsilatMusteri by remember { mutableStateOf<Musteri?>(null) }
    var raporMusteri by remember { mutableStateOf<Musteri?>(null) }
    var silinecekMusteri by remember { mutableStateOf<Musteri?>(null) }
    var genelMusteriDialogAcikMi by remember { mutableStateOf(false) }

    var horizontalDragAccumulator by remember { mutableStateOf(0f) }

    Scaffold(
        containerColor = Color(0xFFF2F2F7),
        modifier = Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = { horizontalDragAccumulator = 0f },
                onDragEnd = {
                    if (horizontalDragAccumulator > 150f) {
                        onNavigateBack()
                    }
                },
                onDragCancel = { horizontalDragAccumulator = 0f },
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    horizontalDragAccumulator += dragAmount
                }
            )
        },
        topBar = {
            TopAppBar(
                title = { Text("Müşteriler", fontWeight = FontWeight.Bold, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { dialogAcikMi = true },
                containerColor = Color(0xFF007AFF)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ekle", tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (loading && musteriler.isEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Toplam Müşteri", fontSize = 13.sp, color = Color(0xFF8E8E93))
                        Text("${musteriler.size}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Toplam Müşteri Borcu", fontSize = 13.sp, color = Color(0xFF8E8E93))
                        val toplamBakiye = musteriler.sumOf { it.bakiye }
                        Text(
                            "₺${formatMusteriCariIkiBasamak(toplamBakiye)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (toplamBakiye > 0) Color(0xFFFF9500) else Color(0xFF34C759)
                        )
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
                    .clickable { genelMusteriDialogAcikMi = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF8E8E93).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF8E8E93))
                        }
                        Text("Genel Müşteri Satışları", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF8E8E93))
                }
            }

            if (musteriler.isEmpty() && !loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Henüz müşteri eklenmedi", color = Color(0xFF8E8E93), fontSize = 15.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(musteriler) { musteri ->
                        MusteriKart(
                            musteri = musteri,
                            onTikla = { detayGosterilenMusteri = musteri },
                            onSil = { silinecekMusteri = musteri }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

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
                        val result = apiService.createMusteri(Musteri(ad = ad, telefon = telefon, adres = adres, bakiye = bakiye))
                        if (result.isSuccess) {
                            yenilemeTetikleyici++
                            dialogAcikMi = false
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Bilinmeyen sunucu hatası"
                        }
                    } catch (e: Exception) {
                        errorMessage = "Bağlantı hatası: ${e.message}"
                        e.printStackTrace()
                    } finally {
                        isSaving = false
                    }
                }
            }
        )
    }

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

    duzenlenenMusteri?.let { musteri ->
        MusteriDuzenleDialog(
            musteri = musteri,
            onDismiss = { duzenlenenMusteri = null },
            onKaydet = { ad, telefon, adres ->
                scope.launch {
                    try {
                        apiService.updateMusteri(musteri.id ?: 0L, musteri.copy(ad = ad, telefon = telefon, adres = adres))
                        yenilemeTetikleyici++
                    } catch (e: Exception) { e.printStackTrace() }
                }
                duzenlenenMusteri = null
            }
        )
    }

    bakiyeDuzenlenenMusteri?.let { musteri ->
        BakiyeDuzenleDialog(
            musteri = musteri,
            onDismiss = { bakiyeDuzenlenenMusteri = null },
            onKaydet = { yeniBakiye ->
                scope.launch {
                    try {
                        apiService.updateMusteri(musteri.id ?: 0L, musteri.copy(bakiye = yeniBakiye))
                        yenilemeTetikleyici++
                    } catch (e: Exception) { e.printStackTrace() }
                }
                bakiyeDuzenlenenMusteri = null
            }
        )
    }

    tahsilatMusteri?.let { musteri ->
        TahsilatGirDialog(
            musteri = musteri,
            onDismiss = { tahsilatMusteri = null },
            onKaydet = { tahsilatTutari ->
                scope.launch {
                    try {
                        apiService.createTahsilat(TahsilatRequest(musteriId = musteri.id ?: 0L, tutar = tahsilatTutari))
                        yenilemeTetikleyici++
                    } catch (e: Exception) { e.printStackTrace() }
                }
                tahsilatMusteri = null
            }
        )
    }

    raporMusteri?.let { musteri ->
        TarihAralikliSatisRaporDialog(
            apiService = apiService,
            musteri = musteri,
            onDismiss = { raporMusteri = null }
        )
    }

    if (genelMusteriDialogAcikMi) {
        GenelMusteriSatisDialog(
            apiService = apiService,
            onDismiss = { genelMusteriDialogAcikMi = false }
        )
    }

    silinecekMusteri?.let { musteri ->
        AlertDialog(
            onDismissRequest = { silinecekMusteri = null },
            title = { Text("Müşteriyi Sil", fontWeight = FontWeight.Bold) },
            text = { Text("\"${musteri.ad}\" müşterisini silmek istediğinize emin misiniz? Bu işlem geri alınamaz.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                apiService.deleteMusteri(musteri.id ?: 0L)
                                yenilemeTetikleyici++
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                        silinecekMusteri = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
                ) { Text("Sil", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { silinecekMusteri = null }) {
                    Text("Vazgeç", color = Color(0xFF007AFF))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun MusteriKart(musteri: Musteri, onTikla: () -> Unit, onSil: () -> Unit) {
    val (bakiyeFormatli, renk) = bakiyeMetniVeRengi(musteri.bakiye)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().clickable { onTikla() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF007AFF).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF007AFF))
                }
                Column {
                    Text(musteri.ad, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text(musteri.telefon, fontSize = 13.sp, color = Color(0xFF8E8E93))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = bakiyeFormatli,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = renk
                )
                IconButton(onClick = onSil, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFFF3B30))
                }
            }
        }
    }
}

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
            gecmisSatislar.forEach { s ->
                kalemMap[s.id ?: 0L] = apiService.getSatisKalemler(s.id ?: 0L)
            }
            satisKalemleri = kalemMap
        } catch (e: Exception) { e.printStackTrace() }
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
                Text(musteri.ad, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDuzenle, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = Color(0xFF007AFF))
                }
            }
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                Text(musteri.adres, fontSize = 13.sp, color = Color(0xFF8E8E93))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Mevcut Durum: $bakiyeFormatli",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = renk
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    IletisimButonu(
                        baslik = "Tahsilat Gir (Ödeme Al)",
                        ikon = Icons.Default.Payments,
                        renk = Color(0xFF34C759),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        onTahsilatGir()
                    }

                    IletisimButonu(
                        baslik = "Tarih Aralıklı Satış Detayı",
                        ikon = Icons.Default.DateRange,
                        renk = Color(0xFFD435CD),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        onRaporGoster()
                    }

                    IletisimButonu(
                        baslik = "Doğrudan Bakiye/Borç Düzenle",
                        ikon = Icons.Default.Edit,
                        renk = Color(0xFF5856D6),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        onBakiyeDuzenle()
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        IletisimButonu(
                            baslik = "Ara",
                            ikon = Icons.Default.Call,
                            renk = Color(0xFF007AFF),
                            modifier = Modifier.weight(1f)
                        ) {
                            urlAcici.ac(telefonLinkOlustur(musteri.telefon))
                        }
                        IletisimButonu(
                            baslik = "WhatsApp",
                            ikon = Icons.Default.Chat,
                            renk = Color(0xFF34C759),
                            modifier = Modifier.weight(1f)
                        ) {
                            urlAcici.ac(whatsappLinkOlustur(musteri.telefon))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "GEÇMİŞ SATIŞLAR (SON 6)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8E8E93)
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (gecmisSatislar.isEmpty()) {
                    Text("Henüz kayıtlı satış yapılmadı", fontSize = 13.sp, color = Color(0xFF8E8E93))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                Text("Kapat", color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
fun TahsilatGirDialog(
    musteri: Musteri,
    onDismiss: () -> Unit,
    onKaydet: (tutar: Double) -> Unit
) {
    var tutarText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Tahsilat İşle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text("“${musteri.ad}” isimli müşteriden nakit ödeme alıyorsunuz.", fontSize = 13.sp, color = Color(0xFF8E8E93))
                Text("Güncel Borç: ₺${formatMusteriCariIkiBasamak(musteri.bakiye)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                OutlinedTextField(
                    value = tutarText,
                    onValueChange = { tutarText = it },
                    label = { Text("Alınan Tutar (₺)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val temizTutarText = tutarText.replace(',', '.')
                val tutar = temizTutarText.toDoubleOrNull() ?: 0.0
                if (tutar > 0.0) {
                    onKaydet(tutar)
                }
            }) { Text("Tahsilatı Kaydet", color = Color(0xFF34C759), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TarihAralikliSatisRaporDialog(
    apiService: ApiService,
    musteri: Musteri,
    onDismiss: () -> Unit
) {
    var baslangicSeciciAcik by remember { mutableStateOf(false) }
    var bitisSeciciAcik by remember { mutableStateOf(false) }

    val baslangicTarihState = rememberDatePickerState()
    val bitisTarihState = rememberDatePickerState()

    var tumSatislar by remember { mutableStateOf<List<Satis>>(emptyList()) }
    var loadingSatislar by remember { mutableStateOf(true) }

    LaunchedEffect(musteri.id) {
        try {
            tumSatislar = apiService.getSatislar().filter { it.musteriId == musteri.id }
        } catch (e: Exception) { e.printStackTrace() }
        finally { loadingSatislar = false }
    }

    val filtrelenmisSatislar = remember(tumSatislar, baslangicTarihState.selectedDateMillis, bitisTarihState.selectedDateMillis) {
        tumSatislar.filter { satis ->
            val satisZamani = parseTarihMillis(satis.tarih)
            val baslangicKosulu = baslangicTarihState.selectedDateMillis?.let { satisZamani >= it } ?: true
            val bitisKosulu = bitisTarihState.selectedDateMillis?.let { satisZamani <= (it + 86400000L) } ?: true
            baslangicKosulu && bitisKosulu
        }
    }

    val gruplanmisSatislar = remember(filtrelenmisSatislar) {
        filtrelenmisSatislar.groupBy { satis -> formatTarih(satis.tarih).substringBefore(" ") }
    }

    val toplamRaporTutari = remember(filtrelenmisSatislar) {
        filtrelenmisSatislar.sumOf { it.toplamTutar ?: 0.0 }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Tarih Bazlı Satış Detayı", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                Text("${musteri.ad} için tarih filtreli satış raporu.", fontSize = 13.sp, color = Color(0xFF8E8E93))
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { baslangicSeciciAcik = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F2F7), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val basMetni = baslangicTarihState.selectedDateMillis?.let { formatTarih(it.toString()).substringBefore(" ") } ?: "Başlangıç Seç"
                        Text(basMetni, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = { bitisSeciciAcik = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F2F7), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val bitMetni = bitisTarihState.selectedDateMillis?.let { formatTarih(it.toString()).substringBefore(" ") } ?: "Bitiş Seç"
                        Text(bitMetni, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Dönem Toplam Satış:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text("₺${formatMusteriCariIkiBasamak(toplamRaporTutari)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34C759))
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (loadingSatislar) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if (gruplanmisSatislar.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Seçilen aralıkta satış kaydı bulunamadı.", color = Color(0xFF8E8E93), fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gruplanmisSatislar.forEach { (tarihBasligi, satislarListesi) ->
                            item {
                                Text(
                                    text = tarihBasligi,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF007AFF),
                                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                                )
                            }
                            items(satislarListesi) { satis ->
                                var kalemler by remember { mutableStateOf<List<SatisKalemi>>(emptyList()) }
                                LaunchedEffect(satis.id) {
                                    try { kalemler = apiService.getSatisKalemler(satis.id ?: 0L) } catch (e: Exception) {}
                                }
                                val satisSaati = formatSaat(satis.tarih)

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF2F2F7), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Satış",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF8E8E93)
                                        )
                                        Text(
                                            text = satisSaati,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF007AFF)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                                    Spacer(modifier = Modifier.height(4.dp))

                                    kalemler.forEach { kalem ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${kalem.urunAdi} (${kalem.adet} ${kalem.birim} × ₺${formatMusteriCariIkiBasamak(kalem.birimFiyat)})",
                                                fontSize = 13.sp,
                                                color = Color.Black,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "₺${formatMusteriCariIkiBasamak(kalem.toplam)}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF3C3C43)
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
            TextButton(onClick = onDismiss) { Text("Kapat", color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenelMusteriSatisDialog(
    apiService: ApiService,
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
            tumSatislar = apiService.getSatislar().filter { it.musteriId == null }
        } catch (e: Exception) { e.printStackTrace() }
        finally { loadingSatislar = false }
    }

    val filtrelenmisSatislar = remember(tumSatislar, baslangicTarihState.selectedDateMillis, bitisTarihState.selectedDateMillis) {
        tumSatislar.filter { satis ->
            val satisZamani = parseTarihMillis(satis.tarih)
            val baslangicKosulu = baslangicTarihState.selectedDateMillis?.let { satisZamani >= it } ?: true
            val bitisKosulu = bitisTarihState.selectedDateMillis?.let { satisZamani <= (it + 86400000L) } ?: true
            baslangicKosulu && bitisKosulu
        }
    }

    val gruplanmisSatislar = remember(filtrelenmisSatislar) {
        filtrelenmisSatislar.groupBy { satis -> formatTarih(satis.tarih).substringBefore(" ") }
    }

    val toplamRaporTutari = remember(filtrelenmisSatislar) {
        filtrelenmisSatislar.sumOf { it.toplamTutar ?: 0.0 }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Genel Müşteri Satışları", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                Text("Müşteri seçilmeden yapılan satışların tarih filtreli dökümü.", fontSize = 13.sp, color = Color(0xFF8E8E93))
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { baslangicSeciciAcik = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F2F7), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val basMetni = baslangicTarihState.selectedDateMillis?.let { formatTarih(it.toString()).substringBefore(" ") } ?: "Başlangıç Seç"
                        Text(basMetni, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = { bitisSeciciAcik = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F2F7), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val bitMetni = bitisTarihState.selectedDateMillis?.let { formatTarih(it.toString()).substringBefore(" ") } ?: "Bitiş Seç"
                        Text(bitMetni, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Dönem Toplam Satış:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text("₺${formatMusteriCariIkiBasamak(toplamRaporTutari)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34C759))
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (loadingSatislar) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if (gruplanmisSatislar.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Seçilen aralıkta satış kaydı bulunamadı.", color = Color(0xFF8E8E93), fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gruplanmisSatislar.forEach { (tarihBasligi, satislarListesi) ->
                            item {
                                Text(
                                    text = tarihBasligi,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF007AFF),
                                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                                )
                            }
                            items(satislarListesi) { satis ->
                                var kalemler by remember { mutableStateOf<List<SatisKalemi>>(emptyList()) }
                                LaunchedEffect(satis.id) {
                                    try { kalemler = apiService.getSatisKalemler(satis.id ?: 0L) } catch (e: Exception) {}
                                }
                                val satisSaati = formatSaat(satis.tarih)

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF2F2F7), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Satış",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF8E8E93)
                                        )
                                        Text(
                                            text = satisSaati,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF007AFF)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                                    Spacer(modifier = Modifier.height(4.dp))

                                    kalemler.forEach { kalem ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${kalem.urunAdi} (${kalem.adet} ${kalem.birim} × ₺${formatMusteriCariIkiBasamak(kalem.birimFiyat)})",
                                                fontSize = 13.sp,
                                                color = Color.Black,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "₺${formatMusteriCariIkiBasamak(kalem.toplam)}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF3C3C43)
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
            TextButton(onClick = onDismiss) { Text("Kapat", color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold) }
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

@Composable
fun GecmisSatisKarti(satis: Satis, kalemler: List<SatisKalemi>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF2F2F7), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Satış", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF3C3C43))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "₺${formatMusteriCariIkiBasamak(satis.toplamTutar ?: 0.0)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF34C759)
                )
                Text(formatTarih(satis.tarih), fontSize = 10.sp, color = Color(0xFF8E8E93))
            }
        }

        if (kalemler.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 1.dp)
            Spacer(modifier = Modifier.height(6.dp))
            kalemler.forEach { kalem ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${kalem.urunAdi} — ${kalem.adet} ${kalem.birim} × ₺${formatMusteriCariIkiBasamak(kalem.birimFiyat)}",
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "₺${formatMusteriCariIkiBasamak(kalem.toplam)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF3C3C43)
                    )
                }
            }
        }
    }
}

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
            .height(44.dp)
            .background(renk.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(ikon, contentDescription = null, tint = renk, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(baslik, color = renk, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Yeni Müşteri Kaydı", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (errorMessage != null) {
                    Text(errorMessage, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(value = ad, onValueChange = { ad = it }, label = { Text("Ad Soyad") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !isSaving)
                OutlinedTextField(
                    value = telefon,
                    onValueChange = { telefon = it },
                    label = { Text("Telefon (05XX XXX XX XX)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                OutlinedTextField(value = adres, onValueChange = { adres = it }, label = { Text("Adres") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !isSaving)
                OutlinedTextField(
                    value = bakiye,
                    onValueChange = { bakiye = it },
                    label = { Text("Mevcut Başlangıç Borcu (₺)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )

                if (isSaving) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    if (ad.isNotBlank()) {
                        val temizBakiyeText = bakiye.replace(',', '.')
                        onKaydet(ad, telefon, adres, temizBakiyeText.toDoubleOrNull() ?: 0.0)
                    }
                }
            ) { Text(if (isSaving) "Kaydediliyor..." else "Kaydet", color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}

@Composable
fun MusteriDuzenleDialog(
    musteri: Musteri,
    onDismiss: () -> Unit,
    onKaydet: (ad: String, telefon: String, adres: String) -> Unit
) {
    var ad by remember { mutableStateOf(musteri.ad) }
    var telefon by remember { mutableStateOf(musteri.telefon) }
    var adres by remember { mutableStateOf(musteri.adres) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Müşteri Bilgilerini Düzenle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = ad, onValueChange = { ad = it }, label = { Text("Ad Soyad") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = telefon,
                    onValueChange = { telefon = it },
                    label = { Text("Telefon") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = adres, onValueChange = { adres = it }, label = { Text("Adres") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text(
                    "Not: Borç/Bakiye bilgisi bu ekrandan değiştirilemez, satış veya tahsilat menülerinden otomatik güncellenmesi önerilir.",
                    fontSize = 11.sp,
                    color = Color(0xFF8E8E93)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (ad.isNotBlank()) {
                    onKaydet(ad, telefon, adres)
                }
            }) { Text("Kaydet", color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}

@Composable
fun BakiyeDuzenleDialog(
    musteri: Musteri,
    onDismiss: () -> Unit,
    onKaydet: (yeniBakiye: Double) -> Unit
) {
    var yeniBakiyeText by remember { mutableStateOf(musteri.bakiye.toString()) }
    val (bakiyeFormatli, _) = bakiyeMetniVeRengi(musteri.bakiye)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Net Borç Ayarla", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Mevcut durum: $bakiyeFormatli",
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93)
                )
                OutlinedTextField(
                    value = yeniBakiyeText,
                    onValueChange = { yeniBakiyeText = it },
                    label = { Text("Yeni Net Borç Tutarı (₺)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Dikkat: Gireceğiniz tutar müşterinin güncel net borcu sayılacaktır. Müşteri size borçluysa düz rakam (Örn: 500), eğer siz müşteriye borçluysanız eksi değer (Örn: -200) giriniz.",
                    fontSize = 11.sp,
                    color = Color(0xFF8E8E93)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val temizBakiyeText = yeniBakiyeText.replace(',', '.')
                val yeniBakiye = temizBakiyeText.toDoubleOrNull()
                if (yeniBakiye != null) {
                    onKaydet(yeniBakiye)
                }
            }) { Text("Güncelle", color = Color(0xFF5856D6), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}

// Hem ISO string ("2026-09-06T17:45:06") hem de milisaniye ("1725637200000") destekler
fun formatSaat(tarih: String?): String {
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
    val toplamSaniye = millis / 1000
    val gunIciSaniye = toplamSaniye % 86400

    val toplamSaatSaniye = gunIciSaniye + (3 * 3600)
    val duzeltilmisSaniye = if (toplamSaatSaniye >= 86400) toplamSaatSaniye - 86400 else toplamSaatSaniye

    val saat = (duzeltilmisSaniye / 3600).toString().padStart(2, '0')
    val dakika = ((duzeltilmisSaniye % 3600) / 60).toString().padStart(2, '0')

    return "$saat:$dakika"
}

private fun formatMusteriCariIkiBasamak(deger: Double): String {
    val negatifMi = deger < 0
    val mutlakDeger = if (negatifMi) -deger else deger
    val yuvarlanmis = ((mutlakDeger * 100.0) + 0.5).toLong() / 100.0
    val tamKisim = yuvarlanmis.toLong()
    val kesirKisim = (((yuvarlanmis - tamKisim) * 100.0) + 0.5).toLong()
    val kesirStr = kesirKisim.toString().padStart(2, '0')
    return "${if (negatifMi) "-" else ""}$tamKisim.$kesirStr"
}