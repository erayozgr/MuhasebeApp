package com.eray.muhasebeapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eray.muhasebeapp.database.shared.AppDatabase
import com.eray.muhasebeapp.database.Musteri
import com.eray.muhasebeapp.database.UrunEntity
import com.eray.muhasebeapp.getEpochMillis

// Sepetteki bir kalemi temsil eden basit veri sınıfı
data class SepetKalemi(
    val urun: UrunEntity,
    val adet: Int
) {
    val toplam: Double get() = urun.satisFiyati * adet
}

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
    var odemeTuru by remember { mutableStateOf("Peşin") } // "Peşin" veya "Veresiye"

    var urunDialogAcikMi by remember { mutableStateOf(false) }
    var musteriDropdownAcikMi by remember { mutableStateOf(false) }
    var basariliMesajGoster by remember { mutableStateOf(false) }

    val toplamTutar = sepet.sumOf { it.toplam }

    Scaffold(
        containerColor = Color(0xFFF2F2F7),
        topBar = {
            TopAppBar(
                title = { Text("Satış", fontWeight = FontWeight.Bold, color = Color.Black) },
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

            // MÜŞTERİ SEÇİMİ
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
                                text = seciliMusteri?.ad ?: "Peşin Müşteri (Genel)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF8E8E93))
                    }
                    DropdownMenu(
                        expanded = musteriDropdownAcikMi,
                        onDismissRequest = { musteriDropdownAcikMi = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Peşin Müşteri (Genel)") },
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

            // ÖDEME TÜRÜ SEÇİMİ
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OdemeTuruButon(
                    baslik = "Peşin",
                    seciliMi = odemeTuru == "Peşin",
                    renk = Color(0xFF34C759),
                    modifier = Modifier.weight(1f)
                ) { odemeTuru = "Peşin" }

                OdemeTuruButon(
                    baslik = "Veresiye",
                    seciliMi = odemeTuru == "Veresiye",
                    renk = Color(0xFFFF9500),
                    modifier = Modifier.weight(1f),
                    aktifMi = seciliMusteri != null
                ) { if (seciliMusteri != null) odemeTuru = "Veresiye" }
            }

            if (odemeTuru == "Veresiye" && seciliMusteri == null) {
                Text(
                    "Veresiye satış için müşteri seçmelisin",
                    fontSize = 12.sp,
                    color = Color(0xFFFF3B30),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SEPET",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (sepet.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Sepet boş, ürün eklemek için + tuşuna bas", color = Color(0xFF8E8E93), fontSize = 14.sp)
                }
            } else {
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
                    // 🎯 DEĞİŞİKLİK: Listenin son elemanları FAB butonunun arkasında kalmasın diye Spacer boyutu büyütüldü.
                    item { Spacer(modifier = Modifier.height(88.dp)) }
                }
            }

            // ALT ÖZET VE TAMAMLA BUTONU
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
                            "₺${toplamTutar}",
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
                                    musteri = seciliMusteri,
                                    odemeTuru = odemeTuru
                                )
                                sepet = listOf()
                                seciliMusteri = null
                                odemeTuru = "Peşin"
                                basariliMesajGoster = true
                            }
                        },
                        enabled = sepet.isNotEmpty() && !(odemeTuru == "Veresiye" && seciliMusteri == null),
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
        // 🎯 DEĞİŞİKLİK: Sepet state'i güncel kalan stok hesabı için alt dialoga aktarıldı.
        UrunSecDialog(
            urunler = urunler,
            sepet = sepet,
            onDismiss = { urunDialogAcikMi = false },
            onEkle = { urun, adet ->
                val mevcutIndex = sepet.indexOfFirst { it.urun.id == urun.id }
                sepet = if (mevcutIndex >= 0) {
                    sepet.toMutableList().apply {
                        this[mevcutIndex] = this[mevcutIndex].copy(adet = this[mevcutIndex].adet + adet)
                    }
                } else {
                    sepet + SepetKalemi(urun, adet)
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
            text = { Text("Stok ve bakiye güncellemeleri yapıldı.") },
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
    musteri: Musteri?,
    odemeTuru: String
) {
    val toplamTutar = sepet.sumOf { it.toplam }
    val queries = database.appDatabaseQueries

    queries.transaction {
        queries.insertSatis(
            musteri?.id,
            musteri?.ad ?: "Peşin Müşteri",
            tarih = getEpochMillis().toString(),
            toplamTutar,
            odemeTuru
        )
        val satisId = queries.lastInsertId().executeAsOne()

        sepet.forEach { kalem ->
            queries.insertSatisKalemi(
                satisId,
                kalem.urun.id,
                kalem.urun.ad,
                kalem.adet.toLong(),
                kalem.urun.birim,
                kalem.urun.satisFiyati,
                kalem.toplam
            )
            val yeniStok = kalem.urun.stokAdedi - kalem.adet
            queries.updateUrunStok(yeniStok, kalem.urun.id)
        }

        if (odemeTuru == "Veresiye" && musteri != null) {
            queries.updateMusteriBakiye(musteri.bakiye + toplamTutar, musteri.id)
        }
    }
}

@Composable
fun OdemeTuruButon(
    baslik: String,
    seciliMi: Boolean,
    renk: Color,
    aktifMi: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(
                if (seciliMi) renk else Color.White,
                RoundedCornerShape(10.dp)
            )
            .clickable(enabled = aktifMi) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            baslik,
            color = if (seciliMi) Color.White else if (aktifMi) Color.Black else Color(0xFFC7C7CC),
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
    }
}

@Composable
fun SepetKalemKart(kalem: SepetKalemi, onSil: () -> Unit) {
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
                    "${kalem.adet} ${kalem.urun.birim} × ₺${kalem.urun.satisFiyati}",
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("₺${kalem.toplam}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF34C759))
                IconButton(onClick = onSil, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFFF3B30))
                }
            }
        }
    }
}

