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
import com.eray.muhasebeapp.data.model.*
import com.eray.muhasebeapp.data.network.ApiService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StokScreen(
    apiService: ApiService,
    onNavigateBack: () -> Unit
) {
    var yenilemeTetikleyici by remember { mutableStateOf(0) }
    var urunler by remember { mutableStateOf<List<Urun>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var hataMesaji by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(yenilemeTetikleyici) {
        loading = true
        try {
            urunler = apiService.getUrunler()
        } catch (e: Exception) {
            println("Stok Listesi Yükleme Hatası: ${e.message}")
            e.printStackTrace()
        }
        finally { loading = false }
    }

    var aramaMetni by remember { mutableStateOf("") }
    var seciliUrun by remember { mutableStateOf<Urun?>(null) }

    val filtreliUrunler = remember(urunler, aramaMetni) {
        if (aramaMetni.isBlank()) urunler
        else urunler.filter {
            it.ad.contains(aramaMetni, ignoreCase = true) || it.barkod.contains(aramaMetni, ignoreCase = true)
        }
    }

    val kritikStoklar = urunler.filter { it.stokAdedi <= 5L }
    val toplamStokAdedi = urunler.sumOf { it.stokAdedi }

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
                title = { Text("Stok", fontWeight = FontWeight.Bold, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (loading && urunler.isEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // ÜST ÖZET KARTLARI
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StokOzetKart("Toplam Ürün", "${urunler.size}", Color(0xFF5AC8FA), Modifier.weight(1f))
                StokOzetKart("Toplam Adet", "$toplamStokAdedi", Color(0xFF007AFF), Modifier.weight(1f))
                StokOzetKart(
                    "Kritik Stok",
                    "${kritikStoklar.size}",
                    if (kritikStoklar.isNotEmpty()) Color(0xFFFF3B30) else Color(0xFF34C759),
                    Modifier.weight(1f)
                )
            }

            // ARAMA ÇUBUĞU
            OutlinedTextField(
                value = aramaMetni,
                onValueChange = { aramaMetni = it },
                label = { Text("Ürün ara (isim veya barkod)") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Text(
                text = "STOK LİSTESİ",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            if (filtreliUrunler.isEmpty() && !loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ürün bulunamadı", color = Color(0xFF8E8E93), fontSize = 15.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtreliUrunler) { urun ->
                        StokKart(
                            urun = urun,
                            onDuzenle = { seciliUrun = urun }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    seciliUrun?.let { urun ->
        StokDuzenleDialog(
            urun = urun,
            onDismiss = { seciliUrun = null },
            onKaydet = { hareketTuru, miktar, aciklama ->
                scope.launch {
                    try {
                        apiService.createStokHareketi(
                            StokHareketi(
                                urunId = urun.id ?: 0L,
                                urunAdi = urun.ad,
                                hareketTuru = hareketTuru,
                                miktar = miktar.toLong(),
                                birimFiyat = 0.0,
                                aciklama = aciklama.ifBlank { "Manuel düzeltme" },
                                tarih = com.eray.muhasebeapp.getEpochMillis().toString()
                            )
                        )
                        yenilemeTetikleyici++
                    } catch (e: Exception) {
                        println("Stok Düzeltme Hatası: ${e.message}")
                        hataMesaji = "İşlem başarısız. Tekrar deneyin."
                        e.printStackTrace()
                    }
                }
                seciliUrun = null
            }
        )
    }

    if (hataMesaji != null) {
        AlertDialog(
            onDismissRequest = { hataMesaji = null },
            containerColor = Color.White,
            icon = { Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red) },
            title = { Text("Hata Oluştu") },
            text = { Text(hataMesaji ?: "") },
            confirmButton = {
                TextButton(onClick = { hataMesaji = null }) {
                    Text("Tamam", color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun StokKart(urun: Urun, onDuzenle: () -> Unit) {
    val kritikMi = urun.stokAdedi <= 5L
    val renk = if (kritikMi) Color(0xFFFF3B30) else Color(0xFF5AC8FA)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().clickable { onDuzenle() }
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
                        .background(renk.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (kritikMi) Icons.Default.Warning else Icons.Default.Storage,
                        contentDescription = null,
                        tint = renk
                    )
                }
                Column {
                    Text(urun.ad, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text(urun.barkod, fontSize = 12.sp, color = Color(0xFF8E8E93))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${urun.stokAdedi} ${urun.birim}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = renk
                )
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF8E8E93))
            }
        }
    }
}

@Composable
private fun StokDuzenleDialog(
    urun: Urun,
    onDismiss: () -> Unit,
    onKaydet: (hareketTuru: String, miktar: Int, aciklama: String) -> Unit
) {
    var hareketTuru by remember { mutableStateOf("Giriş") }
    var miktarText by remember { mutableStateOf("") }
    var aciklama by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("${urun.ad} - Stok Düzelt", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Mevcut stok: ${urun.stokAdedi} ${urun.birim}",
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HareketTuruButon(
                        baslik = "Giriş",
                        seciliMi = hareketTuru == "Giriş",
                        renk = Color(0xFF34C759),
                        modifier = Modifier.weight(1f)
                    ) { hareketTuru = "Giriş" }

                    HareketTuruButon(
                        baslik = "Çıkış",
                        seciliMi = hareketTuru == "Çıkış",
                        renk = Color(0xFFFF3B30),
                        modifier = Modifier.weight(1f)
                    ) { hareketTuru = "Çıkış" }
                }

                OutlinedTextField(
                    value = miktarText,
                    onValueChange = { miktarText = it },
                    label = { Text("Miktar") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = aciklama,
                    onValueChange = { aciklama = it },
                    label = { Text("Açıklama (fire, sayım, iade vb.)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val temizMiktarText = miktarText.replace(',', '.')
                val miktar = temizMiktarText.toDoubleOrNull()?.toInt() ?: temizMiktarText.toIntOrNull() ?: 0

                if (miktar > 0) {
                    onKaydet(hareketTuru, miktar, aciklama)
                }
            }) { Text("Kaydet", color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}

@Composable
private fun HareketTuruButon(
    baslik: String,
    seciliMi: Boolean,
    renk: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(if (seciliMi) renk else Color(0xFFF2F2F7), RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            baslik,
            color = if (seciliMi) Color.White else Color.Black,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun StokOzetKart(baslik: String, deger: String, renk: Color, modifier: Modifier = Modifier) {
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
