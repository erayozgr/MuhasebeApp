package com.eray.muhasebeapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eray.muhasebeapp.database.shared.AppDatabase
import com.eray.muhasebeapp.database.Musteri
import com.eray.muhasebeapp.database.UrunEntity
import com.eray.muhasebeapp.database.Satis
import com.eray.muhasebeapp.database.SatisKalemi
import com.eray.muhasebeapp.getEpochMillis
import com.eray.muhasebeapp.formatTarih
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SepetKalemi(
    val urun: UrunEntity,
    val adet: Int,
    val satisFiyati: Double
) {
    val toplam: Double get() = satisFiyati * adet
}

data class GecmisSatisKaydi(
    val satis: Satis,
    val kalemler: List<SatisKalemi>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatisScreen(
    database: AppDatabase,
    onNavigateBack: () -> Unit
) {
    val urunler = remember { database.appDatabaseQueries.selectAllUrun().executeAsList() }
    val musteriler = remember { database.appDatabaseQueries.selectAllMusteri().executeAsList() }

    var sepet by remember { mutableStateOf(listOf<SepetKalemi>()) }
    var seciliMusteri by remember { mutableStateOf<Musteri?>(null) }

    var urunDialogAcikMi by remember { mutableStateOf(false) }
    var musteriDropdownAcikMi by remember { mutableStateOf(false) }
    var basariliMesajGoster by remember { mutableStateOf(false) }

    val toplamTutar = sepet.sumOf { it.toplam }

    var horizontalDragAccumulator by remember { mutableStateOf(0f) }

    var gecmisTumSatislarHam by remember { mutableStateOf<List<Satis>?>(null) }
    var gecmisLimit by remember { mutableStateOf(20) }
    var gecmisSatislar by remember { mutableStateOf(listOf<GecmisSatisKaydi>()) }
    var gecmisYukleniyor by remember { mutableStateOf(false) }
    var gecmisDahaFazlaVar by remember { mutableStateOf(true) }

    LaunchedEffect(sepet.isEmpty(), gecmisLimit) {
        if (sepet.isEmpty()) {
            gecmisYukleniyor = true
            withContext(Dispatchers.Default) {
                if (gecmisTumSatislarHam == null) {
                    gecmisTumSatislarHam = database.appDatabaseQueries.selectAllSatis().executeAsList()
                }
                val ham = gecmisTumSatislarHam ?: emptyList()
                val limitli = ham.take(gecmisLimit)
                gecmisDahaFazlaVar = ham.size > gecmisLimit
                gecmisSatislar = limitli.map { s ->
                    val kalemler = database.appDatabaseQueries.selectKalemlerBySatisId(s.id).executeAsList()
                    GecmisSatisKaydi(s, kalemler)
                }
            }
            gecmisYukleniyor = false
        }
    }

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
                title = { Text("Satış İşlemi", fontWeight = FontWeight.Bold, color = Color.Black) },
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
                onClick = { urunDialogAcikMi = true },
                containerColor = Color(0xFF34C759),
                modifier = Modifier.padding(bottom = 110.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ürün Ekle", tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 8.dp)
            ) {
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { musteriDropdownAcikMi = true }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF007AFF))
                            Text(
                                text = seciliMusteri?.ad ?: "Müşteri Seçin (İsteğe Bağlı)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (seciliMusteri == null) Color(0xFF8E8E93) else Color.Black
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF8E8E93))
                    }
                    DropdownMenu(
                        expanded = musteriDropdownAcikMi,
                        onDismissRequest = { musteriDropdownAcikMi = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Genel Müşteri") },
                            onClick = {
                                seciliMusteri = null
                                musteriDropdownAcikMi = false
                            }
                        )
                        musteriler.forEach { musteri ->
                            DropdownMenuItem(
                                text = { Text(musteri.ad) },
                                onClick = {
                                    seciliMusteri = musteri
                                    musteriDropdownAcikMi = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (sepet.isEmpty()) {
                Text(
                    text = "GEÇMİŞ SATIŞLAR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                if (gecmisSatislar.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (gecmisYukleniyor) "Yükleniyor..." else "Sepet boş, ürün eklemek için + tuşuna bas",
                            color = Color(0xFF8E8E93),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(gecmisSatislar) { kayit ->
                            GecmisSatisKart(kayit)
                        }
                        if (gecmisDahaFazlaVar) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TextButton(
                                        onClick = { gecmisLimit += 20 },
                                        enabled = !gecmisYukleniyor,
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF007AFF))
                                    ) {
                                        Text(
                                            if (gecmisYukleniyor) "Yükleniyor..." else "Daha Fazla Yükle (+20)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(88.dp)) }
                    }
                }
            } else {
                Text(
                    text = "SEPET",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sepet) { kalem ->
                        SepetKalemKart(
                            kalem = kalem,
                            onSil = { sepet = sepet - kalem }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(88.dp)) }
                }
            }

            Card(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Genel Toplam", fontSize = 15.sp, color = Color(0xFF3C3C43))
                        Text(
                            "₺${formatSatisFiyatiIkiBasamak(toplamTutar)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34C759)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (sepet.isNotEmpty()) {
                                satisiTamamla(
                                    database = database,
                                    sepet = sepet,
                                    musteri = seciliMusteri
                                )
                                sepet = listOf()
                                seciliMusteri = null
                                basariliMesajGoster = true
                                gecmisTumSatislarHam = null
                                gecmisLimit = 20
                            }
                        },
                        enabled = sepet.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Satışı Tamamla", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }
        }
    }

    if (urunDialogAcikMi) {
        UrunSecDialog(
            urunler = urunler,
            sepet = sepet,
            onDismiss = { urunDialogAcikMi = false },
            onEkle = { urun, adet, girilenFiyat ->
                val mevcutIndex = sepet.indexOfFirst { it.urun.id == urun.id && it.satisFiyati == girilenFiyat }
                sepet = if (mevcutIndex >= 0) {
                    sepet.toMutableList().apply {
                        this[mevcutIndex] = this[mevcutIndex].copy(adet = this[mevcutIndex].adet + adet)
                    }
                } else {
                    sepet + SepetKalemi(urun, adet, girilenFiyat)
                }
                urunDialogAcikMi = false
            }
        )
    }

    if (basariliMesajGoster) {
        AlertDialog(
            onDismissRequest = { basariliMesajGoster = false },
            containerColor = Color.White,
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34C759)) },
            title = { Text("Satış Tamamlandı") },
            text = {
                Text("Satış kaydı başarıyla oluşturuldu ve stoklar güncellendi.")
            },
            confirmButton = {
                TextButton(onClick = { basariliMesajGoster = false }) {
                    Text("Tamam", color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

private fun satisiTamamla(
    database: AppDatabase,
    sepet: List<SepetKalemi>,
    musteri: Musteri?
) {
    val toplamTutar = sepet.sumOf { it.toplam }
    val queries = database.appDatabaseQueries

    queries.transaction {
        queries.insertSatis(
            musteri?.id,
            musteri?.ad ?: "Genel Müşteri",
            tarih = getEpochMillis().toString(),
            toplamTutar
        )
        val satisId = queries.lastInsertId().executeAsOne()

        sepet.forEach { kalem ->
            queries.insertSatisKalemi(
                satisId,
                kalem.urun.id,
                kalem.urun.ad,
                kalem.adet.toLong(),
                kalem.urun.birim,
                kalem.satisFiyati,
                kalem.toplam
            )
            val yeniStok = kalem.urun.stokAdedi - kalem.adet
            queries.updateUrunStok(yeniStok, kalem.urun.id)
        }

        if (musteri != null) {
            queries.updateMusteriBakiye(musteri.bakiye + toplamTutar, musteri.id)
        }
    }
}

@Composable
private fun SepetKalemKart(kalem: SepetKalemi, onSil: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(kalem.urun.ad, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                Text(
                    "${kalem.adet} ${kalem.urun.birim} × ₺${formatSatisFiyatiIkiBasamak(kalem.satisFiyati)}",
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("₺${formatSatisFiyatiIkiBasamak(kalem.toplam)}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF34C759))
                IconButton(onClick = onSil, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFFF3B30))
                }
            }
        }
    }
}

@Composable
private fun GecmisSatisKart(kayit: GecmisSatisKaydi) {
    val urunListesi = kayit.kalemler.joinToString(", ") { "${it.urunAdi} x${it.adet}" }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(Color(0xFF34C759).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF34C759))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(kayit.satis.musteriAdi, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                if (urunListesi.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(urunListesi, fontSize = 13.sp, color = Color(0xFF8E8E93), lineHeight = 16.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "₺${formatSatisFiyatiIkiBasamak(kayit.satis.toplamTutar)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF34C759)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(formatTarih(kayit.satis.tarih), fontSize = 11.sp, color = Color(0xFF8E8E93))
            }
        }
    }
}

