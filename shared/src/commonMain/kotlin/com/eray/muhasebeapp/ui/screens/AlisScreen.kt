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
import com.eray.muhasebeapp.database.Tedarikci
import com.eray.muhasebeapp.database.UrunEntity
import com.eray.muhasebeapp.getEpochMillis

data class AlisSepetKalemi(
    val urun: UrunEntity,
    val adet: Int,
    val alisFiyati: Double
) {
    val toplam: Double get() = alisFiyati * adet
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

    var urunDialogAcikMi by remember { mutableStateOf(false) }
    var tedarikciDropdownAcikMi by remember { mutableStateOf(false) }
    var basariliMesajGoster by remember { mutableStateOf(false) }

    val toplamTutar = sepet.sumOf { it.toplam }

    // Sağa kaydırarak geri dönme (Swipe Back) takibi için drag durumu
    var horizontalDragAccumulator by remember { mutableStateOf(0f) }

    Scaffold(
        containerColor = Color(0xFFF2F2F7),
        modifier = Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = { horizontalDragAccumulator = 0f },
                onDragEnd = {
                    // Sağa doğru yeterli miktarda kaydırıldıysa ana menüye dön
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
                containerColor = Color(0xFFFF9500),
                modifier = Modifier.padding(bottom = 110.dp)
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
                                text = seciliTedarikci?.ad ?: "Peşin Tedarikçi (Genel - İsteğe Bağlı)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (seciliTedarikci == null) Color(0xFF8E8E93) else Color.Black
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

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ALINACAK ÜRÜNLER (ALIŞ SEPETİ)",
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
                            // 🎯 DEĞİŞTİRİLDİ: Genel maliyet toplamı iki basamak yapıldı
                            "₺${formatAlisFiyatiIkiBasamak(toplamTutar)}",
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
                                    tedarikci = seciliTedarikci
                                )
                                sepet = listOf()
                                seciliTedarikci = null
                                basariliMesajGoster = true
                            }
                        },
                        enabled = sepet.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5856D6)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        val butonYazisi = if (seciliTedarikci == null) "Alışı Tamamla" else "Alışı Vadeli Tamamla"
                        Text(butonYazisi, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }
        }
    }

    if (urunDialogAcikMi) {
        AlisUrunSecDialog(
            urunler = urunler,
            onDismiss = { urunDialogAcikMi = false },
            onEkle = { urun, adet, girilenFiyat ->
                val mevcutIndex = sepet.indexOfFirst { it.urun.id == urun.id && it.alisFiyati == girilenFiyat }
                sepet = if (mevcutIndex >= 0) {
                    sepet.toMutableList().apply {
                        this[mevcutIndex] = this[mevcutIndex].copy(adet = this[mevcutIndex].adet + adet)
                    }
                } else {
                    sepet + AlisSepetKalemi(urun, adet, girilenFiyat)
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
            text = {
                val detayMetni = if (seciliTedarikci == null) "Ürün stokları artırıldı." else "Ürün stokları artırıldı ve tedarikçi borç bakiyesi güncellendi."
                Text(detayMetni)
            },
            confirmButton = {
                TextButton(onClick = { basariliMesajGoster = false }) {
                    Text("Tamam", color = Color(0xFF5856D6), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

private fun satisiTamamla(
    database: AppDatabase,
    sepet: List<AlisSepetKalemi>,
    tedarikci: Tedarikci?
) {
    // Eğer projede çağrılan bu satisiTamamla yerine alisiTamamla kullanılıyorsa, alisiTamamla çağrısını koruyoruz.
}

private fun alisiTamamla(
    database: AppDatabase,
    sepet: List<AlisSepetKalemi>,
    tedarikci: Tedarikci?
) {
    val toplamTutar = sepet.sumOf { it.toplam }
    val queries = database.appDatabaseQueries

    queries.transaction {
        queries.insertAlis(
            tedarikci?.id,
            tedarikci?.ad ?: "Peşin Tedarikçi",
            tarih = getEpochMillis().toString(),
            toplamTutar
        )
        val alisId = queries.lastInsertIdAlis().executeAsOne()

        sepet.forEach { kalem ->
            queries.insertAlisKalemi(
                alisId,
                kalem.urun.id,
                kalem.urun.ad,
                kalem.adet.toLong(),
                kalem.alisFiyati,
                kalem.toplam
            )
            val yeniStok = kalem.urun.stokAdedi + kalem.adet
            queries.updateUrunStok(yeniStok, kalem.urun.id)
        }

        if (tedarikci != null) {
            queries.updateTedarikciBakiye(tedarikci.bakiye + toplamTutar, tedarikci.id)
        }
    }
}

// 🎯 ÇAKIŞMALARI ENGELLEMEK İÇİN YARDIMCI BİLEŞENLERE PRIVATE EKLENMİŞTİR
@Composable
private fun AlisSepetKart(kalem: AlisSepetKalemi, onSil: () -> Unit) {
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
                    // 🎯 DEĞİŞTİRİLDİ: Sepet içindeki tekil alış maliyeti formatlandı
                    "${kalem.adet} ${kalem.urun.birim} × ₺${formatAlisFiyatiIkiBasamak(kalem.alisFiyati)}",
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("₺${formatAlisFiyatiIkiBasamak(kalem.toplam)}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFF9500))
                IconButton(onClick = onSil, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFFF3B30))
                }
            }
        }
    }
}

@Composable
private fun AlisUrunSecDialog(
    urunler: List<UrunEntity>,
    onDismiss: () -> Unit,
    onEkle: (UrunEntity, Int, Double) -> Unit
) {
    var seciliUrun by remember { mutableStateOf<UrunEntity?>(null) }
    var adetText by remember { mutableStateOf("") }
    var fiyatText by remember { mutableStateOf("") }
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
                            // 🎯 DEĞİŞTİRİLDİ: Açılır menü listesindeki öneri fiyatı iki basamak yapıldı
                            DropdownMenuItem(
                                text = { Text("${urun.ad} (Mevcut Stok: ${urun.stokAdedi} • ₺${formatAlisFiyatiIkiBasamak(urun.alisFiyati)})") },
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
                    label = { Text("Alış Fiyatı (₺)") },
                    placeholder = {
                        Text(text = seciliUrun?.let { "Öneri: ₺${formatAlisFiyatiIkiBasamak(it.alisFiyati)}" } ?: "0.00")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = adetText,
                    onValueChange = { adetText = it },
                    label = { Text("Alınacak Adet") },
                    placeholder = { Text(text = "Öneri: 1") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                seciliUrun?.let {
                    // 🎯 DEĞİŞTİRİLDİ: Bilgilendirme alt metnindeki maliyet değeri iki basamak yapıldı
                    Text(
                        text = "Mevcut Depo Stoku: ${it.stokAdedi} ${it.birim} | Kayıtlı Alış Maliyeti: ₺${formatAlisFiyatiIkiBasamak(it.alisFiyati)}",
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val urun = seciliUrun

                val adet = if (adetText.isBlank()) 1 else (adetText.toIntOrNull() ?: 0)
                val girilenFiyat = if (fiyatText.isBlank()) (urun?.alisFiyati ?: 0.0) else (fiyatText.toDoubleOrNull() ?: 0.0)

                if (urun != null && adet > 0 && girilenFiyat > 0.0) {
                    onEkle(urun, adet, girilenFiyat)
                }
            }) { Text("Sepete Ekle", color = Color(0xFF5856D6), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}

// KMP uyumlu, kuruş hassasiyetini virgülden sonra net 2 basamağa sabitleyen yardımcı fonksiyon
private fun formatAlisFiyatiIkiBasamak(deger: Double): String {
    val negatifMi = deger < 0
    val mutlakDeger = if (negatifMi) -deger else deger
    val yuvarlanmis = ((mutlakDeger * 100.0) + 0.5).toLong() / 100.0
    val tamKisim = yuvarlanmis.toLong()
    val kesirKisim = (((yuvarlanmis - tamKisim) * 100.0) + 0.5).toLong()
    val kesirStr = kesirKisim.toString().padStart(2, '0')
    return "${if (negatifMi) "-" else ""}$tamKisim.$kesirStr"
}