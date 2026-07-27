package com.eray.muhasebeapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eray.muhasebeapp.database.shared.AppDatabase
import com.eray.muhasebeapp.IslemKaydi
import com.eray.muhasebeapp.rememberDosyaPaylasici
import com.eray.muhasebeapp.formatTarih
import com.eray.muhasebeapp.excelXlsxOlustur
import com.eray.muhasebeapp.database.Musteri
import com.eray.muhasebeapp.database.Tedarikci
import com.eray.muhasebeapp.database.SatisKalemi
import com.eray.muhasebeapp.database.AlisKalemi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.text.selection.SelectionContainer

enum class RaporDonemi(val etiket: String) {
    TUM_ZAMANLAR("Tüm Zamanlar"),
    SON_6_AY("Son 6 Ay"),
    BU_AY("Bu Ay"),
    BU_HAFTA("Bu Hafta"),
    BUGUN("Bugün")
}

fun donemBaslangicMillis(donem: RaporDonemi, simdiMillis: Long): Long? {
    val tzOffsetMillis = 3L * 3600L * 1000L
    val yerelSimdi = simdiMillis + tzOffsetMillis
    val toplamSaniye = yerelSimdi / 1000L
    val toplamGun = toplamSaniye / 86400L
    val gunIciMilis = yerelSimdi % 86400000L

    val bugunBaslangicUtcMillis = simdiMillis - gunIciMilis

    fun daysToDate(totalDays: Long): Triple<Int, Int, Int> {
        val days = totalDays + 719468L
        val era = (if (days >= 0L) days else days - 146096L) / 146097L
        val doe = days - era * 146097L
        val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L
        val y = yoe + era * 400L
        val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = mp + if (mp < 10L) 3L else -9L
        val year = (y + if (m <= 2L) 1L else 0L).toInt()
        return Triple(year, m.toInt(), d.toInt())
    }

    fun dateToDays(year: Int, month: Int, day: Int): Long {
        val y = if (month <= 2) (year - 1).toLong() else year.toLong()
        val m = if (month <= 2) (month + 9).toLong() else (month - 3).toLong()
        val era = (if (y >= 0L) y else y - 399L) / 400L
        val yoe = y - era * 400L
        val doy = (153 * m + 2L) / 5L + day.toLong() - 1L
        val doe = yoe * 365L + yoe / 4L - yoe / 100L + doy
        return era * 146097L + doe - 719468L
    }

    return when (donem) {
        RaporDonemi.TUM_ZAMANLAR -> null
        RaporDonemi.BUGUN -> bugunBaslangicUtcMillis
        RaporDonemi.BU_HAFTA -> {
            val gunIndex = (toplamGun + 3L) % 7L
            bugunBaslangicUtcMillis - (gunIndex * 86400000L)
        }
        RaporDonemi.BU_AY -> {
            val (year, month, _) = daysToDate(toplamGun)
            val hedefGun = dateToDays(year, month, 1)
            hedefGun * 86400000L - tzOffsetMillis
        }
        RaporDonemi.SON_6_AY -> {
            val (year, month, day) = daysToDate(totalDays = toplamGun)
            var yeniMonth = month - 6
            var yeniYear = year
            if (yeniMonth <= 0) {
                yeniMonth += 12
                yeniYear -= 1
            }
            val hedefGun = dateToDays(yeniYear, yeniMonth, day)
            hedefGun * 86400000L - tzOffsetMillis
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaporlamaScreen(
    database: AppDatabase,
    simdiMillis: Long,
    onNavigateBack: () -> Unit
) {
    var yukleniyor by remember { mutableStateOf(true) }
    var hataMesaji by remember { mutableStateOf<String?>(null) }
    var gruplanmisIslemler by remember { mutableStateOf<Map<String, List<IslemKaydi>>>(emptyMap()) }

    var seciliDonem by remember { mutableStateOf(RaporDonemi.TUM_ZAMANLAR) }
    var mevcutLimit by remember { mutableStateOf(30) }
    var toplamSatis by remember { mutableStateOf(0.0) }
    var toplamAlis by remember { mutableStateOf(0.0) }
    var toplamMasraf by remember { mutableStateOf(0.0) }
    var netKar by remember { mutableStateOf(0.0) }
    var islemSayisi by remember { mutableStateOf(0) }
    var ortalamaSatisTutari by remember { mutableStateOf(0.0) }

    var tumSatislarHam by remember { mutableStateOf<List<com.eray.muhasebeapp.database.Satis>>(emptyList()) }
    var tumAlislarHam by remember { mutableStateOf<List<com.eray.muhasebeapp.database.Alis>>(emptyList()) }
    var tumMasraflarHam by remember { mutableStateOf<List<com.eray.muhasebeapp.database.Masraf>>(emptyList()) }
    var tumStoklarHam by remember { mutableStateOf<List<com.eray.muhasebeapp.database.StokHareketi>>(emptyList()) }
    var tumTahsilatlarHam by remember { mutableStateOf<List<com.eray.muhasebeapp.database.Tahsilat>>(emptyList()) }

    // Sağa kaydırarak geri dönme (Swipe Back) durumu
    var horizontalDragAccumulator by remember { mutableStateOf(0f) }

    // Dönem veya Limit değiştiğinde tetiklenen AĞIR İŞ PARÇACIĞI (Dispatchers.Default)
    LaunchedEffect(seciliDonem, mevcutLimit) {
        yukleniyor = true
        withContext(Dispatchers.Default) {
            try {
                val donemBaslangic = donemBaslangicMillis(seciliDonem, simdiMillis)

                if (tumSatislarHam.isEmpty()) tumSatislarHam = database.appDatabaseQueries.selectAllSatis().executeAsList()
                if (tumAlislarHam.isEmpty()) tumAlislarHam = database.appDatabaseQueries.selectAllAlis().executeAsList()
                if (tumMasraflarHam.isEmpty()) tumMasraflarHam = database.appDatabaseQueries.selectAllMasraf().executeAsList()
                if (tumStoklarHam.isEmpty()) tumStoklarHam = database.appDatabaseQueries.selectAllStokHareketi().executeAsList()
                if (tumTahsilatlarHam.isEmpty()) tumTahsilatlarHam = database.appDatabaseQueries.selectAllTahsilat().executeAsList()

                val satislarFiltreli = tumSatislarHam.filter { donemBaslangic == null || (it.tarih.toLongOrNull() ?: 0L) >= donemBaslangic }
                val alislarFiltreli = tumAlislarHam.filter { donemBaslangic == null || (it.tarih.toLongOrNull() ?: 0L) >= donemBaslangic }
                val masraflarFiltreli = tumMasraflarHam.filter { donemBaslangic == null || (it.tarih.toLongOrNull() ?: 0L) >= donemBaslangic }
                val stokHareketleriFiltreli = tumStoklarHam.filter { donemBaslangic == null || (it.tarih.toLongOrNull() ?: 0L) >= donemBaslangic }
                val tahsilatlarFiltreli = tumTahsilatlarHam.filter { donemBaslangic == null || (it.tarih.toLongOrNull() ?: 0L) >= donemBaslangic }

                toplamSatis = satislarFiltreli.sumOf { it.toplamTutar }
                toplamAlis = alislarFiltreli.sumOf { it.toplamTutar }
                toplamMasraf = masraflarFiltreli.sumOf { it.tutar }
                netKar = toplamSatis - toplamAlis - toplamMasraf
                islemSayisi = satislarFiltreli.size
                ortalamaSatisTutari = if (islemSayisi > 0) toplamSatis / islemSayisi else 0.0

                val hafifList = (satislarFiltreli.map { HafifIslem.S(it) } +
                        alislarFiltreli.map { HafifIslem.A(it) } +
                        masraflarFiltreli.map { HafifIslem.M(it) } +
                        stokHareketleriFiltreli.map { HafifIslem.St(it) } +
                        tahsilatlarFiltreli.map { HafifIslem.T(it) })
                    .sortedByDescending { it.tarih.toLongOrNull() ?: 0L }

                val limitliHafifList = hafifList.take(mevcutLimit)

                val tamIslemler = limitliHafifList.map { hafif ->
                    when (hafif) {
                        is HafifIslem.S -> {
                            val kalemler = database.appDatabaseQueries.selectKalemlerBySatisId(hafif.satis.id).executeAsList()
                            IslemKaydi.SatisIslemi(hafif.satis, kalemler)
                        }
                        is HafifIslem.A -> {
                            val kalemler = database.appDatabaseQueries.selectKalemlerByAlisId(hafif.alis.id).executeAsList()
                            IslemKaydi.AlisIslemi(hafif.alis, kalemler)
                        }
                        is HafifIslem.M -> IslemKaydi.MasrafIslemi(hafif.masraf)
                        is HafifIslem.St -> IslemKaydi.StokIslemi(hafif.stok)
                        is HafifIslem.T -> IslemKaydi.TahsilatIslemi(hafif.tahsilat)
                    }
                }

                gruplanmisIslemler = tamIslemler.groupBy { islem -> formatTarih(islem.tarih).substringBefore(" ") }

            } catch (e: Throwable) {
                hataMesaji = "VERİ İŞLEME HATASI: ${e::class.simpleName}: ${e.message}\n${e.stackTraceToString().take(1500)}"
            } finally {
                yukleniyor = false
            }
        }
    }

    if (hataMesaji != null) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopStart) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Bir hata oluştu, lütfen bu metni kopyala:", fontWeight = FontWeight.Bold, color = Color.Red)
                Spacer(modifier = Modifier.height(8.dp))
                SelectionContainer { Text(hataMesaji ?: "", fontSize = 12.sp) }
            }
        }
        return
    }

    // 🎯 DÜZELTİLDİ: gruplanmisMasraflar yerine sayfanın kendi verisi olan gruplanmisIslemler kontrol ediliyor
    if (yukleniyor && gruplanmisIslemler.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF007AFF))
        }
        return
    }

    val dosyaPaylasici = rememberDosyaPaylasici()
    var detayliRaporDialogAcik by remember { mutableStateOf(false) }

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
                title = { Text("Raporlama", fontWeight = FontWeight.Bold, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Black)
                    }
                },
                actions = {
                    IconButton(onClick = { detayliRaporDialogAcik = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Rapor Çıkar", tint = Color(0xFF007AFF))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = scaffoldPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RaporDonemi.entries.forEach { donem ->
                        FilterChip(
                            selected = seciliDonem == donem,
                            onClick = { seciliDonem = donem; mevcutLimit = 30 },
                            label = { Text(donem.etiket, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF007AFF),
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RaporOzetKart("Satış (Ciro)", "₺${formatRaporIkiBasamak(toplamSatis)}", Color(0xFF34C759), Modifier.weight(1f))
                    RaporOzetKart("Alış", "₺${formatRaporIkiBasamak(toplamAlis)}", Color(0xFFFF9500), Modifier.weight(1f))
                }
            }

            item {
                RaporOzetKart(
                    baslik = "Toplam Masraf",
                    deger = "₺${formatRaporIkiBasamak(toplamMasraf)}",
                    renk = Color(0xFFFF3B30),
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 8.dp)
                )
            }

            item {
                RaporOzetKart(
                    baslik = "Net Kâr / Zarar (Nakit)",
                    deger = "₺${formatRaporIkiBasamak(netKar)}",
                    renk = if (netKar >= 0) Color(0xFF007AFF) else Color(0xFFFF3B30),
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RaporOzetKart("İşlem Sayısı (Satış)", "$islemSayisi", Color(0xFF007AFF), Modifier.weight(1f))
                    RaporOzetKart("Ort. Satış Tutarı", "₺${formatRaporIkiBasamak(ortalamaSatisTutari)}", Color(0xFF5856D6), Modifier.weight(1f))
                }
            }

            item {
                Button(
                    onClick = { detayliRaporDialogAcik = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 0.dp).height(48.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rapor Çıkar (Excel)", fontWeight = FontWeight.SemiBold)
                }
            }

            item {
                Text(
                    text = "TÜM İŞLEMLER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp)
                )
            }

            if (gruplanmisIslemler.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Text(if(yukleniyor) "Yükleniyor..." else "Henüz işlem yok", color = Color(0xFF8E8E93), fontSize = 15.sp)
                    }
                }
            } else {
                gruplanmisIslemler.forEach { (tarihBasligi, islemlerListesi) ->
                    item {
                        Text(
                            text = tarihBasligi,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007AFF),
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 2.dp)
                        )
                    }
                    items(islemlerListesi) { islem ->
                        Box(modifier = Modifier.padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 0.dp)) {
                            IslemKart(islem)
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = { mevcutLimit += 30 },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF007AFF))
                    ) {
                        Text("Daha Fazla İşlem Yükle (+30)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (detayliRaporDialogAcik) {
        DetayliRaporFiltreDialog(
            database = database,
            simdiMillis = simdiMillis,
            onDismiss = { detayliRaporDialogAcik = false },
            onRaporOlustur = { raporTuru, baslangicMs, bitisMs, seciliMusteriId, seciliTedarikciId ->
                val filteredSatislar = tumSatislarHam.filter {
                    val t = it.tarih.toLongOrNull() ?: 0L
                    val uyar = (baslangicMs?.let { b -> t >= b } ?: true) && (bitisMs?.let { b -> t <= (b + 86400000L) } ?: true)
                    val musteriUyar = seciliMusteriId?.let { id -> it.musteriId == id } ?: true
                    uyar && musteriUyar && (raporTuru == "Genel Rapor" || raporTuru == "Satış Raporu")
                }
                val filteredAlislar = tumAlislarHam.filter {
                    val t = it.tarih.toLongOrNull() ?: 0L
                    val uyar = (baslangicMs?.let { b -> t >= b } ?: true) && (bitisMs?.let { b -> t <= (b + 86400000L) } ?: true)
                    val tedarikciUyar = seciliTedarikciId?.let { id -> it.tedarikciId == id } ?: true
                    uyar && tedarikciUyar && (raporTuru == "Genel Rapor" || raporTuru == "Alış Raporu")
                }
                val filteredMasraflar = tumMasraflarHam.filter {
                    val t = it.tarih.toLongOrNull() ?: 0L
                    val uyar = (baslangicMs?.let { b -> t >= b } ?: true) && (bitisMs?.let { b -> t <= (b + 86400000L) } ?: true)
                    uyar && (raporTuru == "Genel Rapor" || raporTuru == "Masraf Raporu")
                }
                val filteredStoklar = tumStoklarHam.filter {
                    val t = it.tarih.toLongOrNull() ?: 0L
                    val uyar = (baslangicMs?.let { b -> t >= b } ?: true) && (bitisMs?.let { b -> t <= (b + 86400000L) } ?: true)
                    uyar && (raporTuru == "Genel Rapor" || raporTuru == "Stok Hareketi Raporu")
                }
                val filteredTahsilatlar = tumTahsilatlarHam.filter {
                    val t = it.tarih.toLongOrNull() ?: 0L
                    val uyar = (baslangicMs?.let { b -> t >= b } ?: true) && (bitisMs?.let { b -> t <= (b + 86400000L) } ?: true)
                    val musteriUyar = seciliMusteriId?.let { id -> it.musteriId == id } ?: true
                    uyar && musteriUyar && (raporTuru == "Genel Rapor" || raporTuru == "Tahsilat Raporu")
                }

                val bytes = excelXlsxOlustur(
                    satislar = filteredSatislar,
                    alislar = filteredAlislar,
                    masraflar = filteredMasraflar,
                    stokHareketleri = filteredStoklar,
                    tahsilatlar = filteredTahsilatlar,
                    raporTuru = raporTuru,
                    satisKalemleriGetir = { satisId -> database.appDatabaseQueries.selectKalemlerBySatisId(satisId).executeAsList() },
                    alisKalemleriGetir = { alisId -> database.appDatabaseQueries.selectKalemlerByAlisId(alisId).executeAsList() }
                )

                val bugununTarihi = formatTarih(simdiMillis.toString()).substringBefore(" ").replace(".", "-")
                val dosyaAdi = "${dosyaAdiIcinTemizle(raporTuru)}_$bugununTarihi.xlsx"
                dosyaPaylasici.paylasBytes(dosyaAdi, bytes)
                detayliRaporDialogAcik = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetayliRaporFiltreDialog(
    database: AppDatabase,
    simdiMillis: Long,
    onDismiss: () -> Unit,
    onRaporOlustur: (raporTuru: String, baslangic: Long?, bitis: Long?, musteriId: Long?, tedarikciId: Long?) -> Unit
) {
    var raporTuru by remember { mutableStateOf("Genel Rapor") }
    var raporTuruMenuAcik by remember { mutableStateOf(false) }

    var seciliDialogDonem by remember { mutableStateOf(RaporDonemi.TUM_ZAMANLAR) }
    var donemMenuAcik by remember { mutableStateOf(false) }

    val musteriler = remember { database.appDatabaseQueries.selectAllMusteri().executeAsList() }
    var seciliMusteri by remember { mutableStateOf<Musteri?>(null) }
    var musteriMenuAcik by remember { mutableStateOf(false) }

    val tedarikciler = remember { database.appDatabaseQueries.selectAllTedarikci().executeAsList() }
    var seciliTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var tedarikciMenuAcik by remember { mutableStateOf(false) }

    var baslangicPickerAcik by remember { mutableStateOf(false) }
    var bitisPickerAcik by remember { mutableStateOf(false) }

    val baslangicState = rememberDatePickerState()
    val bitisState = rememberDatePickerState()

    val raporTurleri = listOf("Genel Rapor", "Satış Raporu", "Alış Raporu", "Masraf Raporu", "Stok Hareketi Raporu", "Tahsilat Raporu")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Detaylı Rapor Sihirbazı", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Rapor İçeriği Seçin", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                ExposedDropdownMenuBox(
                    expanded = raporTuruMenuAcik,
                    onExpandedChange = { raporTuruMenuAcik = it }
                ) {
                    OutlinedTextField(
                        value = raporTuru,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = raporTuruMenuAcik) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = raporTuruMenuAcik,
                        onDismissRequest = { raporTuruMenuAcik = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        raporTurleri.forEach { tur ->
                            DropdownMenuItem(
                                text = { Text(tur) },
                                onClick = {
                                    raporTuru = tur
                                    raporTuruMenuAcik = false
                                }
                            )
                        }
                    }
                }

                Text("Dönem Seçin (Hazız Filtre)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                ExposedDropdownMenuBox(
                    expanded = donemMenuAcik,
                    onExpandedChange = { donemMenuAcik = it }
                ) {
                    OutlinedTextField(
                        value = seciliDialogDonem.etiket,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = donemMenuAcik) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = donemMenuAcik,
                        onDismissRequest = { donemMenuAcik = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        RaporDonemi.entries.forEach { donem ->
                            DropdownMenuItem(
                                text = { Text(donem.etiket) },
                                onClick = {
                                    seciliDialogDonem = donem
                                    donemMenuAcik = false
                                }
                            )
                        }
                    }
                }

                if (raporTuru == "Satış Raporu" || raporTuru == "Tahsilat Raporu") {
                    Text("Filtrelenecek Müşteri", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    ExposedDropdownMenuBox(
                        expanded = musteriMenuAcik,
                        onExpandedChange = { musteriMenuAcik = it }
                    ) {
                        OutlinedTextField(
                            value = seciliMusteri?.ad ?: "Tüm Müşteriler",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = musteriMenuAcik) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = musteriMenuAcik,
                            onDismissRequest = { musteriMenuAcik = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tüm Müşteriler") },
                                onClick = {
                                    seciliMusteri = null
                                    musteriMenuAcik = false
                                }
                            )
                            musteriler.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.ad) },
                                    onClick = {
                                        seciliMusteri = m
                                        musteriMenuAcik = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (raporTuru == "Alış Raporu") {
                    Text("Filtrelenecek Tedarikçi", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    ExposedDropdownMenuBox(
                        expanded = tedarikciMenuAcik,
                        onExpandedChange = { tedarikciMenuAcik = it }
                    ) {
                        OutlinedTextField(
                            value = seciliTedarikci?.ad ?: "Tüm Tedarikçiler",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tedarikciMenuAcik) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = tedarikciMenuAcik,
                            onDismissRequest = { tedarikciMenuAcik = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tüm Tedarikçiler") },
                                onClick = {
                                    seciliTedarikci = null
                                    tedarikciMenuAcik = false
                                }
                            )
                            tedarikciler.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.ad) },
                                    onClick = {
                                        seciliTedarikci = t
                                        tedarikciMenuAcik = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (seciliDialogDonem == RaporDonemi.TUM_ZAMANLAR) {
                    Text("Özel Tarih Aralığı (İsteğe Bağlı)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { baslangicPickerAcik = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F2F7), contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val metin = baslangicState.selectedDateMillis?.let { formatTarih(it.toString()).substringBefore(" ") } ?: "Başlangıç"
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(metin, fontSize = 11.sp, maxLines = 1)
                        }

                        Button(
                            onClick = { bitisPickerAcik = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F2F7), contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val metin = bitisState.selectedDateMillis?.let { formatTarih(it.toString()).substringBefore(" ") } ?: "Bitiş"
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(metin, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalBaslangicMs = if (seciliDialogDonem != RaporDonemi.TUM_ZAMANLAR) {
                        donemBaslangicMillis(seciliDialogDonem, simdiMillis)
                    } else {
                        baslangicState.selectedDateMillis
                    }

                    onRaporOlustur(
                        raporTuru,
                        finalBaslangicMs,
                        bitisState.selectedDateMillis,
                        seciliMusteri?.id,
                        seciliTedarikci?.id
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Excel Çıkar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    )

    if (baslangicPickerAcik) {
        DatePickerDialog(
            onDismissRequest = { baslangicPickerAcik = false },
            confirmButton = { TextButton(onClick = { baslangicPickerAcik = false }) { Text("Seç") } }
        ) { DatePicker(state = baslangicState) }
    }

    if (bitisPickerAcik) {
        DatePickerDialog(
            onDismissRequest = { bitisPickerAcik = false },
            confirmButton = { TextButton(onClick = { bitisPickerAcik = false }) { Text("Seç") } }
        ) { DatePicker(state = bitisState) }
    }
}

@Composable
private fun RaporOzetKart(baslik: String, deger: String, renk: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 14.dp)) {
            Text(baslik, fontSize = 12.sp, color = Color(0xFF8E8E93))
            Spacer(modifier = Modifier.height(4.dp))
            Text(deger, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = renk)
        }
    }
}

@Composable
private fun IslemKart(islem: IslemKaydi) {
    val (baslik, altBaslik, tutar, renk, ikon) = when (islem) {
        is IslemKaydi.MasrafIslemi -> {
            IslemGoruntu(
                "Masraf",
                islem.masraf.kategori,
                islem.masraf.tutar,
                Color(0xFFFF3B30),
                Icons.Default.Receipt
            )
        }
        is IslemKaydi.StokIslemi -> IslemGoruntu(
            "Stok Hareketi", "${islem.stokHareketi.urunAdi} • ${islem.stokHareketi.hareketTuru} (${islem.stokHareketi.miktar})",
            islem.tutar, Color(0xFF5856D6), Icons.Default.Inventory2
        )
        is IslemKaydi.SatisIslemi -> {
            val urunListesi = islem.kalemler.joinToString(", ") { "${it.urunAdi} x${it.adet}" }
            IslemGoruntu(
                "Satış",
                if (urunListesi.isNotEmpty()) "$urunListesi\n${islem.satis.musteriAdi}" else islem.satis.musteriAdi,
                islem.satis.toplamTutar, Color(0xFF34C759), Icons.Default.TrendingUp
            )
        }
        is IslemKaydi.AlisIslemi -> {
            val urunListesi = islem.kalemler.joinToString(", ") { "${it.urunAdi} x${it.adet}" }
            IslemGoruntu(
                "Alış",
                if (urunListesi.isNotEmpty()) "$urunListesi\n${islem.alis.tedarikciAdi}" else islem.alis.tedarikciAdi,
                islem.alis.toplamTutar, Color(0xFFFF9500), Icons.Default.ShoppingBag
            )
        }
        is IslemKaydi.TahsilatIslemi -> IslemGoruntu(
            "Tahsilat (Ödeme Alındı)",
            "${islem.tahsilat.musteriAdi} tarafından yapılan ödeme",
            islem.tahsilat.tutar,
            Color(0xFF34C759),
            Icons.Default.Payments
        )
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(renk.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(ikon, contentDescription = null, tint = renk)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = baslik, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = altBaslik, fontSize = 13.sp, color = Color(0xFF8E8E93), lineHeight = 16.sp)
            }

            Column(horizontalAlignment = Alignment.End, modifier = Modifier.wrapContentWidth()) {
                Text(text = "₺${formatRaporIkiBasamak(tutar)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = renk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = formatTarih(islem.tarih), fontSize = 11.sp, color = Color(0xFF8E8E93), maxLines = 1)
            }
        }
    }
}

private data class IslemGoruntu(
    val baslik: String,
    val altBaslik: String,
    val tutar: Double,
    val renk: Color,
    val ikon: androidx.compose.ui.graphics.vector.ImageVector
)

private operator fun IslemGoruntu.component1() = baslik
private operator fun IslemGoruntu.component2() = altBaslik
private operator fun IslemGoruntu.component3() = tutar
private operator fun IslemGoruntu.component4() = renk
private operator fun IslemGoruntu.component5() = ikon

private fun formatRaporIkiBasamak(deger: Double): String {
    val negatifMi = deger < 0
    val mutlakDeger = if (negatifMi) -deger else deger
    val yuvarlanmis = ((mutlakDeger * 100.0) + 0.5).toLong() / 100.0
    val tamKisim = yuvarlanmis.toLong()
    val kesirKisim = (((yuvarlanmis - tamKisim) * 100.0) + 0.5).toLong()
    val kesirStr = kesirKisim.toString().padStart(2, '0')
    return "${if (negatifMi) "-" else ""}$tamKisim.$kesirStr"
}

private fun dosyaAdiIcinTemizle(metin: String): String = metin
    .replace(" ", "_")
    .replace("ş", "s").replace("Ş", "S")
    .replace("ç", "c").replace("Ç", "C")
    .replace("ı", "i").replace("İ", "I")
    .replace("ğ", "g").replace("Ğ", "G")
    .replace("ü", "u").replace("Ü", "U")
    .replace("ö", "o").replace("Ö", "O")

sealed class HafifIslem(val tarih: String) {
    class S(val satis: com.eray.muhasebeapp.database.Satis) : HafifIslem(satis.tarih)
    class A(val alis: com.eray.muhasebeapp.database.Alis) : HafifIslem(alis.tarih)
    class M(val masraf: com.eray.muhasebeapp.database.Masraf) : HafifIslem(masraf.tarih)
    class St(val stok: com.eray.muhasebeapp.database.StokHareketi) : HafifIslem(stok.tarih)
    class T(val tahsilat: com.eray.muhasebeapp.database.Tahsilat) : HafifIslem(tahsilat.tarih)
}