@Composable
private fun UrunSecDialog(
    urunler: List<UrunEntity>,
    sepet: List<SepetKalemi>,
    onDismiss: () -> Unit,
    onEkle: (UrunEntity, Int, Double) -> Unit
) {
    var seciliUrun by remember { mutableStateOf<UrunEntity?>(null) }
    var adetText by remember { mutableStateOf("") }
    var fiyatText by remember { mutableStateOf("") }
    var dropdownAcikMi by remember { mutableStateOf(false) }

    val sepettekiAdet = remember(seciliUrun, sepet) {
        sepet.filter { it.urun.id == seciliUrun?.id }.sumOf { it.adet }
    }
    val kalanStok = (seciliUrun?.stokAdedi ?: 0L) - sepettekiAdet

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Ürün Ekle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    OutlinedTextField(
                        value = seciliUrun?.ad ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ürün Seç") },
                        modifier = Modifier.fillMaxWidth().clickable { dropdownAcikMi = true },
                        trailingIcon = {
                            IconButton(onClick = { dropdownAcikMi = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = dropdownAcikMi,
                        onDismissRequest = { dropdownAcikMi = false }
                    ) {
                        urunler.forEach { urun ->
                            val urunSepetAdet = sepet.filter { it.urun.id == urun.id }.sumOf { it.adet }
                            val urunKalanStok = urun.stokAdedi - urunSepetAdet

                            DropdownMenuItem(
                                text = { Text("${urun.ad} (Kalan Stok: $urunKalanStok)") },
                                onClick = {
                                    seciliUrun = urun
                                    fiyatText = ""
                                    adetText = ""
                                    dropdownAcikMi = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = fiyatText,
                    onValueChange = { fiyatText = it },
                    label = { Text("Satış Fiyatı (₺)") },
                    placeholder = {
                        Text(text = seciliUrun?.let { "Öneri: ₺${it.satisFiyati}" } ?: "0.0")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), // 🎯 Güncellendi
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = adetText,
                    onValueChange = { adetText = it },
                    label = { Text("Adet") },
                    placeholder = { Text(text = "Öneri: 1") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // 🎯 Güncellendi
                    modifier = Modifier.fillMaxWidth()
                )

                seciliUrun?.let {
                    Text(
                        text = "Toplam Stok: ${it.stokAdedi} | Sepette: $sepettekiAdet | Kalan Açıktaki Stok: $kalanStok",
                        fontSize = 12.sp,
                        color = if (kalanStok <= 0) Color(0xFFFF3B30) else Color(0xFF8E8E93),
                        fontWeight = if (kalanStok <= 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val urun = seciliUrun
                val adet = if (adetText.isBlank()) 1 else (adetText.toIntOrNull() ?: 0)
                val temizFiyatText = fiyatText.replace(',', '.')
                val girilenFiyat = if (temizFiyatText.isBlank()) (urun?.satisFiyati ?: 0.0) else (temizFiyatText.toDoubleOrNull() ?: 0.0)

                if (urun != null && adet > 0 && adet <= kalanStok && girilenFiyat > 0.0) {
                    onEkle(urun, adet, girilenFiyat)
                }
            }) { Text("Sepete Ekle", color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}

private fun formatSatisFiyatiIkiBasamak(deger: Double): String {
    val negatifMi = deger < 0
    val mutlakDeger = if (negatifMi) -deger else deger
    val yuvarlanmis = ((mutlakDeger * 100.0) + 0.5).toLong() / 100.0
    val tamKisim = yuvarlanmis.toLong()
    val kesirKisim = (((yuvarlanmis - tamKisim) * 100.0) + 0.5).toLong()
    val kesirStr = kesirKisim.toString().padStart(2, '0')
    return "${if (negatifMi) "-" else ""}$tamKisim.$kesirStr"
}