@Composable
fun UrunSecDialog(
    urunler: List<UrunEntity>,
    sepet: List<SepetKalemi>, // 🎯 DEĞİŞİKLİK: Sepet parametre olarak alındı.
    onDismiss: () -> Unit,
    onEkle: (UrunEntity, Int) -> Unit
) {
    var seciliUrun by remember { mutableStateOf<UrunEntity?>(null) }
    var adetText by remember { mutableStateOf("1") }
    var dropdownAcikMi by remember { mutableStateOf(false) }

    // 🎯 DEĞİŞİKLİK: Seçilen ürünün sepette halihazırda kaç adet olduğu bulunur.
    val sepettekiAdet = remember(seciliUrun, sepet) {
        sepet.find { it.urun.id == seciliUrun?.id }?.adet ?: 0
    }
    // 🎯 DEĞİŞİKLİK: Kalan gerçek satılabilir stok dinamik hesaplanır.
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
                            // 🎯 DEĞİŞİKLİK: Dropdown menü içinde de ürünlerin sepetteki durumu düşülerek kalan stok gösterilir.
                            val urunSepetAdet = sepet.find { it.urun.id == urun.id }?.adet ?: 0
                            val urunKalanStok = urun.stokAdedi - urunSepetAdet

                            DropdownMenuItem(
                                text = { Text("${urun.ad} (Kalan Stok: $urunKalanStok)") },
                                onClick = {
                                    seciliUrun = urun
                                    dropdownAcikMi = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = adetText,
                    onValueChange = { adetText = it },
                    label = { Text("Adet") },
                    singleLine = true
                )
                seciliUrun?.let {
                    //  DEĞİŞİKLİK: Kullanıcıya bilgilendirme metni detaylandırıldı.
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
                val adet = adetText.toIntOrNull() ?: 0
                val urun = seciliUrun
                // 🎯 DEĞİŞİKLİK: Doğrulama mekanizması veri tabanı stoku yerine "kalanStok" değerine bağlandı.
                if (urun != null && adet > 0 && adet <= kalanStok) {
                    onEkle(urun, adet)
                }
            }) { Text("Sepete Ekle", color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}