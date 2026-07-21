package com.eray.muhasebeapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eray.muhasebeapp.database.shared.AppDatabase
import com.eray.muhasebeapp.database.Tedarikci
import com.eray.muhasebeapp.database.UrunEntity
import com.eray.muhasebeapp.getEpochMillis

// Alış sepetindeki bir kalemi temsil eden veri sınıfı
data class AlisSepetKalemi(
    val urun: UrunEntity,
    val adet: Int
) {
    val toplam: Double get() = urun.alisFiyati * adet
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlisScreen(
    database: AppDatabase,
    onNavigateBack: () -> Unit
) {
    val urunler = remember { database.appDatabaseQueries.selectAllUrun().executeAsList() }
    val tedarikciler = remember { database.appDatabaseQueries.selectAllTedarikci().executeAsList() }

    var sepet by remember { mutableStateOf(listOf<AlisSepetKalemi>()) }
    var seciliTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var odemeTuru by remember { mutableStateOf("Peşin") } // "Peşin" veya "Veresiye"

    var urunDialogAcikMi by remember { mutableStateOf(false) }
    var tedarikciDropdownAcikMi by remember { mutableStateOf(false) }
    var basariliMesajGoster by remember { mutableStateOf(false) }

    val toplamTutar = sepet.sumOf { it.toplam }

    Scaffold(
        containerColor = Color(0xFFF2F2F7),
        topBar = {
            TopAppBar(
                title = { Text("Alış (Mal Alımı)", fontWeight = FontWeight.Bold, color = Color.Black) },
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
                containerColor = Color(0xFFFF9500), // Alış teması turuncu
                modifier = Modifier.padding(bottom = 110.dp) // Alt kartın üstünde kalması için güvenli mesafe
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ürün Ekle", tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // TEDARİKÇİ SEÇİMİ
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
                            .clickable { tedarikciDropdownAcikMi = true }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFF5856D6))
                            Text(
                                text = seciliTedarikci?.ad ?: "Peşin Tedarikçi (Genel)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF8E8E93))
                    }
                    DropdownMenu(
                        expanded = tedarikciDropdownAcikMi,
                        onDismissRequest = { tedarikciDropdownAcikMi = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Peşin Tedarikçi (Genel)") },
                            onClick = {
                                seciliTedarikci = null
                                tedarikciDropdownAcikMi = false
                            }
                        )
                        tedarikciler.forEach { tedarikci ->
                            DropdownMenuItem(
                                text = { Text(tedarikci.ad) },
                                onClick = {
                                    seciliTedarikci = tedarikci
                                    tedarikciDropdownAcikMi = false
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
                AlisOdemeButonu(
                    baslik = "Peşin",
                    seciliMi = odemeTuru == "Peşin",
                    renk = Color(0xFF34C759),
                    modifier = Modifier.weight(1f)
                ) { odemeTuru = "Peşin" }

                AlisOdemeButonu(
                    baslik = "Veresiye (Vadeli)",
                    seciliMi = odemeTuru == "Veresiye",
                    renk = Color(0xFFFF9500),
                    modifier = Modifier.weight(1f),
                    aktifMi = seciliTedarikci != null
                ) { if (seciliTedarikci != null) odemeTuru = "Veresiye" }
            }

            if (odemeTuru == "Veresiye" && seciliTedarikci == null) {
                Text(
                    "Vadeli alış için tedarikçi seçmelisin",
                    fontSize = 12.sp,
                    color = Color(0xFFFF3B30),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ALINACAK ÜRÜNLER (ALKIŞ SEPETİ)",
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
                        AlisSepetKart(
                            kalem = kalem,
                            onSil = { sepet = sepet - kalem }
                        )
                    }
                    // 🎯 DÜZELTME: Ürünlerin butonun ve alt kartın arkasında kalmasını önleyen Spacer alanı
                    item { Spacer(modifier = Modifier.height(96.dp)) }
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
                        Text("Maliyet Toplamı", fontSize = 15.sp, color = Color(0xFF3C3C43))
                        Text(
                            "₺${toplamTutar}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9500)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (sepet.isNotEmpty()) {
                                alisiTamamla(
                                    database = database,
                                    sepet = sepet,
                                    tedarikci = seciliTedarikci,
                                    odemeTuru = odemeTuru
                                )
                                sepet = listOf()
                                seciliTedarikci = null
                                odemeTuru = "Peşin"
                                basariliMesajGoster = true
                            }
                        },
                        enabled = sepet.isNotEmpty() && !(odemeTuru == "Veresiye" && seciliTedarikci == null),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5856D6)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Alışı Tamamla (Stoka Ekle)", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }
        }
    }

    if (urunDialogAcikMi) {
        AlisUrunSecDialog(
            urunler = urunler,
            onDismiss = { urunDialogAcikMi = false },
            onEkle = { urun, adet ->
                val mevcutIndex = sepet.indexOfFirst { it.urun.id == urun.id }
                sepet = if (mevcutIndex >= 0) {
                    sepet.toMutableList().apply {
                        this[mevcutIndex] = this[mevcutIndex].copy(adet = this[mevcutIndex].adet + adet)
                    }
                } else {
                    sepet + AlisSepetKalemi(urun, adet)
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
            title = { Text("Alış Kaydedildi") },
            text = { Text("Ürün stokları artırıldı ve tedarikçi hesap bakiye güncellemeleri yapıldı.") },
            confirmButton = {
                TextButton(onClick = { basariliMesajGoster = false }) {
                    Text("Tamam", color = Color(0xFF5856D6), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

private fun alisiTamamla(
    database: AppDatabase,
    sepet: List<AlisSepetKalemi>,
    tedarikci: Tedarikci?,
    odemeTuru: String
) {
    val toplamTutar = sepet.sumOf { it.toplam }
    val queries = database.appDatabaseQueries

    queries.transaction {
        queries.insertAlis(
            tedarikci?.id,
            tedarikci?.ad ?: "Peşin Tedarikçi",
            tarih = getEpochMillis().toString(),
            toplamTutar,
            odemeTuru
        )
        val alisId = queries.lastInsertIdAlis().executeAsOne()

        sepet.forEach { kalem ->
            queries.insertAlisKalemi(
                alisId,
                kalem.urun.id,
                kalem.urun.ad,
                kalem.adet.toLong(),
                kalem.urun.alisFiyati,
                kalem.toplam
            )
            // 🎯 BİLGİ: Mal alınca mevcut depodaki stok miktarı artar
            val yeniStok = kalem.urun.stokAdedi + kalem.adet
            queries.updateUrunStok(yeniStok, kalem.urun.id)
        }

        if (odemeTuru == "Veresiye" && tedarikci != null) {
            // Tedarikçiye olan borcumuz artar
            queries.updateTedarikciBakiye(tedarikci.bakiye + toplamTutar, tedarikci.id)
        }
    }
}

@Composable
fun AlisOdemeButonu(
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
fun AlisSepetKart(kalem: AlisSepetKalemi, onSil: () -> Unit) {
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
                    "${kalem.adet} ${kalem.urun.birim} × ₺${kalem.urun.alisFiyati}",
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("₺${kalem.toplam}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFF9500))
                IconButton(onClick = onSil, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFFF3B30))
                }
            }
        }
    }
}

@Composable
fun AlisUrunSecDialog(
    urunler: List<UrunEntity>,
    onDismiss: () -> Unit,
    onEkle: (UrunEntity, Int) -> Unit
) {
    var seciliUrun by remember { mutableStateOf<UrunEntity?>(null) }
    var adetText by remember { mutableStateOf("1") }
    var dropdownAcikMi by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Alış İçin Ürün Seç", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    OutlinedTextField(
                        value = seciliUrun?.ad ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ürün") },
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
                            // 🎯 DEĞİŞİKLİK: Ürün seçerken alış fiyatının yanında mevcut stok durumunu da gösterir
                            DropdownMenuItem(
                                text = { Text("${urun.ad} (Mevcut Stok: ${urun.stokAdedi} • ₺${urun.alisFiyati})") },
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
                    label = { Text("Alınacak Adet") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                seciliUrun?.let {
                    // 🎯 DEĞİŞİKLİK: Bilgilendirme satırında alış fiyatı ve stok detaylı gösterilir
                    Text(
                        text = "Mevcut Depo Stoku: ${it.stokAdedi} ${it.birim} | Alış Maliyeti: ₺${it.alisFiyati}",
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val adet = adetText.toIntOrNull() ?: 0
                val urun = seciliUrun
                if (urun != null && adet > 0) {
                    onEkle(urun, adet)
                }
            }) { Text("Sepete Ekle", color = Color(0xFF5856D6), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}