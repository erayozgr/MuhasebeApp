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
import com.eray.muhasebeapp.database.shared.AppDatabase
import com.eray.muhasebeapp.database.Tedarikci
import com.eray.muhasebeapp.database.Alis
import com.eray.muhasebeapp.database.AlisKalemi
import com.eray.muhasebeapp.rememberUrlAcici
import com.eray.muhasebeapp.telefonLinkOlustur
import com.eray.muhasebeapp.whatsappLinkOlustur
import com.eray.muhasebeapp.formatTarih

private fun tedarikciBakiyeMetniVeRengi(bakiye: Double): Pair<String, Color> {
    val formatliBakiye = formatTedarikciCariIkiBasamak(bakiye)
    return when {
        bakiye > 0 -> "₺$formatliBakiye (Borcumuz)" to Color(0xFFFF3B30)
        bakiye < 0 -> "₺${formatTedarikciCariIkiBasamak(-bakiye)} (Alacaklıyız)" to Color(0xFF007AFF)
        else -> "₺0.00 (Dengede)" to Color(0xFF34C759)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TedarikcilerScreen(
    database: AppDatabase,
    onNavigateBack: () -> Unit
) {
    var yenilemeTetikleyici by remember { mutableStateOf(0) }
    val tedarikciler = remember(yenilemeTetikleyici) {
        database.appDatabaseQueries.selectAllTedarikci().executeAsList()
    }

    var dialogAcikMi by remember { mutableStateOf(false) }
    var detayGosterilenTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var duzenlenenTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var bakiyeDuzenlenenTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var odemeTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var raporTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var silinecekTedarikci by remember { mutableStateOf<Tedarikci?>(null) }

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
                title = { Text("Tedarikçiler", fontWeight = FontWeight.Bold, color = Color.Black) },
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
                containerColor = Color(0xFF5856D6)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ekle", tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

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
                        Text("Toplam Tedarikçi", fontSize = 13.sp, color = Color(0xFF8E8E93))
                        Text("${tedarikciler.size}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5856D6))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Toplam Borcumuz", fontSize = 13.sp, color = Color(0xFF8E8E93))
                        val toplamBorc = tedarikciler.sumOf { it.bakiye }
                        Text(
                            "₺${formatTedarikciCariIkiBasamak(toplamBorc)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (toplamBorc > 0) Color(0xFFFF9500) else Color(0xFF34C759)
                        )
                    }
                }
            }

            if (tedarikciler.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Henüz tedarikçi eklenmedi", color = Color(0xFF8E8E93), fontSize = 15.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tedarikciler) { tedarikci ->
                        TedarikciKart(
                            tedarikci = tedarikci,
                            onTikla = { detayGosterilenTedarikci = tedarikci },
                            onSil = { silinecekTedarikci = tedarikci }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (dialogAcikMi) {
        TedarikciEkleDialog(
            onDismiss = { dialogAcikMi = false },
            onKaydet = { ad, telefon, adres, bakiye ->
                database.appDatabaseQueries.insertTedarikci(ad, telefon, adres, bakiye)
                yenilemeTetikleyici++
                dialogAcikMi = false
            }
        )
    }

    detayGosterilenTedarikci?.let { tedarikci ->
        TedarikciDetayDialog(
            database = database,
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

    duzenlenenTedarikci?.let { tedarikci ->
        TedarikciDuzenleDialog(
            tedarikci = tedarikci,
            onDismiss = { duzenlenenTedarikci = null },
            onKaydet = { ad, telefon, adres ->
                database.appDatabaseQueries.updateTedarikci(ad, telefon, adres, tedarikci.id)
                yenilemeTetikleyici++
                duzenlenenTedarikci = null
            }
        )
    }

    bakiyeDuzenlenenTedarikci?.let { tedarikci ->
        TedarikciBakiyeDuzenleDialog(
            tedarikci = tedarikci,
            onDismiss = { bakiyeDuzenlenenTedarikci = null },
            onKaydet = { yeniBakiye ->
                database.appDatabaseQueries.updateTedarikciBakiye(yeniBakiye, tedarikci.id)
                yenilemeTetikleyici++
                bakiyeDuzenlenenTedarikci = null
            }
        )
    }

    odemeTedarikci?.let { tedarikci ->
        TedarikciOdemeGirDialog(
            tedarikci = tedarikci,
            onDismiss = { odemeTedarikci = null },
            onKaydet = { odemeTutari ->
                // 🎯 ÖDEME YAPILDIĞINDA RAPORLAMAYA DÜŞMESİ İÇİN KRONOLOJİK LOGLAMA YAPILIYOR
                database.appDatabaseQueries.transaction {
                    database.appDatabaseQueries.updateTedarikciBakiye(tedarikci.bakiye - odemeTutari, tedarikci.id)
                    database.appDatabaseQueries.insertTedarikciOdemesi(
                        tedarikciId = tedarikci.id,
                        tedarikciAdi = tedarikci.ad,
                        tutar = odemeTutari,
                        tarih = com.eray.muhasebeapp.getEpochMillis().toString()
                    )
                }
                yenilemeTetikleyici++
                odemeTedarikci = null
            }
        )
    }

    raporTedarikci?.let { tedarikci ->
        TarihAralikliAlisRaporDialog(
            database = database,
            tedarikci = tedarikci,
            onDismiss = { raporTedarikci = null }
        )
    }

    silinecekTedarikci?.let { tedarikci ->
        AlertDialog(
            onDismissRequest = { silinecekTedarikci = null },
            title = { Text("Tedarikçiyi Sil", fontWeight = FontWeight.Bold) },
            text = { Text("\"${tedarikci.ad}\" tedarikçisini silmek istediğinize emin misiniz? Bu işlem geri alınamaz.") },
            confirmButton = {
                Button(
                    onClick = {
                        database.appDatabaseQueries.deleteTedarikci(tedarikci.id)
                        yenilemeTetikleyici++
                        silinecekTedarikci = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
                ) { Text("Sil", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { silinecekTedarikci = null }) {
                    Text("Vazgeç", color = Color(0xFF5856D6))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun TedarikciKart(tedarikci: Tedarikci, onTikla: () -> Unit, onSil: () -> Unit) {
    val (bakiyeFormatli, renk) = tedarikciBakiyeMetniVeRengi(tedarikci.bakiye)

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
                        .background(Color(0xFF5856D6).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFF5856D6))
                }
                Column {
                    Text(tedarikci.ad, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text(tedarikci.telefon, fontSize = 13.sp, color = Color(0xFF8E8E93))
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
fun TedarikciDetayDialog(
    database: AppDatabase,
    tedarikci: Tedarikci,
    onDismiss: () -> Unit,
    onDuzenle: () -> Unit,
    onBakiyeDuzenle: () -> Unit,
    onOdemeYapGir: () -> Unit,
    onRaporGoster: () -> Unit
) {
    val urlAcici = rememberUrlAcici()
    val (bakiyeFormatli, renk) = tedarikciBakiyeMetniVeRengi(tedarikci.bakiye)

    val gecmisAlislar = remember(tedarikci.id) {
        database.appDatabaseQueries.selectAlisByTedarikciId(tedarikci.id).executeAsList().take(6)
    }

    val alisKalemleri = remember(gecmisAlislar) {
        gecmisAlislar.associate { alis ->
            alis.id to database.appDatabaseQueries.selectKalemlerByAlisId(alis.id).executeAsList()
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
                Text(tedarikci.ad, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDuzenle, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = Color(0xFF5856D6))
                }
            }
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                Text(tedarikci.adres, fontSize = 13.sp, color = Color(0xFF8E8E93))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Mevcut Durum: $bakiyeFormatli",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = renk
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    TedarikciIletisimButonu(
                        baslik = "Ödeme Yap (Tedarikçiye Öde)",
                        ikon = Icons.Default.Payments,
                        renk = Color(0xFF34C759),
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
                        renk = Color(0xFF5856D6),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onBakiyeDuzenle() }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        TedarikciIletisimButonu(
                            baslik = "Ara",
                            ikon = Icons.Default.Call,
                            renk = Color(0xFF5856D6),
                            modifier = Modifier.weight(1f),
                            onClick = { urlAcici.ac(telefonLinkOlustur(tedarikci.telefon)) }
                        )
                        TedarikciIletisimButonu(
                            baslik = "WhatsApp",
                            ikon = Icons.Default.Chat,
                            renk = Color(0xFF34C759),
                            modifier = Modifier.weight(1f),
                            onClick = { urlAcici.ac(whatsappLinkOlustur(tedarikci.telefon)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "GEÇMİŞ ALIŞLAR (SON 6)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8E8E93)
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (gecmisAlislar.isEmpty()) {
                    Text("Henüz kayıtlı alış yapılmadı", fontSize = 13.sp, color = Color(0xFF8E8E93))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        gecmisAlislar.forEach { alis ->
                            GecmisAlisKarti(
                                alis = alis,
                                kalemler = alisKalemleri[alis.id] ?: emptyList()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat", color = Color(0xFF5856D6), fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
fun TedarikciOdemeGirDialog(
    tedarikci: Tedarikci,
    onDismiss: () -> Unit,
    onKaydet: (tutar: Double) -> Unit
) {
    var tutarText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Ödeme İşle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text("${tedarikci.ad} firmasına nakit/havale ödeme yapıyorsunuz.", fontSize = 13.sp, color = Color(0xFF8E8E93))
                Text("Güncel Borcumuz: ₺${formatTedarikciCariIkiBasamak(tedarikci.bakiye)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                OutlinedTextField(
                    value = tutarText,
                    onValueChange = { tutarText = it },
                    label = { Text("Ödenen Tutar (₺)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
            }) { Text("Ödemeyi Kaydet", color = Color(0xFF34C759), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TarihAralikliAlisRaporDialog(
    database: AppDatabase,
    tedarikci: Tedarikci,
    onDismiss: () -> Unit
) {
    var baslangicSeciciAcik by remember { mutableStateOf(false) }
    var bitisSeciciAcik by remember { mutableStateOf(false) }

    val baslangicTarihState = rememberDatePickerState()
    val bitisTarihState = rememberDatePickerState()

    val tumAlislar = remember(tedarikci.id) {
        database.appDatabaseQueries.selectAlisByTedarikciId(tedarikci.id).executeAsList()
    }

    val filtrelenmisAlislar = remember(tumAlislar, baslangicTarihState.selectedDateMillis, bitisTarihState.selectedDateMillis) {
        tumAlislar.filter { alis ->
            val alisZamani = alis.tarih.toLongOrNull() ?: 0L
            val baslangicKosulu = baslangicTarihState.selectedDateMillis?.let { alisZamani >= it } ?: true
            val bitisKosulu = bitisTarihState.selectedDateMillis?.let { alisZamani <= (it + 86400000L) } ?: true
            baslangicKosulu && bitisKosulu
        }
    }

    val gruplanmisAlislar = remember(filtrelenmisAlislar) {
        filtrelenmisAlislar.groupBy { alis -> formatTarih(alis.tarih).substringBefore(" ") }
    }

    val toplamRaporTutari = remember(filtrelenmisAlislar) {
        filtrelenmisAlislar.sumOf { alis ->
            database.appDatabaseQueries.selectKalemlerByAlisId(alis.id).executeAsList().sumOf { it.toplam }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Tarih Bazlı Alış Detayı", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                Text("${tedarikci.ad} firmasından yapılan tarih filtreli alış raporu.", fontSize = 13.sp, color = Color(0xFF8E8E93))
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
                    Text("Dönem Toplam Alış:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text("₺${formatTedarikciCariIkiBasamak(toplamRaporTutari)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9500))
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (gruplanmisAlislar.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Seçilen aralıkta alış kaydı bulunamadı.", color = Color(0xFF8E8E93), fontSize = 13.sp)
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
                                    color = Color(0xFF5856D6),
                                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                                )
                            }
                            items(alislarListesi) { alis ->
                                val kalemler = database.appDatabaseQueries.selectKalemlerByAlisId(alis.id).executeAsList()
                                val alisSaati = formatTedarikciSaat(alis.tarih)

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
                                            text = "Alış",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF8E8E93)
                                        )
                                        Text(
                                            text = alisSaati,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF5856D6)
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
                                                text = "${kalem.urunAdi} (${kalem.adet} × ₺${formatTedarikciCariIkiBasamak(kalem.birimFiyat)})",
                                                fontSize = 13.sp,
                                                color = Color.Black,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "₺${formatTedarikciCariIkiBasamak(kalem.toplam)}",
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
            TextButton(onClick = onDismiss) { Text("Kapat", color = Color(0xFF5856D6), fontWeight = FontWeight.SemiBold) }
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
fun GecmisAlisKarti(alis: Alis, kalemler: List<AlisKalemi>) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Alış", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF3C3C43))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "₺${formatTedarikciCariIkiBasamak(alis.toplamTutar)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9500)
                    )
                    Text(formatTarih(alis.tarih), fontSize = 10.sp, color = Color(0xFF8E8E93))
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
                            "${kalem.urunAdi} — ${kalem.adet} × ₺${formatTedarikciCariIkiBasamak(kalem.birimFiyat)}",
                            fontSize = 12.sp,
                            color = Color(0xFF8E8E93),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "₺${formatTedarikciCariIkiBasamak(kalem.toplam)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF3C3C43)
                        )
                    }
                }
            }
        }
    }
}

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
fun TedarikciEkleDialog(
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
        title = { Text("Yeni Tedarikçi Kaydı", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = ad, onValueChange = { ad = it }, label = { Text("Firma / Ad Soyad") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = telefon,
                    onValueChange = { telefon = it },
                    label = { Text("Telefon (05XX XXX XX XX)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = adres, onValueChange = { adres = it }, label = { Text("Adres") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = bakiye,
                    onValueChange = { bakiye = it },
                    label = { Text("Mevcut Başlangıç Borcumuz (₺)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (ad.isNotBlank()) {
                    val temizBakiyeText = bakiye.replace(',', '.')
                    onKaydet(ad, telefon, adres, temizBakiyeText.toDoubleOrNull() ?: 0.0)
                }
            }) { Text("Kaydet", color = Color(0xFF5856D6), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}

@Composable
fun TedarikciDuzenleDialog(
    tedarikci: Tedarikci,
    onDismiss: () -> Unit,
    onKaydet: (ad: String, telefon: String, adres: String) -> Unit
) {
    var ad by remember { mutableStateOf(tedarikci.ad) }
    var telefon by remember { mutableStateOf(tedarikci.telefon) }
    var adres by remember { mutableStateOf(tedarikci.adres) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Tedarikçi Bilgilerini Düzenle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = ad, onValueChange = { ad = it }, label = { Text("Firma / Ad Soyad") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = telefon, onValueChange = { telefon = it }, label = { Text("Telefon") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = adres, onValueChange = { adres = it }, label = { Text("Adres") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text(
                    "Not: Borç bilgisi bu ekrandan değiştirilemez, mal alışı veya ödeme menülerinden otomatik güncellenmesi önerilir.",
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
            }) { Text("Kaydet", color = Color(0xFF5856D6), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}

@Composable
fun TedarikciBakiyeDuzenleDialog(
    tedarikci: Tedarikci,
    onDismiss: () -> Unit,
    onKaydet: (yeniBakiye: Double) -> Unit
) {
    var yeniBakiyeText by remember { mutableStateOf(tedarikci.bakiye.toString()) }
    val (bakiyeFormatli, _) = tedarikciBakiyeMetniVeRengi(tedarikci.bakiye)

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
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Dikkat: Gireceğiniz tutar firmaya olan güncel toplam net borcumuz sayılacaktır. Firmaya borçluysak düz rakam (Örn: 1500), eğer firmadan alacaklıysak eksi değer (Örn: -300) giriniz.",
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

private fun formatTedarikciSaat(epochMillisStr: String): String {
    val millis = epochMillisStr.toLongOrNull() ?: return ""

    val toplamSaniye = millis / 1000
    val gunIciSaniye = toplamSaniye % 86400

    val toplamSaatSaniye = gunIciSaniye + (3 * 3600)
    val duzeltilmisSaniye = if (toplamSaatSaniye >= 86400) toplamSaatSaniye - 86400 else toplamSaatSaniye

    val saat = (duzeltilmisSaniye / 3600).toString().padStart(2, '0')
    val dakika = ((duzeltilmisSaniye % 3600) / 60).toString().padStart(2, '0')

    return "$saat:$dakika"
}

private fun formatTedarikciCariIkiBasamak(deger: Double): String {
    val negatifMi = deger < 0
    val mutlakDeger = if (negatifMi) -deger else deger
    val yuvarlanmis = ((mutlakDeger * 100.0) + 0.5).toLong() / 100.0
    val tamKisim = yuvarlanmis.toLong()
    val kesirKisim = (((yuvarlanmis - tamKisim) * 100.0) + 0.5).toLong()
    val kesirStr = kesirKisim.toString().padStart(2, '0')
    return "${if (negatifMi) "-" else ""}$tamKisim.$kesirStr"
}