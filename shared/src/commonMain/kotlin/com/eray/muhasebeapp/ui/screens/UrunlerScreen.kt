package com.eray.muhasebeapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
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
import com.eray.muhasebeapp.database.UrunEntity

fun Double.toParaFormat(): String {
    val tamKisim = this.toInt()
    val kurusKisimi = ((this - tamKisim) * 100).toInt()
    val kurusStr = if (kurusKisimi < 10) "0$kurusKisimi" else "$kurusKisimi"
    return "$tamKisim,$kurusStr"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrunlerScreen(
    database: AppDatabase,
    onNavigateBack: () -> Unit
) {
    var aramaMetni by remember { mutableStateOf("") }
    var urunEklemeDialogGoster by remember { mutableStateOf(false) }
    var duzenlenecekUrun by remember { mutableStateOf<UrunEntity?>(null) }
    var silinecekUrun by remember { mutableStateOf<UrunEntity?>(null) }

    var refreshTrigger by remember { mutableStateOf(0) }

    // 🎯 Kritik Stok Filtresi Açık mı Kapsalı mı State'i
    var kritikStokFiltresiAcik by remember { mutableStateOf(false) }

    // Ürün listesini hem aramaya, hem refresh tetikleyicisine hem de kritik stok filtresine bağladık
    val urunListesi by remember(aramaMetni, refreshTrigger, kritikStokFiltresiAcik) {
        derivedStateOf {
            val hamListe = if (aramaMetni.isEmpty()) {
                database.appDatabaseQueries.selectAllUrun().executeAsList()
            } else {
                database.appDatabaseQueries.searchUrun(aramaMetni).executeAsList()
            }

            // Eğer filtre açıksa sadece stoğu 5 ve altında olanları filtrele
            if (kritikStokFiltresiAcik) {
                hamListe.filter { it.stokAdedi <= 5L }
            } else {
                hamListe
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F7))) {

        // --- Üst Bar ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "Geri",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = if (kritikStokFiltresiAcik) "Kritik Stok Listesi" else "Ürün Listesi",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (kritikStokFiltresiAcik) Color(0xFFFF3B30) else Color.Black
            )

            IconButton(
                onClick = { urunEklemeDialogGoster = true },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Ekle", tint = Color(0xFF007AFF))
            }
        }

        // --- Özet Kartları ---
        val tumUrunler = remember(refreshTrigger) {
            database.appDatabaseQueries.selectAllUrun().executeAsList()
        }
        val toplamStokDeger = tumUrunler.sumOf { it.alisFiyati * it.stokAdedi }
        val kritikStokSayisi = tumUrunler.count { it.stokAdedi <= 5L }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OzetKart(
                baslik = "Toplam Ürün",
                deger = "${tumUrunler.size}",
                renk = Color(0xFF007AFF),
                modifier = Modifier.weight(1f)
            )
            OzetKart(
                baslik = "Stok Değeri",
                deger = "₺${toplamStokDeger.toParaFormat()}",
                renk = Color(0xFF34C759),
                modifier = Modifier.weight(1f)
            )

            // 🎯 Kritik Stok Özet Kartı artık tıklanabilir interaktif bir buton oldu
            OzetKart(
                baslik = if (kritikStokFiltresiAcik) "Filtreyi Kaldır" else "Kritik Stok",
                deger = "$kritikStokSayisi",
                // Aktifken kırmızı, pasifken duruma göre gri/kırmızı renk alır
                renk = if (kritikStokFiltresiAcik) Color.White else (if (kritikStokSayisi > 0) Color(0xFFFF3B30) else Color(0xFF8E8E93)),
                containerColor = if (kritikStokFiltresiAcik) Color(0xFFFF3B30) else Color.White,
                modifier = Modifier
                    .weight(1f)
                    .clickable { kritikStokFiltresiAcik = !kritikStokFiltresiAcik } // Tıklanınca durumu tersine çevirir
            )
        }

        // --- Arama Çubuğu ---
        TextField(
            value = aramaMetni,
            onValueChange = { aramaMetni = it },
            placeholder = { Text("Ürün adı veya barkod ara...", color = Color(0xFF8E8E93), fontSize = 15.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ara", tint = Color(0xFF8E8E93)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFE3E3E8),
                unfocusedContainerColor = Color(0xFFE3E3E8),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        // --- Ürün Listesi ---
        if (urunListesi.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = when {
                        kritikStokFiltresiAcik -> "Kritik stokta ürün bulunmuyor."
                        aramaMetni.isEmpty() -> "Ürün bulunamadı. Eklemek için + butonuna basın."
                        else -> "\"$aramaMetni\" için sonuç yok."
                    },
                    color = Color(0xFF8E8E93),
                    fontSize = 15.sp
                )
            }
        } else {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)
            ) {
                LazyColumn {
                    itemsIndexed(urunListesi) { index, urun ->
                        UrunSatiri(
                            urun = urun,
                            onDuzenle = { duzenlenecekUrun = urun },
                            onSil = { silinecekUrun = urun }
                        )
                        if (index < urunListesi.lastIndex) {
                            HorizontalDivider(
                                color = Color(0xFFC6C6C8),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Yeni Ürün Ekleme Dialog ---
    if (urunEklemeDialogGoster) {
        UrunFormDialog(
            baslik = "Yeni Ürün Ekle",
            onKaydet = { barkod, ad, alis, satis, stok, birim, kdv ->
                database.appDatabaseQueries.insertUrun(
                    barkod = barkod,
                    ad = ad,
                    alisFiyati = alis,
                    satisFiyati = satis,
                    stokAdedi = stok,
                    birim = birim,
                    kdvOrani = kdv
                )
                refreshTrigger++
                urunEklemeDialogGoster = false
            },
            onIptal = { urunEklemeDialogGoster = false }
        )
    }

    // --- Ürün Düzenleme Dialog ---
    duzenlenecekUrun?.let { urun ->
        UrunFormDialog(
            baslik = "Ürünü Düzenle",
            mevcutBarkod = urun.barkod,
            mevcutAd = urun.ad,
            mevcutAlis = urun.alisFiyati.toString(),
            mevcutSatis = urun.satisFiyati.toString(),
            mevcutStok = urun.stokAdedi.toString(),
            mevcutBirim = urun.birim,
            mevcutKdv = urun.kdvOrani.toString(),
            onKaydet = { barkod, ad, alis, satis, stok, birim, kdv ->
                database.appDatabaseQueries.updateUrun(
                    barkod = barkod,
                    ad = ad,
                    alisFiyati = alis,
                    satisFiyati = satis,
                    stokAdedi = stok,
                    birim = birim,
                    kdvOrani = kdv,
                    id = urun.id
                )
                refreshTrigger++
                duzenlenecekUrun = null
            },
            onIptal = { duzenlenecekUrun = null }
        )
    }

    // --- Silme Onay Dialog ---
    silinecekUrun?.let { urun ->
        AlertDialog(
            onDismissRequest = { silinecekUrun = null },
            title = { Text("Ürünü Sil", fontWeight = FontWeight.Bold) },
            text = { Text("\"${urun.ad}\" ürününü silmek istediğinize emin misiniz? Bu işlem geri alınamaz.") },
            confirmButton = {
                Button(
                    onClick = {
                        database.appDatabaseQueries.deleteUrun(urun.id)
                        refreshTrigger++
                        silinecekUrun = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
                ) { Text("Sil", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { silinecekUrun = null }) {
                    Text("Vazgeç", color = Color(0xFF007AFF))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrunFormDialog(
    baslik: String,
    mevcutBarkod: String = "",
    mevcutAd: String = "",
    mevcutAlis: String = "",
    mevcutSatis: String = "",
    mevcutStok: String = "",
    mevcutBirim: String = "Adet",
    mevcutKdv: String = "20",
    onKaydet: (barkod: String, ad: String, alis: Double, satis: Double, stok: Long, birim: String, kdv: Long) -> Unit,
    onIptal: () -> Unit
) {
    var ad by remember { mutableStateOf(mevcutAd) }
    var barkod by remember { mutableStateOf(mevcutBarkod) }
    var alis by remember { mutableStateOf(mevcutAlis) }
    var satis by remember { mutableStateOf(mevcutSatis) }
    var stok by remember { mutableStateOf(mevcutStok) }
    var birim by remember { mutableStateOf(mevcutBirim) }
    var kdv by remember { mutableStateOf(mevcutKdv) }
    var adHata by remember { mutableStateOf(false) }

    val birimSecenekleri = listOf("Adet", "Kg", "Lt", "Mt", "Kutu", "Paket")
    val kdvSecenekleri = listOf("0", "1", "10", "20")
    var birimMenuAcik by remember { mutableStateOf(false) }
    var kdvMenuAcik by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onIptal,
        title = { Text(baslik, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = ad,
                    onValueChange = { ad = it; adHata = false },
                    label = { Text("Ürün Adı *") },
                    isError = adHata,
                    supportingText = if (adHata) {{ Text("Ürün adı zorunludur") }} else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = barkod,
                    onValueChange = { barkod = it },
                    label = { Text("Barkod No") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = alis,
                        onValueChange = { alis = it },
                        label = { Text("Alış Fiyatı (₺)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = satis,
                        onValueChange = { satis = it },
                        label = { Text("Satış Fiyatı (₺)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stok,
                        onValueChange = { stok = it },
                        label = { Text("Stok Adedi") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = "%$kdv",
                            onValueChange = {},
                            label = { Text("KDV Oranı") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { kdvMenuAcik = true })

                        DropdownMenu(
                            expanded = kdvMenuAcik,
                            onDismissRequest = { kdvMenuAcik = false }
                        ) {
                            kdvSecenekleri.forEach { oran ->
                                DropdownMenuItem(
                                    text = { Text("%$oran") },
                                    onClick = { kdv = oran; kdvMenuAcik = false }
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = birim,
                        onValueChange = {},
                        label = { Text("Birim") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { birimMenuAcik = true })

                    DropdownMenu(
                        expanded = birimMenuAcik,
                        onDismissRequest = { birimMenuAcik = false }
                    ) {
                        birimSecenekleri.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b) },
                                onClick = { birim = b; birimMenuAcik = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (ad.isEmpty()) {
                        adHata = true
                        return@Button
                    }
                    onKaydet(
                        barkod,
                        ad,
                        alis.toDoubleOrNull() ?: 0.0,
                        satis.toDoubleOrNull() ?: 0.0,
                        stok.toLongOrNull() ?: 0L,
                        birim,
                        kdv.toLongOrNull() ?: 20L
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
            ) { Text("Kaydet", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onIptal) { Text("Vazgeç", color = Color(0xFFFF3B30)) }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}

// 🎯 Dinamik Arka Plan Rengi Alan Geliştirilmiş OzetKart
@Composable
fun OzetKart(
    baslik: String,
    deger: String,
    renk: Color,
    containerColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = deger, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = renk)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = baslik,
                fontSize = 11.sp,
                color = if (containerColor == Color.White) Color(0xFF8E8E93) else Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun UrunSatiri(
    urun: UrunEntity,
    onDuzenle: () -> Unit,
    onSil: () -> Unit
) {
    val stokUyarisi = urun.stokAdedi <= 5L
    val stokRengi = if (stokUyarisi) Color(0xFFFF3B30) else Color(0xFF8E8E93)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = urun.ad, fontSize = 17.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "KDV: %${urun.kdvOrani}", fontSize = 12.sp, color = Color(0xFF8E8E93))
                Text(text = "•", fontSize = 12.sp, color = Color(0xFFC6C6C8))
                Text(text = "Barkod: ${urun.barkod.ifEmpty { "-" }}", fontSize = 12.sp, color = Color(0xFF8E8E93))
            }
        }

        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 4.dp)) {
            Text(
                text = "₺${urun.satisFiyati.toParaFormat()}",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Stok: ${urun.stokAdedi} ${urun.birim}",
                fontSize = 13.sp,
                color = stokRengi
            )
        }

        Row {
            IconButton(onClick = onDuzenle) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Düzenle",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onSil) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Sil",
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}